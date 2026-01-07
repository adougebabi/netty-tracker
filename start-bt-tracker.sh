#!/bin/bash
# Start BitTorrent Tracker Server

echo "Starting BitTorrent Tracker Server on port 6969..."
mvn exec:java -Dexec.mainClass="com.zhaoyang.boot.nettytest.bittorrent.server.BitTorrentTrackerServer" -Dexec.args="6969"
