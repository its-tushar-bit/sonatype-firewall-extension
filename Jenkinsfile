/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
@Library(['private-pipeline-library', 'jenkins-shared', 'iq-pipeline-library']) _

/*
 * Pipeline Dispatcher
 *
 * This Jenkinsfile routes builds to the appropriate pipeline based on branch type or build mode:
 *
 * Jenkinsfile.main - Used for:
 *   - main branch: Full build with deployment, artifact publishing, and comprehensive testing
 *   - release branches: Release builds with signing and distribution
 *   - Any branch with buildMode=MAIN (via Build With Parameters)
 *
 * Jenkinsfile.feature - Used for:
 *   - feature branches: Fast validation builds with build caching and selective testing
 *   - merge queue (gh-readonly-queue/*): Pre-merge validation before auto-merge to main
 *
 * The buildMode parameter is defined in each sub-pipeline's configureBranchJob().
 * On first run, it defaults based on branch type. Subsequent runs use the selected value.
 */
node {
  checkout scm

  def pipelineScript
  def branchName = env.BRANCH_NAME ?: gitBranch(env)

  // Determine if this is a main/release branch (always uses Jenkinsfile.main)
  def isMainOrReleaseBranch = isDeployBranch(env, 'main') ||
      currentBuild.fullProjectName.contains('insight-brain/release') ||
      branchName.startsWith('release')

  // For feature branches, check the buildMode parameter
  // On first run, params.buildMode will be null, so default to FEATURE
  def effectiveBuildMode = params.buildMode ?: (isMainOrReleaseBranch ? 'MAIN' : 'FEATURE')

  if (effectiveBuildMode == 'MAIN' || isMainOrReleaseBranch) {
    pipelineScript = 'Jenkinsfile.main'
  } else {
    pipelineScript = 'Jenkinsfile.feature'
  }

  echo "Loading pipeline: ${pipelineScript} for branch: ${branchName} (buildMode: ${effectiveBuildMode})"
  load(pipelineScript)
}
