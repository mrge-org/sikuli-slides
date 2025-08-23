#!/usr/bin/env bash
set -euo pipefail

# Build Sikuli Slides API and Apps with tests fully skipped (compile+run)
# Usage: ./build-all.sh

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOCAL_REPO="$(mvn -q -DforceStdout -Dexpression=settings.localRepository help:evaluate 2>/dev/null || true)"
if [ -z "$LOCAL_REPO" ]; then
  LOCAL_REPO="(local Maven repository)"
fi

## Guard: disallow raw System.out/err usage in main Java sources (use Log4j instead)
# - Scan only main sources (exclude tests)
# - Ignore occurrences on commented lines (//, /*, *)
RAW_VIOLATIONS=$(grep -R -nE 'System\.(out|err)\.print' \
  "$ROOT_DIR/src/main/java" "$ROOT_DIR/apps/src/main/java" \
  --include='*.java' 2>/dev/null \
  | grep -v 'TeeOutputStream' \
  | grep -v 'apps/src/main/java/com/sampullara/cli/' || true)
# Filter out commented lines by removing leading file:line: prefix before applying comment filters
FILTERED=$(printf "%s\n" "$RAW_VIOLATIONS" \
  | sed -E 's|^[^:]+:[0-9]+:||' \
  | grep -v -E '^[[:space:]]*//' \
  | grep -v -E '^[[:space:]]*/\*' \
  | grep -v -E '^[[:space:]]*\*' || true)
if [ -n "$FILTERED" ]; then
  echo "Build guard failed: Found System.out/err usage in Java sources (use Log4j)."
  echo "$RAW_VIOLATIONS"
  exit 3
fi

echo "[1/2] Building API at $ROOT_DIR (compile tests, skip running)"
(mkdir -p "$ROOT_DIR/target" >/dev/null 2>&1 || true)
(cd "$ROOT_DIR" && mvn -DskipTests=true clean package install)

echo "[2/2] Building apps module at $ROOT_DIR/apps (compile tests, skip running)"
(cd "$ROOT_DIR/apps" && mvn -DskipTests=true clean package)

echo "Done. Artifacts:"
echo " - API: $ROOT_DIR/target/*.jar (installed to: $LOCAL_REPO)"
echo " - Apps shaded jar: $ROOT_DIR/apps/target/sikuli-slides-*-SNAPSHOT.jar"
