/**
 * Smoke Test — verify the app is alive before running heavier tests.
 * 1 VU, 30 s, strict thresholds.
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { generateHtmlReport } from './html-report.js';

const errorRate = new Rate('errors');
const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

export const options = {
    vus: 1,
    duration: '30s',
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500'],
        errors: ['rate<0.01'],
    },
};

export function setup() {
    const res = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({ username: 'admin', password: 'admin123' }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    check(res, { 'login OK': (r) => r.status === 200 });
    return { token: res.json('token') };
}

export default function (data) {
    // GET /movies (no filter)
    let r = http.get(`${BASE_URL}/movies`);
    const moviesOk = check(r, { 'GET /movies 200': (r) => r.status === 200 });
    errorRate.add(!moviesOk);

    // GET /movies/{id}
    r = http.get(`${BASE_URL}/movies/1`);
    const movieOk = check(r, { 'GET /movies/1 200': (r) => r.status === 200 });
    errorRate.add(!movieOk);

    sleep(1);
}

export function handleSummary(data) {
    return {
        '/k6-results/smoke-report.html':  generateHtmlReport(data, 'Smoke Test'),
        '/k6-results/smoke-summary.json': JSON.stringify(data, null, 2),
    };
}
