#!/usr/bin/env bash
# One command to verify the whole repository (macOS / Linux / Git Bash):
#   1. Task 1 - JUnit tests of the packaging selector
#   2. Task 2 - REST Assured tests of the promo-code API
#   3. Task 2 - starts the reference service and runs the Postman collection with Newman
# Requirements: JDK 17+ (Maven is downloaded by the wrapper), Node.js 18+ for step 3.
set -uo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
T1="$ROOT/Task 1/Additional/Automation/packaging-selector"
T2="$ROOT/Task 2/Additional/Automation/promo-code-api"
PM="$ROOT/Task 2/Additional/Postman"
LOGS="$ROOT/.run-logs"
PORT="${PORT:-8080}"
NEWMAN="newman@6.2.1"
mkdir -p "$LOGS"

if [ -t 1 ]; then B=$'\e[1m'; G=$'\e[32m'; R=$'\e[31m'; Y=$'\e[33m'; C=$'\e[36m'; D=$'\e[2m'; N=$'\e[0m'; else B= G= R= Y= C= D= N=; fi
ok()   { printf "  ${G}✔${N} %s\n" "$*"; }
bad()  { printf "  ${R}✘${N} %s\n" "$*"; }
info() { printf "  ${D}%s${N}\n" "$*"; }
step() { printf "\n${B}${C}▶ %s${N}\n" "$*"; }

printf "${B}\n  ┌──────────────────────────────────────────────┐\n  │  devxed_test_task · run everything           │\n  └──────────────────────────────────────────────┘${N}\n"

# ------------------------------------------------------------------ prerequisites
step "Checking prerequisites"
if ! command -v java >/dev/null 2>&1; then
  bad "Java not found. Install JDK 17+ (e.g. https://adoptium.net) and run again."; exit 1
fi
JV=$(java -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')
if [ "${JV:-0}" -lt 17 ] 2>/dev/null; then bad "JDK $JV found, 17+ is required."; exit 1; fi
ok "JDK $JV"
HAVE_NODE=1
if command -v npx >/dev/null 2>&1; then ok "Node.js $(node -v)"; else HAVE_NODE=0; printf "  ${Y}!${N} Node.js not found - the Postman/Newman step will be skipped (install from https://nodejs.org)\n"; fi

RESULTS=()
FAILED=0

count_surefire() { # prints "tests failures+errors skipped"
  cat "$1"/target/surefire-reports/TEST-*.xml 2>/dev/null | grep -o '<testsuite [^>]*>' | awk '
    { for (i = 1; i <= NF; i++) { split($i, kv, "="); gsub(/"|>/, "", kv[2]);
        if (kv[1] == "tests") t += kv[2]; if (kv[1] == "failures" || kv[1] == "errors") f += kv[2]; if (kv[1] == "skipped") s += kv[2] } }
    END { printf "%d %d %d", t, f, s }'
}

run_maven() { # name dir log
  local name="$1" dir="$2" log="$LOGS/$3" start=$SECONDS
  info "./mvnw test  (log: .run-logs/$3)"
  (cd "$dir" && chmod +x mvnw 2>/dev/null; ./mvnw -B -ntp test >"$log" 2>&1)
  local rc=$? res; res=$(count_surefire "$dir"); read -r t f s <<<"$res"
  if [ $rc -eq 0 ]; then ok "$name: ${B}$t tests${N}, 0 failed  ${D}($((SECONDS - start)) s)${N}"; RESULTS+=("${G}✔${N}|$name|$t tests, 0 failed")
  else bad "$name: $t tests, ${f:-?} failed - last lines of the log:"; tail -n 30 "$log" | sed 's/^/      /'; RESULTS+=("${R}✘${N}|$name|$t tests, ${f:-?} failed"); FAILED=1; fi
}

# ------------------------------------------------------------------ Task 1
step "Task 1 · Packaging selector (JUnit 5 + AssertJ + mutation check)"
run_maven "Task 1 · Java tests" "$T1" "task1-maven.log"

# ------------------------------------------------------------------ Task 2
step "Task 2 · Promo-code API (REST Assured + mutation check)"
run_maven "Task 2 · Java tests" "$T2" "task2-maven.log"

step "Task 2 · Postman collection (Newman against the reference service)"
if [ $HAVE_NODE -eq 0 ]; then
  printf "  ${Y}!${N} skipped (no Node.js)\n"; RESULTS+=("${Y}!${N}|Task 2 · Postman / Newman|skipped - Node.js not installed")
else
  if curl -s -o /dev/null "http://localhost:$PORT/" 2>/dev/null; then bad "port $PORT is already in use - free it or run: PORT=8090 ./run-all.sh"; exit 1; fi
  info "starting the service on http://localhost:$PORT  (log: .run-logs/service.log)"
  (cd "$T2" && PORT=$PORT exec ./mvnw -B -ntp -q compile exec:java) >"$LOGS/service.log" 2>&1 &
  SVC=$!
  cleanup() { kill "$SVC" 2>/dev/null; pkill -f "qa.promo.PromoApplication" 2>/dev/null; true; }
  trap cleanup EXIT
  up=0
  for _ in $(seq 1 90); do
    if curl -s -o /dev/null "http://localhost:$PORT/api/apply-promo-code" 2>/dev/null; then up=1; break; fi
    kill -0 "$SVC" 2>/dev/null || break; sleep 1
  done
  if [ $up -eq 0 ]; then
    bad "the service did not start - see .run-logs/service.log"; tail -n 20 "$LOGS/service.log" | sed 's/^/      /'
    RESULTS+=("${R}✘${N}|Task 2 · Postman / Newman|service did not start"); FAILED=1
  else
    ok "service is up"
    info "npx $NEWMAN run ...  (log: .run-logs/newman.log)"
    (cd "$ROOT/Task 2/Additional" && npx --yes "$NEWMAN" run "Postman/Promo_Code_API.postman_collection.json" \
        -e "Postman/Local.postman_environment.json" --env-var "baseUrl=http://localhost:$PORT" \
        --reporters cli,json --reporter-json-export "$LOGS/newman.json" --color off >"$LOGS/newman.log" 2>&1)
    rc=$?
    stats=$(node -e 'const s=require(process.argv[1]).run.stats;console.log(s.requests.total,s.assertions.total,s.assertions.failed)' "$LOGS/newman.json" 2>/dev/null)
    read -r rq as af <<<"${stats:-? ? ?}"
    if [ $rc -eq 0 ]; then ok "Newman: ${B}$rq requests, $as assertions${N}, 0 failed"; RESULTS+=("${G}✔${N}|Task 2 · Postman / Newman|$rq requests, $as assertions, 0 failed")
    else bad "Newman: $af of $as assertions failed - see .run-logs/newman.log"; tail -n 30 "$LOGS/newman.log" | sed 's/^/      /'; RESULTS+=("${R}✘${N}|Task 2 · Postman / Newman|$af of $as assertions failed"); FAILED=1; fi
  fi
  cleanup; trap - EXIT
fi

# ------------------------------------------------------------------ summary
printf "\n${B}  Summary${N}\n  ──────────────────────────────────────────────────────────\n"
for r in "${RESULTS[@]}"; do IFS='|' read -r i n d <<<"$r"; printf "  %s  %-28s %s\n" "$i" "$n" "$d"; done
printf "  ──────────────────────────────────────────────────────────\n"
if [ $FAILED -eq 0 ]; then printf "  ${G}${B}All green.${N} Logs and reports: .run-logs/, */target/surefire-reports/\n\n"; else printf "  ${R}${B}Something failed${N} - see the logs in .run-logs/\n\n"; fi
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  { echo "### devxed_test_task · test run ($(uname -s))"; echo; echo "| | Suite | Result |"; echo "|:-:|---|---|"
    for r in "${RESULTS[@]}"; do IFS='|' read -r i n d <<<"$r"; case "$i" in *✔*) e="✅";; *✘*) e="❌";; *) e="⚠️";; esac; echo "| $e | $n | $d |"; done
  } >>"$GITHUB_STEP_SUMMARY"
fi
exit $FAILED
