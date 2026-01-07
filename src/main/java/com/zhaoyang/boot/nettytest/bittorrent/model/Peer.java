package com.zhaoyang.boot.nettytest.bittorrent.model;

import java.util.Objects;

/**
 * Peer 节点
 */
public class Peer {
    private final String peerId;
    private final String ip;
    private final int port;
    private long uploaded;
    private long downloaded;
    private long left;
    private long lastSeen;
    private boolean seeder;

    public Peer(String peerId, String ip, int port) {
        this.peerId = peerId;
        this.ip = ip;
        this.port = port;
        this.lastSeen = System.currentTimeMillis();
    }

    public void updateStats(long uploaded, long downloaded, long left) {
        this.uploaded = uploaded;
        this.downloaded = downloaded;
        this.left = left;
        this.seeder = (left == 0);
        this.lastSeen = System.currentTimeMillis();
    }

    public String getPeerId() {
        return peerId;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public long getUploaded() {
        return uploaded;
    }

    public long getDownloaded() {
        return downloaded;
    }

    public long getLeft() {
        return left;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public boolean isSeeder() {
        return seeder;
    }

    public void touch() {
        this.lastSeen = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return Objects.equals(peerId, peer.peerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(peerId);
    }

    @Override
    public String toString() {
        return String.format("Peer{id='%s', ip='%s', port=%d, seeder=%s}",
                peerId, ip, port, seeder);
    }
}
