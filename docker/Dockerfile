#FROM eclipse-temurin:21-jre-alpine-3.20
FROM arm64v8/eclipse-temurin:21-jdk-alpine-3.23
WORKDIR /app

# Copy compiled jar
COPY target/nettytest-1.0-SNAPSHOT.jar app.jar

# Expose tracker port
EXPOSE 6969

# Run BitTorrent Tracker Server
ENTRYPOINT ["java", "-cp", "app.jar", "com.zhaoyang.boot.nettytest.bittorrent.server.BitTorrentTrackerServer"]
CMD ["6969"]
