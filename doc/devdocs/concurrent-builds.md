# Disabling Concurrent Builds for Feature Branches

## Overview
Concurrent builds have been disabled by default for feature branches in `insight-brain`.
This change ensures that only one build runs at a time per feature branch,
preventing unnecessary parallel executions. However, developers can override this
setting for their own branches if needed.

## How to Enable Concurrent Builds for Your Branch
If you want to allow concurrent builds for your feature branch,
you need to modify the `configureBranchJob()` method in your branch's `Jenkinsfile`.

### Steps to Override:
1. Locate the following line in `configureBranchJob()`:
   ```groovy
   if (!projName.toLowerCase().contains('master-snapshot')) {
       propertyList.add(disableConcurrentBuilds(abortPrevious: true))
   }
   ```
2. Replace `master-snapshot` with your branch name:
   ```groovy
   if (!projName.toLowerCase().contains('<your-branch-name>')) {
       propertyList.add(disableConcurrentBuilds(abortPrevious: true))
   }
   ```
   Replace `<your-branch-name>` with the name of your branch.

3. **Reminder:** Before merging your branch into `main`,
   revert this change to ensure it does not affect future builds.

4. **Note:** At least one build must start in order to override the default behavior.
