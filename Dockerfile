FROM eclipse-temurin:17
WORKDIR /app
COPY demo/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
