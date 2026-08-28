# ---- Stage 1: build ----
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml trước, tải dependency thành 1 layer riêng — sửa code sau này không phá cache layer này
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]