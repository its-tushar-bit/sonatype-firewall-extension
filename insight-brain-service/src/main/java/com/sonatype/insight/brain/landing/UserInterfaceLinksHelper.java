/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.landing;

import javax.ws.rs.core.UriBuilder;

public class UserInterfaceLinksHelper
{
  public static final String RESOURCE_PATH = "ui/links";

  public static final String MANAGEMENT_PATH = "{ownerType: application|organization}/{ownerId}/management";

  public static final String SBOM_MANAGEMENT_PATH =
      "sbomManager/{ownerType: application|organization}/{ownerId}/management";

  public static final String ITEM_MANAGEMENT_EDIT_PATH =
      "{ownerType: application|organization}/{ownerId}/{itemType: category|label|policy}/{itemId}/management/edit";

  public static final String LATEST_REPORT_PATH = "application/{applicationPublicId}/latestReport/{stageId}";

  public static final String REPORT_PATH = "application/{applicationPublicId}/report/{scanId}";

  public static final String EMBEDDABLE_REPORT_PATH = "application/{applicationPublicId}/report/{scanId}/embeddable";

  public static final String PDF_PATH = "application/{applicationPublicId}/report/{scanId}/pdf";

  public static final String PRIORITIES_PATH = "/development/priorities/{applicationPublicId}/{scanId}";

  public static final String REPO_RESULT_PATH = "repository/{repositoryId}/result";

  public static final String COMPONENT_SCAN_REPORT_PATH =
      "application/{applicationPublicId}/report/{scanId}/componentDetails/{componentScanHash}";

  public static final String POLICY_VIOLATION_REPORT_PATH = "policyViolationReport/{policyViolationId}";

  public static final String VULNERABILITY_DETAILS_PATH = "vln/{vulnerabilityId}";

  public static final String LATEST_VERSION_SBOM_REPORT_PATH = "cycloneDx/{applicationId}/reports/{scanId}";

  public static final String QUARANTINED_COMPONENT_REPORT_PATH = "repositories/quarantinedComponent/{token}";

  public static final String LATEST_VERSION_SPDX_REPORT_PATH = "spdx/{applicationId}/reports/{scanId}";

  public static final String POLICY_VIOLATION_DETAILS_PATH = "policyViolation/{violationId}";

  public static final String ADD_WAIVER_PATH = "addWaiver/{violationId}";

  public static final String SBOM_BOM_VIEW_PATH =
      "sbomManager/management/view/application/{applicationPublicId}/bom/{version}";

  private static String buildStableUrl(String path, Object... parameters) {
    return UriBuilder.fromPath(RESOURCE_PATH).path(path).build(parameters).toString();
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
}
