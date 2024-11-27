<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->
# GitHub Merge Queue

Sonatype utilizes the GitHub merge queue to manage the merging of pull requests in a controlled and efficient manner.

---

## Why Use a Merge Queue?

The merge queue offers several advantages over the alternative, such as enabling the "Require branches to be up-to-date
before merging" rule. Without a merge queue, developers must manually merge `main` into their branches and wait for
status checks, which can be time-consuming and error-prone.

### Benefits

1. **Maintains a Stable `main` Branch**  
   Ensures outdated pull requests are never merged and that all changes compile successfully with the latest code in
   `main`.

2. **Saves Developer Time**  
   Developers no longer need to manually rebase their feature branches on `main` before merging.

3. **Supports Additional Checks**
    - The merge queue enables running additional checks that may have a low failure rate.
    - Running these checks on every commit would slow feedback loops, while running them only on `main` could disrupt
      the whole team due to failures.
    - The merge queue strikes a balance by running such checks in a targeted manner.

4. **Integrates with "Dismiss Stale Reviews"**
    - To ensure every commit to `main` has been reviewed, the "Dismiss stale reviews" feature automatically removes
      approvals when new commits are pushed to a branch.
    - Without the merge queue, merges from `main` into feature branches would dismiss approvals, creating unnecessary
      churn.

---

## Configuration

The merge queue offers several configurable options to tailor its behavior to team needs:

- **Build Concurrency**  
  Limits the number of queued pull requests requesting checks and workflow runs simultaneously.  
  _Set to: 5_

- **Minimum Group Size**  
  The minimum number of pull requests to be merged together in a group.  
  _Set to: 1_

- **Maximum Group Size**  
  The maximum number of pull requests to be merged together in a group.  
  _Set to: 5_

- **Wait Time for Minimum Group Size (minutes)**  
  Time to wait after the first PR is added to the queue for the minimum group size to be met. After this time, smaller
  groups can proceed.  
  _Set to: 5_

- **Require All Queue Entries to Pass Required Checks**  
  Ensures all queued PRs pass required checks before merging. When disabled, only the head commit of the group needs to
  pass.  
  _Set to: Enabled_

- **Status Check Timeout (minutes)**  
  Maximum time for a required status check to report a conclusion. Unreported checks after this time will be considered
  failed.  
  _Set to: 60_

---

## Investigating Failures

If a merged PR fails, follow these steps to identify the cause:

1. Check the PR timeline for merge queue events. It will display the reason for failure and provide a link to the
   Jenkins build.
2. Alternatively, view the list
   of [Jenkins feature branch builds](https://jenkins.ci.sonatype.dev/job/insight/job/insight-brain/job/feature-snapshots/).
    - Merge queue branches are named `pr-<PR number>`, and there will be a corresponding build job for each.

---

## Adding Additional Checks

Merge queue builds use the same `Jenkinsfile` as feature branch and `main` builds. To control which checks are run on
the merge queue, use the `isMergeQueueBranch()` method in your pipeline configuration.

```groovy
if (isMergeQueueBranch()) {
    // Add specific checks here
}
