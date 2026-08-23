# Stage 1: Build the Java application
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy dependency libraries and source files
COPY lib/ ./lib/
COPY src/ ./src/
COPY database/schema.sql ./database/schema.sql

# Find all Java source files and compile them recursively
RUN mkdir bin && \
    find src -name "*.java" > sources.txt && \
    javac -encoding UTF-8 -d bin -cp "bin:lib/*" @sources.txt

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy compiled classes and libraries
COPY --from=builder /app/bin ./bin
COPY --from=builder /app/lib ./lib
COPY --from=builder /app/database/schema.sql ./database/schema.sql

# Copy knowledge base files
COPY knowledge-base/ ./knowledge-base/

# Expose HTTP port 8080
EXPOSE 8080

# Run the Java Main class using Linux classpath separators (colon)
CMD ["java", "-cp", "bin:lib/*", "Main"]
