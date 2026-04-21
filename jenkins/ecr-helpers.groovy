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

// Staging ECR Account — placeholder until staging infrastructure is ready
// TODO: Replace with actual staging account ID and credential ID when infrastructure is available
@Field final Map STAGING_ECR_ACCOUNT = [
    id: '<PLACEHOLDER-staging-account-id>',
    credentialId: '<PLACEHOLDER-staging-credential-id>'
]

@Field final String ECR_REPOSITORY = 'sca-cloud/mtiq-server'

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
        // ECR_REGION and ecrRegistry are derived from constants, not user-controlled
        sh(script: "aws ecr get-login-password --region '${ECR_REGION}' | docker login --username AWS --password-stdin '${ecrRegistry}'")

        // Use withEnv to pass values safely to shell (prevents shell injection from branch name)
        withEnv(["ECR_IMAGE_REF=${ecrImageRef}", "SONATYPE_REGISTRY=${sonatypeDockerRegistryId()}", "IQ_VERSION=${iqVersion}"]) {
          sh '''
            docker buildx build --platform=linux/amd64,linux/arm64 \
              --build-arg SONATYPE_PRIVATE_REGISTRY="$SONATYPE_REGISTRY" \
              --build-arg IQ_SERVER_VERSION="$IQ_VERSION" \
              --push \
              --tag "$ECR_IMAGE_REF" .
          '''
        }

        // Add extra tag: branch-latest (via ECR API)
        // ImageAlreadyExistsException is tolerated (tag already points to this image),
        // but other errors fail the build to avoid silent issues.
        final String latestTag = "${branch}-latest"
        echo "Adding ECR tag: ${latestTag}"
        // Use withEnv to pass user-controlled values safely to shell (prevents shell injection)
        def output
        withEnv(["REPOSITORY_ARG=${ecrRepo}", "IMAGE_TAG_ARG=${ecrTag}", "LATEST_TAG_ARG=${latestTag}", "REGION_ARG=${ECR_REGION}"]) {
          output = sh(script: '''
            MANIFEST=$(aws ecr batch-get-image \
              --repository-name "$REPOSITORY_ARG" \
              --image-ids "imageTag=$IMAGE_TAG_ARG" \
              --region "$REGION_ARG" \
              --output text \
              --query 'images[0].imageManifest')
            aws ecr put-image \
              --repository-name "$REPOSITORY_ARG" \
              --image-tag "$LATEST_TAG_ARG" \
              --image-manifest "$MANIFEST" \
              --region "$REGION_ARG" \
              2>&1 || true
          ''', returnStdout: true).trim()
        }
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

/**
 * Validates that an image exists in ECR.
 *
 * @param account Account configuration map with 'id' and 'credentialId'
 * @param repository ECR repository name
 * @param imageTag Image tag to validate
 * @param region AWS region
 */
void validateImageExists(Map account, String repository, String imageTag, String region = ECR_REGION) {
  withAwsRole(credentialsId: account.credentialId, role: 'jenkins',
      roleAccount: account.id, region: region) {
    // Use withEnv to pass user-controlled values safely to shell (prevents shell injection)
    withEnv(["REPOSITORY_ARG=${repository}", "IMAGE_TAG_ARG=${imageTag}", "REGION_ARG=${region}"]) {
      def result = sh(script: '''
        aws ecr describe-images \
          --repository-name "$REPOSITORY_ARG" \
          --image-ids "imageTag=$IMAGE_TAG_ARG" \
          --region "$REGION_ARG" \
          --output json
      ''', returnStatus: true)

      if (result != 0) {
        error "Image '${imageTag}' not found in ${repository} (account: ${account.id})"
      }
      echo "Validated image exists: ${imageTag}"
    }
  }
}

/**
 * Retrieves the image manifest from ECR.
 * Works for both single-architecture and multi-architecture (manifest list) images.
 *
 * @param account Account configuration map with 'id' and 'credentialId'
 * @param repository ECR repository name
 * @param imageTag Image tag to retrieve manifest for
 * @param region AWS region
 * @return Image manifest JSON string
 */
String getImageManifest(Map account, String repository, String imageTag, String region = ECR_REGION) {
  def result = null
  withAwsRole(credentialsId: account.credentialId, role: 'jenkins',
      roleAccount: account.id, region: region) {
    // Use withEnv to pass user-controlled values safely to shell (prevents shell injection)
    withEnv(["REPOSITORY_ARG=${repository}", "IMAGE_TAG_ARG=${imageTag}", "REGION_ARG=${region}"]) {
      def manifest = sh(script: '''
        aws ecr batch-get-image \
          --repository-name "$REPOSITORY_ARG" \
          --image-ids "imageTag=$IMAGE_TAG_ARG" \
          --region "$REGION_ARG" \
          --output text \
          --query 'images[0].imageManifest'
      ''', returnStdout: true).trim()

      if (!manifest || manifest == 'None') {
        error "Failed to retrieve manifest for '${imageTag}'"
      }
      echo "Retrieved manifest for ${imageTag} (${manifest.length()} chars)"
      result = manifest
    }
  }
  return result
}

/**
 * Pushes an image manifest to ECR, creating or updating a tag.
 *
 * @param account Account configuration map with 'id' and 'credentialId'
 * @param repository ECR repository name
 * @param imageTag Tag to apply to the image
 * @param manifest Image manifest JSON string
 * @param region AWS region
 */
void putImageManifest(Map account, String repository, String imageTag, String manifest, String region = ECR_REGION) {
  withAwsRole(credentialsId: account.credentialId, role: 'jenkins',
      roleAccount: account.id, region: region) {
    // Write manifest to temp file to avoid shell escaping issues with large JSON
    writeFile file: 'image-manifest.json', text: manifest

    // Use withEnv to pass user-controlled values safely to shell (prevents shell injection)
    withEnv(["REPOSITORY_ARG=${repository}", "IMAGE_TAG_ARG=${imageTag}", "REGION_ARG=${region}"]) {
      sh '''
        aws ecr put-image \
          --repository-name "$REPOSITORY_ARG" \
          --image-tag "$IMAGE_TAG_ARG" \
          --image-manifest 'file://image-manifest.json' \
          --region "$REGION_ARG"
      '''
    }

    echo "Pushed manifest with tag: ${imageTag}"
  }
}

/**
 * Promotes an image from one ECR account to another via manifest copy.
 * This is a cross-account operation that does NOT require Docker.
 * Preserves multi-architecture images (manifest lists).
 *
 * @param sourceAccount Source account configuration map
 * @param targetAccount Target account configuration map
 * @param repository ECR repository name (must exist in both accounts)
 * @param sourceTag Source image tag
 * @param additionalTags Optional additional tags to apply in target (e.g., 'staging-latest')
 * @param region AWS region
 */
void promoteImage(Map sourceAccount, Map targetAccount, String repository, String sourceTag,
    List<String> additionalTags = [], String region = ECR_REGION) {

  echo "Promoting image ${sourceTag} from account ${sourceAccount.id} to ${targetAccount.id}"

  // Step 1: Validate image exists in source
  validateImageExists(sourceAccount, repository, sourceTag, region)

  // Step 2: Get manifest from source
  def manifest = getImageManifest(sourceAccount, repository, sourceTag, region)

  // Step 3: Push manifest to target with original tag
  putImageManifest(targetAccount, repository, sourceTag, manifest, region)

  // Step 4: Apply additional tags if specified
  for (String tag : additionalTags) {
    putImageManifest(targetAccount, repository, tag, manifest, region)
  }

  echo "Successfully promoted ${sourceTag} to account ${targetAccount.id} with tags: ${[sourceTag] + additionalTags}"
}

return this
