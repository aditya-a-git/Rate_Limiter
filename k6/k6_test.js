import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import exec from 'k6/execution';

// ============================================================
// Custom metrics
// ============================================================

const allowedRequests = new Counter('allowed_requests');
const rejectedRequests = new Counter('rejected_requests');

const isolationAllowed = new Counter('isolation_allowed');
const isolationRejected = new Counter('isolation_rejected');

const performanceLatency = new Trend('performance_latency', true);


// ============================================================
// Test configuration
// ============================================================

export const options = {

    scenarios: {

        // ----------------------------------------------------
        // 1. Performance test
        // ----------------------------------------------------
        performance: {
            executor: 'ramping-vus',

            startVUs: 0,

            stages: [
                { duration: '10s', target: 10 },
                { duration: '20s', target: 50 },
                { duration: '20s', target: 100 },
                { duration: '10s', target: 0 },
            ],

            gracefulRampDown: '5s',
        },


        // ----------------------------------------------------
        // 2. Rate-limit test
        // ----------------------------------------------------
        rate_limit: {
            executor: 'constant-vus',

            vus: 10,
            duration: '30s',

            startTime: '1m10s',
        },


        // ----------------------------------------------------
        // 3. API-key isolation test
        // ----------------------------------------------------
        isolation: {
            executor: 'per-vu-iterations',

            vus: 2,
            iterations: 120,

            startTime: '1m50s',
        },
    },


    thresholds: {

        // Overall latency
        http_req_duration: [
            'p(95)<100',
            'p(99)<200',
        ],

        // Performance latency
        performance_latency: [
            'p(95)<100',
            'p(99)<200',
        ],
    },
};


// ============================================================
// Scenario dispatcher
// ============================================================

export default function () {

    const scenario = exec.scenario.name;

    if (scenario === 'performance') {
        performanceTest();
    }

    else if (scenario === 'rate_limit') {
        rateLimitTest();
    }

    else if (scenario === 'isolation') {
        isolationTest();
    }
}


// ============================================================
// 1. Performance test
// ============================================================

function performanceTest() {

    /*
     * Every VU gets a different API key.
     *
     * VU 1   -> performance-user-1
     * VU 2   -> performance-user-2
     * ...
     *
     * This prevents all VUs from sharing one rate-limit bucket.
     */

    const apiKey = `performance-user-${__VU}`;

    const res = http.get(
        'http://localhost:8000/server',
        {
            headers: {
                'X-API-Key': apiKey,
            },

            tags: {
                test_type: 'performance',
            },
        }
    );


    performanceLatency.add(res.timings.duration);


    if (res.status === 200) {
        allowedRequests.add(1);
    }

    if (res.status === 429) {
        rejectedRequests.add(1);
    }


    check(res, {
        'performance: valid response':
            (r) => r.status === 200 || r.status === 429,
    });
}


// ============================================================
// 2. Rate-limit test
// ============================================================

function rateLimitTest() {

    /*
     * Each VU gets its own API-key bucket.
     *
     * 10 VUs
     * ×
     * 100 requests per key
     *
     * = approximately 1000 allowed requests
     * before the keys start returning 429.
     */

    const apiKey = `rate-limit-user-${__VU}`;

    const res = http.get(
        'http://localhost:8000/server',
        {
            headers: {
                'X-API-Key': apiKey,
            },

            tags: {
                test_type: 'rate_limit',
            },
        }
    );


    if (res.status === 200) {
        allowedRequests.add(1);
    }

    if (res.status === 429) {
        rejectedRequests.add(1);
    }


    check(res, {

        'rate-limit: valid response':
            (r) => r.status === 200 || r.status === 429,

        'rate-limit: has RateLimit-Limit':
            (r) => r.headers['Ratelimit-Limit'] !== undefined,

        'rate-limit: has RateLimit-Remaining':
            (r) => r.headers['Ratelimit-Remaining'] !== undefined,

        'rate-limit: has RateLimit-Reset-After':
            (r) => r.headers['Ratelimit-Reset-After'] !== undefined,
    });
}


// ============================================================
// 3. API-key isolation test
// ============================================================

function isolationTest() {

    /*
     * VU 1:
     *
     * Uses isolation-user-A.
     * Sends 120 requests.
     * Expected:
     *
     * 100 -> 200
     * 20  -> 429
     *
     *
     * VU 2:
     *
     * Uses isolation-user-B.
     * Sends only 50 requests.
     * Expected:
     *
     * 50 -> 200
     *
     *
     * This proves that exhausting A's bucket does not
     * affect B's bucket.
     */


    // --------------------------------------------------------
    // User A
    // --------------------------------------------------------

    if (__VU === 1) {

        const res = http.get(
            'http://localhost:8000/server',
            {
                headers: {
                    'X-API-Key': 'isolation-user-A',
                },

                tags: {
                    test_type: 'isolation',
                    client: 'A',
                },
            }
        );


        if (res.status === 200) {
            isolationAllowed.add(1);
        }

        if (res.status === 429) {
            isolationRejected.add(1);
        }


        check(res, {

            'isolation A: valid response':
                (r) => r.status === 200 || r.status === 429,
        });

        return;
    }


    // --------------------------------------------------------
    // User B
    // --------------------------------------------------------

    if (__VU === 2) {

        /*
         * Only send 50 requests.
         *
         * Since the limit is 100 requests,
         * User B should remain completely allowed.
         */

        if (__ITER >= 50) {
            return;
        }


        const res = http.get(
            'http://localhost:8000/server',
            {
                headers: {
                    'X-API-Key': 'isolation-user-B',
                },

                tags: {
                    test_type: 'isolation',
                    client: 'B',
                },
            }
        );


        check(res, {

            'isolation B: still allowed':
                (r) => r.status === 200,
        });
    }
}