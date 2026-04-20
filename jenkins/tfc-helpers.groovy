/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Terraform Cloud deployment helpers for MTIQ cells.
 *
 * Used by Jenkinsfile.deploy-to-shared-dev via:
 *   def tfc = load 'jenkins/tfc-helpers.groovy'
 *   tfc.deployToCells(ecrTag)
 *
 * ── MODERN_CELL_WORKSPACES ──────────────────────────────────────────────────────────────────────
 * An explicit allowlist of Terraform Cloud workspace names that deployToCells() will deploy
 * to. The allowlist exists to prevent accidental targeting of legacy cell workspaces.
 * To add a new workspace: append its TFC workspace name to the MODERN_CELL_WORKSPACES list below.
 *
 * ── Auto-approval safety guard ──────────────────────────────────────────────────────────────────
 * tfcDeploy() polls the Terraform run and auto-approves plans that include at most 1 resource
 * destruction (the normal case for ECS task-definition replacement). Plans with more than 1
 * destruction are discarded and the pipeline fails to prevent unintended infrastructure teardown.
 */
import groovy.transform.Field

@Field final String TFC_BASE_URL = 'https://app.terraform.io'
@Field final String TFC_ORG = 'Sonatype-Cloud'
@Field final String TFC_CREDENTIAL = 'sca-cloud-terraform-cloud-jenkins-credential'
@Field final String TFC_IMAGE_VARIABLE = 'ecs_container_image'
@Field final String TFC_ECR_OUTPUT = 'global_mtiq_ecr_repository_url'

// Modern cell workspaces — explicit allowlist (never target legacy cells)
@Field final List<String> MODERN_CELL_WORKSPACES = [
  'sca_aws_dev_cell-shared-dev'
]

void deployToCells(String ecrTag) {
  if (MODERN_CELL_WORKSPACES.isEmpty()) {
    echo 'No modern cell workspaces configured, skipping TFC deployment'
    return
  }

  if (!ecrTag) {
    error 'deployToCells: ecrTag parameter is required'
  }

  withCredentials([string(credentialsId: TFC_CREDENTIAL, variable: 'TFC_TOKEN')]) {
    // Get ECR URL from global workspace state
    final String globalWorkspace = 'sca_aws_dev_global'
    final String ecrUrl = tfcGetStateOutput(globalWorkspace, TFC_ECR_OUTPUT)
    if (!ecrUrl) {
      error "Could not read ${TFC_ECR_OUTPUT} from ${globalWorkspace} state"
    }

    final String fullImageUri = "${ecrUrl}:${ecrTag}"
    echo "Deploying image: ${fullImageUri}"

    for (String workspace : MODERN_CELL_WORKSPACES) {
      tfcDeploy(workspace, fullImageUri)
    }
  }
}

void tfcDeploy(String workspaceName, String imageUri) {
  if (!workspaceName) {
    error 'tfcDeploy: workspaceName parameter is required'
  }
  if (!imageUri) {
    error 'tfcDeploy: imageUri parameter is required'
  }

  final List customHeaders = [
    [name: 'Authorization', value: "Bearer ${TFC_TOKEN}", maskValue: true],
    [name: 'Content-Type', value: 'application/vnd.api+json'],
  ]

  // Look up workspace ID
  def response = httpRequest(
    url: "${TFC_BASE_URL}/api/v2/organizations/${TFC_ORG}/workspaces/${workspaceName}",
    httpMode: 'GET',
    customHeaders: customHeaders,
    validResponseCodes: '2XX',
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
    validResponseCodes: '2XX',
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
      validResponseCodes: '2XX',
    )
  } else {
    httpRequest(
      url: "${TFC_BASE_URL}/api/v2/workspaces/${workspaceId}/vars",
      httpMode: 'POST',
      customHeaders: customHeaders,
      requestBody: groovy.json.JsonOutput.toJson([
        data: [type: 'vars', attributes: [key: TFC_IMAGE_VARIABLE, value: imageUri, category: 'terraform']],
      ]),
      validResponseCodes: '2XX',
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
    validResponseCodes: '2XX',
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
    response = httpRequest(url: "${TFC_BASE_URL}${runLink}", httpMode: 'GET', customHeaders: customHeaders, validResponseCodes: '2XX')
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

    // Auto-approve safe plans (<=1 destruction for old task def replacement)
    if (status in ['planned', 'cost_estimated', 'policy_checked']) {
      response = httpRequest(url: "${TFC_BASE_URL}/api/v2/runs/${runId}/plan", httpMode: 'GET', customHeaders: customHeaders, validResponseCodes: '2XX')
      json = readJSON text: response.content
      if (!json.data?.attributes) {
        error "TFC returned no data for plan — refusing to auto-approve: ${uiLink}"
      }

      // Null check: refuse auto-approve if resource-destructions is missing
      final Integer destructionsRaw = json.data.attributes.'resource-destructions'
      if (destructionsRaw == null) {
        error "Could not read resource-destructions from TFC plan response — refusing to auto-approve: ${uiLink}"
      }
      final int destructions = destructionsRaw

      if (destructions > 1) {
        httpRequest(
          url: "${TFC_BASE_URL}/api/v2/runs/${runId}/actions/discard",
          httpMode: 'POST',
          customHeaders: customHeaders,
          requestBody: groovy.json.JsonOutput.toJson([comment: "Discarded: plan includes ${destructions} resource destruction(s)"]),
          validResponseCodes: '2XX',
        )
        error "Terraform plan includes ${destructions} resource destruction(s) (expected at most 1 for task def replacement), discarded: ${uiLink}"
      }

      echo "Plan looks safe, confirming apply"
      def applyResponse = httpRequest(
        url: "${TFC_BASE_URL}/api/v2/runs/${runId}/actions/apply",
        httpMode: 'POST',
        customHeaders: customHeaders,
        requestBody: groovy.json.JsonOutput.toJson([comment: "Auto-approved by Jenkins: MTIQ ${imageUri}"]),
        validResponseCodes: '2XX',
      )
      echo "Apply request accepted (HTTP ${applyResponse.status})"
    }
  }
  error "Terraform apply timed out after 10 minutes: ${uiLink}"
}

String tfcGetStateOutput(String workspaceName, String outputName) {
  final List customHeaders = [
    [name: 'Authorization', value: "Bearer ${TFC_TOKEN}", maskValue: true],
  ]

  def response = httpRequest(
    url: "${TFC_BASE_URL}/api/v2/organizations/${TFC_ORG}/workspaces/${workspaceName}",
    httpMode: 'GET',
    customHeaders: customHeaders,
    validResponseCodes: '2XX',
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
    validResponseCodes: '2XX',
  )
  json = readJSON text: response.content
  if (!json.data) {
    error "TFC returned no data for state version outputs in workspace ${workspaceId}"
  }
  return json.data.find { it.attributes.name == outputName }?.attributes?.value
}

return this
