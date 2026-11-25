# Use an official Java runtime as a parent image
FROM eclipse-temurin:17-jdk-jammy

# Set the working directory inside the container
WORKDIR /app

# Copy the .jar file into the container at /app
COPY target/welcome-page-0.0.1-SNAPSHOT.jar /app/welcome-page.jar

# Make the container's port 8080 available to the outside world
EXPOSE 8181

# Run the jar file when the container starts
ENTRYPOINT ["java", "-jar", "welcome-page.jar"]
