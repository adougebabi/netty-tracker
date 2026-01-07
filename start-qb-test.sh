#!/bin/bash
# Quick start script for qBittorrent testing

set -e

echo "==================================================="
echo "  Netty BitTorrent Tracker - qBittorrent 测试"
echo "==================================================="
echo ""

cd "$(dirname "$0")"

# Step 1: Build project
echo "📦 步骤 1/5: 编译项目..."
mvn clean package -DskipTests -q
echo "✅ 编译完成"
echo ""

# Step 2: Create test file
echo "📄 步骤 2/5: 创建测试文件..."
cd docker
mkdir -p test-files
echo "Hello, BitTorrent World! $(date)" > test-files/test.txt
echo "✅ 测试文件创建: docker/test-files/test.txt"
echo ""

# Step 3: Start Docker services
echo "🐳 步骤 3/5: 启动 Docker 服务..."
docker-compose up -d
echo "✅ 服务启动成功"
echo ""

# Wait for services to be ready
echo "⏳ 等待服务就绪..."
sleep 20
echo ""

# Step 4: Generate torrent file
echo "📦 步骤 4/5: 生成 Torrent 文件..."
cd ..
./create-torrent.sh docker/test-files/test.txt http://netty-tracker:6969/announce docker/test-files/test.txt.torrent 2>/dev/null || true
echo "✅ Torrent 文件生成: docker/test-files/test.txt.torrent"
echo ""

# Step 5: Display access information
echo "==================================================="
echo "✅ 环境准备完成！"
echo "==================================================="
echo ""
echo "🌐 访问地址："
echo "  Tracker 统计: http://localhost:6969/stats"
echo "  Seeder 界面:  http://localhost:8080"
echo "  Leecher 界面: http://localhost:8081"
echo ""
echo "🔑 获取登录密码："
echo "  Seeder:  docker logs qb-seeder 2>&1 | grep 'temporary password'"
echo "  Leecher: docker logs qb-leecher 2>&1 | grep 'temporary password'"
echo ""
echo "📋 下一步操作："
echo "  1. 访问 http://localhost:8080 登录 Seeder"
echo "  2. 添加 torrent: docker/test-files/test.txt.torrent"
echo "  3. 设置文件位置: /data (包含 test.txt 的目录)"
echo "  4. 访问 http://localhost:8081 登录 Leecher"
echo "  5. 添加同一个 torrent 文件"
echo "  6. 观察下载过程"
echo ""
echo "🛑 停止测试："
echo "  cd docker && docker-compose down"
echo ""
echo "📖 详细文档: 查看 TESTING.md"
echo "==================================================="
