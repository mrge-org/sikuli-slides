#!/usr/bin/env bash
set -euo pipefail

# Build Sikuli Slides API and Apps with tests fully skipped (compile+run)
# Usage: ./build-all.sh

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "[1/2] Building API at $ROOT_DIR (skip tests compile+run)"
(mkdir -p "$ROOT_DIR/target" >/dev/null 2>&1 || true)
(cd "$ROOT_DIR" && mvn -Dmaven.test.skip=true clean package install)

echo "[2/2] Building apps module at $ROOT_DIR/apps (skip tests compile+run)"
(cd "$ROOT_DIR/apps" && mvn -Dmaven.test.skip=true clean package)

echo "Done. Artifacts:"
echo " - API: $ROOT_DIR/target/*.jar (installed to local ~/.m2)"
echo " - Apps shaded jar: $ROOT_DIR/apps/target/sikuli-slides-*-SNAPSHOT.jar"
