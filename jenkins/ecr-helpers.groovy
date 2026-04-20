/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * ECR push helper for MTIQ Docker images (cached variant).
 *
 * Must be called AFTER pushMTIQDockerImage() on the same agent — reuses the
 * active buildx builder so all layers are cached and this is a push-only operation.
 *
 * Used by Jenkinsfile.main and Jenkinsfile.feature via:
 *   def ecr = load 'jenkins/ecr-helpers.groovy'
 *   ecr.pushToEcrCached()
 *
 * Returns the ECR tag on success.
 *
 * ── ECR tag format ──────────────────────────────────────────────────────────────────────────────
 * Tags follow the pattern:  {dateSection}-{branch}-{buildNumber}-{commitId}
 *   dateSection — UTC timestamp in yyyyMMddHHmm format (e.g., 202604161432)
 *   branch      — branch name, slashes replaced with underscores, truncated to 95 chars
 *   buildNumber — Jenkins BUILD_NUMBER
 *   commitId    — first 8 chars of GIT_COMMIT
 * Example: 202604161432-main-142-a1b2c3d4
 *
 * The date prefix provides chronological ordering and human-readable build time identification,
 * matching the format used for RSC image tags in Jenkinsfile.main's pushMTIQDockerImage().
 *
 * ── Tag length constraints ──────────────────────────────────────────────────────────────────────
 * Docker/OCI specification limits tags to 128 characters. With the format above:
 *   - dateSection: 12 chars
 *   - separators:  3 chars (hyphens)
 *   - buildNumber: up to 6 chars
 *   - commitId:    8 chars
 *   - Total fixed: 29 chars
 *   - Available for branch: 99 chars (128 - 29)
 *
 * Branch is truncated to 95 chars to provide a 4-char safety margin.
 *
 * ── Additional mutable tag ───────────────────────────────────────────────────────────────────────
 * A second mutable tag is also applied via the ECR API: {branch}-latest
 * This points to the most recent image for that branch.
 * ImageAlreadyExistsException is tolerated, other errors fail the build.
 *
 * ── branchOverride vs gitBranch(env) ────────────────────────────────────────────────────────────
 * The optional branchOverride parameter controls the branch segment of the tag:
 *   - Omitted (Jenkinsfile.feature):         gitBranch(env) is used — the branch currently
 *                                             building in Jenkins.
 *   - Supplied (Jenkinsfile.main):            the caller-provided value is used (e.g., 'main').
 */
import groovy.transform.Field

@Field final String ECR_REGION = 'us-east-2'
@Field final Map ECR_ACCOUNT = [id: '468017555316', credentialId: 'cloud-native-dev-external-id']

String pushToEcrCached(String branchOverride = null) {
  if (!env.GIT_COMMIT) {
    error 'env.GIT_COMMIT is not set — ensure the workspace was checked out before calling pushToEcrCached()'
  }
  final String commitId = env.GIT_COMMIT.take(8)
  final String rawBranch = branchOverride ?: gitBranch(env)
  if (!rawBranch) {
    error 'Could not determine branch name: branchOverride is null and gitBranch(env) returned null. ' +
          'Pass branchOverride explicitly or ensure env.GIT_BRANCH / env.BRANCH_NAME is set.'
  }
  // Truncate branch to 95 chars to stay under Docker's 128-char tag limit with date prefix format
  // See file header comment for tag length calculation breakdown
  final String branch = rawBranch.replace('/', '_').take(95)
  final String dateSection = new Date().format("yyyyMMddHHmm", TimeZone.getTimeZone('UTC'))
  final String ecrTag = "${dateSection}-${branch}-${env.BUILD_NUMBER}-${commitId}"
  final String ecrRegistry = "${ECR_ACCOUNT.id}.dkr.ecr.${ECR_REGION}.amazonaws.com"
  final String ecrRepo = "sca-cloud/mtiq-server"
  final String ecrImageRef = "${ecrRegistry}/${ecrRepo}:${ecrTag}"
  final String iqVersion = getMavenProjectVersion('.')

  echo "Pushing to ECR (cached): ${ecrImageRef}"

  dir("nexus-mtiq-server") {
    withSonatypeDockerRegistry() {
      withAwsRole(credentialsId: ECR_ACCOUNT.credentialId, role: 'jenkins',
                  roleAccount: ECR_ACCOUNT.id, region: ECR_REGION) {
        // Authenticate to ECR - pipe directly to avoid logging password
        sh(script: "aws ecr get-login-password --region '${ECR_REGION}' | docker login --username AWS --password-stdin '${ecrRegistry}'")

        sh "docker buildx build --platform=linux/amd64,linux/arm64 " +
            " --build-arg SONATYPE_PRIVATE_REGISTRY=${sonatypeDockerRegistryId()} " +
            " --build-arg IQ_SERVER_VERSION=${iqVersion} " +
            " --push " +
            " --tag '${ecrImageRef}' ."

        // Add extra tag: branch-latest (via ECR API)
        // ImageAlreadyExistsException is tolerated (tag already points to this image),
        // but other errors fail the build to avoid silent issues.
        final String latestTag = "${branch}-latest"
        echo "Adding ECR tag: ${latestTag}"
        def output = sh(script: """\
          MANIFEST=\$(aws ecr batch-get-image \
            --repository-name '${ecrRepo}' \
            --image-ids imageTag='${ecrTag}' \
            --region '${ECR_REGION}' \
            --output text \
            --query 'images[0].imageManifest')
          aws ecr put-image \
            --repository-name '${ecrRepo}' \
            --image-tag '${latestTag}' \
            --image-manifest "\$MANIFEST" \
            --region '${ECR_REGION}' \
            2>&1 || true
        """, returnStdout: true).trim()
        if (output.contains('ImageAlreadyExistsException')) {
          echo "Tag '${latestTag}' already points to this image, skipping"
        } else if (output.contains('Error') || output.contains('error') || output.contains('denied')) {
          error "Failed to apply ECR tag '${latestTag}': ${output}"
        }

        echo "ECR push successful: ${ecrImageRef} (+ ${latestTag})"
      }
    }
  }

  return ecrTag
}

return this
