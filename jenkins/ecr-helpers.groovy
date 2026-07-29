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

@Field final Map PROD_ECR_ACCOUNT = [
    id: '348313797720',
    credentialId: 'mtiq-prod-external-id',
    role: 'jenkins'
]

@Field final String ECR_REPOSITORY = 'sca-cloud/mtiq-server'

// Observe agent mirror: the private ECR repository (same name in every account)
// and the upstream Docker Hub source repository it is mirrored from.
@Field final String OBSERVE_AGENT_ECR_REPOSITORY = 'sca-cloud/observe-agent'
@Field final String OBSERVE_AGENT_DOCKERHUB_REPOSITORY = 'observeinc/observe-agent'

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

/**
 * Verifies that observe-agent:<version> exists upstream on Docker Hub, queried
 * through the Sonatype registry proxy. Fails the build with an actionable
 * message if the tag cannot be resolved, so a typo'd or non-existent version
 * stops the job before any ECR write.
 *
 * @param version observe-agent version to check (e.g., '2.17.0')
 */
void verifyUpstreamObserveAgentExists(String version) {
  withSonatypeDockerRegistry() {
    // version is passed via withEnv (not interpolated into the shell) to prevent injection
    withEnv(["SOURCE_IMAGE=${sonatypeDockerRegistryId()}/${OBSERVE_AGENT_DOCKERHUB_REPOSITORY}:${version}"]) {
      def status = sh(script: 'docker buildx imagetools inspect "$SOURCE_IMAGE" > /dev/null 2>&1', returnStatus: true)
      if (status != 0) {
        error "observe-agent version '${version}' was not found upstream (${OBSERVE_AGENT_DOCKERHUB_REPOSITORY}:${version}). " +
              'Check the version against the tags published at https://hub.docker.com/r/observeinc/observe-agent/tags.'
      }
    }
  }
  echo "Verified upstream ${OBSERVE_AGENT_DOCKERHUB_REPOSITORY}:${version} exists"
}

/**
 * Mirrors a specific observe-agent version from Docker Hub (pulled through the
 * Sonatype registry proxy) into the given account's ECR, tagging it as both
 * :<version> and :latest.
 *
 * Uses `docker buildx imagetools create`, which copies the full multi-arch
 * manifest list. Only the :<version> and :latest tags are written, so
 * re-running with an OLDER version moves :latest back to it without removing
 * any newer version tag — this is the rollback path.
 *
 * @param account Target ECR account configuration map
 * @param version observe-agent version to mirror (e.g., '2.17.0')
 * @param region AWS region
 */
void mirrorObserveAgentToEcr(Map account, String version, String region = ECR_REGION) {
  final String targetRepo = "${account.id}.dkr.ecr.${region}.amazonaws.com/${OBSERVE_AGENT_ECR_REPOSITORY}"

  echo "Mirroring ${OBSERVE_AGENT_DOCKERHUB_REPOSITORY}:${version} to ${targetRepo} (tags: ${version}, latest)"

  withSonatypeDockerRegistry() {
    // Authenticate Docker to the target ECR; the token persists in ~/.docker/config.json
    // alongside the Sonatype proxy login, so imagetools can read source and write target.
    authenticateToEcr(account, region)

    withEnv(["SOURCE_IMAGE=${sonatypeDockerRegistryId()}/${OBSERVE_AGENT_DOCKERHUB_REPOSITORY}:${version}",
             "TARGET_VERSION=${targetRepo}:${version}",
             "TARGET_LATEST=${targetRepo}:latest"]) {
      sh 'docker buildx imagetools create --tag "$TARGET_VERSION" --tag "$TARGET_LATEST" "$SOURCE_IMAGE"'
    }
  }

  echo "Mirrored ${version} to ${targetRepo} with tags: [${version}, latest]"
}

/**
 * Best-effort check for whether a newer observe-agent version is available
 * upstream on Docker Hub than the :latest currently published in the given ECR
 * account.
 *
 * Compares the upstream :latest manifest-list digest (read through the Sonatype
 * registry proxy) against the :latest digest in ECR. When they differ, the
 * upstream version string is resolved from the Docker Hub tags API by matching
 * the digest.
 *
 * Intentionally non-fatal: any error (Docker Hub unreachable, rate limited, ECR
 * read failure, parse error) is swallowed so callers can log a warning and
 * continue without failing or destabilizing the build.
 *
 * @param account ECR account whose :latest is compared against upstream
 * @param region AWS region
 * @return a map [drift: boolean, version: String|null]; drift is false on any error
 */
Map observeAgentDriftInfo(Map account, String region = ECR_REGION) {
  try {
    String upstreamDigest = null
    withSonatypeDockerRegistry() {
      withEnv(["SOURCE_IMAGE=${sonatypeDockerRegistryId()}/${OBSERVE_AGENT_DOCKERHUB_REPOSITORY}:latest"]) {
        upstreamDigest = sh(script: 'docker buildx imagetools inspect "$SOURCE_IMAGE" --format "{{.Manifest.Digest}}"',
                            returnStdout: true).trim()
      }
    }
    if (!upstreamDigest) {
      echo 'WARNING: observe-agent drift check: could not read upstream :latest digest — skipping'
      return [drift: false, version: null]
    }

    String ecrDigest = null
    withAwsRole(credentialsId: account.credentialId, role: account.role,
        roleAccount: account.id, region: region) {
      withEnv(["REPOSITORY_ARG=${OBSERVE_AGENT_ECR_REPOSITORY}", "REGION_ARG=${region}"]) {
        ecrDigest = sh(script: '''
          aws ecr describe-images \
            --repository-name "$REPOSITORY_ARG" \
            --image-ids imageTag=latest \
            --region "$REGION_ARG" \
            --query 'imageDetails[0].imageDigest' \
            --output text
        ''', returnStdout: true).trim()
      }
    }
    if (!ecrDigest || ecrDigest == 'None') {
      echo 'WARNING: observe-agent drift check: could not read ECR :latest digest — skipping'
      return [drift: false, version: null]
    }

    if (upstreamDigest == ecrDigest) {
      echo "observe-agent drift check: ECR :latest is in sync with upstream (${upstreamDigest})"
      return [drift: false, version: null]
    }

    echo "observe-agent drift check: upstream :latest (${upstreamDigest}) differs from ECR :latest (${ecrDigest})"
    return [drift: true, version: resolveObserveAgentVersionForDigest(upstreamDigest)]
  } catch (Exception e) {
    echo "WARNING: observe-agent drift check failed (non-fatal): ${e.message}"
    return [drift: false, version: null]
  }
}

/**
 * Resolves the observe-agent version tag that points at the given manifest
 * digest, using the Docker Hub tags API. Returns null if it cannot be resolved
 * (the caller falls back to a generic message).
 *
 * @param digest manifest-list digest to match (e.g., 'sha256:...')
 * @return the matching semver-style tag name, or null
 */
String resolveObserveAgentVersionForDigest(String digest) {
  try {
    // Pass the repo constant via withEnv (never interpolate into the shell body) to
    // match the injection-safe pattern used by every other sh step in this file.
    String response = null
    withEnv(["REPO=${OBSERVE_AGENT_DOCKERHUB_REPOSITORY}"]) {
      response = sh(
          script: 'curl -sf "https://hub.docker.com/v2/repositories/$REPO/tags?page_size=100&ordering=last_updated"',
          returnStdout: true).trim()
    }
    if (!response) {
      return null
    }
    def json = readJSON text: response
    def match = json.results.find { it.name != 'latest' && it.digest == digest && it.name ==~ /\d+\.\d+\.\d+.*/ }
    return match?.name
  } catch (Exception e) {
    echo "WARNING: could not resolve observe-agent version from Docker Hub tags API: ${e.message}"
    return null
  }
}

return this
