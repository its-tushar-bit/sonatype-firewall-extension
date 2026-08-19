/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.indexquery;

import static org.assertj.core.api.Assertions.assertThat;

import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.junit.jupiter.api.Test;

public class IndexQueryRowMapperTest
{
  @Test
  public void waiver_manualWithPolicyName_usesPolicyNameAsTitle() {
    SearchResultItemDTO d = manualWaiver();
    d.policyWaiverPolicyName = "Security-High";

    IndexQueryRow row = IndexQueryRowMapper.toRow(IndexQueryType.WAIVER, d);

    assertThat(row.getTitle()).isEqualTo("Security-High");
    assertThat(row.getFields().get("policyName")).isEqualTo("Security-High");
  }

  @Test
  public void waiver_manualWithNullPolicyName_fallsBackToGenericTitle() {
    // An orphaned-policy manual waiver indexes a null policyWaiverPolicyName (buildPolicyWaiverDocs).
    // The row must still render a non-blank title rather than a blank cell.
    SearchResultItemDTO d = manualWaiver();
    d.policyWaiverPolicyName = null;

    IndexQueryRow row = IndexQueryRowMapper.toRow(IndexQueryType.WAIVER, d);

    assertThat(row.getTitle()).isEqualTo("Waiver");
    assertThat(row.getFields().get("policyName")).isNull();
  }

  @Test
  public void waiver_manualWithBlankPolicyName_fallsBackToGenericTitle() {
    SearchResultItemDTO d = manualWaiver();
    d.policyWaiverPolicyName = "   ";

    IndexQueryRow row = IndexQueryRowMapper.toRow(IndexQueryType.WAIVER, d);

    assertThat(row.getTitle()).isEqualTo("Waiver");
  }

  @Test
  public void waiver_autoWithNullPolicyName_usesSyntheticAutoTitle() {
    SearchResultItemDTO d = manualWaiver();
    d.policyWaiverIsAuto = Boolean.TRUE;
    d.policyWaiverPolicyName = null;
    d.policyWaiverThreatLevel = 10;

    IndexQueryRow row = IndexQueryRowMapper.toRow(IndexQueryType.WAIVER, d);

    assertThat(row.getTitle()).isEqualTo("Auto-waiver (threat >= 10)");
  }

  @Test
  public void waiver_autoWithNullPolicyNameAndNullThreat_usesBareAutoTitle() {
    SearchResultItemDTO d = manualWaiver();
    d.policyWaiverIsAuto = Boolean.TRUE;
    d.policyWaiverPolicyName = null;
    d.policyWaiverThreatLevel = null;

    IndexQueryRow row = IndexQueryRowMapper.toRow(IndexQueryType.WAIVER, d);

    assertThat(row.getTitle()).isEqualTo("Auto-waiver");
  }

  @Test
  public void waiver_legacyAutoOnlyField_fallsBackForSyntheticTitle() {
    // Hand-built / pre-reindex DTO: only Classic policyWaiverAuto is set.
    SearchResultItemDTO d = manualWaiver();
    d.policyWaiverAuto = Boolean.TRUE;
    d.policyWaiverIsAuto = null;
    d.policyWaiverPolicyName = null;
    d.policyWaiverThreatLevel = 10;

    IndexQueryRow row = IndexQueryRowMapper.toRow(IndexQueryType.WAIVER, d);

    assertThat(row.getTitle()).isEqualTo("Auto-waiver (threat >= 10)");
    assertThat(row.getFields().get("isAuto")).isEqualTo(true);
  }

  @Test
  public void waiver_href_carriesPreviewPrefix() {
    SearchResultItemDTO d = manualWaiver();

    IndexQueryRow row = IndexQueryRowMapper.toRow(IndexQueryType.WAIVER, d);

    assertThat(row.getHref()).isEqualTo("/preview/waivers/application/app-1/w-1");
  }

  @Test
  public void waiverRequest_carriesRequestFieldsAndRequestedHref() {
    SearchResultItemDTO d = manualWaiver();
    d.itemType = "POLICY_WAIVER_REQUEST";
    d.policyWaiverPolicyName = "Security-High";
    d.policyWaiverRequestStatus = "REQUESTED";
    d.requesterName = "Alice";
    d.rejectionReason = null;

    IndexQueryRow row = IndexQueryRowMapper.toRow(IndexQueryType.WAIVER, d);

    assertThat(row.getFields().get("isRequested")).isEqualTo(Boolean.TRUE);
    assertThat(row.getFields().get("status")).isEqualTo("REQUESTED");
    assertThat(row.getFields().get("requesterName")).isEqualTo("Alice");
    // Request href carries the ?requested=true discriminator.
    assertThat(row.getHref()).isEqualTo("/preview/waivers/application/app-1/w-1?requested=true");
  }

  @Test
  public void waiver_policyTypeDefaultsToOtherWhenMissing() {
    SearchResultItemDTO d = manualWaiver();
    d.policyWaiverPolicyType = null;

    IndexQueryRow row = IndexQueryRowMapper.toRow(IndexQueryType.WAIVER, d);

    // Missing denormalized policyType reads back as "other" per the index contract.
    assertThat(row.getFields().get("policyType")).isEqualTo("other");
  }

  private static SearchResultItemDTO manualWaiver() {
    SearchResultItemDTO d = new SearchResultItemDTO();
    d.policyWaiverId = "w-1";
    d.policyWaiverAuto = Boolean.FALSE;
    d.policyWaiverScopeOwnerType = "APPLICATION";
    d.policyWaiverScopeOwnerId = "app-1";
    d.applicationName = "App One";
    d.organizationName = "Acme";
    d.policyWaiverThreatLevel = 7;
    return d;
  }
}
