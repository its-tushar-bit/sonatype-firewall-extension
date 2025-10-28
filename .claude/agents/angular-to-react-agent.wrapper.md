## Task Wrapper Template (for humans)

- **Audience:** Engineers providing the `angular-to-react-agent` with existing Jira tasks for execution.
- **Usage:** Copy the empty template below, fill it out with your Jira ticket details, then paste the completed version into Claude.
  - Alternatively, **if you have configured the Atlassian MCP**, you can skip this and tell Claude to lookup the task by key/id

### Empty template

```
use angular-to-react-agent

[Jira Task]
<task key> — <task title>

[Business context]
* (why this matters / blocks or unblocks other tasks)

[Acceptance criteria from Jira]
* ...

[Known constraints]
* ...

[Artifacts I can provide on request]
* (screenshots of working UI states)

[Please respond using the agent’s Output format.]
```

### Example completed template

```
use angular-to-react-agent

[Jira Task]
CLM-35194 — Refactor userSession.js to store its state in redux

[Business context]
* A previous migration tasks migrated an old service to userSession and used a static promise to store the logic state. Ideally this would instead be stored in redux and queried from the store like any other redux property.

[Acceptance criteria from Jira]
* userSession state is stored in redux and loaded from the redux store everywhere it's needed

[Artifacts I can provide on request]
* Commit hash of previous related refactor

[Please respond using the agent’s Output format.]
```

### Example if using Atlassian MCP

```
use angular-to-react-agent to implement https://sonatype.atlassian.net/browse/CLM-35194
```
