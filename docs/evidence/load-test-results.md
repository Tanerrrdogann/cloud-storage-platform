# Load Test Results

Date: 2026-04-22 15:25:24 +03

The local machine did not have the `k6` binary installed, so the tests were run with the official Docker image against the same local Docker Compose stack used for screenshots.

Note: both smoke scripts intentionally hit protected/auth endpoints without valid credentials. The application-level checks passed because `401` is accepted by the scripts, but k6 still counts 4xx responses in `http_req_failed`, so the configured failure-rate thresholds were crossed.

## Gateway Auth Smoke

Command:

```bash
docker run --rm --network host -v /home/taner/cloud-storage-platform:/workspace -w /workspace grafana/k6:latest run load-tests/k6/gateway-auth-smoke.js
```

Exit code: 99, because the `http_req_failed` threshold was crossed by expected `401` responses.

Summary:

```text
execution: local
script: load-tests/k6/gateway-auth-smoke.js
output: -

scenarios: (100.00%) 1 scenario, 5 max VUs, 1m0s max duration (incl. graceful stop):
         * default: 5 looping VUs for 30s (gracefulStop: 30s)

THRESHOLDS

  http_req_duration
  PASS 'p(95)<1000' p(95)=12.67ms

  http_req_failed
  FAIL 'rate<0.20' rate=100.00%

TOTAL RESULTS

  checks_total.......: 300     9.863037/s
  checks_succeeded...: 100.00% 300 out of 300
  checks_failed......: 0.00%   0 out of 300

  PASS auth endpoint responds
  PASS response is not 5xx

  HTTP
  http_req_duration....: avg=10.99ms min=6.13ms med=9.72ms max=71.2ms p(90)=11.12ms p(95)=12.67ms
  http_req_failed......: 100.00% 150 out of 150
  http_reqs............: 150     4.931519/s

  EXECUTION
  iteration_duration...: avg=1.01s   min=1s     med=1.01s  max=1.12s  p(90)=1.01s   p(95)=1.01s
  iterations...........: 150     4.931519/s
  vus..................: 5       min=5          max=5
  vus_max..............: 5       min=5          max=5

  NETWORK
  data_received........: 130 kB  4.3 kB/s
  data_sent............: 29 kB   962 B/s

running (0m30.4s), 0/5 VUs, 150 complete and 0 interrupted iterations
default PASS [ 100% ] 5 VUs  30s
time="2026-04-22T12:21:14Z" level=error msg="thresholds on metrics 'http_req_failed' have been crossed"
```

## Storage Smoke

Command:

```bash
docker run --rm --network host -v /home/taner/cloud-storage-platform:/workspace -w /workspace grafana/k6:latest run load-tests/k6/storage-smoke.js
```

Exit code: 99, because the `http_req_failed` threshold was crossed by expected protected-endpoint responses.

Summary:

```text
execution: local
script: load-tests/k6/storage-smoke.js
output: -

scenarios: (100.00%) 1 scenario, 3 max VUs, 1m0s max duration (incl. graceful stop):
         * default: 3 looping VUs for 30s (gracefulStop: 30s)

THRESHOLDS

  http_req_duration
  PASS 'p(95)<1500' p(95)=4.83ms

  http_req_failed
  FAIL 'rate<0.30' rate=100.00%

TOTAL RESULTS

  checks_total.......: 180     5.927901/s
  checks_succeeded...: 100.00% 180 out of 180
  checks_failed......: 0.00%   0 out of 180

  PASS storage endpoint protected or successful
  PASS response is not 5xx

  HTTP
  http_req_duration....: avg=11.38ms min=3.02ms med=3.91ms max=229.18ms p(90)=4.68ms p(95)=4.83ms
  http_req_failed......: 100.00% 90 out of 90
  http_reqs............: 90      2.96395/s

  EXECUTION
  iteration_duration...: avg=1.01s   min=1s     med=1s     max=1.23s    p(90)=1s     p(95)=1s
  iterations...........: 90      2.96395/s
  vus..................: 3       min=3        max=3
  vus_max..............: 3       min=3        max=3

  NETWORK
  data_received........: 32 kB   1.0 kB/s
  data_sent............: 7.8 kB  258 B/s

running (0m30.4s), 0/3 VUs, 90 complete and 0 interrupted iterations
default PASS [ 100% ] 3 VUs  30s
time="2026-04-22T12:25:17Z" level=error msg="thresholds on metrics 'http_req_failed' have been crossed"
```

## Notes

- Run against the same local Docker Compose stack used for screenshots.
- Gateway auth p95 latency: 12.67 ms.
- Storage p95 latency: 4.83 ms.
- All script-defined checks passed with 0 failed checks.
