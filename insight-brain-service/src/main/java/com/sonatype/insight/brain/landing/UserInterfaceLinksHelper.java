/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import jakarta.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;

public class UserInterfaceLinksHelper
{
  public static final String RESOURCE_PATH = "ui/links";

  public static final String DEVELOPER_HOME_PATH = "developer/dashboard";

  public static final String FIREWALL_HOME_PATH = "firewall/dashboard";

  public static final String LIFECYCLE_HOME_PATH = "lifecycle/dashboard";

  public static final String LIFECYCLE_ALT_HOME_PATH = "lifecycle/reports";

  public static final String SBOM_MANAGER_HOME_PATH = "sbomManager/dashboard";

  public static final String GUIDE_HOME_PATH = "guide/dashboard";

  public static final String GUIDE_SPA_PATH = "assets/guide/index.html#/";

  public static final String MANAGEMENT_PATH = "{ownerType: application|organization}/{ownerId}/management";

  public static final String SBOM_MANAGEMENT_PATH =
      "sbomManager/{ownerType: application|organization}/{ownerId}/management";

  public static final String SOURCE_CONTROL_MANAGEMENT_PATH =
      "{ownerType: application|organization}/{ownerId}/source-control/management";

  public static final String ITEM_MANAGEMENT_EDIT_PATH =
      "{ownerType: application|organization}/{ownerId}/{itemType: category|label|policy}/{itemId}/management/edit";

  public static final String LATEST_REPORT_PATH = "application/{applicationPublicId}/latestReport/{stageId}";

  public static final String REPORT_PATH = "application/{applicationPublicId}/report/{scanId}";

  public static final String EMBEDDABLE_REPORT_PATH = "application/{applicationPublicId}/report/{scanId}/embeddable";

  public static final String PDF_PATH = "application/{applicationPublicId}/report/{scanId}/pdf";

  public static final String PRIORITIES_PATH = "/developer/priorities/{applicationPublicId}/{scanId}";

  public static final String INTEGRATIONS_PRIORITIES_PATH =
      "/developer/integrations/{applicationPublicId}/{scanId}/{integration}";

  public static final String PRIORITIES_PATH_LEGACY = "/development/priorities/{applicationPublicId}/{scanId}";

  public static final String REPO_RESULT_PATH = "repository/{repositoryId}/result";

  public static final String COMPONENT_SCAN_REPORT_PATH =
      "application/{applicationPublicId}/report/{scanId}/componentDetails/{componentScanHash}";

  public static final String POLICY_VIOLATION_REPORT_PATH = "policyViolationReport/{policyViolationId}";

  public static final String VULNERABILITY_DETAILS_PATH = "vln/{vulnerabilityId}";

  public static final String LATEST_VERSION_SBOM_REPORT_PATH = "cycloneDx/{applicationId}/reports/{scanId}";

  public static final String QUARANTINED_COMPONENT_REPORT_PATH =
      "firewall/repositories/quarantinedComponent/{token}";

  public static final String LATEST_VERSION_SPDX_REPORT_PATH = "spdx/{applicationId}/reports/{scanId}";

  public static final String POLICY_VIOLATION_DETAILS_PATH = "policyViolation/{violationId}";

  public static final String ADD_WAIVER_PATH = "addWaiver/{violationId}";

  public static final String REVIEW_WAIVER_REQUEST_PATH =
      "requestWaiverReview/{ownerType}/{ownerId}/{policyWaiverRequestId}";

  public static final String SBOM_BOM_VIEW_PATH =
      "sbomManager/management/view/application/{applicationPublicId}/bom/{version}";

  public static final String ENTERPRISE_REPORTING_DASHBOARD_PATH =
      "enterpriseReporting/{dashboardId}";

  public static final String FIREWALL_CONTAINER_IMAGE_EVALUATION_REPORT_PATH =
      "firewall/containerReport/{containerImagePublicId}/report/{scanId}";

  public static final String MALWARE_DEFENSE_CONTAINER_IMAGE_EVALUATION_REPORT_PATH =
      "malware-defense/containerReport/{containerImagePublicId}/report/{scanId}";

  public static final String MALWARE_DEFENSE_REPOSITORY_RESULTS_PATH =
      "malware-defense/repository/{repositoryId}/result";

  private static String buildStableUrl(String path) {
    return UriBuilder.fromPath(RESOURCE_PATH).path(path).build().toString();
  }

  private static String buildStableUrl(String path, Object... parameters) {
    return UriBuilder.fromPath(RESOURCE_PATH).path(path).build(parameters).toString();
  }

  public static String getDeveloperHomePath() {
    return buildStableUrl(DEVELOPER_HOME_PATH);
  }

  public static String getFirewallHomePath() {
    return buildStableUrl(FIREWALL_HOME_PATH);
  }

  public static String getLifecycleHomePath() {
    return buildStableUrl(LIFECYCLE_HOME_PATH);
  }

  public static String getLifecycleAltHomePath() {
    return buildStableUrl(LIFECYCLE_ALT_HOME_PATH);
  }

  public static String getSbomManagerHomePath() {
    return buildStableUrl(SBOM_MANAGER_HOME_PATH);
  }

  public static String getGuideHomePath() {
    return buildStableUrl(GUIDE_HOME_PATH);
  }

  public static String getGuideSpaPath() {
    return GUIDE_SPA_PATH;
  }

  public static String getVulnerabilityDetailsUrl(String vulnerabilityId) {
    return buildStableUrl(VULNERABILITY_DETAILS_PATH, vulnerabilityId);
  }

  public static String getPolicyViolationReportPath(final String policyViolationId) {
    return buildStableUrl(POLICY_VIOLATION_REPORT_PATH, policyViolationId);
  }

  public static String getLatestReportUrl(String applicationPublicId, String stageId) {
    return buildStableUrl(LATEST_REPORT_PATH, applicationPublicId, stageId);
  }

  public static String getApplicationReportTab(String tab) {
    String defaultTab = "overview";
    if (!StringUtils.isEmpty(tab)) {
      Set<String> tabs = new HashSet<>(Arrays.asList(defaultTab, "violations", "security", "legal", "labels", "audit"));
      if (tabs.contains(tab)) {
        return tab;
      }
    }
    return defaultTab;
  }

  /**
   * Gets the relative URL to the stable hyperlink for the HTML report of the given application and scan.
   */
  public static String getReportUrl(String applicationPublicId, String scanId) {
    return buildStableUrl(REPORT_PATH, applicationPublicId, scanId);
  }

  /**
   * Gets the relative URL to the stable hyperlink for the embeddable HTML report of the given application and scan.
   *
   * @since 1.16
   */
  public static String getEmbeddableReportUrl(String applicationPublicId, String scanId) {
    return buildStableUrl(EMBEDDABLE_REPORT_PATH, applicationPublicId, scanId);
  }

  /**
   * Gets the relative URL to the stable hyperlink for the PDF report of the given application and scan.
   *
   * @since 1.9
   */
  public static String getPdfUrl(String applicationPublicId, String scanId) {
    return buildStableUrl(PDF_PATH, applicationPublicId, scanId);
  }

  /**
   * Gets the relative URL to the stable hyperlink for the repository audit report for a given rm/repository
   *
   * @since 1.17
   */
  public static String getRepositoryReportUrl(String repositoryId) {
    return buildStableUrl(REPO_RESULT_PATH, repositoryId);
  }

  /**
   * Gets the relative URL to the stable hyperlink for the quarantined component report for a given token
   *
   * @since 1.125
   */
  public static String getQuarantinedComponentReportPath(String token) {
    return buildStableUrl(QUARANTINED_COMPONENT_REPORT_PATH, token);
  }

  /**
   * Gets the relative URL to the stable hyperlink for the management path given owner type and owner id
   *
   * @since 1.138
   */
  public static String getManagementPath(String ownerType, String ownerId, boolean isSbomManager) {
    return buildStableUrl(isSbomManager ? SBOM_MANAGEMENT_PATH : MANAGEMENT_PATH, ownerType, ownerId);
  }

  /**
   * Gets the relative URL to the stable hyperlink for the management edit path given owner type, owner id, item type
   * and item id
   *
   * @since 1.138
   */
  public static String getItemManagementPathEdit(String ownerType, String ownerId, String itemType, String itemId) {
    return buildStableUrl(ITEM_MANAGEMENT_EDIT_PATH, ownerType, ownerId, itemType, itemId);
  }

  /**
   * Gets the relative URL to the Bill of material page of the SBOM document version for a given application
   *
   * @since 1.176
   */
  public static String getSBOMBillOfMaterialPath(String applicationPublicId, String version) {
    return buildStableUrl(SBOM_BOM_VIEW_PATH, applicationPublicId, version);
  }

  /**
   * Gets the URL to the priorities report given an appId, and a scanId
   *
   * @since 1.176
   */
  public static String getPrioritiesUrl(String applicationPublicId, String scanId) {
    return buildStableUrl(PRIORITIES_PATH, applicationPublicId, scanId);
  }

  public static String getIntegrationsPrioritiesUrl(String applicationPublicId, String scanId) {
    return buildStableUrl(INTEGRATIONS_PRIORITIES_PATH, applicationPublicId, scanId, "");
  }

  /**
   * Gets the relative URL to the stable hyperlink for reviewing a waiver request.
   *
   * @since 1.192
   */
  public static String getReviewWaiverRequestUrl(String ownerType, String ownerId, String policyWaiverRequestId) {
    return buildStableUrl(REVIEW_WAIVER_REQUEST_PATH, ownerType, ownerId, policyWaiverRequestId);
  }

  /**
   * Builds the relative URL for adding a waiver with optional comments and reasonId.
   *
   * @since 1.192
   */
  public static String getAddWaiverUrl(String violationId, String comments, String reasonId) {
    UriBuilder uriBuilder = UriBuilder.fromPath(RESOURCE_PATH)
        .path(ADD_WAIVER_PATH);
    if (comments != null) {
      uriBuilder.queryParam("comments", comments);
    }
    if (reasonId != null) {
      uriBuilder.queryParam("reasonId", reasonId);
    }
    return uriBuilder.build(violationId).toString();
  }

  /**
   * Builds the relative URL for the policy violation details page.
   *
   * @since 1.192
   */
  public static String getPolicyViolationDetailsUrl(String violationId) {
    return buildStableUrl(POLICY_VIOLATION_DETAILS_PATH, violationId);
  }

  /**
   * Gets the relative URL to the stable hyperlink for the HTML container image report of the given container and scan.
   */
  public static String getFirewallContainerImageEvaluationReportUrl(
      String containerImagePublicId,
      String scanId)
  {
    return buildStableUrl(FIREWALL_CONTAINER_IMAGE_EVALUATION_REPORT_PATH, containerImagePublicId, scanId);
  }
}
