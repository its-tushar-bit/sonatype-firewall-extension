# Pull Request Process

This document outlines the standard process for creating and merging pull requests in the insight-brain project.

## 1. Create a Branch

- Branch from `main` with the ticket ID as a prefix followed by a descriptive name
- Example: `CLM-36377_optimize_sbom_export_process`

```bash
git checkout main
git pull
git checkout -b CLM-36377_optimize_sbom_export_process
```

## 2. Open a Pull Request

- Write a clear description that includes:
  - What problem you're solving
  - Testing steps (if applicable)
- Follow the PR template guidelines
- Ensure the first line of the PR description links to the Jira ticket:
  ```
  Jira: https://sonatype.atlassian.net/browse/CLM-36377
  ```

## 3. Run a Full Build

Before requesting review, you must run a full build with functional tests enabled:

1. Go to [Feature Snapshots](https://jenkins.ci.sonatype.dev/job/insight/job/insight-brain/job/feature-snapshots/) on Jenkins
2. Select your specific branch
3. Click **Build with Parameters**
4. Enable the `functionalTestsEnabled` option
5. Click **Build**

See the [Full Build documentation](full-build.md) for more details on why this is required.

## 4. Code Review

- Wait for review feedback from team members
- Address any comments or requested changes
- Push updates to your branch

## 5. Testing

- After code review approval, move the Jira ticket to the **IN TEST** column
- Team members will validate the changes following the testing steps provided in the PR description

## 6. Merge

- Once testing confirms everything looks good, merge the PR to `main`
- Move the Jira ticket to **Approval** if you want the Product Owner to validate and test the changes (this is optional)

## 7. Handling Conflicts

When resolving conflicts with the main branch:
- **Merging** is preferred over rebasing
- Merge `main` into your feature branch to resolve conflicts

```bash
git checkout your-feature-branch
git merge main
# Resolve conflicts
git commit
git push
```

## Best Practices

- Keep PRs focused and reasonably sized
- Ensure all CI checks pass before requesting review
- Respond to review comments promptly
- Keep your branch up to date with `main`
