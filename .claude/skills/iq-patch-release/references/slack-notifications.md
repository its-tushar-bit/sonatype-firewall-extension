# Slack notifications for a patch release

This file backs **RULE 1 (notify at kickoff)** and **RULE 2 (pause & ask)** in `../SKILL.md`.

## Primary channel
- **#releases-sca-core** — `CTBAFJDQD` — "Coordination of IQ Server Releases". This is where the
  kickoff, docs heads-up, and (optional) completion notifications go.

## Groups to tag
| Group | Mention syntax to post | Subteam ID | Confidence | Used for |
|---|---|---|---|---|
| `@sca-em` (Engineering Managers) | `<!subteam^SD1JQS6QG>` | `SD1JQS6QG` | **High** (seen in the exact release-notification sentence others type as `@sca-em`) | Patch inclusion **approval**; Jira fix-version requests |
| `@sca-docs` (Docs / tech writers) | `<!subteam^S06A0ERB76F>` | `S06A0ERB76F` | **Medium — verify** (paired with `@sca-em` for "release notes ready for review") | Prepare patch doc / release-note updates |
| `@sca-segment-tech-leads` | *(ID not resolved — type `@sca-segment-tech-leads` and verify it linkifies)* | unknown | Low | Escalation / "need help" |

> To actually **notify** a user group via the API you must use the `<!subteam^ID>` syntax —
> a plain-text `@sca-em` does NOT ping the group. Use the IDs above. If an ID is unknown or
> you are not confident it resolves, **do not silently post a dead mention** — apply RULE 2:
> hand the drafted message to the human and ask them to post/verify the correct group is tagged.

## Other channels referenced by the process (may be private / unresolvable by the bot)
- **#sca-em** — EM approvals and Jira fix-version requests. (The release token could not resolve
  this channel — if you can't post, RULE 2: ask the human to post there.)
- **#iq-releases** — where template bugs are reported (tag `@sca-em`, `@sca-segment-tech-leads`).

---

## MANDATORY pre-flight before any Slack post
1. Run `slack_whoami`. If it errors → **RULE 2**: print the drafted message + target channel +
   who to tag, ask the human to post it, and continue only after they confirm.
2. Post with `slack_post_message` to the channel, using `<!subteam^ID>` mentions from the table.
3. If the post fails, a scope is missing, or a required group ID is unknown/unverified → **RULE 2**.
4. After a successful post, share the returned permalink with the human.

---

## Message templates
Fill the `{{…}}` placeholders. Keep them short; post in `#releases-sca-core` unless noted.

### 1. Kickoff / EM approval request  *(Phase 0)*
```
<!subteam^SD1JQS6QG> <!subteam^S06A0ERB76F> :rocket: Starting an IQ Server *patch release*.

• Version being patched: {{fromVersion}}  →  new patch: *{{patchVersion}}*
• Release branch: `release-{{patchVersion}}`
• Fixes / tickets: {{JIRA-1}}, {{JIRA-2}}
• Jira fix version: `selfhosted-{{release}}.{{patch}}`

@sca-em — please *approve* including these fixes in the patch. 🙏
@sca-docs — heads-up so you can prep patch doc updates once the build is green.
```

### 2. Docs heads-up  *(Step 1.4 — after cherry-picks finalized & build expected to pass)*
```
<!subteam^S06A0ERB76F> Cherry-picks for *{{patchVersion}}* are finalized on `release-{{patchVersion}}`
and we expect the build to pass. Please start prepping the patch doc / release-note updates.
Tickets in this patch: {{JIRA-1}}, {{JIRA-2}} (Jira fix version `selfhosted-{{release}}.{{patch}}`).
```

### 3. Jira fix-version request  *(Step 1.3 — #sca-em or a CLM project admin)*
```
Could a CLM project admin please create the Jira fix version `selfhosted-{{release}}.{{patch}}`
for the IQ Server patch release {{patchVersion}}? I'll assign the cherry-picked tickets
({{JIRA-1}}, {{JIRA-2}} + dependents) to it. Thanks!
```

### 4. Release starting  *(optional — only if you did NOT set `skipReleaseStartingNotification`)*
Post exactly ONE of these, matching the latest-vs-older decision in `../SKILL.md`. Each block is
ready to post as-is — no in-place editing needed.

**4a — OLDER line**
```
<!here> Starting the *Release IQ Server* job for patch *{{patchVersion}}* from `release-{{patchVersion}}`.
Patch of an older line — will NOT be marked `latest` in Docker or "released" in Jira.
```
**4b — CURRENT LATEST line**
```
<!here> Starting the *Release IQ Server* job for patch *{{patchVersion}}* from `release-{{patchVersion}}`.
Patch of the current latest line — WILL be marked `latest` in Docker and "released" in Jira.
```

### 5. Release complete  *(optional / Verification)*
Post exactly ONE of these, matching the latest-vs-older decision. Each block is ready to post as-is.

**5a — OLDER line**
```
<!here> :white_check_mark: IQ Server patch *{{patchVersion}}* is released.
• Docker: `docker pull sonatype/nexus-iq-server:{{patchVersion}}` (specific version tag only — `latest` not moved)
• Binaries in staging + production
• Jira fix version `selfhosted-{{release}}.{{patch}}` lists all shipped tickets (version NOT marked "released" in Jira)
```
**5b — CURRENT LATEST line**
```
<!here> :white_check_mark: IQ Server patch *{{patchVersion}}* is released.
• Docker: `docker pull sonatype/nexus-iq-server:{{patchVersion}}` (also tagged `latest`)
• Binaries in staging + production
• Jira fix version `selfhosted-{{release}}.{{patch}}` lists all shipped tickets (version marked "released" in Jira)
```

---

## Maintenance note
The subteam IDs above were recovered from `#releases-sca-core` history, not from the
usergroups API (that scope was unavailable). If a mention doesn't ping the right group,
re-verify the ID (an admin can read it from `https://sonatype.slack.com/admin/user_groups`)
and update this table.
