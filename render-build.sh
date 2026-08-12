#!/usr/bin/env bash
# Render Build Script for Minet SACCO Backend

set -e  # Exit on error

echo "========================================="
echo "Minet SACCO Backend - Render Build"
echo "========================================="

# Navigate to backend directory
cd backend

# Verify Java installation
echo "Checking Java version..."
java -version

# Verify JAVA_HOME
echo "JAVA_HOME: $JAVA_HOME"

# Make mvnw executable (backup safety)
chmod +x mvnw

# Clean and build
echo "Building application..."
./mvnw clean package -DskipTests

# Verify JAR was created
echo "Verifying build artifacts..."
ls -lh target/*.jar

echo "========================================="
echo "Build completed successfully!"
echo "========================================="
