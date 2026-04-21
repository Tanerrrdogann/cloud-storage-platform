import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 5,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.20'],
    http_req_duration: ['p(95)<1000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8088/api/v1';

export default function () {
  const payload = JSON.stringify({
    usernameOrEmail: 'none@example.com',
    password: 'bad',
  });

  const response = http.post(`${BASE_URL}/auth/login`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(response, {
    'auth endpoint responds': (r) => [200, 401, 429].includes(r.status),
    'response is not 5xx': (r) => r.status < 500,
  });

  sleep(1);
}

