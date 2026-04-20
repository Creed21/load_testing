#!/usr/bin/env bash
set -e

case "$1" in
  start)
    docker compose up -d postgres
    ;;
  stop)
    docker compose stop postgres
    ;;
  fresh)
    docker compose down -v
    docker compose up -d postgres
    ;;
  app)
    docker compose up --build
    ;;
  *)
    echo "Usage: $0 {start|stop|fresh|app}"
    echo ""
    echo "  start  — start Postgres in background"
    echo "  stop   — stop Postgres"
    echo "  fresh  — wipe volume and restart Postgres"
    echo "  app    — rebuild and start everything"
    exit 1
    ;;
esac
