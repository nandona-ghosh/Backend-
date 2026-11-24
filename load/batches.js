import http from 'k6/http';
import { check } from 'k6';
export const options = {
  scenarios: { festival: { executor: 'constant-arrival-rate', rate: 200, timeUnit: '1s', duration: '1m', preAllocatedVUs: 100, maxVUs: 250 } },
  thresholds: { http_req_failed: ['rate<0.01'], http_req_duration: ['p(95)<300'], dropped_iterations: ['count==0'] }
};
export default function () {
  const res = http.get(`${__ENV.BASE_URL || 'http://localhost:8080'}/api/v1/batches?size=20`, { headers: { Authorization: `Bearer ${__ENV.TOKEN}` }, tags: { name: 'list_batches' } });
  check(res, { 'HTTP 200': r => r.status === 200 });
}
