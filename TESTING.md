# qBittorrent 测试指南

本指南将帮助你使用 Docker 快速测试 Netty BitTorrent Tracker。

## 🚀 快速开始

### 一键启动（推荐）

```bash
./start-qb-test.sh
```

这个脚本会自动：
1. ✅ 编译项目
2. ✅ 创建测试文件
3. ✅ 启动 Tracker + 2个 qBittorrent
4. ✅ 生成 Torrent 文件
5. ✅ 显示访问地址和密码获取方式

### 访问地址

启动完成后，访问以下地址：

| 服务 | 地址 | 说明 |
|------|------|------|
| **Tracker 统计** | http://localhost:6969/stats | 查看 torrents 和 peers 统计 |
| **Seeder (做种)** | http://localhost:8080 | qBittorrent Web UI |
| **Leecher (下载)** | http://localhost:8081 | qBittorrent Web UI |

### 获取登录密码

```bash
# Seeder 密码
docker logs qb-seeder 2>&1 | grep "temporary password"

# Leecher 密码
docker logs qb-leecher 2>&1 | grep "temporary password"
```

默认用户名：**admin**

## 📝 测试步骤

### 1. 配置 Seeder（做种者）

访问 http://localhost:8080，登录后：

1. **添加 Torrent**
   - 点击左上角 "+" 按钮
   - 选择 `docker/test-files/test.txt.torrent`
   - **重要**：设置保存路径为 `/data`
   - 点击确定

2. **验证状态**
   - 状态应显示：**做种** (Seeding)
   - 完成度：**100%**
   - Tracker 状态：**Working**

### 2. 配置 Leecher（下载者）

访问 http://localhost:8081，登录后：

1. **添加 Torrent**
   - 点击左上角 "+" 按钮
   - 选择同一个 `docker/test-files/test.txt.torrent`
   - 保存路径：使用默认 `/downloads`
   - 点击确定

2. **观察下载**
   - 状态：**下载中** → **做种**
   - 进度：**0%** → **100%**
   - 可以看到从 Seeder 下载数据

### 3. 查看 Tracker 统计

访问 http://localhost:6969/stats

应该看到：
```
Active Torrents: 1
Total Peers: 2
Seeders: 1 → 2 (下载完成后)
Leechers: 1 → 0 (下载完成后)
```

### 4. 验证文件完整性

```bash
# 查看原始文件
cat docker/test-files/test.txt

# 查看下载的文件
docker exec qb-leecher cat /downloads/test.txt

# 比较 hash
shasum -a 256 docker/test-files/test.txt
docker exec qb-leecher sha256sum /downloads/test.txt
```

两个文件的 hash 应该完全一致！

## 🔧 高级操作

### 查看 Tracker 日志

```bash
docker logs -f netty-tracker
```

你应该看到：
```
INFO  TorrentManager - New peer announced: -qB4500-xxx for torrent abc...
INFO  TrackerHttpHandler - Announce from peer xxx: ip=172.18.0.3, port=6881
```

### 查看 Docker 容器状态

```bash
cd docker
docker-compose ps
```

### 重启服务

```bash
cd docker
docker-compose restart
```

### 添加第三个 Leecher

```bash
cd docker

docker run -d \
  --name qb-leecher2 \
  --network docker_bt-network \
  -e WEBUI_PORT=8082 \
  -p 8082:8082 -p 6883:6883 \
  -v $(pwd)/downloads2:/downloads \
  lscr.io/linuxserver/qbittorrent:latest
```

访问 http://localhost:8082

## 🧹 清理环境

### 停止所有服务

```bash
cd docker
docker-compose down
```

### 完全清理（包括数据）

```bash
cd docker
docker-compose down -v
rm -rf config-* downloads test-files
```

## 🐛 故障排查

### 问题 1: Leecher 一直等待，不下载

**检查**：

```bash
# 1. 查看 Tracker 统计
curl http://localhost:6969/stats

# 2. 查看 Seeder 是否真的在做种
# 访问 http://localhost:8080，状态应该是 "做种"

# 3. 查看 Tracker 日志
docker logs netty-tracker | grep "Announce from peer"

# 应该看到 2 个 peer，IP 不同（如 172.18.0.3 和 172.18.0.4）
```

**解决**：
- 确保 Seeder 文件路径正确（`/data`）
- 确认两个 qBittorrent 在同一个 Docker 网络

### 问题 2: 无法访问 Web UI

```bash
# 检查容器是否运行
docker ps | grep qb

# 查看容器日志
docker logs qb-seeder
docker logs qb-leecher

# 重启容器
cd docker
docker-compose restart
```

### 问题 3: Torrent 文件生成失败

```bash
# 手动生成
./create-torrent.sh docker/test-files/test.txt http://netty-tracker:6969/announce docker/test-files/test.txt.torrent

# 检查文件
ls -lh docker/test-files/test.txt.torrent
strings docker/test-files/test.txt.torrent | grep announce
```

### 问题 4: Tracker 无响应

```bash
# 检查 Tracker 容器
docker logs netty-tracker

# 重启 Tracker
cd docker
docker-compose restart tracker

# 测试连接
curl http://localhost:6969/stats
```

## 📊 性能测试

### 测试大文件传输

```bash
# 1. 创建 100MB 测试文件
dd if=/dev/urandom of=docker/test-files/large.dat bs=1M count=100

# 2. 生成 torrent
./create-torrent.sh docker/test-files/large.dat http://netty-tracker:6969/announce docker/test-files/large.dat.torrent

# 3. 在 Seeder 和 Leecher 中添加
# 4. 观察传输速度和 Tracker 性能
```

### 监控 Tracker 性能

```bash
# 实时查看统计
watch -n 1 'curl -s http://localhost:6969/stats | grep -E "Torrents|Peers|Seeders|Leechers"'

# 查看日志
docker logs -f netty-tracker | grep "Announce from peer"
```

## 📖 项目结构

```
nettytest/
├── docker/
│   ├── docker-compose.yml      # Docker Compose 配置
│   ├── Dockerfile              # Tracker 镜像
│   ├── test-files/             # 测试文件目录
│   │   ├── test.txt           # 测试文件
│   │   └── test.txt.torrent   # Torrent 文件
│   ├── config-seeder/          # Seeder 配置（自动生成）
│   ├── config-leecher/         # Leecher 配置（自动生成）
│   └── downloads/              # Leecher 下载目录（自动生成）
├── src/                        # 源代码
├── start-qb-test.sh           # 一键启动脚本
├── create-torrent.sh          # Torrent 生成脚本
└── TESTING.md                 # 本文档
```

## 🎯 测试检查清单

完成测试后，确认以下项目：

- [ ] Tracker 启动成功（访问 /stats 正常）
- [ ] Seeder 状态为 "做种"，完成度 100%
- [ ] Leecher 成功下载并变为 "做种"
- [ ] Tracker 显示 2 个 Seeders
- [ ] 文件 hash 验证一致
- [ ] Tracker 日志显示两个不同的 peer IP（172.18.0.x）
- [ ] 下载完成后 Peers 列表为空（正常现象）

全部通过表示 BitTorrent Tracker 工作正常！🎉

## 💡 提示

1. **下载完成后 Peers 列表为空是正常的**
   - 两个 Seeders 之间暂时不需要连接
   - 当有新的 Leecher 时会重新建立连接

2. **首次启动需要等待**
   - qBittorrent 容器首次启动需要 ~30 秒
   - 使用 `docker logs -f qb-seeder` 查看启动进度

3. **修改代码后**
   ```bash
   # 重新编译并重启
   mvn clean package -DskipTests
   cd docker
   docker-compose up -d --build tracker
   ```

4. **查看所有日志**
   ```bash
   cd docker
   docker-compose logs -f
   ```
