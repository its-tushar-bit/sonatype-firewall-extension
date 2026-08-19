# Patch release verification checklist

Walk the human through this after the *Release IQ Server* job completes. Each item is a human
verification — if you cannot confirm it yourself, apply **RULE 2** and ask the human to check
and confirm before marking it done.

- [ ] **Binaries** are available in the **staging and production** repositories.
- [ ] **Docker images** are published (check Docker Hub).
- [ ] Images carry the **correct version tag** (e.g. `1.199.1`).
- [ ] **`latest` tag** is correct for this patch: for an **older** line it must **NOT** move to this
      patch; for a patch of the **current latest** line it **should** now point here.
- [ ] **No in-product notification** was created.
- [ ] **No post-release Jira issues** were created.
- [ ] **Jira version released?** For an **older** line the version is **NOT** marked released; for a
      patch of the **latest** line it **is** marked released.
- [ ] The **Jira fix version** (e.g. `selfhosted-205.3`) exists and lists **every** cherry-picked ticket.

## Quick spot-check
```bash
# Version tag exists and is pullable:
docker pull sonatype/nexus-iq-server:{{patchVersion}}
# For an OLDER line: confirm `latest` was NOT moved to this patch (it still points at the newest stable release).
# For a patch of the CURRENT latest line: confirm `latest` now points at this patch.
```

If any item fails, STOP and escalate in **#releases-sca-core** (see
[slack-notifications.md](slack-notifications.md)); do not consider the patch done.
