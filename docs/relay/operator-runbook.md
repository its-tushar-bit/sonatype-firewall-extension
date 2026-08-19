<!--

    Copyright (c) 2025-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# SCM Relay — operator runbook

Recovery procedures for SCM relay integration failure modes that an operator
or support engineer may need to resolve. Companion to the codebase under
`insight-brain-service/.../relay/` and the `scm-relay` deployment.

## Quick state model

There are two halves of relay state that must agree:

| Side       | Storage                                                 | Owner      |
| ---------- | ------------------------------------------------------- | ---------- |
| IQ-side    | `relay_configuration` table (one row per IQ tenant)     | IQ DB      |
| Relay-side | DynamoDB customer record + SQS queue + install-index    | scm-relay  |

A normal customer is registered on **both** sides and the api key in IQ's
`relay_configuration` row is the proof-of-possession that ties them together.
Most stuck states arise when one side has state the other lacks.

## Stuck state #1 — IQ has no row, relay holds the customer

**Symptom**: IQ logs show a repeating WARN every poll cycle:

```
Pre-flight relay registration rejected (HTTP 401) for tenant <slug>; the relay
holds a customer record for this license but IQ has no credential to recover
it. Manual recovery required: either rotate the IQ license (creates a fresh
customer) or have an operator drop the relay-side customer via relay admin
tooling, then retry registration. Cause: <detail>
```

**Root cause**: Someone (operator panic, restored-from-old-backup, manual
DB edit) cleared the IQ-side `relay_configuration` row while the relay still
has the customer record + SQS queue under the IQ's license fingerprint. IQ's
pre-flight register tries a fresh `register()` call with no api key and no
webhook token, and the relay refuses because the license is already in use
by a customer record IQ can't prove ownership of.

**Recovery options**, in order of preference:

### Option A — drop the relay-side customer, then re-register from IQ

Use this when the relay-side queue is **empty or expendable** (you do not
need to recover events that were enqueued before the desync).

1. Identify the relay customer id. It is in the relay's CloudWatch logs
   under `customerId=<uuid>` for the recent `unauthenticated re-registration
   rejected` lines.
2. Have a relay admin (someone with access to the relay's `secretsmanager`
   admin tooling or DynamoDB) delete the customer row keyed by that id.
   This drops the SQS queue and the installation-index entries.
3. On the IQ side, trigger a fresh registration from the SCM admin UI
   (`Source Control → Register with SCM relay`) or via REST:

   ```bash
   curl -X POST https://<iq>/api/v2/sourceControl/relay/register \
     -H 'X-Auth-Token: <admin-token>'
   ```

4. IQ's next poll cycle picks up the new `relay_configuration` row; the
   stuck-state WARN stops; events flow again.

### Option B — rotate the IQ license

Use this when the relay-side admin is unreachable and you need IQ-side
recovery alone. The new license has a different fingerprint, which the
relay treats as a different customer; the orphaned record on the relay
side ages out per the relay's retention policy.

1. Install a fresh license bytes file via `PUT /api/v2/product/license`.
2. From the SCM admin UI, click `Register with SCM relay`. The new license
   fingerprint creates a fresh customer record on the relay side.
3. The orphaned customer record on the relay side does NOT auto-clean up;
   schedule a manual deregister later if relay-side hygiene matters.

### Option C — direct DB recovery (last resort)

Only do this if you can recover the previous IQ row's `api_key` ciphertext
from a DB backup. Restore the `relay_configuration` row exactly as it was
(including encrypted api key); IQ's polling cycle will then validate
against the relay successfully.

## Stuck state #2 — IQ has row, relay does not

**Symptom**: IQ poll cycles fail with HTTP 404 from `/api/events`.

**Root cause**: Relay-side state was deleted (operator action, lambda redeploy
that wiped DDB, region failover with stale state) while IQ's
`relay_configuration` row still claims the relay holds a customer.

**Recovery**:

1. Trigger `POST /api/v2/sourceControl/relay/deregister` from the SCM
   admin UI. IQ tolerates a 404 from the relay (treated as "already gone")
   and removes the local row.
2. Re-register: `POST /api/v2/sourceControl/relay/register`.

## Stuck state #3 — orphaned installation index

**Symptom**: GitHub webhooks for an installation never reach IQ; relay log
shows `unknown installation` for the installation id, even after the App is
re-installed.

**Root cause**: An IQ admin deleted a `github_app` row directly in the DB
while the App was still installed on github.com, leaving the relay-side
installation-index entry intact. The next webhook from that installation id
hits the index but the routed-to customer no longer claims it (or has
itself been deregistered).

**Recovery**:

1. From IQ admin: uninstall the App on github.com (if still installed).
2. From IQ admin: re-create the App via `Source Control → Manage GitHub
   Apps → Create New`. The fresh installation produces a new id and the
   stale index entry is overwritten on first webhook.

If the relay's installation index needs explicit cleanup, use the relay
admin tooling: `DELETE /api/installations/<id>` with the customer's api key.

## Stuck state #4 — feature flag toggled mid-cycle

**Symptom**: Polling neither runs nor falls back; both relay and legacy
SCM polling appear inactive.

**Root cause**: The flag was disabled while a poll cycle was in flight;
the in-flight cycle saw `isFeatureGateOpen() == true` but completed after
shutdown of the per-tenant executor. Subsequent cycles see gate closed
and no-op.

**Recovery**: Toggle the flag back to enabled; the next register cycle
re-creates the per-tenant executor and polling resumes. If polling does
NOT resume within `pollIntervalSeconds * 2`, restart the IQ process.

## Stuck state #5 — jOOQ generated classes missing

**Symptom**: Operations that touch a jOOQ DAO fail with
`NoClassDefFoundError: com/sonatype/insight/brain/jooq/generated/ods/Tables`
(or a sibling generated class name). Visible failure surfaces include:
GitHub App registration returning 500, polling cycle ERRORs in logs
(`Relay polling cycle threw unexpectedly: ...NoClassDefFoundError...`),
support zip showing `pollErrors` incrementing without a network
explanation.

**Root cause**: The jOOQ codegen wrote source files under
`insight-brain-db/target/generated-sources/jooq/.../Tables.java` but a
matching `.class` was never produced in `target/classes/`. Common
triggers:

- Incremental Maven build that re-ran codegen but skipped recompile.
- An IDE "build" that compiles `src/main/java` but not
  `target/generated-sources/`.
- A failed earlier compile step that left codegen sources stale.

**Recovery**: full Maven rebuild from the repo root.

```bash
mvn clean install -DskipTests -pl '!insight-brain-frontend,!nexus-mtiq-server'
```

Then restart the IQ process. The polling cycle's ERROR-on-Throwable
guard \(`RelayPollingService.runPollCycleWithErrorBoundary`\) keeps
the schedule alive across this failure mode, so users still get PR
scans via legacy SCM polling fallback while the build is corrected;
but the failing operations themselves \(e.g. App registration\) cannot
recover until the classes are rebuilt.

**Long-term mitigation**: build profiles that re-run codegen MUST also
recompile generated sources before producing artifacts. A startup-time
sanity check that imports `Tables`/`Indexes`/schema-binding generated
classes and fails fast on `ClassNotFoundException` would surface this
at boot rather than at first request — tracked as a separate
build/observability follow-up.

## Diagnostics shortcuts

### Verify per-tenant polling is alive (MTIQ)

```bash
jstack <iq-pid> | grep -E "RelayPolling-[a-z]+-[0-9]+"
```

There should be one thread per registered tenant. A tenant with the
feature ON but no thread is a registration-time failure (look for the
`Tenant <slug> relay polling could not start` WARN).

### Tail relevant logs

```bash
# Per-tenant cycle activity (DEBUG)
grep -E "Tenant [a-z-]+ skipping poll|Relay polling registered" /var/log/iq/insight-brain.log

# 5-minute api key grace and rotation activity
grep -E "Relay api key rotated|previousKeyExpiresAt" /var/log/iq/insight-brain.log

# Stuck-state signal
grep "Pre-flight relay registration rejected (HTTP 401)" /var/log/iq/insight-brain.log
```

### Check `relay_configuration` state

```sql
-- single-tenant
SELECT customer_id, webhook_url IS NOT NULL AS has_webhook_url,
       registered_at FROM relay_configuration;

-- MTIQ (replace <slug>)
SET search_path = t_<slug>;
SELECT customer_id, webhook_url IS NOT NULL AS has_webhook_url,
       registered_at FROM relay_configuration;
```

## Out of scope

Recovery from a relay-side data loss event (DynamoDB table dropped,
SQS queues deleted, lambda code rolled back to a buggy version) is the
relay deployment's concern; from IQ's perspective the recovery is always
"deregister + re-register" once the relay is healthy again.
