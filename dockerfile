FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY . .
RUN ./mvnw test
CMD ["java", "-jar", "target/Kittop-0.0.1-SNAPSHOT.jar"]