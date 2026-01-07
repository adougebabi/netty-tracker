#!/bin/bash
# Create a test torrent file

if [ $# -lt 1 ]; then
    echo "Usage: ./create-torrent.sh <file> [announce_url] [output.torrent]"
    echo "Example: ./create-torrent.sh test.txt"
    exit 1
fi

FILE=$1
ANNOUNCE=${2:-"http://localhost:6969/announce"}
OUTPUT=${3:-"$FILE.torrent"}

echo "Creating torrent file..."
echo "  File: $FILE"
echo "  Tracker: $ANNOUNCE"
echo "  Output: $OUTPUT"
echo ""

mvn exec:java -Dexec.mainClass="com.zhaoyang.boot.nettytest.bittorrent.util.TorrentCreator" \
  -Dexec.args="$FILE $ANNOUNCE $OUTPUT" -q

echo ""
echo "Torrent file created successfully!"
echo "You can now add $OUTPUT to qBittorrent"
