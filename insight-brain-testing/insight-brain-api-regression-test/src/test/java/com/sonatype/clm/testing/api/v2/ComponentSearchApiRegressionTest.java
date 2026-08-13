/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.api.v2;

import java.util.HashSet;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.ComponentNearestFixedVersions;
import com.sonatype.clm.testing.api.AbstractIqApiTest;
import com.sonatype.clm.testing.api.categories.ApiRegressionTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.hds.AffectedComponentDTO;
import com.sonatype.insight.brain.hds.AffectedComponentList;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * API regression suite for {@code api/v2/componentSearch}.
 *
 * <p>
 * Covers: CVE-affected components list (happy path, validation errors); CSV export (happy path,
 * React2Shell default); and the unauthenticated auth contract (401) for both sub-paths.
 *
 * <p>
 * HDS stubs follow the same shape as
 * {@code ApiComponentSearchResourceTest.setupHdsMocksForStandardSearch()} in the service
 * module's integration test suite.
 */
@Category(ApiRegressionTest.class)
public class ComponentSearchApiRegressionTest
    extends AbstractIqApiTest
{
  private static final String CVE_COMPONENTS_PATH =
      PublicApiPaths.COMPONENT_SEARCH_RESOURCE_PATH + "/cveAffectedComponents";

  private static final String DOWNLOAD_REPORT_PATH =
      PublicApiPaths.COMPONENT_SEARCH_RESOURCE_PATH + "/downloadComponentSearchReport";

  private static final String TEST_CVE = "CVE-2025-55182";

  private static final String COMP_GROUP = "com.example";

  private static final String COMP_ARTIFACT = "vulnerable-lib";

  private static final String COMP_VERSION = "1.0.0";

  private static final String COMP_PURL =
      "pkg:maven/" + COMP_GROUP + "/" + COMP_ARTIFACT + "@" + COMP_VERSION + "?type=jar";

  private static final String CSV_APP_HEADER = "Application Name";

  @Test
  public void testGetCveAffectedComponents_returns200() throws Exception {
    seedAppAndComponent();
    setupHdsMocks();

    // apiRequest() required: multiple distinct query params — apiGet() overload supports only one param name
    HttpResponse response = apiRequest()
        .path(CVE_COMPONENTS_PATH)
        .query("cveId", TEST_CVE)
        .query("pageNumber", "1")
        .query("pageSize", "10")
        .get();

    assertResponseStatus(200, response);
    assertThatJson(response.getBodyText()).node("pageNumber").isEqualTo(1);
    assertThatJson(response.getBodyText()).node("results").isArray().hasSize(1);
    assertThatJson(response.getBodyText())
        .node("results[0].packageUrl")
        .asString()
        .contains(COMP_ARTIFACT);
  }

  @Test
  public void testGetCveAffectedComponents_missingCveId_returns400() throws Exception {
    HttpResponse response = apiGet(CVE_COMPONENTS_PATH, "pageNumber", "1");
    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("At least one CVE ID is required");
  }

  @Test
  public void testGetCveAffectedComponents_invalidSortOrder_returns400() throws Exception {
    // apiRequest() required: multiple distinct query params
    HttpResponse response = apiRequest()
        .path(CVE_COMPONENTS_PATH)
        .query("cveId", TEST_CVE)
        .query("sortOrder", "invalid")
        .get();

    assertResponseStatus(400, response);
    assertThat(response.getBodyText()).containsIgnoringCase("sortOrder must be either");
  }

  @Test
  public void testGetCveAffectedComponents_invalidPageNumber_returns400() throws Exception {
    // apiRequest() required: multiple distinct query params
    HttpResponse response = apiRequest()
        .path(CVE_COMPONENTS_PATH)
        .query("cveId", TEST_CVE)
        .query("pageNumber", "0")
        .get();

    assertResponseStatus(400, response);
    // Bean Validation @Min(1) on pageNumber produces a generic Spring Boot error body;
    // assert it is NOT the cveId-missing message to pin it to the pageNumber constraint.
    assertThat(response.getBodyText()).doesNotContain("At least one CVE ID is required");
  }

  @Test
  public void testDownloadComponentSearchReport_returns200WithCsvHeader() throws Exception {
    seedAppAndComponent();
    setupHdsMocks();

    HttpResponse response = apiGet(DOWNLOAD_REPORT_PATH, "cveId", TEST_CVE);

    assertResponseStatus(200, response);
    assertThat(response.getHeader("Content-Type")).contains("text/csv");
    assertThat(response.getBodyText()).contains(CSV_APP_HEADER);
    assertThat(response.getBodyText()).contains(COMP_ARTIFACT);
  }

  @Test
  public void testDownloadComponentSearchReport_noCveId_defaultsToReact2Shell_returns200() throws Exception {
    seedAppAndComponent();
    setupHdsMocks();

    HttpResponse response = apiGet(DOWNLOAD_REPORT_PATH);

    assertResponseStatus(200, response);
    assertThat(response.getHeader("Content-Type")).contains("text/csv");
    assertThat(response.getBodyText()).contains(CSV_APP_HEADER);
    assertThat(response.getBodyText()).contains(COMP_ARTIFACT);
  }

  /** Auth contract: unauthenticated callers get 401, not 200/403/404. */
  @Test
  public void testGetCveAffectedComponents_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(CVE_COMPONENTS_PATH);
    assertResponseStatus(401, response);
  }

  /** Auth contract: unauthenticated callers get 401, not 200/403/404. */
  @Test
  public void testDownloadComponentSearchReport_unauthenticated_returns401() throws Exception {
    HttpResponse response = anonApiGet(DOWNLOAD_REPORT_PATH);
    assertResponseStatus(401, response);
  }

  private void seedAppAndComponent() throws Exception {
    Application app =
        tempEntity.newApplication(uniqueId("comp-search-app"), Organization.ROOT_ORGANIZATION_ID);
    tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, uniqueId("scan-cs"));
    ComponentIdentifier componentId =
        ComponentIdentifier.createMavenCoordinates(COMP_GROUP, COMP_ARTIFACT, COMP_VERSION);
    String componentHash = tempEntity.newRandomHash();
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, componentHash, componentId,
        COMP_PURL);
  }

  private void setupHdsMocks() {
    List<AffectedComponentDTO> affectedComponents =
        List.of(new AffectedComponentDTO("maven", COMP_GROUP, COMP_ARTIFACT, COMP_VERSION, null));
    // withoutLicense(): the embedded test server has no HDS licence; this bypasses the licence check
    hdsRespondWith(new AffectedComponentList(affectedComponents, null, null))
        .atUri("/rest/vulnerability/affected?refId=" + TEST_CVE)
        .withoutLicense();

    // Raw JSON: no public HDS DTO for the /rest/vulnerability/details/json response shape
    String vulnDataJson = "{"
        + "\"vulnerabilities\":{"
        + "\"" + TEST_CVE + "\":{"
        + "\"identifier\":\"" + TEST_CVE + "\","
        + "\"severity\":\"HIGH\","
        + "\"cvssScore\":7.5"
        + "}}}";
    hdsRespondWith(vulnDataJson)
        .atUri("/rest/vulnerability/details/json")
        .withoutLicense(); // embedded server has no HDS licence

    ComponentNearestFixedVersions fixedVersions = new ComponentNearestFixedVersions();
    fixedVersions.setPackageUrl(COMP_PURL);
    ComponentNearestFixedVersions.ComponentNearestFixedVersionsRanges range =
        new ComponentNearestFixedVersions.ComponentNearestFixedVersionsRanges();
    range.setIdentifier(TEST_CVE);
    range.setNearestFixedUpgrade("1.0.1");
    fixedVersions.setSecurityIssues(new HashSet<>(List.of(range)));
    hdsRespondWith(List.of(fixedVersions))
        .atUri("/api/v2/component/nearestFixedVersions")
        .withoutLicense();
  }
}
