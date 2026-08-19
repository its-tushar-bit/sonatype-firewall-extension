---
name: iq-patch-release
description: >-
  Drive an IQ Server (insight-brain) patch release end to end following Sonatype's
  patch release process. A patch release ships only critical bug fixes onto a release branch
  as an incremented patch (e.g. 1.199.1, selfhosted-205.3); whether it is tagged Docker `latest`
  and marked Jira-released depends on whether it targets the current latest release line.
  Use when the user wants to cut/backport a patch release, create a release-X.Y.Z branch,
  cherry-pick fixes, or run the aggregate-release + Release IQ Server Jenkins jobs with
  patch skip parameters. This skill notifies #releases-sca-core (EMs, docs, etc.) at
  kickoff and PAUSES for the human whenever Slack is unavailable or a step needs a person.
---

# IQ Server Patch Release Process

Source of truth: [IQ Server Patch Release Process](https://sonatype.atlassian.net/wiki/spaces/SP/pages/2243264521/IQ+Server+Patch+Release+Process)
(child of the [IQ Release Process Template](https://sonatype.atlassian.net/wiki/spaces/SP/pages/1979908103)).
If in doubt about a step this skill does not cover, re-read the wiki — it wins.

A **patch release** ships only critical bug fixes on top of an already-released version, as an
incremented patch (e.g. `1.199.1`, `1.199.2`). It follows a simplified version of the standard
release process. Use it when:

- You need to ship a fix as a patch of an existing release line (e.g. 1.199.x).
- The release version is a patch (e.g. `1.199.1`), NOT a new minor (`1.199.0`).

**Latest vs. older line — decide this first; it controls two Phase 3 parameters.** A patch can
target either the current latest release line or an older one:

- **Older line** (e.g. patching 1.198.x / 1.199.x while 1.200.x is latest): do **NOT** mark it
  Docker `latest` or Jira-"released" — set `skipMarkAsLatest=true` and `skipMarkJiraVersionReleased=true`.
- **Current latest line** (the patch becomes the newest stable release): mark it `latest` and
  Jira-"released" as normal — `skipMarkAsLatest=false`, `skipMarkJiraVersionReleased=false`.

---

## TWO NON-NEGOTIABLE RULES

**RULE 1 — Notify at kickoff.** The moment a patch release starts, post the kickoff
notification in **#releases-sca-core** tagging the necessary people (EMs for approval,
Docs team, etc.). See [references/slack-notifications.md](references/slack-notifications.md).

**RULE 2 — Pause and ask when blocked.** Whenever a step needs a human to act — **and always
if Slack cannot be reached, a scope is missing, a group mention can't be resolved, or a post
fails** — STOP. Do not fake progress or skip ahead. Present the exact action (and, for Slack,
the exact drafted message + channel + who to tag), then wait for the human to confirm it is
done before continuing. This applies to EVERY point in the process, not just Slack.

You are the orchestrator and note-taker. Steps like triggering Jenkins release jobs, EM
approval, and creating a Jira fix version are inherently human actions — drive them, draft
everything, but gate each one behind explicit human confirmation.

---

## Before you start: gather inputs

Collect (ask the human for anything missing — do not guess):

1. **Version being patched** and the **new patch version** (e.g. patch 1.199.0 → `1.199.1`;
   if `1.199.1` exists, use `1.199.2`).
2. **Release branch name**: `release-<newPatchVersion>` (e.g. `release-1.199.1`).
3. **Base commit** for the branch — the commit that released the version being patched
   (look for messages like `Release 1.198.0-01 (Snapshot: …, Commit: …)`).
4. **Commit SHA(s) to cherry-pick** (the fixes) **plus any dependent commits** needed to apply cleanly.
5. **Jira ticket(s)** being shipped (and any dependent tickets whose commits are cherry-picked).
6. **Jira fix version name**: `selfhosted-<release>.<patch>` (e.g. `selfhosted-205.3`, `selfhosted-199.1`).

Confirm Slack access first: run `slack_whoami`. If it fails → RULE 2 (hand every notification
to the human to post manually and continue only on their confirmation).

---

## Phase 0 — EM approval + kickoff notification

1. **Post the kickoff / approval request** in **#releases-sca-core**, tagging **@sca-em**
   (approval to include this patch) and **@sca-docs** (heads-up). Use the *Kickoff* template
   in [references/slack-notifications.md](references/slack-notifications.md).
2. **Wait for EM approval.** EMs must approve the patch's inclusion (tagged with `@sca-em`
   in the relevant thread). Do not proceed to cutting the branch until an EM approves — RULE 2.

---

## Phase 1 — Create release branch & cherry-pick fixes

### 1.1 Create the release branch (from the target version's commit)
```bash
git checkout -b release-1.199.1 <base-commit-sha>
git push origin release-1.199.1
```
Naming convention: `release-X.Y.Z`. First patch of 1.199.0 → `release-1.199.1`; second → `release-1.199.2`.

### 1.2 Cherry-pick the fixes
```bash
git cherry-pick <commit-sha>        # repeat for each fix (and dependents)
# resolve any conflicts as they arise
```
Then set the project to a SNAPSHOT version (**required — CI won't run otherwise**), from the repo root:
```bash
./set-version.sh 1.199.1-SNAPSHOT
```
Push the changes to the release branch. (You may run these git steps yourself once the human
gives you the SHAs, or hand them to the human — either way, confirm the branch state before moving on.)

### 1.3 Create the Jira fix version and assign every cherry-picked ticket
A dedicated fix version is the ONLY record of what shipped in the patch — it is not automated.
1. **Request the fix version** be created by someone with permission to create fix versions on
   the CLM project (post in **#sca-em** or ask a project admin). Use the *Fix-version request*
   template. If you lack permission, RULE 2 — hand it to the human.
2. Name it `selfhosted-<release>.<patch>` (e.g. `selfhosted-205.3`, `selfhosted-199.1`).
3. **Assign every cherry-picked ticket** to it — the fixes AND any dependent tickets whose
   commits were cherry-picked.
4. **Double-check** the fix version lists exactly the tickets whose commits are on the branch.

### 1.4 Inform the Docs team
Once cherry-picks are finalized and the build is expected to succeed, notify **@sca-docs** in
**#releases-sca-core** so they can prepare patch doc updates. Use the *Docs heads-up* template.

### 1.5 Run the full branch build (Feature Snapshots)
1. Go to [Feature Snapshots](https://jenkins.ci.sonatype.dev/job/insight/job/insight-brain/job/feature-snapshots/).
2. Find the patch branch (e.g. `release-1.199.1`).
3. **Wait for the build to PASS.** This is critical — do not proceed until green.
   Triggering/monitoring Jenkins is a human action → RULE 2 (ask the human to run it and report the result).

**If the build fails and the patch is abandoned**, don't leave the Jira fix version (Step 1.3)
orphaned. Ask the human (RULE 2) which applies: **keep** `selfhosted-<release>.<patch>` if the same
patch number will be retried; or, if the patch number is dropped, **unassign the tickets and
archive/delete** the fix version so Jira doesn't show an empty/false patch.

---

## Phase 2 — Build aggregate release

Follow "Step 2: Build Aggregate Release" in the IQ Release Process Template. Key points for patches:

- Use a **patch** version number (e.g. `1.199.1`), NOT a minor (`1.199.0`).
  - If `1.199.0` shipped → use `1.199.1`; if `1.199.1` shipped → `1.199.2`.
- **Verify the version does not already exist** in the artifact repositories first.
- Run [aggregate-release](https://jenkins.ci.sonatype.dev/job/insight/job/insight-brain/job/aggregate-release/) with:
  - `BRANCH`: your release branch (e.g. `release-1.199.1`)
  - other parameters per the template.

Running this Jenkins job is a human action → RULE 2.

---

## Phase 3 — Release IQ Server

Follow "Phase 3: Release IQ Server" in the template, running
[Release IQ Server](https://jenkins.ci.sonatype.dev/job/insight/job/insight-brain/job/Release%20IQ%20Server/)
with the patch **skip parameters** below.

### Required skip parameters for patch releases
| Parameter | Value | Reason |
|---|---|---|
| `skipMarkJiraVersionReleased` | **Conditional** | `true` for an **older** line (Jira tracks the latest release); `false` when the patch **is** the latest line |
| `skipCreateProductNotification` | `true` | No in-product notification |
| `skipDeployNotificationToStaging` | `true` | No notification to deploy |
| `skipDeployNotificationToProduction` | `true` | No notification to deploy |
| `skipPostReleaseBuild` | `true` | Skip Helm charts and post-release Jira issues |
| `skipMarkAsLatest` | **Conditional** | `true` for an **older** line (don't move the Docker `latest` tag backward); `false` when the patch **is** the latest line (it should become `latest`) |
| `skipPublishCloudFormation` | `true` | **Required (template):** CloudFormation publishing has been failing; prevents build failure |

Optional (up to you): `skipReleaseStartingNotification`, `skipReleaseCompleteNotifications`
(only send Slack/email), and "Send Policy Results" (keep for visibility).

### Stage summary (what runs vs. skips)
| Stage | Action |
|---|---|
| Confirm Start | ✅ Keep — manual confirmation |
| Send Release Starting Notification | ❓ Optional |
| Validate Release Binaries | ✅ Keep — validates version match + build status |
| Mark Jira Version as Released | ⛔ Skip for an older line; ✅ keep when the patch is the latest line |
| Deploy Binaries to Staging | ✅ Keep |
| Deploy Binaries to Production | ✅ Keep |
| Create Product Notification | ⛔ Skip |
| Deploy Notification to Staging/Production | ⛔ Skip |
| Send Policy Results | ❓ Optional |
| Publish Slim / Alpine / RedHat / Standard Docker Image | ✅ Keep — first confirm the images have no blocking policy violations (see note below) |
| Send Release Complete Notifications | ❓ Optional |
| Trigger Post-Release Build | ⛔ Skip |
| Mark as latest | ⛔ Not applied for an older line (`skipMarkAsLatest=true`); ✅ applied when the patch is the latest line |

**Validate image policy before publishing:** before the Docker publish stages, confirm the images
have no blocking policy violations — unresolved violations can block the publish pipelines from
running. Resolve or waive them first.

Docker behavior: images always publish with the specific version tag (e.g. `1.199.1`). The `latest`
tag is moved to this patch **only when it targets the current latest line**; for an older line
(`skipMarkAsLatest=true`) `latest` is left untouched. Customers can always pull the exact version:
`docker pull sonatype/nexus-iq-server:1.199.1`.

Running Release IQ Server is a human action → RULE 2.

---

## Verification & completion

After the job completes, walk the human through
[references/verification-checklist.md](references/verification-checklist.md).

If notifications weren't skipped (or if the team wants visibility), post the *Release complete*
message in **#releases-sca-core** (see templates). If Slack is unavailable → RULE 2.

---

## Reference links
- Patch process (source of truth): https://sonatype.atlassian.net/wiki/spaces/SP/pages/2243264521
- IQ Release Process Template: https://sonatype.atlassian.net/wiki/spaces/SP/pages/1979908103
- Feature Snapshots: https://jenkins.ci.sonatype.dev/job/insight/job/insight-brain/job/feature-snapshots/
- aggregate-release: https://jenkins.ci.sonatype.dev/job/insight/job/insight-brain/job/aggregate-release/
- Release IQ Server: https://jenkins.ci.sonatype.dev/job/insight/job/insight-brain/job/Release%20IQ%20Server/
- Aggregate Release docs: https://sonatype.atlassian.net/browse/CLM-38593

## FAQ
- **When do I set `skipMarkJiraVersionReleased`?** `true` for a patch of an **older** line (Jira
  tracks the latest release, so marking an old patch "released" is confusing); `false` when the
  patch **is** the latest line — then mark it released normally.
- **When do I set `skipMarkAsLatest`?** `true` for a patch of an **older** line, so the Docker
  `latest` tag isn't moved backward onto an old release; `false` when the patch targets the
  **current latest** line — then it should become `latest`.
- **Can I skip the notification stages?** Yes — `skipReleaseStartingNotification` /
  `skipReleaseCompleteNotifications` only send Slack/email. Skip for a quiet patch, keep for visibility.
- **Multiple patches?** Increment the patch number (`1.199.1`, `1.199.2`, …); each gets its own
  release branch and its own Jira fix version (`selfhosted-199.1`, `selfhosted-199.2`, …).
