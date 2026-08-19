/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Terraform Cloud deployment helpers for MTIQ cells.
 *
 * Usage:
 *   def tfc = load 'jenkins/tfc-helpers.groovy'
 *   tfc.deployCells('dev', cellWorkspaces, globalWorkspace, ecrTag)
 *
 * Cell workspaces and global workspace names are defined in the calling Jenkinsfile,
 * keeping environment-specific configuration close to the job that uses it.
 *
 * ── Threshold-based approval ──────────────────────────────────────────────────────────────────
 * tfcDeploy() polls the Terraform run and auto-approves plans within configurable thresholds
 * for additions, changes, and destructions. Plans exceeding thresholds fail the build by default.
 * Set gateEnabled: true in the thresholds map to instead pause for manual operator approval via
 * Jenkins input step. Callers can override the default thresholds (max 4 of each type) by
 * passing a thresholds map to deployCells().
 */
import groovy.transform.Field

@Field final String TFC_BASE_URL = 'https://app.terraform.io'
@Field final String TFC_ORG = 'Sonatype-Cloud'
@Field final String TFC_CREDENTIAL = 'sca-cloud-terraform-cloud-jenkins-credential'
@Field final String TFC_IMAGE_VARIABLE = 'ecs_container_image'
@Field final String TFC_ECR_OUTPUT = 'global_mtiq_ecr_repository_url'

@Field final Map DEFAULT_THRESHOLDS = [
  maxDestructions: 4,
  maxAdditions: 4,
  maxChanges: 4,
  gateEnabled: false,
]

/**
 * Deploy to cell workspaces via Terraform Cloud.
 *
 * @param envName        Human-readable environment name (for log messages)
 * @param cellWorkspaces TFC workspace names to deploy to
 * @param globalWorkspace TFC global workspace to read ECR URL from
 * @param ecrTag         The ECR image tag to deploy
 * @param thresholds     Optional threshold overrides (e.g., [maxDestructions: 5])
 */
void deployCells(String envName, List<String> cellWorkspaces, String globalWorkspace, String ecrTag, Map thresholds = [:]) {
  if (!ecrTag) {
    error "deployCells(${envName}): ecrTag parameter is required"
  }

  if (!cellWorkspaces) {
    echo "No ${envName} cell workspaces configured, skipping TFC deployment"
    return
  }

  withCredentials([string(credentialsId: TFC_CREDENTIAL, variable: 'TFC_TOKEN')]) {
    final String ecrUrl = tfcGetStateOutput(globalWorkspace, TFC_ECR_OUTPUT)
    if (!ecrUrl) {
      error "Could not read ${TFC_ECR_OUTPUT} from ${globalWorkspace} state"
    }

    final String fullImageUri = "${ecrUrl}:${ecrTag}"
    echo "Deploying image to ${envName}: ${fullImageUri}"

    for (String workspace : cellWorkspaces) {
      tfcDeploy(workspace, fullImageUri, thresholds)
    }
  }
}

void tfcDeploy(String workspaceName, String imageUri, Map thresholds = [:]) {
  if (!workspaceName) {
    error 'tfcDeploy: workspaceName parameter is required'
  }
  if (!imageUri) {
    error 'tfcDeploy: imageUri parameter is required'
  }

  // Use string concatenation (not interpolation) to avoid exposing TFC_TOKEN in logs
  // See https://jenkins.io/redirect/groovy-string-interpolation
  final List customHeaders = [
    [name: 'Authorization', value: 'Bearer ' + TFC_TOKEN, maskValue: true],
    [name: 'Content-Type', value: 'application/vnd.api+json'],
  ]

  // Look up workspace ID
  def response = httpRequest(
    url: "${TFC_BASE_URL}/api/v2/organizations/${TFC_ORG}/workspaces/${workspaceName}",
    httpMode: 'GET',
    customHeaders: customHeaders,
    validResponseCodes: '200:299',
  )
  def json = readJSON text: response.content
  if (!json.data) {
    error "TFC returned no data for workspace '${workspaceName}' — check the workspace name and TFC organization"
  }
  final String workspaceId = json.data.id

  // Find or create the image variable
  response = httpRequest(
    url: "${TFC_BASE_URL}/api/v2/workspaces/${workspaceId}/vars",
    httpMode: 'GET',
    customHeaders: customHeaders,
    validResponseCodes: '200:299',
  )
  json = readJSON text: response.content
  if (!json.data) {
    error "TFC returned no data for workspace variables — check workspace ${workspaceId}"
  }
  final String variableId = json.data.find { it.attributes.key == TFC_IMAGE_VARIABLE }?.id

  echo "Setting ${TFC_IMAGE_VARIABLE} to ${imageUri} in workspace ${workspaceName}"
  if (variableId) {
    httpRequest(
      url: "${TFC_BASE_URL}/api/v2/workspaces/${workspaceId}/vars/${variableId}",
      httpMode: 'PATCH',
      customHeaders: customHeaders,
      requestBody: groovy.json.JsonOutput.toJson([
        data: [id: variableId, type: 'vars', attributes: [value: imageUri]],
      ]),
      validResponseCodes: '200:299',
    )
  } else {
    httpRequest(
      url: "${TFC_BASE_URL}/api/v2/workspaces/${workspaceId}/vars",
      httpMode: 'POST',
      customHeaders: customHeaders,
      requestBody: groovy.json.JsonOutput.toJson([
        data: [type: 'vars', attributes: [key: TFC_IMAGE_VARIABLE, value: imageUri, category: 'terraform']],
      ]),
      validResponseCodes: '200:299',
    )
  }

  // Trigger run
  echo "Triggering Terraform plan for workspace ${workspaceName}"
  response = httpRequest(
    url: "${TFC_BASE_URL}/api/v2/runs",
    httpMode: 'POST',
    customHeaders: customHeaders,
    requestBody: groovy.json.JsonOutput.toJson([
      data: [
        type: 'runs',
        attributes: [message: "Deploy MTIQ ${imageUri}"],
        relationships: [workspace: [data: [id: workspaceId, type: 'workspaces']]],
      ],
    ]),
    validResponseCodes: '200:299',
  )
  json = readJSON text: response.content
  if (!json.data) {
    error "TFC returned no data when creating run for workspace ${workspaceName}"
  }
  final String runId = json.data.id
  final String runLink = json.data.links.self
  final String uiLink = "https://app.terraform.io/app/${TFC_ORG}/workspaces/${workspaceName}${runLink.replace('/api/v2', '')}"
  echo "Monitoring ${uiLink}"

  // Poll for completion (10 min timeout)
  for (int i = 0; i < 20; i++) {
    sleep 30
    response = httpRequest(url: "${TFC_BASE_URL}${runLink}", httpMode: 'GET', customHeaders: customHeaders, validResponseCodes: '200:299')
    json = readJSON text: response.content
    if (!json.data?.attributes?.status) {
      echo "WARNING: TFC returned unexpected response while polling run status (iteration ${i + 1}/20), will retry"
      continue
    }
    final String status = json.data.attributes.status
    echo "Terraform status: ${status}"

    if (status == 'applied' || status == 'planned_and_finished') {
      echo "TFC run ${status}: ${workspaceName}"
      return
    }
    if (status in ['errored', 'canceled', 'force_canceled', 'discarded']) {
      error "Terraform apply failed (${status}): ${uiLink}"
    }

    // Apply threshold-based approval logic
    if (status in ['planned', 'cost_estimated', 'policy_checked']) {
      response = httpRequest(url: "${TFC_BASE_URL}/api/v2/runs/${runId}/plan", httpMode: 'GET', customHeaders: customHeaders, validResponseCodes: '200:299')
      json = readJSON text: response.content
      if (!json.data?.attributes) {
        error "TFC returned no data for plan — refusing to auto-approve: ${uiLink}"
      }

      // Null checks: refuse auto-approve if any metric is missing
      final Integer destructionsRaw = json.data.attributes.'resource-destructions'
      final Integer additionsRaw = json.data.attributes.'resource-additions'
      final Integer changesRaw = json.data.attributes.'resource-changes'
      if (destructionsRaw == null) {
        error "Could not read resource-destructions from TFC plan response — refusing to auto-approve: ${uiLink}"
      }
      if (additionsRaw == null) {
        error "Could not read resource-additions from TFC plan response — refusing to auto-approve: ${uiLink}"
      }
      if (changesRaw == null) {
        error "Could not read resource-changes from TFC plan response — refusing to auto-approve: ${uiLink}"
      }
      final int destructions = destructionsRaw
      final int additions = additionsRaw
      final int changes = changesRaw

      // Merge caller thresholds with defaults
      def effectiveThresholds = DEFAULT_THRESHOLDS + thresholds

      // Check if all metrics are within thresholds
      boolean withinThresholds = destructions <= effectiveThresholds.maxDestructions &&
                                  additions <= effectiveThresholds.maxAdditions &&
                                  changes <= effectiveThresholds.maxChanges

      if (withinThresholds) {
        echo "Plan looks safe, confirming apply"
        def applyResponse = httpRequest(
          url: "${TFC_BASE_URL}/api/v2/runs/${runId}/actions/apply",
          httpMode: 'POST',
          customHeaders: customHeaders,
          requestBody: groovy.json.JsonOutput.toJson([comment: "Auto-approved by Jenkins: MTIQ ${imageUri}"]),
          validResponseCodes: '200:299',
        )
        echo "Apply request accepted (HTTP ${applyResponse.status})"
      } else if (effectiveThresholds.gateEnabled) {
        // Plan exceeds thresholds — require manual approval (opt-in)
        echo "Plan summary: ${additions} additions, ${changes} changes, ${destructions} destructions"
        input message: "Terraform plan for ${workspaceName} exceeds expected thresholds.\n" +
                       "  Additions: ${additions} (expected ≤ ${effectiveThresholds.maxAdditions})\n" +
                       "  Changes: ${changes} (expected ≤ ${effectiveThresholds.maxChanges})\n" +
                       "  Destructions: ${destructions} (expected ≤ ${effectiveThresholds.maxDestructions})\n" +
                       "Review the plan: ${uiLink}",
              ok: 'Approve Apply'
        echo "Manual approval granted, confirming apply"
        def applyResponse = httpRequest(
          url: "${TFC_BASE_URL}/api/v2/runs/${runId}/actions/apply",
          httpMode: 'POST',
          customHeaders: customHeaders,
          requestBody: groovy.json.JsonOutput.toJson([comment: "Manually approved via Jenkins: MTIQ ${imageUri}"]),
          validResponseCodes: '200:299',
        )
        echo "Apply request accepted (HTTP ${applyResponse.status})"
      } else {
        // Plan exceeds thresholds — fail the build (gate not enabled)
        error "Terraform plan for ${workspaceName} exceeds expected thresholds. " +
              "Additions: ${additions} (expected ≤ ${effectiveThresholds.maxAdditions}), " +
              "Changes: ${changes} (expected ≤ ${effectiveThresholds.maxChanges}), " +
              "Destructions: ${destructions} (expected ≤ ${effectiveThresholds.maxDestructions}). " +
              "Review the plan: ${uiLink}"
      }
    }
  }
  error "Terraform apply timed out after 10 minutes: ${uiLink}"
}

String tfcGetStateOutput(String workspaceName, String outputName) {
  // Use string concatenation (not interpolation) to avoid exposing TFC_TOKEN in logs
  // See https://jenkins.io/redirect/groovy-string-interpolation
  final List customHeaders = [
    [name: 'Authorization', value: 'Bearer ' + TFC_TOKEN, maskValue: true],
  ]

  def response = httpRequest(
    url: "${TFC_BASE_URL}/api/v2/organizations/${TFC_ORG}/workspaces/${workspaceName}",
    httpMode: 'GET',
    customHeaders: customHeaders,
    validResponseCodes: '200:299',
  )
  def json = readJSON text: response.content
  if (!json.data) {
    error "TFC returned no data for workspace '${workspaceName}'"
  }
  final String workspaceId = json.data.id

  response = httpRequest(
    url: "${TFC_BASE_URL}/api/v2/workspaces/${workspaceId}/current-state-version-outputs",
    httpMode: 'GET',
    customHeaders: customHeaders,
    validResponseCodes: '200:299',
  )
  json = readJSON text: response.content
  if (!json.data) {
    error "TFC returned no data for state version outputs in workspace ${workspaceId}"
  }
  return json.data.find { it.attributes.name == outputName }?.attributes?.value
}

return this
