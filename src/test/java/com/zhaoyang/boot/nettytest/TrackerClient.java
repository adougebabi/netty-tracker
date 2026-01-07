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

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * 测试客户端
 */
public class TrackerClient {
    private static final Logger logger = LoggerFactory.getLogger(TrackerClient.class);

    private final String host;
    private final int port;
    private final String clientId;
    private EventLoopGroup group;
    private Channel channel;

    public TrackerClient(String host, int port, String clientId) {
        this.host = host;
        this.port = port;
        this.clientId = clientId;
    }

    public void connect() throws InterruptedException {
        group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            pipeline.addLast(new LineBasedFrameDecoder(1024));
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new ClientHandler());
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();

            logger.info("Connected to {}:{}", host, port);

            register();

        } catch (InterruptedException e) {
            logger.error("Connection failed", e);
            disconnect();
            throw e;
        }
    }

    public void register() {
        TrackerMessage registerMsg = new TrackerMessage(
                TrackerMessage.MessageType.REGISTER,
                clientId,
                "Client registration"
        );
        send(registerMsg);
    }

    public void sendHeartbeat() {
        TrackerMessage heartbeat = new TrackerMessage(
                TrackerMessage.MessageType.HEARTBEAT,
                clientId,
                ""
        );
        send(heartbeat);
    }

    public void sendMessage(String payload) {
        TrackerMessage message = new TrackerMessage(
                TrackerMessage.MessageType.MESSAGE,
                clientId,
                payload
        );
        send(message);
    }

    private void send(TrackerMessage message) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(message.encode());
            logger.debug("Sent: {}", message);
        } else {
            logger.warn("Channel is not active, cannot send message");
        }
    }

    public void disconnect() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        logger.info("Disconnected");
    }

    private static class ClientHandler extends SimpleChannelInboundHandler<String> {
        private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            logger.info("Received: {}", msg);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Exception", cause);
            ctx.close();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8888;
        String clientId = args.length > 2 ? args[2] : "CLIENT-" + System.currentTimeMillis();

        TrackerClient client = new TrackerClient(host, port, clientId);

        Runtime.getRuntime().addShutdownHook(new Thread(client::disconnect));

        try {
            client.connect();

            Thread heartbeatThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        TimeUnit.SECONDS.sleep(10);
                        client.sendHeartbeat();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            heartbeatThread.setDaemon(true);
            heartbeatThread.start();

            Scanner scanner = new Scanner(System.in);
            System.out.println("Type messages to send (or 'quit' to exit):");

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if ("quit".equalsIgnoreCase(line.trim())) {
                    break;
                }
                client.sendMessage(line);
            }

        } finally {
            client.disconnect();
        }
    }
}
