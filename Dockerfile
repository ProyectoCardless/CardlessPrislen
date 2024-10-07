FROM openjdk:17-jdk-slim AS build

# Instalar Maven
RUN apt-get update && apt-get install -y maven

# Definir el directorio de trabajo
WORKDIR /app

# Copiar el archivo pom.xml y descargar las dependencias
COPY pom.xml .
COPY src ./src
RUN mvn dependency:go-offline

# Construir el proyecto
RUN mvn package -DskipTests

# Etapa de ejecución
FROM openjdk:17-jdk-slim

# Copiar el archivo JAR desde la etapa de construcción
COPY --from=build /app/target/CajerosCardless-0.0.1-SNAPSHOT.jar app.jar

# Configurar el puerto que la aplicación va a usar
ENV PORT 8080
EXPOSE $PORT

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "/app.jar"]
