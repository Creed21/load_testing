#!/usr/bin/env bash
# Run k6 smoke → load → stress tests against a running application.
# Usage:  bash scripts/load-test.sh [TARGET_URL]
# Default TARGET_URL: http://localhost:8080
set -e

TARGET_URL="${1:-http://localhost:8080}"
RESULTS_DIR="${RESULTS_DIR:-k6-results}"

mkdir -p "$RESULTS_DIR"

echo "======================================================"
echo "  k6 Load Testing Suite"
echo "  Target : $TARGET_URL"
echo "  Results: $RESULTS_DIR/"
echo "======================================================"

# ── Wait for app to be ready ──────────────────────────────────────────────────
echo ""
echo "Waiting for application at $TARGET_URL/movies ..."
for i in $(seq 1 30); do
    if curl -sf --max-time 5 "$TARGET_URL/movies" > /dev/null 2>&1; then
        echo "Application is ready (attempt $i)."
        break
    fi
    if [ "$i" -eq 30 ]; then
        echo "ERROR: application did not respond after 30 attempts. Aborting."
        exit 1
    fi
    echo "  attempt $i/30 — waiting 10 s..."
    sleep 10
done

# ── Smoke test ────────────────────────────────────────────────────────────────
echo ""
echo ">>> [1/3] Running SMOKE test..."
k6 run \
    --out json="$RESULTS_DIR/smoke-results.json" \
    --summary-export="$RESULTS_DIR/smoke-summary.json" \
    -e TARGET_URL="$TARGET_URL" \
    k6/smoke-test.js
echo "Smoke test complete."

# ── Load test ─────────────────────────────────────────────────────────────────
echo ""
echo ">>> [2/3] Running LOAD test..."
k6 run \
    --out json="$RESULTS_DIR/load-results.json" \
    --summary-export="$RESULTS_DIR/load-summary.json" \
    -e TARGET_URL="$TARGET_URL" \
    k6/load-test.js
echo "Load test complete."

# ── Stress test ───────────────────────────────────────────────────────────────
echo ""
echo ">>> [3/3] Running STRESS test..."
k6 run \
    --out json="$RESULTS_DIR/stress-results.json" \
    --summary-export="$RESULTS_DIR/stress-summary.json" \
    -e TARGET_URL="$TARGET_URL" \
    k6/stress-test.js
echo "Stress test complete."

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "======================================================"
echo "  All tests finished. Results written to $RESULTS_DIR/"
echo "======================================================"
ls -lh "$RESULTS_DIR/"
