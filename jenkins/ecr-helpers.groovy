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
@Field final Map DEV_ECR_ACCOUNT = [
    id: '468017555316',
    credentialId: 'cloud-native-dev-external-id',
    role: 'jenkins'
]

// Staging ECR Account
@Field final Map STAGING_ECR_ACCOUNT = [
    id: '460469849438',
    credentialId: 'aws-jenkins-role-sca',
    role: 'JenkinsCICDRole'
]

// Prod ECR Account — placeholder until prod infrastructure is ready
// TODO: Replace with actual prod account ID, credential ID, and role when infrastructure is available
@Field final Map PROD_ECR_ACCOUNT = [
    id: '<PLACEHOLDER-prod-account-id>',
    credentialId: '<PLACEHOLDER-prod-credential-id>',
    role: '<PLACEHOLDER-prod-role>'
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
  final String ecrRegistry = "${DEV_ECR_ACCOUNT.id}.dkr.ecr.${ECR_REGION}.amazonaws.com"
  final String ecrRepo = ECR_REPOSITORY
  final String ecrImageRef = "${ecrRegistry}/${ecrRepo}:${ecrTag}"
  final String iqVersion = getMavenProjectVersion('.')

  echo "Pushing to ECR (cached): ${ecrImageRef}"

  dir("nexus-mtiq-server") {
    withSonatypeDockerRegistry() {
      withAwsRole(credentialsId: DEV_ECR_ACCOUNT.credentialId, role: DEV_ECR_ACCOUNT.role,
                  roleAccount: DEV_ECR_ACCOUNT.id, region: ECR_REGION) {
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

        echo "ECR push successful: ${ecrImageRef}"
      }
    }
  }

  final String latestTag = "${branch}-latest"
  tagImageInEcr(DEV_ECR_ACCOUNT, ecrRepo, ecrTag, latestTag, ECR_REGION)
  echo "Tagged ${latestTag} in dev ECR"

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
  withAwsRole(credentialsId: account.credentialId, role: account.role,
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
  withAwsRole(credentialsId: account.credentialId, role: account.role,
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
  withAwsRole(credentialsId: account.credentialId, role: account.role,
      roleAccount: account.id, region: region) {
    // Write manifest to temp file to avoid shell escaping issues with large JSON
    writeFile file: 'image-manifest.json', text: manifest

    // Use withEnv to pass user-controlled values safely to shell (prevents shell injection)
    withEnv(["REPOSITORY_ARG=${repository}", "IMAGE_TAG_ARG=${imageTag}", "REGION_ARG=${region}"]) {
      def result = sh(script: '''
        aws ecr put-image \
          --repository-name "$REPOSITORY_ARG" \
          --image-tag "$IMAGE_TAG_ARG" \
          --image-manifest 'file://image-manifest.json' \
          --region "$REGION_ARG" \
          2>&1 || true
      ''', returnStdout: true).trim()

      if (result.contains('ImageAlreadyExistsException')) {
        echo "Tag '${imageTag}' already points to this image"
      } else if (result.contains('"image":')) {
        echo "Pushed manifest with tag: ${imageTag}"
      } else {
        error "Failed to push manifest for tag '${imageTag}': ${result}"
      }
    }
  }
}

/**
 * Applies an additional tag to an existing image in ECR.
 * Tolerates ImageAlreadyExistsException (tag already points to this image).
 * All other errors fail the build.
 *
 * @param account Account configuration map
 * @param repository ECR repository name
 * @param sourceTag Existing image tag to copy from
 * @param newTag New tag to apply
 * @param region AWS region
 */
void tagImageInEcr(Map account, String repository, String sourceTag, String newTag, String region = ECR_REGION) {
  def manifest = getImageManifest(account, repository, sourceTag, region)

  withAwsRole(credentialsId: account.credentialId, role: account.role,
      roleAccount: account.id, region: region) {
    writeFile file: 'image-manifest.json', text: manifest

    withEnv(["REPOSITORY_ARG=${repository}", "NEW_TAG_ARG=${newTag}", "REGION_ARG=${region}"]) {
      def result = sh(script: '''
        aws ecr put-image \
          --repository-name "$REPOSITORY_ARG" \
          --image-tag "$NEW_TAG_ARG" \
          --image-manifest 'file://image-manifest.json' \
          --region "$REGION_ARG" \
          2>&1 || true
      ''', returnStdout: true).trim()

      if (result.contains('ImageAlreadyExistsException')) {
        echo "Tag '${newTag}' already points to this image, skipping"
      } else if (result.contains('"image":')) {
        echo "Applied tag: ${newTag}"
      } else {
        error "Failed to apply tag '${newTag}': ${result}"
      }
    }
  }
}

/**
 * Authenticates Docker to an ECR registry.
 * The authentication token persists in ~/.docker/config.json for subsequent operations.
 *
 * @param account Account configuration map with 'id', 'credentialId', and 'role'
 * @param region AWS region
 */
void authenticateToEcr(Map account, String region = ECR_REGION) {
  final String registry = "${account.id}.dkr.ecr.${region}.amazonaws.com"
  withAwsRole(credentialsId: account.credentialId, role: account.role,
      roleAccount: account.id, region: region) {
    sh "aws ecr get-login-password --region '${region}' | docker login --username AWS --password-stdin '${registry}'"
  }
}

/**
 * Promotes an image from one ECR account to another.
 * Uses `docker buildx imagetools create` to copy all layers and multi-architecture
 * manifests between registries. Requires Docker CLI with buildx (daemon not needed).
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

  final String sourceRegistry = "${sourceAccount.id}.dkr.ecr.${region}.amazonaws.com"
  final String targetRegistry = "${targetAccount.id}.dkr.ecr.${region}.amazonaws.com"

  echo "Promoting image ${sourceTag} from ${sourceRegistry} to ${targetRegistry}"

  validateImageExists(sourceAccount, repository, sourceTag, region)

  // Authenticate Docker to both registries (ECR tokens persist in ~/.docker/config.json)
  authenticateToEcr(sourceAccount, region)
  authenticateToEcr(targetAccount, region)

  // Copy image with all architectures and layers (requires Docker CLI with buildx, not the daemon)
  withEnv(["SOURCE_IMAGE=${sourceRegistry}/${repository}:${sourceTag}",
           "TARGET_IMAGE=${targetRegistry}/${repository}:${sourceTag}"]) {
    sh 'docker buildx imagetools create --tag "$TARGET_IMAGE" "$SOURCE_IMAGE"'
  }

  // Apply additional tags in target (layers now exist from the copy above)
  for (String tag : additionalTags) {
    tagImageInEcr(targetAccount, repository, sourceTag, tag, region)
  }

  echo "Promoted ${sourceTag} to ${targetRegistry} with tags: ${[sourceTag] + additionalTags}"
}

return this
