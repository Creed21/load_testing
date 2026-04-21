/**
 * Stress Test — ramp virtual users until the system shows stress.
 *
 * Phases:
 *   warm-up    0 →  50 VUs over 2 min
 *   ramp       50 → 150 VUs over 3 min
 *   peak      150 → 300 VUs over 3 min  ← where things typically break
 *   recover   300 →   0 VUs over 2 min
 *
 * All requests hit GET /movies (no filter) — the worst-case endpoint when the
 * DB has 100k rows and there is no pagination.
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { generateHtmlReport } from './html-report.js';

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

const responseTime = new Trend('stress_response_time', true);
const errorRate = new Rate('stress_error_rate');
const timeouts = new Counter('stress_timeouts');

export const options = {
    stages: [
        { duration: '2m', target: 50 },   // warm-up
        { duration: '3m', target: 150 },  // ramp
        { duration: '3m', target: 300 },  // peak
        { duration: '2m', target: 0 },    // recovery
    ],
    thresholds: {
        // Allow higher error rate under stress — we're finding breaking points
        http_req_failed: ['rate<0.10'],
        stress_error_rate: ['rate<0.10'],
        // p(99) under 30s — just making sure we record everything
        http_req_duration: ['p(99)<30000'],
    },
};

export default function () {
    const res = http.get(`${BASE_URL}/movies`, {
        timeout: '30s',
        tags: { endpoint: 'list_movies_stress' },
    });

    responseTime.add(res.timings.duration);

    const ok = check(res, {
        'status 200': (r) => r.status === 200,
        'responded in time': (r) => r.timings.duration < 15000,
    });

    if (!ok) {
        errorRate.add(1);
        if (res.timings.duration >= 15000 || res.status === 0) {
            timeouts.add(1);
        }
    } else {
        errorRate.add(0);
    }

    sleep(0.5);
}

export function handleSummary(data) {
    return {
        '/k6-results/stress-report.html':  generateHtmlReport(data, 'Stress Test'),
        '/k6-results/stress-summary.json': JSON.stringify(data, null, 2),
    };
}
