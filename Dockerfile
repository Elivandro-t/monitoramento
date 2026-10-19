# ---- build ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package

# ---- runtime ----
FROM eclipse-temurin:17-jre
# fping: usado pelo monitoramento; iputils-ping: usado pelo terminal da tela
RUN apt-get update \
    && apt-get install -y --no-install-recommends fping iputils-ping tzdata \
    && rm -rf /var/lib/apt/lists/*
ENV TZ=America/Fortaleza
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
