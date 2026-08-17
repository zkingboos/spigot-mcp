#!/bin/bash
# Spigot MCP Test Runner

set -e

echo "Building plugin..."
./gradlew build

echo "Starting Spigot server with FAWE..."
docker-compose up -d

echo "Waiting for server to start..."
sleep 10

echo "Server logs:"
docker-compose logs -f spigot