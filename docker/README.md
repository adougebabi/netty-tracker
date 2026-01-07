# Docker 测试环境

这个目录包含完整的 Docker Compose 测试栈，用于快速测试 BitTorrent Tracker。

## 📁 目录结构

```
docker/
├── docker-compose.yml      # Docker Compose 配置
├── Dockerfile              # Tracker 镜像定义
├── test-files/             # 测试文件目录
│   ├── test.txt           # 测试文件（自动生成）
│   └── test.txt.torrent   # Torrent 文件（自动生成）
├── config-seeder/          # Seeder 配置（自动生成）
├── config-leecher/         # Leecher 配置（自动生成）
└── downloads/              # Leecher 下载目录（自动生成）
```

## 🚀 使用方式

### 快速启动（推荐）

从项目根目录运行：

```bash
./start-qb-test.sh
```

### 手动启动

```bash
# 1. 编译项目
mvn clean package -DskipTests

# 2. 进入 docker 目录
cd docker

# 3. 创建测试文件
mkdir -p test-files
echo "Hello, BT!" > test-files/test.txt

# 4. 生成 torrent
cd ..
./create-torrent.sh docker/test-files/test.txt http://netty-tracker:6969/announce docker/test-files/test.txt.torrent

# 5. 启动服务
cd docker
docker-compose up -d

# 6. 查看日志
docker-compose logs -f
```

## 🌐 服务访问

| 服务 | 地址 | 用户名 | 密码获取 |
|------|------|--------|----------|
| Tracker | http://localhost:6969/stats | - | - |
| Seeder | http://localhost:8080 | admin | `docker logs qb-seeder 2>&1 \| grep password` |
| Leecher | http://localhost:8081 | admin | `docker logs qb-leecher 2>&1 \| grep password` |

## 🔧 常用命令

```bash
# 查看运行状态
docker-compose ps

# 查看日志
docker-compose logs -f
docker-compose logs tracker    # 只看 Tracker
docker-compose logs qb-seeder  # 只看 Seeder

# 重启服务
docker-compose restart

# 停止服务
docker-compose down

# 完全清理（包括数据）
docker-compose down -v
rm -rf config-* downloads test-files
```

## 📝 注意事项

1. **首次启动需等待**: qBittorrent 容器首次启动约需 30 秒
2. **文件路径**: Seeder 必须设置文件位置为 `/data`
3. **网络**: 所有容器在 `bt-network` 中，可互相访问
4. **持久化**: 配置和下载文件会保存在本地目录

## 🐛 故障排查

参见 [TESTING.md](../TESTING.md) 的故障排查章节。
