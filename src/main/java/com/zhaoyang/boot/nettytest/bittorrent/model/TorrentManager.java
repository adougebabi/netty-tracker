package com.zhaoyang.boot.nettytest.bittorrent.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Torrent 管理器
 */
public class TorrentManager {
    private static final Logger logger = LoggerFactory.getLogger(TorrentManager.class);
    private static final long PEER_TIMEOUT = 180_000;

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Peer>> torrents = new ConcurrentHashMap<>();

    public void announcePeer(String infoHash, String peerId, String ip, int port,
                             long uploaded, long downloaded, long left) {
        torrents.computeIfAbsent(infoHash, k -> new ConcurrentHashMap<>())
                .compute(peerId, (k, existingPeer) -> {
                    if (existingPeer == null) {
                        logger.info("New peer announced: {} for torrent {}", peerId, infoHash);
                        Peer peer = new Peer(peerId, ip, port);
                        peer.updateStats(uploaded, downloaded, left);
                        return peer;
                    } else {
                        existingPeer.updateStats(uploaded, downloaded, left);
                        return existingPeer;
                    }
                });
    }

    public void removePeer(String infoHash, String peerId) {
        ConcurrentHashMap<String, Peer> peers = torrents.get(infoHash);
        if (peers != null) {
            peers.remove(peerId);
            logger.info("Peer removed: {} from torrent {}", peerId, infoHash);

            if (peers.isEmpty()) {
                torrents.remove(infoHash);
                logger.info("Torrent removed (no peers): {}", infoHash);
            }
        }
    }

    public List<Peer> getPeers(String infoHash, int numWant) {
        ConcurrentHashMap<String, Peer> peers = torrents.get(infoHash);
        if (peers == null || peers.isEmpty()) {
            return Collections.emptyList();
        }

        cleanupStalePeers(infoHash, peers);

        List<Peer> peerList = new ArrayList<>(peers.values());

        if (numWant > 0 && peerList.size() > numWant) {
            Collections.shuffle(peerList);
            return peerList.subList(0, numWant);
        }

        return peerList;
    }

    public int getCompleteCount(String infoHash) {
        ConcurrentHashMap<String, Peer> peers = torrents.get(infoHash);
        if (peers == null) return 0;

        return (int) peers.values().stream()
                .filter(Peer::isSeeder)
                .count();
    }

    public int getIncompleteCount(String infoHash) {
        ConcurrentHashMap<String, Peer> peers = torrents.get(infoHash);
        if (peers == null) return 0;

        return (int) peers.values().stream()
                .filter(p -> !p.isSeeder())
                .count();
    }

    private void cleanupStalePeers(String infoHash, ConcurrentHashMap<String, Peer> peers) {
        long now = System.currentTimeMillis();
        peers.entrySet().removeIf(entry -> {
            boolean stale = (now - entry.getValue().getLastSeen()) > PEER_TIMEOUT;
            if (stale) {
                logger.debug("Removing stale peer: {}", entry.getKey());
            }
            return stale;
        });
    }

    public Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("torrents", torrents.size());

        int totalPeers = 0;
        int totalSeeders = 0;

        for (ConcurrentHashMap<String, Peer> peers : torrents.values()) {
            totalPeers += peers.size();
            totalSeeders += peers.values().stream().filter(Peer::isSeeder).count();
        }

        stats.put("peers", totalPeers);
        stats.put("seeders", totalSeeders);
        stats.put("leechers", totalPeers - totalSeeders);

        return stats;
    }
}
