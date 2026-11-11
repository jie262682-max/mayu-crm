# 码域CRM 后端镜像（多阶段：构建 + 运行）
# 构建：docker build -t maoyu-crm-server .
# AGPL-3.0 © 2026 合肥市码域科技
FROM docker.1ms.run/library/maven:3.8-jdk-8 AS builder
WORKDIR /build
COPY pom.xml .
COPY freecrm-common/pom.xml freecrm-common/
COPY freecrm-system/pom.xml freecrm-system/
COPY freecrm-crm/pom.xml freecrm-crm/
COPY freecrm-oa/pom.xml freecrm-oa/
COPY freecrm-eam/pom.xml freecrm-eam/
COPY freecrm-server/pom.xml freecrm-server/
RUN mvn -q dependency:go-offline -DskipTests || true
COPY freecrm-common freecrm-common
COPY freecrm-system freecrm-system
COPY freecrm-crm freecrm-crm
COPY freecrm-oa freecrm-oa
COPY freecrm-eam freecrm-eam
COPY freecrm-server freecrm-server
RUN mvn -q clean package -DskipTests

FROM docker.1ms.run/library/eclipse-temurin:8-jre
WORKDIR /app
COPY --from=builder /build/freecrm-server/target/freecrm-server-0.1.0-SNAPSHOT.jar app.jar
# 外部配置与上传目录（compose 挂载覆盖）
ENV TZ=Asia/Shanghai JAVA_OPTS="-Xms256m -Xmx512m"
EXPOSE 48100
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
