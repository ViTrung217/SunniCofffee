FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --chown=10001:10001 target/sunicoffee-1.0.0.jar /app/sunicoffee.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/sunicoffee.jar"]
