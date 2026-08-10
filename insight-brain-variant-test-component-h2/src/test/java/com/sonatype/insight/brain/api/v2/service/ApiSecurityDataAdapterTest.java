/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentEvaluationDataList.ComponentEvaluationData;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityDataDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiSecurityIssueDTO;
import com.sonatype.insight.brain.api.v2.dto.SecurityVulnerabilityCustomDataDTO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class ApiSecurityDataAdapterTest
    extends AbstractComponentH2Test
{
  @Inject
  private ApiSecurityDataAdapter adapter;

  @BeforeEach
  public void setup() {
    setBaseUrl("http://localhost:8070/");
  }

  @Test
  public void testConvertToDTOFromComponent() {
    Component component = new Component();
    List<SecurityVulnerability> vulnerabilities = new ArrayList<>();
    SecurityVulnerability vuln = new SecurityVulnerability();
    vuln.setSource("source");
    vuln.setRefId("reference");
    vuln.setSeverity(9.4F);
    vuln.setStatus(SecurityVulnerabilityOverrideStatus.CONFIRMED);
    vuln.setUrl("url");

    ThirdPartyVulnerabilityExploitabilityExchange analysis = new ThirdPartyVulnerabilityExploitabilityExchange();
    analysis.setDetail("Some detail");
    analysis.setState("resolved");
    analysis.setJustification("code_not_reachable");
    analysis.setResponse("will_not_fix,update");
    vuln.setAnalysis(analysis);

    vulnerabilities.add(vuln);
    component.setSecurityVulnerabilities(vulnerabilities);

    ApiSecurityDataDTO dto = adapter.convertToDTO(component);
    assertThat(dto).isNotNull();
    List<ApiSecurityIssueDTO> securityIssues = dto.securityIssues;
    assertThat(securityIssues).isNotNull();
    assertThat(securityIssues).hasSize(1);
    ApiSecurityIssueDTO securityIssue = securityIssues.get(0);
    assertThat(securityIssue).isNotNull();
    assertThat(securityIssue.source).isEqualTo("source");
    assertThat(securityIssue.reference).isEqualTo("reference");
    assertThat(securityIssue.severity).isEqualTo(9.4F);
    assertThat(securityIssue.status).isEqualTo("Confirmed");
    assertThat(securityIssue.url).isEqualTo("url");
    assertThat(securityIssue.threatCategory).isEqualTo("critical");
    assertThat(securityIssue.analysis.detail).isEqualTo(analysis.getDetail());
    assertThat(securityIssue.analysis.state).isEqualTo(analysis.getState());
    assertThat(securityIssue.analysis.justification).isEqualTo(analysis.getJustification());
    assertThat(securityIssue.analysis.response).isEqualTo(analysis.getResponse());
  }

  @Test
  public void testConvertToDTOFromComponentWithSonatypeUrl() {
    Component component = new Component();
    List<SecurityVulnerability> vulnerabilities = new ArrayList<>();
    SecurityVulnerability vuln = new SecurityVulnerability();
    vuln.setSource("sonatype");
    vuln.setRefId("reference");
    vuln.setSeverity(9.4F);
    vuln.setStatus(SecurityVulnerabilityOverrideStatus.CONFIRMED);
    vuln.setUrl(null);

    vulnerabilities.add(vuln);
    component.setSecurityVulnerabilities(vulnerabilities);

    ApiSecurityDataDTO dto = adapter.convertToDTO(component);
    assertThat(dto).isNotNull();
    List<ApiSecurityIssueDTO> securityIssues = dto.securityIssues;
    assertThat(securityIssues).isNotNull();
    assertThat(securityIssues).hasSize(1);
    ApiSecurityIssueDTO securityIssue = securityIssues.get(0);
    assertThat(securityIssue).isNotNull();
    assertThat(securityIssue.source).isEqualTo("sonatype");
    assertThat(securityIssue.reference).isEqualTo("reference");
    assertThat(securityIssue.severity).isEqualTo(9.4F);
    assertThat(securityIssue.status).isEqualTo("Confirmed");
    assertThat(securityIssue.url).isEqualTo("http://localhost:8070/ui/links/vln/reference");
    assertThat(securityIssue.threatCategory).isEqualTo("critical");
  }

  @Test
  public void testConvertToDTOFromComponentWithNullUrl() {
    Component component = new Component();
    List<SecurityVulnerability> vulnerabilities = new ArrayList<>();
    SecurityVulnerability vuln = new SecurityVulnerability();
    vuln.setSource("source");
    vuln.setRefId("reference");
    vuln.setSeverity(9.4F);
    vuln.setStatus(SecurityVulnerabilityOverrideStatus.CONFIRMED);
    vuln.setUrl(null);

    vulnerabilities.add(vuln);
    component.setSecurityVulnerabilities(vulnerabilities);

    ApiSecurityDataDTO dto = adapter.convertToDTO(component);
    assertThat(dto).isNotNull();
    List<ApiSecurityIssueDTO> securityIssues = dto.securityIssues;
    assertThat(securityIssues).isNotNull();
    assertThat(securityIssues).hasSize(1);
    ApiSecurityIssueDTO securityIssue = securityIssues.get(0);
    assertThat(securityIssue).isNotNull();
    assertThat(securityIssue.source).isEqualTo("source");
    assertThat(securityIssue.reference).isEqualTo("reference");
    assertThat(securityIssue.severity).isEqualTo(9.4F);
    assertThat(securityIssue.status).isEqualTo("Confirmed");
    assertThat(securityIssue.url).isEqualTo(null);
    assertThat(securityIssue.threatCategory).isEqualTo("critical");
  }

  @Test
  public void testConvertToDTOFromComponentEvaluationData() {
    ComponentEvaluationData componentDetailsFromHds = new ComponentEvaluationData();
    List<com.sonatype.clm.dto.model.SecurityVulnerability> vulnerabilities = new ArrayList<>();
    com.sonatype.clm.dto.model.SecurityVulnerability vuln = new com.sonatype.clm.dto.model.SecurityVulnerability();
    vuln.setSource("source");
    vuln.setRefId("reference");
    vuln.setSeverity(9.4F);
    vuln.setUrl("url");

    vulnerabilities.add(vuln);
    componentDetailsFromHds.securityVulnerabilities = vulnerabilities;

    ApiSecurityDataDTO dto = adapter.convertToDTO(componentDetailsFromHds);
    assertThat(dto).isNotNull();
    List<ApiSecurityIssueDTO> securityIssues = dto.securityIssues;
    assertThat(securityIssues).isNotNull();
    assertThat(securityIssues).hasSize(1);
    ApiSecurityIssueDTO securityIssue = securityIssues.get(0);
    assertThat(securityIssue).isNotNull();
    assertThat(securityIssue.source).isEqualTo("source");
    assertThat(securityIssue.reference).isEqualTo("reference");
    assertThat(securityIssue.severity).isEqualTo(9.4F);
    assertThat(securityIssue.status).isEqualTo("Open");
    assertThat(securityIssue.url).isEqualTo("url");
    assertThat(securityIssue.threatCategory).isEqualTo("critical");
  }

  @Test
  public void testConvertToDTOFromComponentEvaluationDataWithSonatypeUrl() {
    ComponentEvaluationData componentDetailsFromHds = new ComponentEvaluationData();
    List<com.sonatype.clm.dto.model.SecurityVulnerability> vulnerabilities = new ArrayList<>();
    com.sonatype.clm.dto.model.SecurityVulnerability vuln = new com.sonatype.clm.dto.model.SecurityVulnerability();
    vuln.setSource("sonatype");
    vuln.setRefId("reference");
    vuln.setSeverity(9.4F);
    vuln.setUrl(null);

    vulnerabilities.add(vuln);
    componentDetailsFromHds.securityVulnerabilities = vulnerabilities;

    ApiSecurityDataDTO dto = adapter.convertToDTO(componentDetailsFromHds);
    assertThat(dto).isNotNull();
    List<ApiSecurityIssueDTO> securityIssues = dto.securityIssues;
    assertThat(securityIssues).isNotNull();
    assertThat(securityIssues).hasSize(1);
    ApiSecurityIssueDTO securityIssue = securityIssues.get(0);
    assertThat(securityIssue).isNotNull();
    assertThat(securityIssue.source).isEqualTo("sonatype");
    assertThat(securityIssue.reference).isEqualTo("reference");
    assertThat(securityIssue.severity).isEqualTo(9.4F);
    assertThat(securityIssue.status).isEqualTo("Open");
    assertThat(securityIssue.url).isEqualTo("http://localhost:8070/ui/links/vln/reference");
    assertThat(securityIssue.threatCategory).isEqualTo("critical");
  }

  @Test
  public void testConvertToDTOFromComponentEvaluationDataWithNullUrl() {
    ComponentEvaluationData componentDetailsFromHds = new ComponentEvaluationData();
    List<com.sonatype.clm.dto.model.SecurityVulnerability> vulnerabilities = new ArrayList<>();
    com.sonatype.clm.dto.model.SecurityVulnerability vuln = new com.sonatype.clm.dto.model.SecurityVulnerability();
    vuln.setSource("source");
    vuln.setRefId("reference");
    vuln.setSeverity(9.4F);
    vuln.setUrl(null);

    vulnerabilities.add(vuln);
    componentDetailsFromHds.securityVulnerabilities = vulnerabilities;

    ApiSecurityDataDTO dto = adapter.convertToDTO(componentDetailsFromHds);
    assertThat(dto).isNotNull();
    List<ApiSecurityIssueDTO> securityIssues = dto.securityIssues;
    assertThat(securityIssues).isNotNull();
    assertThat(securityIssues).hasSize(1);
    ApiSecurityIssueDTO securityIssue = securityIssues.get(0);
    assertThat(securityIssue).isNotNull();
    assertThat(securityIssue.source).isEqualTo("source");
    assertThat(securityIssue.reference).isEqualTo("reference");
    assertThat(securityIssue.severity).isEqualTo(9.4F);
    assertThat(securityIssue.status).isEqualTo("Open");
    assertThat(securityIssue.url).isEqualTo(null);
    assertThat(securityIssue.threatCategory).isEqualTo("critical");
  }

  @Test
  public void flagFalse_customDataOmitted_evenWhenOverridesPresent() {
    // Part of BDD-002 (flag=false) unit-level coverage.
    Component component = componentWithVulnHavingOverrides("remed", "CWE-79", "AV:N", 8.1f);

    ApiSecurityDataDTO dto = adapter.convertToDTO(component, false);

    assertThat(dto.securityIssues).hasSize(1);
    assertThat(dto.securityIssues.get(0).customData).isNull();
  }

  @Test
  public void flagTrue_noOverrides_customDataOmitted() {
    // BDD-026, AT-023 (holder present, all inner fields null → omit key)
    Component component = componentWithVulnHavingOverrides(null, null, null, null);

    ApiSecurityDataDTO dto = adapter.convertToDTO(component, true);

    assertThat(dto.securityIssues.get(0).customData).isNull();
  }

  @Test
  public void flagTrue_domainObjectMissing_customDataOmitted() {
    // BDD-027, AT-024 (holder absent → omit key, no NPE)
    Component component = new Component();
    SecurityVulnerability vuln = new SecurityVulnerability();
    vuln.setSource("cve");
    vuln.setRefId("CVE-2024-1234");
    vuln.setSeverity(7.5f);
    vuln.setStatus(SecurityVulnerabilityOverrideStatus.OPEN);
    // no setSecurityVulnerabilityCustomData() call
    component.setSecurityVulnerabilities(java.util.Collections.singletonList(vuln));

    ApiSecurityDataDTO dto = adapter.convertToDTO(component, true);

    assertThat(dto.securityIssues.get(0).customData).isNull();
  }

  @Test
  public void flagTrue_allFourFieldsSet_customDataPopulated() {
    // BDD-020, AT-020
    Component component = componentWithVulnHavingOverrides("Upgrade", "CWE-79", "AV:N/AC:L", 9.8f);

    ApiSecurityDataDTO dto = adapter.convertToDTO(component, true);

    SecurityVulnerabilityCustomDataDTO cd = dto.securityIssues.get(0).customData;
    assertThat(cd).isNotNull();
    assertThat(cd.remediation).isEqualTo("Upgrade");
    assertThat(cd.cweId).isEqualTo("CWE-79");
    assertThat(cd.cvssVector).isEqualTo("AV:N/AC:L");
    assertThat(cd.cvssSeverity).isEqualTo(9.8f);
  }

  @Test
  public void flagTrue_onlyRemediationSet_otherFieldsOmitted() {
    // BDD-021, AT-021
    Component component = componentWithVulnHavingOverrides("Upgrade", null, null, null);

    ApiSecurityDataDTO dto = adapter.convertToDTO(component, true);

    SecurityVulnerabilityCustomDataDTO cd = dto.securityIssues.get(0).customData;
    assertThat(cd).isNotNull();
    assertThat(cd.remediation).isEqualTo("Upgrade");
    assertThat(cd.cweId).isNull();
    assertThat(cd.cvssVector).isNull();
    assertThat(cd.cvssSeverity).isNull();
  }

  @Test
  public void flagTrue_onlyCweIdSet_otherFieldsOmitted() {
    // BDD-022, AT-021
    Component component = componentWithVulnHavingOverrides(null, "CWE-79", null, null);

    ApiSecurityDataDTO dto = adapter.convertToDTO(component, true);

    SecurityVulnerabilityCustomDataDTO cd = dto.securityIssues.get(0).customData;
    assertThat(cd).isNotNull();
    assertThat(cd.cweId).isEqualTo("CWE-79");
    assertThat(cd.remediation).isNull();
    assertThat(cd.cvssVector).isNull();
    assertThat(cd.cvssSeverity).isNull();
  }

  @Test
  public void flagTrue_onlyCvssVectorSet_otherFieldsOmitted() {
    // BDD-023, AT-021
    Component component = componentWithVulnHavingOverrides(null, null, "AV:N/AC:L", null);

    ApiSecurityDataDTO dto = adapter.convertToDTO(component, true);

    SecurityVulnerabilityCustomDataDTO cd = dto.securityIssues.get(0).customData;
    assertThat(cd).isNotNull();
    assertThat(cd.cvssVector).isEqualTo("AV:N/AC:L");
    assertThat(cd.remediation).isNull();
    assertThat(cd.cweId).isNull();
    assertThat(cd.cvssSeverity).isNull();
  }

  @Test
  public void flagTrue_onlyCvssSeveritySet_otherFieldsOmitted() {
    // BDD-024, AT-021
    Component component = componentWithVulnHavingOverrides(null, null, null, 9.8f);

    ApiSecurityDataDTO dto = adapter.convertToDTO(component, true);

    SecurityVulnerabilityCustomDataDTO cd = dto.securityIssues.get(0).customData;
    assertThat(cd).isNotNull();
    assertThat(cd.cvssSeverity).isEqualTo(9.8f);
    assertThat(cd.remediation).isNull();
    assertThat(cd.cweId).isNull();
    assertThat(cd.cvssVector).isNull();
  }

  @Test
  public void flagTrue_mixedTwoFieldOverride_onlyBothSetFieldsEmitted() {
    // BDD-025, AT-022
    Component component = componentWithVulnHavingOverrides("Upgrade", null, null, 9.8f);

    ApiSecurityDataDTO dto = adapter.convertToDTO(component, true);

    SecurityVulnerabilityCustomDataDTO cd = dto.securityIssues.get(0).customData;
    assertThat(cd).isNotNull();
    assertThat(cd.remediation).isEqualTo("Upgrade");
    assertThat(cd.cvssSeverity).isEqualTo(9.8f);
    assertThat(cd.cweId).isNull();
    assertThat(cd.cvssVector).isNull();
  }

  @Test
  public void flagTrue_stockFieldsUnchangedByOverrides() {
    // BDD-028, AT-025
    Component component = new Component();
    SecurityVulnerability vuln = new SecurityVulnerability();
    vuln.setSource("cve");
    vuln.setRefId("CVE-2024-1234");
    vuln.setSeverity(7.5f); // stock
    vuln.setVector("CVSS:3.1/A"); // stock
    vuln.setCwe("CWE-20"); // stock
    vuln.setStatus(SecurityVulnerabilityOverrideStatus.OPEN);

    com.sonatype.insight.brain.model.component.SecurityVulnerabilityCustomData cd =
        new com.sonatype.insight.brain.model.component.SecurityVulnerabilityCustomData();
    cd.setRemediation("R");
    cd.setCweId("CWE-79"); // override
    cd.setCvssVector("CVSS:3.1/B"); // override
    cd.setCvssSeverity(8.1f); // override
    vuln.setSecurityVulnerabilityCustomData(cd);
    component.setSecurityVulnerabilities(java.util.Collections.singletonList(vuln));

    ApiSecurityDataDTO dto = adapter.convertToDTO(component, true);
    ApiSecurityIssueDTO sv = dto.securityIssues.get(0);

    // Stock unchanged
    assertThat(sv.severity).isEqualTo(7.5f);
    assertThat(sv.cvssVector).isEqualTo("CVSS:3.1/A");
    assertThat(sv.cwe).isEqualTo("CWE-20");
    // Overrides in nested customData only
    assertThat(sv.customData.cvssSeverity).isEqualTo(8.1f);
    assertThat(sv.customData.cvssVector).isEqualTo("CVSS:3.1/B");
    assertThat(sv.customData.cweId).isEqualTo("CWE-79");
  }

  private Component componentWithVulnHavingOverrides(
      String remediation,
      String cweId,
      String cvssVector,
      Float cvssSeverity)
  {
    Component component = new Component();
    SecurityVulnerability vuln = new SecurityVulnerability();
    vuln.setSource("cve");
    vuln.setRefId("CVE-2024-1234");
    vuln.setSeverity(7.5f);
    vuln.setStatus(SecurityVulnerabilityOverrideStatus.OPEN);

    com.sonatype.insight.brain.model.component.SecurityVulnerabilityCustomData cd =
        new com.sonatype.insight.brain.model.component.SecurityVulnerabilityCustomData();
    cd.setRemediation(remediation);
    cd.setCweId(cweId);
    cd.setCvssVector(cvssVector);
    cd.setCvssSeverity(cvssSeverity);
    vuln.setSecurityVulnerabilityCustomData(cd);

    component.setSecurityVulnerabilities(java.util.Collections.singletonList(vuln));
    return component;
  }
}
