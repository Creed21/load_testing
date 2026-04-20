#!/usr/bin/env bash
set -e

case "$1" in
  build)
    mvn package -Dmaven.test.skip=true -q
    ;;
  test)
    mvn verify -q
    ;;
  run)
    mvn spring-boot:run
    ;;
  *)
    echo "Usage: $0 {build|test|run}"
    echo ""
    echo "  build  — compile and package (skip tests)"
    echo "  test   — run test suite"
    echo "  run    — start app locally via Maven"
    exit 1
    ;;
esac
