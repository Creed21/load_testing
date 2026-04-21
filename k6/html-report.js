/**
 * Generates a self-contained HTML report from a k6 handleSummary data object.
 * Usage in any test script:
 *   import { generateHtmlReport } from './html-report.js';
 *   export function handleSummary(data) {
 *     return { 'k6-results/my-report.html': generateHtmlReport(data, 'My Test') };
 *   }
 */
export function generateHtmlReport(data, testName) {
    const m = data.metrics;

    const dur  = m.http_req_duration?.values  || {};
    const fail = m.http_req_failed?.values    || {};
    const reqs = m.http_reqs?.values          || {};
    const recv = m.data_received?.values      || {};
    const vus  = m.vus_max?.values            || {};

    const totalReqs   = Math.round(reqs.count  || 0);
    const throughput  = (reqs.rate   || 0).toFixed(2);
    const errorRate   = ((fail.rate  || 0) * 100).toFixed(2);
    const dataMb      = ((recv.count || 0) / 1024 / 1024).toFixed(1);
    const maxVus      = Math.round(vus.max || 0);

    const p = (key) => Math.round(dur[key] || 0);

    // Collect threshold results
    const thresholdRows = Object.entries(m)
        .filter(([, v]) => v.thresholds)
        .flatMap(([metric, v]) =>
            Object.entries(v.thresholds).map(([condition, passed]) => ({
                metric,
                condition,
                passed,
            }))
        );

    const thresholdHtml = thresholdRows.length === 0
        ? '<tr><td colspan="3">No thresholds defined</td></tr>'
        : thresholdRows.map(t => `
            <tr class="${t.passed ? 'pass' : 'fail'}">
                <td>${t.metric}</td>
                <td>${t.condition}</td>
                <td>${t.passed ? '✓ PASS' : '✗ FAIL'}</td>
            </tr>`).join('');

    // Collect per-scenario p95 if present
    const scenarios = Object.entries(m)
        .filter(([k]) => k.startsWith('http_req_duration{scenario:'))
        .map(([k, v]) => ({
            name: k.replace('http_req_duration{scenario:', '').replace('}', ''),
            p95: Math.round(v.values?.['p(95)'] || 0),
            avg: Math.round(v.values?.avg       || 0),
            count: Math.round(v.values?.count   || 0),
        }));

    const scenarioHtml = scenarios.length === 0 ? '' : `
        <h2>Per-Scenario Response Times</h2>
        <table>
            <thead><tr><th>Scenario</th><th>Avg</th><th>p95</th><th>Requests</th></tr></thead>
            <tbody>
                ${scenarios.map(s => `
                <tr>
                    <td>${s.name}</td>
                    <td>${s.avg} ms</td>
                    <td>${s.p95} ms</td>
                    <td>${s.count}</td>
                </tr>`).join('')}
            </tbody>
        </table>`;

    const now = new Date().toISOString().replace('T', ' ').substring(0, 19) + ' UTC';

    return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>${testName} — k6 Report</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
         background: #f5f6fa; color: #333; padding: 24px; }
  h1   { font-size: 1.6rem; margin-bottom: 4px; }
  h2   { font-size: 1.1rem; margin: 28px 0 10px; color: #555; }
  .subtitle { color: #888; font-size: 0.9rem; margin-bottom: 28px; }
  .cards { display: flex; flex-wrap: wrap; gap: 16px; margin-bottom: 8px; }
  .card  { background: #fff; border-radius: 8px; padding: 20px 28px;
           box-shadow: 0 1px 4px rgba(0,0,0,.08); min-width: 140px; }
  .card .label { font-size: 0.75rem; text-transform: uppercase;
                 letter-spacing: .06em; color: #888; margin-bottom: 6px; }
  .card .value { font-size: 1.8rem; font-weight: 700; color: #1a1a2e; }
  .card .unit  { font-size: 0.8rem; color: #aaa; margin-left: 2px; }
  table  { width: 100%; border-collapse: collapse; background: #fff;
           border-radius: 8px; overflow: hidden;
           box-shadow: 0 1px 4px rgba(0,0,0,.08); }
  thead  { background: #1a1a2e; color: #fff; }
  th, td { padding: 11px 16px; text-align: left; font-size: 0.9rem; }
  tbody tr:nth-child(even) { background: #f9f9fc; }
  .pass  { color: #27ae60; font-weight: 600; }
  .fail  { color: #e74c3c; font-weight: 600; }
  tr.pass td:last-child { color: #27ae60; }
  tr.fail td:last-child { color: #e74c3c; }
  .section { background: #fff; border-radius: 8px; padding: 20px;
             box-shadow: 0 1px 4px rgba(0,0,0,.08); margin-top: 16px; }
  pre { font-size: 0.8rem; white-space: pre-wrap; color: #444; }
</style>
</head>
<body>

<h1>${testName}</h1>
<p class="subtitle">Generated ${now}</p>

<div class="cards">
  <div class="card">
    <div class="label">Total Requests</div>
    <div class="value">${totalReqs.toLocaleString()}</div>
  </div>
  <div class="card">
    <div class="label">Throughput</div>
    <div class="value">${throughput}<span class="unit"> req/s</span></div>
  </div>
  <div class="card">
    <div class="label">Error Rate</div>
    <div class="value" style="color:${parseFloat(errorRate) > 1 ? '#e74c3c' : '#27ae60'}">${errorRate}<span class="unit">%</span></div>
  </div>
  <div class="card">
    <div class="label">Max VUs</div>
    <div class="value">${maxVus}</div>
  </div>
  <div class="card">
    <div class="label">Data Received</div>
    <div class="value">${dataMb}<span class="unit"> MB</span></div>
  </div>
</div>

<h2>Response Time Distribution (all requests)</h2>
<table>
  <thead><tr><th>avg</th><th>p50</th><th>p90</th><th>p95</th><th>p99</th><th>max</th></tr></thead>
  <tbody>
    <tr>
      <td>${p('avg')} ms</td>
      <td>${p('med')} ms</td>
      <td>${p('p(90)')} ms</td>
      <td>${p('p(95)')} ms</td>
      <td>${p('p(99)')} ms</td>
      <td>${p('max')} ms</td>
    </tr>
  </tbody>
</table>

${scenarioHtml}

<h2>Threshold Results</h2>
<table>
  <thead><tr><th>Metric</th><th>Condition</th><th>Result</th></tr></thead>
  <tbody>${thresholdHtml}</tbody>
</table>

</body>
</html>`;
}

