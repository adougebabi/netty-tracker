package com.zhaoyang.boot.nettytest;

import com.zhaoyang.boot.nettytest.protocol.TrackerMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能测试工具
 */
public class TrackerBenchmark {
    private static final Logger logger = LoggerFactory.getLogger(TrackerBenchmark.class);

    private final String host;
    private final int port;
    private final int numClients;
    private final int messagesPerClient;
    private final int duration;

    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicLong totalLatency = new AtomicLong(0);
    private final ConcurrentLinkedQueue<Long> latencies = new ConcurrentLinkedQueue<>();

    public TrackerBenchmark(String host, int port, int numClients, int messagesPerClient, int duration) {
        this.host = host;
        this.port = port;
        this.numClients = numClients;
        this.messagesPerClient = messagesPerClient;
        this.duration = duration;
    }

    public void run() throws InterruptedException {
        logger.info("Starting benchmark: {} clients, {} messages/client, {} seconds duration",
                numClients, messagesPerClient, duration);

        long startTime = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(numClients);
        CountDownLatch latch = new CountDownLatch(numClients);

        for (int i = 0; i < numClients; i++) {
            final int clientIndex = i;
            executor.submit(() -> {
                try {
                    runClient("CLIENT-" + clientIndex);
                } catch (Exception e) {
                    logger.error("Client {} failed", clientIndex, e);
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(duration, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();

        executor.shutdownNow();

        printResults(startTime, endTime, completed);
    }

    private void runClient(String clientId) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup(1);

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new LineBasedFrameDecoder(1024));
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new BenchmarkClientHandler());
                        }
                    });

            Channel channel = bootstrap.connect(host, port).sync().channel();

            TrackerMessage registerMsg = new TrackerMessage(
                    TrackerMessage.MessageType.REGISTER,
                    clientId,
                    "Benchmark client"
            );
            channel.writeAndFlush(registerMsg.encode()).sync();

            for (int i = 0; i < messagesPerClient; i++) {
                long sendTime = System.nanoTime();

                TrackerMessage message = new TrackerMessage(
                        TrackerMessage.MessageType.MESSAGE,
                        clientId,
                        "Benchmark message " + i
                );

                ChannelFuture future = channel.writeAndFlush(message.encode());
                future.addListener((ChannelFutureListener) f -> {
                    if (f.isSuccess()) {
                        long latency = System.nanoTime() - sendTime;
                        latencies.add(latency);
                        totalLatency.addAndGet(latency);
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                });

                if (i % 100 == 0) {
                    Thread.sleep(10);
                }
            }

            channel.closeFuture().await(5, TimeUnit.SECONDS);
            channel.close().sync();

        } finally {
            group.shutdownGracefully();
        }
    }

    private void printResults(long startTime, long endTime, boolean completed) {
        long duration = endTime - startTime;
        int totalMessages = successCount.get() + failureCount.get();
        double throughput = (double) totalMessages / (duration / 1000.0);

        System.out.println("\n========== Benchmark Results ==========");
        System.out.println("Completed: " + (completed ? "Yes" : "No (Timeout)"));
        System.out.println("Duration: " + duration + " ms");
        System.out.println("Total Clients: " + numClients);
        System.out.println("Messages per Client: " + messagesPerClient);
        System.out.println();

        System.out.println("Total Messages: " + totalMessages);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Failed: " + failureCount.get());
        System.out.println("Success Rate: " + String.format("%.2f%%",
                (double) successCount.get() / totalMessages * 100));
        System.out.println();

        System.out.println("Throughput: " + String.format("%.2f", throughput) + " msg/s");

        if (!latencies.isEmpty()) {
            long avgLatency = totalLatency.get() / successCount.get();
            long[] sortedLatencies = latencies.stream()
                    .mapToLong(Long::longValue)
                    .sorted()
                    .toArray();

            System.out.println();
            System.out.println("Latency Statistics (microseconds):");
            System.out.println("  Average: " + avgLatency / 1000);
            System.out.println("  Min: " + sortedLatencies[0] / 1000);
            System.out.println("  Max: " + sortedLatencies[sortedLatencies.length - 1] / 1000);
            System.out.println("  P50: " + sortedLatencies[sortedLatencies.length / 2] / 1000);
            System.out.println("  P95: " + sortedLatencies[(int) (sortedLatencies.length * 0.95)] / 1000);
            System.out.println("  P99: " + sortedLatencies[(int) (sortedLatencies.length * 0.99)] / 1000);
        }

        System.out.println("========================================\n");
    }

    private static class BenchmarkClientHandler extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.debug("Exception in benchmark client", cause);
            ctx.close();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8888;
        int numClients = args.length > 2 ? Integer.parseInt(args[2]) : 100;
        int messagesPerClient = args.length > 3 ? Integer.parseInt(args[3]) : 100;
        int duration = args.length > 4 ? Integer.parseInt(args[4]) : 60;

        System.out.println("Tracker Server Benchmark Tool");
        System.out.println("Usage: java TrackerBenchmark <host> <port> <numClients> <messagesPerClient> <durationSeconds>");
        System.out.println();

        TrackerBenchmark benchmark = new TrackerBenchmark(host, port, numClients, messagesPerClient, duration);
        benchmark.run();
    }
}
