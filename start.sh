#!/usr/bin/env bash
set -e

if [ ! -f .env ]; then
    echo "Error: No se encontró .env ni .env.example"
    exit 1
fi

echo "Iniciando servicios en Docker..."
docker compose up -d
sleep 10
echo "Iniciando aplicación Minimarket..."
./mvnw spring-boot:run
