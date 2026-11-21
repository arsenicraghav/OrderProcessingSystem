#!/usr/bin/env bash
# End-to-end check of the running Order Processing Service, step by step.
#
#   ./scripts/e2e.sh                  # against http://localhost:8080 (any run mode)
#   BASE_URL=http://host:8080 ./scripts/e2e.sh
#
# Docker Compose stack only (BASE_URL on localhost): also checks Artemis (inbound status updates) and PostgreSQL rows.
# For a quick background-job check, start the stack with a short interval:
#   ORDERS_PROCESSING_INTERVAL=PT15S ORDERS_PROCESSING_INITIAL_DELAY=PT5S docker compose up -d --build
set -u

BASE_URL="${BASE_URL:-http://localhost:8080}"
JOB_WAIT_SECONDS="${JOB_WAIT_SECONDS:-330}"     # default job interval is 5 min (+ startup delay)
ARTEMIS_CONTAINER="${ARTEMIS_CONTAINER:-order-service-artemis-1}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-order-service-postgres-1}"

# Docker-based checks (steps 10-12) only make sense when the app under test is the local compose stack
case "$BASE_URL" in
  *localhost*|*127.0.0.1*) LOCAL_STACK=1 ;;
  *) LOCAL_STACK=0 ;;
esac

PASS=0; FAIL=0
green() { printf '\033[32m%s\033[0m\n' "$*"; }
red()   { printf '\033[31m%s\033[0m\n' "$*"; }
step()  { printf '\n\033[1m== %s\033[0m\n' "$*"; }
ok()    { PASS=$((PASS+1)); green "  PASS  $*"; }
bad()   { FAIL=$((FAIL+1)); red   "  FAIL  $*"; }

# call METHOD PATH [JSON]  -> sets $CODE and $BODY
call() {
  local method="$1" path="$2" data="${3:-}" out
  if [ -n "$data" ]; then
    out=$(curl -s -w '\n%{http_code}' -X "$method" "$BASE_URL$path" -H 'Content-Type: application/json' -d "$data")
  else
    out=$(curl -s -w '\n%{http_code}' -X "$method" "$BASE_URL$path")
  fi
  CODE="${out##*$'\n'}"; BODY="${out%$'\n'*}"
}
expect_code()     { [ "$CODE" = "$1" ] && ok "$2 (HTTP $CODE)" || bad "$2 (expected HTTP $1, got $CODE: $BODY)"; }
expect_contains() { case "$BODY" in *"$1"*) ok "$2";; *) bad "$2 (missing '$1' in: $BODY)";; esac; }
json_id()         { printf '%s' "$BODY" | sed -E 's/^\{"id":([0-9]+).*/\1/'; }
status_of()       { call GET "/api/orders/$1"; printf '%s' "$BODY" | sed -E 's/.*"status":"([A-Z]+)".*/\1/'; }

# ---------------------------------------------------------------------------------------------
step "0. Service is up"
call GET /actuator/health
expect_code 200 "health endpoint"
expect_contains '"status":"UP"' "database and broker connected"

step "1. Create an order with multiple items"
call POST /api/orders '{"customerId":"e2e-cust","items":[
  {"productId":"sku-1","productName":"Keyboard","quantity":2,"unitPrice":49.99},
  {"productId":"sku-2","productName":"Mouse","quantity":1,"unitPrice":20.02}]}'
expect_code 201 "order created"
expect_contains '"status":"PENDING"' "new order starts PENDING"
expect_contains '"totalAmount":120.00' "total computed server-side (2 x 49.99 + 20.02 = 120.00)"
expect_contains '"cancellable":true' "PENDING order is cancellable"
A=$(json_id); echo "  order A = $A"

step "2. Retrieve order details by ID"
call GET "/api/orders/$A"
expect_code 200 "order found"
expect_contains '"productName":"Keyboard"' "item 1 returned"
expect_contains '"productName":"Mouse"' "item 2 returned"
call GET /api/orders/99999999
expect_code 404 "unknown order returns 404"

step "3. Validation (server side)"
call POST /api/orders '{"customerId":"e2e-cust","items":[]}'
expect_code 400 "empty item list rejected"
call POST /api/orders '{"customerId":"e2e-cust","items":[{"productId":"p","productName":"x","quantity":0,"unitPrice":-1}]}'
expect_code 400 "zero quantity / negative price rejected"
call POST /api/orders '{"customerId":"e2e-cust","items":[
  {"productId":"p","productName":"x","quantity":1,"unitPrice":1},
  {"productId":"p","productName":"x","quantity":2,"unitPrice":1}]}'
expect_code 400 "duplicate product rejected by validation chain"
call POST /api/orders '{not json'
expect_code 400 "malformed JSON rejected"

step "4. Cancel an order (only while PENDING)"
call POST /api/orders '{"customerId":"e2e-cust","items":[{"productId":"sku-9","productName":"Cable","quantity":1,"unitPrice":5}]}'
B=$(json_id); echo "  order B = $B"
call POST "/api/orders/$B/cancel"
expect_code 200 "PENDING order cancelled"
expect_contains '"status":"CANCELLED"' "status is CANCELLED"
call POST "/api/orders/$B/cancel"
expect_code 409 "cancelling twice is rejected"

step "5. Status rules (illegal jumps are rejected)"
call PATCH "/api/orders/$A/status" '{"status":"DELIVERED"}'
expect_code 409 "PENDING -> DELIVERED rejected"
call PATCH "/api/orders/$A/status" '{"status":"LOST"}'
expect_code 400 "unknown status rejected"

step "6. List all orders, optionally filtered by status"
call GET "/api/orders?customerId=e2e-cust&size=100"
expect_code 200 "list all (this customer)"
call GET "/api/orders?status=CANCELLED&customerId=e2e-cust&size=100"
expect_contains "{\"id\":$B,\"customerId\"" "CANCELLED filter includes order B"
call GET "/api/orders?status=PENDING&customerId=e2e-cust&size=100"
case "$BODY" in *"{\"id\":$B,\"customerId\""*) bad "PENDING filter must not include cancelled order B";; *) ok "PENDING filter excludes order B";; esac
call GET "/api/orders?status=SHIPPED&customerId=e2e-cust&size=100"
expect_contains '"content":[' "empty-or-not, filtered list has the paged shape"

step "7. Background job moves PENDING -> PROCESSING (waits up to ${JOB_WAIT_SECONDS}s)"
waited=0; s=$(status_of "$A")
while [ "$s" = "PENDING" ] && [ "$waited" -lt "$JOB_WAIT_SECONDS" ]; do
  sleep 5; waited=$((waited+5)); s=$(status_of "$A"); printf '  ...%ss status=%s\r' "$waited" "$s"
done; echo
[ "$s" = "PROCESSING" ] && ok "order A promoted by the scheduled job after ~${waited}s" \
                        || bad "order A is $s after ${waited}s (job interval may be longer; set JOB_WAIT_SECONDS)"
[ "$(status_of "$B")" = "CANCELLED" ] && ok "cancelled order B untouched by the job" || bad "order B changed"

step "8. Cancel is no longer allowed once PROCESSING"
call POST "/api/orders/$A/cancel"
expect_code 409 "PROCESSING order cannot be cancelled"

step "9. Update status through the rest of the lifecycle"
call PATCH "/api/orders/$A/status" '{"status":"SHIPPED"}'
expect_code 200 "PROCESSING -> SHIPPED"
call PATCH "/api/orders/$A/status" '{"status":"DELIVERED"}'
expect_code 200 "SHIPPED -> DELIVERED"
expect_contains '"allowedNextStatuses":[]' "DELIVERED is terminal"
call PATCH "/api/orders/$A/status" '{"status":"SHIPPED"}'
expect_code 409 "no moves after DELIVERED"

# ---------------------------------------------------------------------------------------------
if [ "$LOCAL_STACK" = 1 ] && docker ps --format '{{.Names}}' 2>/dev/null | grep -qx "$ARTEMIS_CONTAINER"; then
  step "10. Messaging: inbound status update via Artemis queue orders.status-updates"
  call POST /api/orders '{"customerId":"e2e-cust","items":[{"productId":"sku-5","productName":"Monitor","quantity":1,"unitPrice":150}]}'
  C=$(json_id); echo "  order C = $C"
  docker exec "$ARTEMIS_CONTAINER" /var/lib/artemis-instance/bin/artemis producer \
      --url tcp://localhost:61616 --user artemis --password artemis \
      --destination queue://orders.status-updates --message-count 1 \
      --message "{\"orderId\":$C,\"status\":\"PROCESSING\"}" >/dev/null 2>&1 \
    && ok "status update sent to the queue" || bad "could not send with the Artemis CLI"
  waited=0; s=$(status_of "$C")
  while [ "$s" != "PROCESSING" ] && [ "$waited" -lt 15 ]; do sleep 1; waited=$((waited+1)); s=$(status_of "$C"); done
  [ "$s" = "PROCESSING" ] && ok "order C updated from the queue (~${waited}s)" || bad "order C is $s"

  step "11. Messaging: outbound events published to orders.events"
  docker exec "$ARTEMIS_CONTAINER" /var/lib/artemis-instance/bin/artemis queue stat \
      --url tcp://localhost:61616 --user artemis --password artemis --queueName orders.events 2>/dev/null \
    | grep -E 'NAME|orders.events' | sed 's/^/  /'
  echo "  (MESSAGE_COUNT > 0 = events waiting for consumers; browse them at http://localhost:8161)"
fi

if [ "$LOCAL_STACK" = 1 ] && docker ps --format '{{.Names}}' 2>/dev/null | grep -qx "$POSTGRES_CONTAINER"; then
  step "12. Data persisted in PostgreSQL"
  docker exec "$POSTGRES_CONTAINER" psql -U orders -d orders -c \
    "select id, customer_id, status, total_amount, version from orders where customer_id='e2e-cust' order by id;" \
    | sed 's/^/  /'
fi

printf '\n'
[ "$FAIL" -eq 0 ] && green "ALL $PASS CHECKS PASSED" || red "$FAIL FAILED, $PASS passed"
exit "$FAIL"