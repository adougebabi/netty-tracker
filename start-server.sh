#!/bin/bash
# Quick start script for Tracker Server

echo "Starting Netty Tracker Server..."
mvn exec:java -Dexec.mainClass="com.zhaoyang.boot.nettytest.server.TrackerServer" -Dexec.args="8888"
