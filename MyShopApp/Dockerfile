# 后端镜像：多阶段构建，运行阶段只保留 JRE 与 jar
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
# 走国内镜像下载依赖，避免部分网络环境下访问 Maven Central 卡死
COPY docker/maven-settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
# 以非 root 用户运行，降低容器被攻破后的影响
RUN groupadd -r myshop && useradd -r -g myshop myshop \
    && mkdir -p /app/uploads /app/logs && chown -R myshop:myshop /app
COPY --from=build /build/target/*.jar /app/app.jar
USER myshop
EXPOSE 8080
ENV JAVA_OPTS="-Xms256m -Xmx512m" \
    SPRING_PROFILES_ACTIVE=prod \
    UPLOAD_DIR=/app/uploads \
    LOG_FILE=/app/logs/myshop.log \
    TZ=Asia/Shanghai
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
