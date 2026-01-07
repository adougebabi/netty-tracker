#!/bin/bash
# Benchmark script for Tracker Server

echo "======================================"
echo "Netty Tracker Server Benchmark Suite"
echo "======================================"
echo ""

# Ensure server is running
if ! nc -z localhost 8888 2>/dev/null; then
    echo "ERROR: Server is not running on port 8888"
    echo "Please start the server first: ./start-server.sh"
    exit 1
fi

echo "Server detected on port 8888"
echo ""

# Test 1: Light load
echo "[Test 1/3] Light Load: 100 clients × 100 messages"
mvn exec:java -Dexec.mainClass="com.zhaoyang.boot.nettytest.TrackerBenchmark" \
  -Dexec.args="localhost 8888 100 100 30" -q

echo ""
echo "Waiting 5 seconds before next test..."
sleep 5
echo ""

# Test 2: Medium load
echo "[Test 2/3] Medium Load: 300 clients × 200 messages"
mvn exec:java -Dexec.mainClass="com.zhaoyang.boot.nettytest.TrackerBenchmark" \
  -Dexec.args="localhost 8888 300 200 60" -q

echo ""
echo "Waiting 5 seconds before next test..."
sleep 5
echo ""

# Test 3: Heavy load
echo "[Test 3/3] Heavy Load: 500 clients × 300 messages"
mvn exec:java -Dexec.mainClass="com.zhaoyang.boot.nettytest.TrackerBenchmark" \
  -Dexec.args="localhost 8888 500 300 90" -q

echo ""
echo "======================================"
echo "Benchmark suite completed!"
echo "======================================"
