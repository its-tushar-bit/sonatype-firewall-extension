/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.componentsearch.api;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.ComponentNearestFixedVersions;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.componentsearch.dto.ComponentSearchPageResultDTO;
import com.sonatype.insight.brain.hds.AffectedComponentDTO;
import com.sonatype.insight.brain.hds.AffectedComponentList;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for ApiComponentSearchResource using actual HTTP calls.
 * Tests basic happy-path functioning and various non authn/authz related error conditions.
 */
public class ApiComponentSearchResourceTest
    extends AbstractResourceTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.COMPONENT_SEARCH_RESOURCE_PATH);
  }

  @Test
  public void testGetCveAffectedComponents_ValidRequest_ReturnsResults() throws Exception {
    Application app = tempEntity.newApplication("Test App", "testapp", Organization.ROOT_ORGANIZATION_ID);

    tempEntity.newPolicyEvaluation(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "scan-123",
        new Date());

    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates(
        "com.example",
        "vulnerable-lib",
        "1.0.0");

    tempEntity.newApplicationComponent(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "hash-123",
        componentId,
        "pkg:maven/com.example/vulnerable-lib@1.0.0?type=jar");

    setupHdsMocksForStandardSearch();

    HttpResponse response = restRequest()
        .path("/cveAffectedComponents")
        .query("cveId", "CVE-2025-55182")
        .query("pageNumber", "1")
        .query("pageSize", "10")
        .query("sortOrder", "asc")
        .get();

    assertResponseStatus(200, response);

    ComponentSearchPageResultDTO result = response.getBody(ComponentSearchPageResultDTO.class);
    assertThat(result).isNotNull();
    assertThat(result.getPageNumber()).isEqualTo(1);
    assertThat(result.getPageSize()).isEqualTo(10);
    assertThat(result.getTotalCount()).isEqualTo(1);
    assertThat(result.getResults()).isNotEmpty();
    assertThat(result.getAggregates()).isNotNull();
  }

  @Test
  public void testGetCveAffectedComponents_MissingCveId_ReturnsBadRequest() throws Exception {
    HttpResponse response = restRequest()
        .path("/cveAffectedComponents")
        .query("pageNumber", "1")
        .query("pageSize", "10")
        .get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("At least one CVE ID is required");
  }

  @Test
  public void testGetCveAffectedComponents_InvalidSortOrder_ReturnsBadRequest() throws Exception {
    HttpResponse response = restRequest()
        .path("/cveAffectedComponents")
        .query("cveId", "CVE-2025-55182")
        .query("sortOrder", "invalid")
        .get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).contains("sortOrder must be either 'asc' or 'desc'");
  }

  @Test
  public void testGetCveAffectedComponents_InvalidSortBy_ReturnsBadRequest() throws Exception {
    HttpResponse response = restRequest()
        .path("/cveAffectedComponents")
        .query("cveId", "CVE-2025-55182")
        .query("sortBy", "invalidField")
        .get();

    assertResponseStatus(400, response);
  }

  @Test
  public void testExportComponentSearchReport_ValidRequest_ReturnsCsv() throws Exception {
    Application app = tempEntity.newApplication("Test App", "testapp", Organization.ROOT_ORGANIZATION_ID);

    tempEntity.newPolicyEvaluation(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "scan-123",
        new Date());

    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates(
        "com.example",
        "vulnerable-lib",
        "1.0.0");

    tempEntity.newApplicationComponent(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "hash-123",
        componentId,
        "pkg:maven/com.example/vulnerable-lib@1.0.0?type=jar");

    setupHdsMocksForStandardSearch();

    HttpResponse response = restRequest()
        .path("/downloadComponentSearchReport")
        .query("cveId", "CVE-2025-55182")
        .get();

    assertResponseStatus(200, response);
    assertThat(response.getHeader("Content-Type")).contains("text/csv");
    assertThat(response.getBodyText()).contains("Application Name,Application ID");
    assertThat(response.getBodyText()).contains("Test App");
  }

  @Test
  public void testExportComponentSearchReport_EmptyResults_ReturnsCsvWithHeaderOnly() throws Exception {
    // Create app with evaluation but no matching components
    Application app = tempEntity.newApplication("Test App", "testapp", Organization.ROOT_ORGANIZATION_ID);

    tempEntity.newPolicyEvaluation(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "scan-123",
        new Date());

    // HDS returns empty list - no components affected by this CVE
    hdsRespondWith(new AffectedComponentList(List.of(), null, null))
        .atUri("/rest/vulnerability/affected?refId=CVE-2025-99999")
        .withoutLicense();

    hdsRespondWith("{\"vulnerabilities\":{}}")
        .atUri("/rest/vulnerability/details/json")
        .withoutLicense();

    HttpResponse response = restRequest()
        .path("/downloadComponentSearchReport")
        .query("cveId", "CVE-2025-99999")
        .get();

    assertResponseStatus(200, response);
    assertThat(response.getHeader("Content-Type")).contains("text/csv");
    assertThat(response.getBodyText()).contains("Application Name,Application ID");
  }

  @Test
  public void testExportComponentSearchReport_NoCveId_DefaultsToReact2Shell() throws Exception {
    Application app = tempEntity.newApplication("Test App", "testapp", Organization.ROOT_ORGANIZATION_ID);

    tempEntity.newPolicyEvaluation(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "scan-123",
        new Date());

    ComponentIdentifier componentId = ComponentIdentifier.createMavenCoordinates(
        "com.example",
        "vulnerable-lib",
        "1.0.0");

    tempEntity.newApplicationComponent(
        app.getId(),
        StageTypes.STAGE_RELEASE.getId(),
        "hash-123",
        componentId,
        "pkg:maven/com.example/vulnerable-lib@1.0.0?type=jar");

    setupHdsMocksForStandardSearch();

    HttpResponse response = restRequest()
        .path("/downloadComponentSearchReport")
        .get();

    assertResponseStatus(200, response);
    assertThat(response.getHeader("Content-Type")).contains("text/csv");
    assertThat(response.getBodyText()).contains("Application Name,Application ID");
    assertThat(response.getBodyText()).contains("Test App");
  }

  @Test
  public void testGetCveAffectedComponents_InvalidPageNumber_ReturnsBadRequest() throws Exception {
    HttpResponse response = restRequest()
        .path("/cveAffectedComponents")
        .query("cveId", "CVE-2025-55182")
        .query("pageNumber", "0")
        .get();

    assertResponseStatus(400, response);
  }

  @Test
  public void testGetCveAffectedComponents_NegativePageNumber_ReturnsBadRequest() throws Exception {
    HttpResponse response = restRequest()
        .path("/cveAffectedComponents")
        .query("cveId", "CVE-2025-55182")
        .query("pageNumber", "-1")
        .get();

    assertResponseStatus(400, response);
  }

  @Test
  public void testGetCveAffectedComponents_ZeroPageSize_ReturnsBadRequest() throws Exception {
    HttpResponse response = restRequest()
        .path("/cveAffectedComponents")
        .query("cveId", "CVE-2025-55182")
        .query("pageSize", "0")
        .get();

    assertResponseStatus(400, response);
  }

  @Test
  public void testGetCveAffectedComponents_PageSizeExceedsMax_ReturnsBadRequest() throws Exception {
    HttpResponse response = restRequest()
        .path("/cveAffectedComponents")
        .query("cveId", "CVE-2025-55182")
        .query("pageSize", "1001")
        .get();

    assertResponseStatus(400, response);
  }

  private void setupHdsMocksForStandardSearch() {
    List<AffectedComponentDTO> affectedComponents = List.of(
        new AffectedComponentDTO("maven", "com.example", "vulnerable-lib", "1.0.0", null));
    AffectedComponentList response = new AffectedComponentList(affectedComponents, null, null);
    hdsRespondWith(response)
        .atUri("/rest/vulnerability/affected?refId=CVE-2025-55182")
        .withoutLicense();

    String vulnDataJson = """
        {
          "vulnerabilities": {
            "CVE-2025-55182": {
              "identifier": "CVE-2025-55182",
              "severity": "HIGH",
              "cvssScore": 7.5
            }
          }
        }
        """;
    hdsRespondWith(vulnDataJson)
        .atUri("/rest/vulnerability/details/json")
        .withoutLicense();

    ComponentNearestFixedVersions fixedVersions = new ComponentNearestFixedVersions();
    fixedVersions.setPackageUrl("pkg:maven/com.example/vulnerable-lib@1.0.0?type=jar");
    ComponentNearestFixedVersions.ComponentNearestFixedVersionsRanges range =
        new ComponentNearestFixedVersions.ComponentNearestFixedVersionsRanges();
    range.setIdentifier("CVE-2025-55182");
    range.setNearestFixedUpgrade("1.0.1");
    fixedVersions.setSecurityIssues(new HashSet<>(List.of(range)));

    hdsRespondWith(List.of(fixedVersions))
        .atUri("/api/v2/component/nearestFixedVersions")
        .withoutLicense();
  }
}
