import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 3,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.30'],
    http_req_duration: ['p(95)<1500'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8088/api/v1';
const TOKEN = __ENV.ACCESS_TOKEN || '';

export default function () {
  const response = http.get(`${BASE_URL}/objects/my`, {
    headers: TOKEN ? { Authorization: `Bearer ${TOKEN}` } : {},
  });

  check(response, {
    'storage endpoint protected or successful': (r) => [200, 401, 403, 429].includes(r.status),
    'response is not 5xx': (r) => r.status < 500,
  });

  sleep(1);
}

