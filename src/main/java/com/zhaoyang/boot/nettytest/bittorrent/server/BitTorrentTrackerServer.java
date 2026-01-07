package com.zhaoyang.boot.nettytest.bittorrent.server;

import com.zhaoyang.boot.nettytest.bittorrent.handler.TrackerHttpHandler;
import com.zhaoyang.boot.nettytest.bittorrent.model.TorrentManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BT Tracker 服务器
 */
public class BitTorrentTrackerServer {
    private static final Logger logger = LoggerFactory.getLogger(BitTorrentTrackerServer.class);

    private final int port;
    private final TorrentManager torrentManager;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public BitTorrentTrackerServer(int port) {
        this.port = port;
        this.torrentManager = new TorrentManager();
    }

    public void start() throws InterruptedException {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            pipeline.addLast(new HttpServerCodec());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            pipeline.addLast(new TrackerHttpHandler(torrentManager));
                        }
                    })
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();

            logger.info("BitTorrent Tracker Server started on port {}", port);
            logger.info("Announce URL: http://localhost:{}/announce", port);

            serverChannel.closeFuture().sync();
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        logger.info("Shutting down BitTorrent Tracker Server...");

        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }

        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        logger.info("BitTorrent Tracker Server stopped");
    }

    public TorrentManager getTorrentManager() {
        return torrentManager;
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 6969;

        BitTorrentTrackerServer server = new BitTorrentTrackerServer(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered");
            server.shutdown();
        }));

        try {
            server.start();
        } catch (InterruptedException e) {
            logger.error("Server interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}
