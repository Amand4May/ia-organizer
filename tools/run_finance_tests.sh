#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
cd "$project_dir"
mkdir -p build/finance-tests
mapfile -t finance_sources < <(rg --files app/src/main/java/com/example/organizadoria/financeiro/domain app/src/main/java/com/example/organizadoria/financeiro/data -g '*.java' -g '!Firestore*')
java com.sun.tools.javac.Main --release 11 -encoding UTF-8 -d build/finance-tests "${finance_sources[@]}" tools/tests/FinanceTestSuite.java
java -ea -cp build/finance-tests FinanceTestSuite
