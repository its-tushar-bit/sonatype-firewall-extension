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
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverrideStatus;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiSecurityDataAdapterTest extends AbstractComponentTest
{
  @Inject
  private ApiSecurityDataAdapter adapter;

  @Before
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
}
