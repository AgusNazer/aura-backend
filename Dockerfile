# ==========================================
# Etapa 1: Build (Maven + JDK 21)
# ==========================================
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copiar configuración de dependencias
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Dar permisos de ejecución al wrapper
RUN chmod +x ./mvnw

# Copiar el código fuente y compilar el JAR final sin correr tests
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# ==========================================
# Etapa 2: Runtime (JRE 21 Liviano)
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear usuario sin privilegios por seguridad
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar el JAR generado desde la etapa de compilación
COPY --from=builder /app/target/*.jar app.jar

# Configuración básica de memoria JVM
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]