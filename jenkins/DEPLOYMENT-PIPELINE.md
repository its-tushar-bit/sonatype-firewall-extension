# MTIQ Deployment Pipeline Runbook

This document describes the progressive deployment pipeline for MTIQ Docker images. Each stage is a standalone Jenkins job that triggers the next downstream job on success.

> **Current state (April 2026):** Staging jobs are now **active**. Production jobs remain in bypass mode until prod infrastructure is ready. See [Bypass Mode & Readiness](#bypass-mode--readiness) for details.

---

## Pipeline Flow

```
Jenkinsfile.main (main branch build)
  │
  ├─ pushes image to dev ECR
  │  tag: {yyyyMMddHHmm}-main-{buildNumber}-{commitId}
  │
  └─► deploy-to-shared-dev (automatic, non-blocking)
        │
        └─► test-shared-dev (automatic)
              │
              └─► push-to-staging (automatic)
                    │
                    └─► deploy-to-staging (automatic)
                      │
                      └─► test-staging (automatic)
                            │
                            └─► deploy-to-prod-internal (automatic)
                                  │
                                  │  ── manual gate ──
                                  │
                                  deploy-to-prod (MANUAL TRIGGER ONLY)
```

**Key transitions:**
- **dev → staging:** `push-to-staging` promotes the image from dev ECR to staging ECR using `docker buildx imagetools create` (requires Docker CLI with buildx on the agent), then triggers `deploy-to-staging` to deploy the promoted image.
- **staging → prod:** `deploy-to-prod-internal` promotes from staging ECR to prod ECR, deploys, then tags the image as verified. Production release requires an operator to manually run `deploy-to-prod` with a verified tag.

---

## Jenkins Jobs

All jobs are under the Jenkins folder path: **`insight/MTIQ/sca-cloud/`**

### 1. deploy-to-shared-dev

| | |
|---|---|
| **Path** | `insight/MTIQ/sca-cloud/deploy-to-shared-dev` |
| **Trigger** | Automatic from `Jenkinsfile.main` after ECR push (non-blocking, non-fatal) |
| **Parameter** | `ECR_TAG` — the tag pushed to dev ECR |
| **Timeout** | 15 minutes |
| **What it does** | Deploys the image to shared-dev cell(s) via Terraform Cloud, then triggers `test-shared-dev` |
| **Downstream** | `test-shared-dev` (automatic) |

**Failure modes:**
- **ECR_TAG is null:** The job errors immediately. This means `Jenkinsfile.main`'s ECR push likely failed — check the main build logs.
- **TFC workspace lookup fails:** Workspace name may be wrong or TFC credentials expired. Check the TFC credential in Jenkins (`sca-cloud-terraform-cloud-jenkins-credential`).
- **TFC apply times out (10 min):** The Terraform plan is running too long. Check the TFC UI link printed in the console log.
- **TFC plan has >2 resource destruction:** The auto-approval safety guard discards the run. This means the plan would destroy more infrastructure than a normal ECS task definition replacement. **Do not override** — inspect the plan in the TFC UI to understand what's being destroyed.
- **TFC plan shows null resource-destructions:** The TFC API response is malformed. The job refuses to auto-approve. Check TFC API status.

### 2. test-shared-dev

| | |
|---|---|
| **Path** | `insight/MTIQ/sca-cloud/test-shared-dev` |
| **Trigger** | Automatic from `deploy-to-shared-dev` |
| **Parameter** | `IMAGE_TAG` — the tag that was deployed |
| **Timeout** | 30 minutes |
| **What it does** | Runs smoke tests against the shared-dev cell using the mtiq-apps PostDeploymentSmokeTest CLI. Checks out mtiq-tools, builds with Maven, and runs tests against `https://cicd-testing.dev.iq.saas.sonatype.dev`. |
| **Downstream** | `push-to-staging` (automatic) |
| **Status** | Active |

**Failure modes:**
- **mtiq-tools checkout fails:** Git credentials may be expired. Check the `sonatypeZionCredentialsId()` credential.
- **Maven build fails:** Dependency resolution or compilation issues. Check the Maven settings and repository access.
- **Smoke test fails:** The application is not responding correctly. Check the smoke test HTML report archived in the build artifacts. Test failures block promotion to staging.
- **Chat notification fails:** Non-fatal to the pipeline but should be investigated.

### 3. push-to-staging

| | |
|---|---|
| **Path** | `insight/MTIQ/sca-cloud/push-to-staging` |
| **Trigger** | Automatic from `test-shared-dev` |
| **Parameter** | `IMAGE_TAG` — the dev ECR tag to promote |
| **Timeout** | 15 minutes |
| **What it does** | Promotes the image from dev ECR to staging ECR using `docker buildx imagetools create` (requires Docker CLI with buildx on the agent). Applies additional tag `staging-latest`. Verifies the promotion. |
| **Downstream** | `deploy-to-staging` (automatic) |
| **Status** | Active |

**Failure modes:**
- **AWS role assumption fails:** Check IAM role trust relationships and the Jenkins credential for the staging ECR account.
- **Manifest copy fails:** The image may not exist in dev ECR, or staging ECR repository may not exist. Verify the source tag exists: `aws ecr describe-images --repository-name sca-cloud/mtiq-server --image-ids imageTag=<tag>`.
- **Verification fails after copy:** The manifest was pushed but the validation read-back failed. Check for eventual consistency delays — re-running the job usually resolves this.

### 4. deploy-to-staging

| | |
|---|---|
| **Path** | `insight/MTIQ/sca-cloud/deploy-to-staging` |
| **Trigger** | Automatic from `push-to-staging`. Also triggerable manually from Jenkins UI. |
| **Parameter** | `IMAGE_TAG` — the ECR tag to deploy (e.g., `202604161432-main-142-a1b2c3d4`) |
| **Timeout** | 20 minutes |
| **What it does** | Validates the image tag exists in staging ECR, then deploys to staging cell(s) via Terraform Cloud. Sends notification to `mtiq-notices` chat room. |
| **Downstream** | `test-staging` (automatic) |
| **Status** | Active |

**Failure modes:**
- **Image tag not found in staging ECR:** The image was not promoted to staging. Check that `push-to-staging` has run successfully for this tag.
- **TFC deployment fails:** Same failure modes as `deploy-to-shared-dev` TFC deployment (workspace lookup, timeout, safety guard). Check console log for TFC UI link.
- **Chat notification fails:** Non-fatal to the pipeline but should be investigated.

**Manual deployment:** To deploy a specific image to staging, run the job manually from Jenkins and set `IMAGE_TAG` to the exact tag (e.g., `202604161432-main-142-a1b2c3d4`).

### 5. test-staging

| | |
|---|---|
| **Path** | `insight/MTIQ/sca-cloud/test-staging` |
| **Trigger** | Automatic from `deploy-to-staging` |
| **Parameter** | `IMAGE_TAG` — the tag that was deployed |
| **Timeout** | 30 minutes |
| **What it does** | Runs smoke tests against staging cells using the mtiq-apps PostDeploymentSmokeTest CLI. Checks out mtiq-tools, builds with Maven, and runs tests against `https://cicd.staging.iq.saas.sonatype.dev`. |
| **Downstream** | `deploy-to-prod-internal` (automatic) |
| **Status** | Active |

**Failure modes:**
- **mtiq-tools checkout fails:** Git credentials may be expired. Check the `sonatypeZionCredentialsId()` credential.
- **Maven build fails:** Dependency resolution or compilation issues. Check the Maven settings and repository access.
- **Smoke test fails:** The application is not responding correctly. Check the smoke test HTML report archived in the build artifacts. Test failures block promotion to prod-internal.
- **Chat notification fails:** Non-fatal to the pipeline but should be investigated.

### 6. deploy-to-prod-internal

| | |
|---|---|
| **Path** | `insight/MTIQ/sca-cloud/deploy-to-prod-internal` |
| **Trigger** | Automatic from `test-staging` |
| **Parameter** | `IMAGE_TAG` — the staging ECR tag to promote |
| **Timeout** | 20 minutes |
| **What it does** | Promotes image from staging ECR to prod ECR (manifest copy), deploys to prod-internal cell(s) via Terraform Cloud, then tags the image as `prod-internal-verified-{yyyyMMdd-HHmm}`. The verified tag is applied **only after** successful deployment — it serves as the safety gate for production releases. |
| **Downstream** | None (production release is manual) |
| **Bypass** | `PROD_INFRASTRUCTURE_READY = false` (CLM-39453) |

**Failure modes:**
- **Image promotion fails:** Same as `push-to-staging` — check IAM roles and ECR repository existence in prod account.
- **TFC deployment fails:** Same failure modes as other TFC deployments. Check console log for TFC UI link.
- **Verified tag fails to apply:** The deployment succeeded but the tag wasn't written. **The image is live but not marked as verified.** Follow the remediation instructions printed in the console log to manually apply the tag via AWS CLI. Without this tag, `deploy-to-prod` will refuse to release the image.

### 7. deploy-to-prod

| | |
|---|---|
| **Path** | `insight/MTIQ/sca-cloud/deploy-to-prod` |
| **Trigger** | **MANUAL ONLY** — an operator must run this from the Jenkins UI |
| **Parameters** | `IMAGE_TAG` (required) — must be a specific immutable tag present in prod ECR; `RELEASE_NAME` (optional) — human-readable name, defaults to `production-{yyyyMMdd-HHmm}` |
| **Timeout** | 30 minutes |
| **What it does** | Validates the image has a `prod-internal-verified-*` tag (proving it passed prod-internal), tags it as `production-latest` and with the release name, then deploys to production cell(s) via Terraform Cloud. |
| **Downstream** | None |
| **Bypass** | `PROD_INFRASTRUCTURE_READY = false` (CLM-39454) |

**Safety gate:** The job **refuses to deploy** any image that does not have a `prod-internal-verified-*` tag. This ensures only images that have been successfully deployed and validated on prod-internal can reach production.

**Failure modes:**
- **Image not found in prod ECR:** The image was never promoted from staging. Run `deploy-to-prod-internal` first.
- **Image missing prod-internal-verified tag:** The image was promoted but never successfully deployed to prod-internal, or the tagging step failed. Check `deploy-to-prod-internal` build history.
- **Production tagging fails:** Tags were not applied but deployment hasn't started yet. Safe to re-run.
- **TFC deployment fails:** Check console log for TFC UI link. See [Rollback](#rollback) below.

---

## Rollback

### Shared-dev / staging
Re-deploy a known-good image by running `deploy-to-shared-dev` or `deploy-to-staging` manually with the previous image tag. Each TFC deployment overwrites the `ecs_container_image` variable and triggers a new plan.

### Prod-internal
Re-run `deploy-to-prod-internal` with a previous known-good staging tag, or manually update the TFC workspace variable and apply.

### Production
1. Identify the previous known-good image tag from prod ECR (look for production-{releaseName} release tag).
2. Re-run `deploy-to-prod` with that tag, **or**
3. Manually update the TFC workspace `ecs_container_image` variable and apply through the TFC UI.

---

## Image Tag Conventions

| Tag | Where | Meaning |
|-----|-------|---------|
| `{yyyyMMddHHmm}-{branch}-{buildNumber}-{commitId}` | Dev ECR | Immutable build tag (e.g., `202604161432-main-142-a1b2c3d4`) |
| `{branch}-latest` | Dev ECR | Mutable pointer to the most recent image for a branch |
| `staging-latest` | Staging ECR | Mutable pointer to the most recent image promoted to staging |
| `prod-internal-latest` | Prod ECR | Mutable pointer to the most recent image promoted to prod |
| `prod-internal-verified-{yyyyMMdd-HHmm}` | Prod ECR | Applied **after** successful prod-internal deployment. Required to release to production. |
| `production-latest` | Prod ECR | Mutable pointer to the current production image |
| `production-{releaseName}` | Prod ECR | Named release tag (operator-supplied or date-based) |

---

## TFC Auto-Approval Safety Guard

Terraform Cloud plans are auto-approved if they include **at most 2 resource destruction** (the normal case for both ECS task definition replacement). Plans with more than 2 destruction are **discarded** and the pipeline fails.

If this happens:
1. Open the TFC UI link from the console log.
2. Review the plan to understand what resources would be destroyed.
3. If the destructions are expected (e.g., workspace restructuring), apply the plan manually through TFC.
4. If the destructions are unexpected, investigate before proceeding.

---

## Bypass Mode & Readiness

Staging jobs are now active. Production jobs remain in bypass mode until their infrastructure is ready.

| Job | Status | Bypass flag | Tracking ticket |
|-----|--------|-------------|-----------------|
| `push-to-staging` | Active | — | CLM-39451 |
| `deploy-to-staging` | Active | — | CLM-39452 |
| `test-shared-dev` | Active | — | CLM-39450 |
| `test-staging` | Active | — | CLM-39471 |
| `deploy-to-prod-internal` | Bypass | `PROD_INFRASTRUCTURE_READY = false` | CLM-39453 |
| `deploy-to-prod` | Bypass | `PROD_INFRASTRUCTURE_READY = false` | CLM-39454 |

**To enable a stage:** Set the corresponding flag to `true` in the Jenkinsfile and update any placeholder values (ECR account IDs, credentials, TFC workspace names). Each Jenkinsfile documents exactly what needs to change in its header comment.

---

## Notifications

| Job | Channel | When |
|-----|---------|------|
| `deploy-to-staging` | `mtiq-notices` | Always (success or failure) |
| `test-shared-dev` | `mtiq-notices` | Failure only |
| `test-staging` | `mtiq-notices` | Failure only |
| `deploy-to-prod` | `mtiq-notices` | Planned (currently commented out) |

---

## Common Troubleshooting

### TFC credential expired
**Symptom:** HTTP 401 from TFC API calls in any deployment job.
**Fix:** Rotate the `sca-cloud-terraform-cloud-jenkins-credential` credential in Jenkins.

### ECR authentication failure
**Symptom:** "denied" or "authorization" errors during ECR operations.
**Fix:** Check the IAM role trust policy for the target account. Verify the Jenkins credential ID matches the account.

### TFC workspace not found
**Symptom:** "TFC returned no data for workspace" error.
**Fix:** Verify the workspace name in the Jenkinsfile matches the actual TFC workspace in the `Sonatype-Cloud` organization.

### deploy-to-prod rejects the image
**Symptom:** "does not have a prod-internal-verified-* tag" error.
**Fix:** The image hasn't passed prod-internal validation. Check the `deploy-to-prod-internal` build history for that tag. If deployment succeeded but tagging failed, follow the manual remediation instructions in the `deploy-to-prod-internal` failure output.

### Job times out after adding workspaces
**Symptom:** A deployment job fails with a timeout error after adding new cell workspaces.
**Fix:** Each TFC workspace deployment takes up to 10 minutes. The job timeout must exceed `10 min × number of cell workspaces + overhead`. Update the `timeout` value in the Jenkinsfile's `options` block. Each Jenkinsfile documents the current calculation in a comment next to the timeout setting.
