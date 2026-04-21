/**
 * Load Test — realistic multi-scenario steady load.
 *
 * Scenarios run concurrently:
 *   list_movies   — GET /movies (no filter)  — 30 VUs, 5 min  ← expected bottleneck
 *   search_movies — GET /movies?genre=X      — 20 VUs, 5 min
 *   get_movie     — GET /movies/{id}         — 30 VUs, 5 min
 *   login         — POST /auth/login         — 10 VUs, 5 min
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';
import { generateHtmlReport } from './html-report.js';

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8080';

const listMoviesDuration = new Trend('list_movies_duration', true);
const searchMoviesDuration = new Trend('search_movies_duration', true);
const getMovieDuration = new Trend('get_movie_duration', true);
const loginDuration = new Trend('login_duration', true);
const errorCount = new Counter('error_count');

const GENRES = ['Action', 'Comedy', 'Drama', 'Sci-Fi', 'Horror', 'Thriller', 'Romance', 'Crime', 'Animation', 'Documentary'];

export const options = {
    scenarios: {
        list_movies: {
            executor: 'constant-vus',
            vus: 30,
            duration: '5m',
            exec: 'listMovies',
            tags: { scenario: 'list_movies' },
        },
        search_movies: {
            executor: 'constant-vus',
            vus: 20,
            duration: '5m',
            exec: 'searchMovies',
            tags: { scenario: 'search_movies' },
        },
        get_movie: {
            executor: 'constant-vus',
            vus: 30,
            duration: '5m',
            exec: 'getMovie',
            tags: { scenario: 'get_movie' },
        },
        login: {
            executor: 'constant-vus',
            vus: 10,
            duration: '5m',
            exec: 'loginScenario',
            tags: { scenario: 'login' },
        },
    },
    thresholds: {
        // Overall
        http_req_failed: ['rate<0.01'],

        // Per scenario — GET /movies (full list) is intentionally lenient because
        // fetching 100k rows is the bottleneck we're measuring
        'http_req_duration{scenario:list_movies}': ['p(95)<10000', 'p(99)<20000'],
        'http_req_duration{scenario:search_movies}': ['p(95)<2000'],
        'http_req_duration{scenario:get_movie}': ['p(95)<300'],
        'http_req_duration{scenario:login}': ['p(95)<500'],
    },
};

// ── Scenario functions ────────────────────────────────────────────────────────

export function listMovies() {
    const res = http.get(`${BASE_URL}/movies`, { tags: { endpoint: 'list_movies' } });
    listMoviesDuration.add(res.timings.duration);
    if (!check(res, { 'list movies 200': (r) => r.status === 200 })) {
        errorCount.add(1);
    }
    sleep(1);
}

export function searchMovies() {
    const genre = GENRES[Math.floor(Math.random() * GENRES.length)];
    const res = http.get(
        `${BASE_URL}/movies?genre=${genre}`,
        { tags: { endpoint: 'search_movies' } }
    );
    searchMoviesDuration.add(res.timings.duration);
    if (!check(res, { 'search movies 200': (r) => r.status === 200 })) {
        errorCount.add(1);
    }
    sleep(0.5);
}

export function getMovie() {
    // Random ID from the bulk seed range
    const id = Math.floor(Math.random() * 100000) + 7; // +7 to skip the 6 named movies
    const res = http.get(`${BASE_URL}/movies/${id}`, { tags: { endpoint: 'get_movie' } });
    getMovieDuration.add(res.timings.duration);
    // 404 is acceptable (not all IDs guaranteed to exist)
    if (!check(res, { 'get movie 2xx or 404': (r) => r.status === 200 || r.status === 404 })) {
        errorCount.add(1);
    }
    sleep(0.5);
}

export function loginScenario() {
    const res = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({ username: 'user', password: 'user123' }),
        {
            headers: { 'Content-Type': 'application/json' },
            tags: { endpoint: 'login' },
        }
    );
    loginDuration.add(res.timings.duration);
    if (!check(res, { 'login 200': (r) => r.status === 200 })) {
        errorCount.add(1);
    }
    sleep(2);
}

export function handleSummary(data) {
    return {
        '/k6-results/load-report.html':  generateHtmlReport(data, 'Load Test'),
        '/k6-results/load-summary.json': JSON.stringify(data, null, 2),
    };
}
