# Imagen del backend para Render (que no tiene runtime Java nativo). Contexto de build: este directorio.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw --batch-mode dependency:go-offline
COPY src src
RUN ./mvnw --batch-mode clean package -DskipTests

FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 --create-home app
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
RUN mkdir -p /app/storage/media && chown -R app:app /app
USER app

# Render Free ofrece 512 MB: limita el heap y usa el recolector más ligero.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"
# Los archivos subidos quedan en disco local; en Render Free es efímero (ver docs/despliegue-cicd-dev-prod.md).
ENV APP_MEDIA_STORAGE_DIR=/app/storage/media
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java -Dserver.port=${PORT:-8080} -jar app.jar"]
