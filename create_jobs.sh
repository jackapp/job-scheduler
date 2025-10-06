#!/bin/bash

for i in {1..100}
do
  echo "Executing request #$i"
  curl -s -o /dev/null -w "Status: %{http_code}\n" -X 'POST' \
    'http://localhost:8080/job' \
    -H 'accept: */*' \
    -H 'Content-Type: application/json' \
    -d '{
      "schedule": "*/1 * * * * ? *",
      "api": {
        "url": "http://localhost:3000/mock-endpoint",
        "httpMethod": "POST",
        "payload": {
          "message": "job-scheduler"
        },
        "readTimeoutMs": 120000
      },
      "type": "ATLEAST_ONCE"
    }'
done

echo "✅ Completed 100 curl executions"

