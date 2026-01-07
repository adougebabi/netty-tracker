package com.zhaoyang.boot.nettytest.manager;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 客户端连接管理器
 */
public class ClientManager {
    private static final Logger logger = LoggerFactory.getLogger(ClientManager.class);

    private final ConcurrentHashMap<String, Channel> clients = new ConcurrentHashMap<>();
    private final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private final AtomicInteger connectionCount = new AtomicInteger(0);

    public void register(String clientId, Channel channel) {
        Channel oldChannel = clients.put(clientId, channel);
        if (oldChannel != null && oldChannel.isActive()) {
            logger.warn("Client {} re-registered, closing old channel", clientId);
            oldChannel.close();
        }
        allChannels.add(channel);
        connectionCount.incrementAndGet();
        logger.info("Client {} registered, total clients: {}", clientId, clients.size());
    }

    public void unregister(String clientId) {
        Channel channel = clients.remove(clientId);
        if (channel != null) {
            allChannels.remove(channel);
            logger.info("Client {} unregistered, total clients: {}", clientId, clients.size());
        }
    }

    public void unregister(Channel channel) {
        clients.entrySet().removeIf(entry -> {
            if (entry.getValue().equals(channel)) {
                logger.info("Client {} unregistered by channel, total clients: {}",
                        entry.getKey(), clients.size() - 1);
                return true;
            }
            return false;
        });
        allChannels.remove(channel);
    }

    public Channel getClient(String clientId) {
        return clients.get(clientId);
    }

    public int getClientCount() {
        return clients.size();
    }

    public int getTotalConnections() {
        return connectionCount.get();
    }

    public void broadcast(String message) {
        allChannels.writeAndFlush(message);
        logger.debug("Broadcast message to {} clients", allChannels.size());
    }

    public void closeAll() {
        allChannels.close().awaitUninterruptibly();
        clients.clear();
        logger.info("All connections closed");
    }
}
