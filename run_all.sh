#!/bin/bash
# One-shot setup + live execution of the whole project (Linux/macOS, MariaDB or MySQL on localhost).
# Usage: ./run_all.sh            (needs MariaDB running and root access via socket: mariadb -uroot)
set -e
cd "$(dirname "$0")"
echo ">> 1/5 Creating database, loading sample data"
mariadb -uroot < database/schema.sql
mariadb -uroot < database/sample_data.sql
mariadb -uroot < database/create_user.sql
echo ">> 2/5 Running database-level tests"
mariadb -uroot --force --table < database/tests.sql > outputs/reports/db_tests_output.txt 2>&1 || true
echo ">> 3/5 Compiling Java"
( cd java && ./build.sh )
echo ">> 4/5 Running Java demo (bookings, cancellations, reports, threads)"
( cd java && ./run.sh --demo | tee ../outputs/reports/java_demo_output.txt )
echo ">> 5/5 Running Python analytics"
pip install -q -r python/requirements.txt 2>/dev/null || pip install -q --break-system-packages -r python/requirements.txt
python3 python/analysis.py
