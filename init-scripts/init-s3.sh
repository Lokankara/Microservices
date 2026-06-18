#!/bin/bash
set -e

echo "Initializing LocalStack S3 buckets..."

BUCKETS=("staging-bucket" "permanent-bucket")
REGION="us-east-1"

for bucket in "${BUCKETS[@]}"; do
  awslocal s3 mb "s3://${bucket}" --region "${REGION}" 2>/dev/null || {
    echo "Bucket ${bucket} already exists or creation skipped"
  }
done

awslocal s3api list-buckets --region "${REGION}" 2>/dev/null || {
  echo "Warning: Could not list buckets"
}

echo "✓ LocalStack S3 initialization complete"
