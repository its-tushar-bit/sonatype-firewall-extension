/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import static com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus.PENDING;
import static com.sonatype.insight.brain.search.export.SearchRowFactory.Header.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.search.export.SearchRowFactory;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractApiAdvancedSearchResourceV2Test
    extends AbstractResourceTest
{
  private IndexService indexService;

  @Before
  public void before() throws Exception {
    TaskScheduler taskScheduler = getCLMServer().getInstance(TaskScheduler.class);
    taskScheduler.disableForTesting = false;
    taskScheduler.start();
    indexService = getCLMServer().getInstance(IndexService.class);
    indexService.disableForTesting = false;
    indexService.register();
  }

  @Test
  public void testCreateSearchIndex() throws Exception {
    HttpResponse response = restRequest().path(ApiAdvancedSearchResourceV2.INDEX_PATH).post();
    awaitIndexCompletion();

    assertResponseStatus(204, response);
    // verify the index exists
    long size = indexService.getIndexSize();
    assertThat(size).isGreaterThan(0);
  }

  @Test
  public void testSearchIndex() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    restRequest().path(ApiAdvancedSearchResourceV2.INDEX_PATH).post();
    awaitIndexCompletion();

    HttpResponse response =
        restRequest().query("query", FieldIdentifier.APPLICATION_ID.label + ":" + application.getId()).get();

    assertResponseStatus(200, response);
    SearchResultDTO searchResultDTO = response.getBody(SearchResultDTO.class);
    assertThat(searchResultDTO.groupingByDTOS).hasSize(1);
    GroupingByDTO groupingByDTO = searchResultDTO.groupingByDTOS.get(0);
    assertThat(groupingByDTO.searchResultItemDTOS).hasSize(1);
    SearchResultItemDTO searchResultItemDTO = groupingByDTO.searchResultItemDTOS.get(0);
    assertThat(searchResultItemDTO.applicationId).isEqualTo(application.getId());
  }

  @Test
  public void testSearchIndex_SBOMManagerMode() throws Exception {
    setFeatures(LicensedFeature.SBOM_MANAGER);
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newSbomEvaluation(app,
        "1.0",
        SbomSpecification.SPDX.toString(),
        PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier.createAnameCoordinates("n", null, "v1")),
        "someScanId1",
        true,
        PENDING);
    restRequest().path(ApiAdvancedSearchResourceV2.INDEX_PATH).post();
    awaitIndexCompletion();

    HttpResponse response = restRequest()
        .query("query", FieldIdentifier.COMPONENT_NAME.label + ":" + "*")
        .query("mode", "sbomManager")
        .get();

    assertResponseStatus(200, response);
    SearchResultDTO searchResultDTO = response.getBody(SearchResultDTO.class);
    assertThat(searchResultDTO.groupingByDTOS).hasSize(1);
    GroupingByDTO groupingByDTO = searchResultDTO.groupingByDTOS.get(0);
    assertThat(groupingByDTO.searchResultItemDTOS).hasSize(1);
    SearchResultItemDTO searchResultItemDTO = groupingByDTO.searchResultItemDTOS.get(0);
    assertThat(searchResultItemDTO.applicationVersion).isEqualTo("1.0");
    assertThat(searchResultItemDTO.componentName).isEqualTo("n v1");
  }

  @Test
  public void testSearchIndex_SBOMManagerMode_MissingLicensedFeature() throws Exception {
    HttpResponse response = restRequest()
        .query("query", FieldIdentifier.COMPONENT_NAME.label + ":" + "*")
        .query("mode", "sbomManager")
        .get();

    assertResponseStatus(402, response);
    assertThat(response.getBodyText()).contains("The SBOM Manager feature is not supported by your license.");
  }

  @Test
  public void testSearchIndex_DefaultMode_MissingProductSupportingDefaultMode() throws Exception {
    setLicenseProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);

    HttpResponse response = restRequest()
        .query("query", FieldIdentifier.COMPONENT_NAME.label + ":" + "*")
        .get();

    assertResponseStatus(402, response);
    assertThat(response.getBodyText()).contains("Only SBOM Manager mode is supported by your license.");
  }

  @Test
  public void testSearchIndex_DefaultMode() throws Exception {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    tempEntity.newSbomEvaluation(app,
        "1.0",
        SbomSpecification.SPDX.toString(),
        PackageUrlIdentifier.fromComponentIdentifier(ComponentIdentifier.createAnameCoordinates("n", null, "v1")),
        "someScanId1",
        true,
        PENDING);
    restRequest().path(ApiAdvancedSearchResourceV2.INDEX_PATH).post();
    awaitIndexCompletion();

    HttpResponse response = restRequest()
        .query("query", FieldIdentifier.COMPONENT_NAME.label + ":" + "*")
        .get();

    assertResponseStatus(200, response);
    SearchResultDTO searchResultDTO = response.getBody(SearchResultDTO.class);
    assertThat(searchResultDTO.groupingByDTOS).isEmpty();
  }

  @Test
  public void testSearchIndex_TokenMgrError() throws Exception {
    restRequest().path(ApiAdvancedSearchResourceV2.INDEX_PATH).post();
    awaitIndexCompletion();

    HttpResponse response = restRequest().query("query", "\"").get();

    assertResponseStatus(400, response);
  }

  @Test
  public void testSearchIndex_Unauthenticated() throws Exception {
    HttpResponse response =
        restRequest().anon().query("query", FieldIdentifier.APPLICATION_ID.label + ":" + "i-am-anon").get();

    assertResponseStatus(401, response);
  }

  @Test
  public void testGetExportResults() throws Exception {
    restRequest().path(ApiAdvancedSearchResourceV2.INDEX_PATH).post();
    awaitIndexCompletion();

    HttpResponse response =
        restRequest().path(ApiAdvancedSearchResourceV2.EXPORT_CSV_REPORT_PATH).query("query", "*").get();
    assertResponseStatus(200, response);
    String[] csvExportSearchHeaders =
        Arrays.stream(response.getBodyText().split(",")).map(String::trim).toArray(String[]::new);
    assertThat(csvExportSearchHeaders).isEqualTo(
        Arrays.asList(ITEM_TYPE, ORGANIZATION, ORGANIZATION_LINK, APPLICATION, APPLICATION_LINK, APPLICATION_CATEGORY,
            APPLICATION_CATEGORY_LINK, COMPONENT_LABEL, COMPONENT_LABEL_LINK, POLICY, THREAT, POLICY_LINK,
            COMPONENT_NAME, REPORT, SECURITY_ISSUE, STAGE,
            POLICY_VIOLATION_NAME, POLICY_VIOLATION_THREAT_CATEGORY, POLICY_VIOLATION_THREAT_LEVEL_EXPORT,
            POLICY_VIOLATION_WAIVER_STATUS,
            COMPONENT_EFFECTIVE_LICENSE, COMPONENT_LICENSE_THREAT_GROUP, COMPONENT_LICENSE_THREAT_LEVEL_EXPORT)
            .stream()
            .map(SearchRowFactory.Header::getHeader)
            .toArray(String[]::new));
  }

  @Test
  public void testGetExportResults_GivenPageSize() throws Exception {
    String applicationPublicIdPrefix = "testGetExportResults";
    for (int i = 0; i < 10; i++) {
      tempEntity.newApplicationWithParent(applicationPublicIdPrefix + i);
    }

    restRequest().path(ApiAdvancedSearchResourceV2.INDEX_PATH).post();
    awaitIndexCompletion();

    assertApplicationExportResults(applicationPublicIdPrefix, 1, 1, 1);
    assertApplicationExportResults(applicationPublicIdPrefix, 1, 10, 1);
    assertApplicationExportResults(applicationPublicIdPrefix, 1, 11, 0);

    assertApplicationExportResults(applicationPublicIdPrefix, 3, 1, 3);
    assertApplicationExportResults(applicationPublicIdPrefix, 3, 4, 1);
    assertApplicationExportResults(applicationPublicIdPrefix, 3, 5, 0);
  }

  @Test
  public void testGetExportResults_NotGivenPageSize() throws Exception {
    String applicationPublicIdPrefix = "testGetExportResults";
    for (int i = 0; i < 10; i++) {
      tempEntity.newApplicationWithParent("testGetExportResults_Paged" + i);
    }

    restRequest().path(ApiAdvancedSearchResourceV2.INDEX_PATH).post();
    awaitIndexCompletion();

    assertApplicationExportResults(applicationPublicIdPrefix, null, 1, 10);
  }

  private void assertApplicationExportResults(
      String applicationPublicIdPrefix,
      Integer pageSize,
      int page,
      int expectedResults) throws Exception
  {
    HttpResponse response = restRequest()
        .path(ApiAdvancedSearchResourceV2.EXPORT_CSV_REPORT_PATH)
        .query("query", "itemType:APPLICATION AND applicationPublicId:" + applicationPublicIdPrefix + "*")
        .query("pageSize", pageSize)
        .query("page", page)
        .get();
    String[] split = response.getBodyText().split("\n");
    assertThat(split).hasSize(expectedResults + 1);
    for (int i = 0; i < expectedResults; i++) {
      assertThat(split[i + 1]).contains(applicationPublicIdPrefix);
    }
  }

  @Test
  public void testGetExportResults_SBOMManagerMode() throws Exception {
    setFeatures(LicensedFeature.SBOM_MANAGER);
    restRequest().path(ApiAdvancedSearchResourceV2.INDEX_PATH).post();
    awaitIndexCompletion();

    HttpResponse response = restRequest()
        .path(ApiAdvancedSearchResourceV2.EXPORT_CSV_REPORT_PATH)
        .query("mode", "sbomManager")
        .query("query", "*")
        .get();
    assertResponseStatus(200, response);
    String[] csvExportSearchHeaders =
        Arrays.stream(response.getBodyText().split(",")).map(String::trim).toArray(String[]::new);
    assertThat(csvExportSearchHeaders).isEqualTo(
        Arrays.asList(ITEM_TYPE, ORGANIZATION, ORGANIZATION_LINK, APPLICATION, APPLICATION_LINK, APPLICATION_CATEGORY,
            APPLICATION_CATEGORY_LINK, POLICY, THREAT, POLICY_LINK, COMPONENT_NAME, SECURITY_ISSUE,
            SECURITY_ISSUE_ID, APPLICATION_VERSION, SBOM_SPECIFICATION,
            POLICY_VIOLATION_NAME, POLICY_VIOLATION_THREAT_CATEGORY, POLICY_VIOLATION_THREAT_LEVEL_EXPORT,
            POLICY_VIOLATION_WAIVER_STATUS,
            COMPONENT_EFFECTIVE_LICENSE, COMPONENT_LICENSE_THREAT_GROUP, COMPONENT_LICENSE_THREAT_LEVEL_EXPORT)
            .stream()
            .map(SearchRowFactory.Header::getHeader)
            .toArray(String[]::new));
  }

  @Test
  public void testGetExportResults_PolicyViolation_LifecycleMode() throws Exception {
    restRequest().path(ApiAdvancedSearchResourceV2.INDEX_PATH).post();
    awaitIndexCompletion();

    HttpResponse response = restRequest()
        .path(ApiAdvancedSearchResourceV2.EXPORT_CSV_REPORT_PATH)
        .query("query", "itemType:POLICY_VIOLATION")
        .get();
    assertResponseStatus(200, response);
    String headerLine = response.getBodyText().split("\n")[0];
    String[] csvExportSearchHeaders = Arrays.stream(headerLine.split(",")).map(String::trim).toArray(String[]::new);
    assertThat(csvExportSearchHeaders).contains(
        ITEM_TYPE.getHeader(),
        ORGANIZATION.getHeader(),
        APPLICATION.getHeader(),
        COMPONENT_NAME.getHeader(),
        POLICY_VIOLATION_NAME.getHeader(),
        POLICY_VIOLATION_THREAT_CATEGORY.getHeader(),
        POLICY_VIOLATION_THREAT_LEVEL_EXPORT.getHeader(),
        POLICY_VIOLATION_WAIVER_STATUS.getHeader(),
        STAGE.getHeader());
  }

  @Test
  public void testGetExportResults_LegalViolation_LifecycleMode() throws Exception {
    restRequest().path(ApiAdvancedSearchResourceV2.INDEX_PATH).post();
    awaitIndexCompletion();

    HttpResponse response = restRequest()
        .path(ApiAdvancedSearchResourceV2.EXPORT_CSV_REPORT_PATH)
        .query("query", "itemType:LEGAL_VIOLATION")
        .get();
    assertResponseStatus(200, response);
    String headerLine = response.getBodyText().split("\n")[0];
    String[] csvExportSearchHeaders = Arrays.stream(headerLine.split(",")).map(String::trim).toArray(String[]::new);
    assertThat(csvExportSearchHeaders).contains(
        ITEM_TYPE.getHeader(),
        ORGANIZATION.getHeader(),
        APPLICATION.getHeader(),
        COMPONENT_NAME.getHeader(),
        COMPONENT_EFFECTIVE_LICENSE.getHeader(),
        COMPONENT_LICENSE_THREAT_GROUP.getHeader(),
        COMPONENT_LICENSE_THREAT_LEVEL_EXPORT.getHeader(),
        STAGE.getHeader());
  }

  @Test
  public void testGetExportResults_PolicyViolation_SBOMManagerMode() throws Exception {
    setFeatures(LicensedFeature.SBOM_MANAGER);
    restRequest().path(ApiAdvancedSearchResourceV2.INDEX_PATH).post();
    awaitIndexCompletion();

    HttpResponse response = restRequest()
        .path(ApiAdvancedSearchResourceV2.EXPORT_CSV_REPORT_PATH)
        .query("query", "itemType:POLICY_VIOLATION")
        .query("mode", "sbomManager")
        .get();
    assertResponseStatus(200, response);
    String headerLine = response.getBodyText().split("\n")[0];
    String[] csvExportSearchHeaders = Arrays.stream(headerLine.split(",")).map(String::trim).toArray(String[]::new);
    assertThat(csvExportSearchHeaders).contains(
        ITEM_TYPE.getHeader(),
        APPLICATION.getHeader(),
        COMPONENT_NAME.getHeader(),
        POLICY_VIOLATION_NAME.getHeader(),
        POLICY_VIOLATION_THREAT_CATEGORY.getHeader(),
        POLICY_VIOLATION_THREAT_LEVEL_EXPORT.getHeader(),
        POLICY_VIOLATION_WAIVER_STATUS.getHeader(),
        APPLICATION_VERSION.getHeader(),
        SBOM_SPECIFICATION.getHeader());
  }

  @Test
  public void testGetExportResults_LegalViolation_SBOMManagerMode() throws Exception {
    setFeatures(LicensedFeature.SBOM_MANAGER);
    restRequest().path(ApiAdvancedSearchResourceV2.INDEX_PATH).post();
    awaitIndexCompletion();

    HttpResponse response = restRequest()
        .path(ApiAdvancedSearchResourceV2.EXPORT_CSV_REPORT_PATH)
        .query("query", "itemType:LEGAL_VIOLATION")
        .query("mode", "sbomManager")
        .get();
    assertResponseStatus(200, response);
    String headerLine = response.getBodyText().split("\n")[0];
    String[] csvExportSearchHeaders = Arrays.stream(headerLine.split(",")).map(String::trim).toArray(String[]::new);
    assertThat(csvExportSearchHeaders).contains(
        ITEM_TYPE.getHeader(),
        APPLICATION.getHeader(),
        COMPONENT_NAME.getHeader(),
        COMPONENT_EFFECTIVE_LICENSE.getHeader(),
        COMPONENT_LICENSE_THREAT_GROUP.getHeader(),
        COMPONENT_LICENSE_THREAT_LEVEL_EXPORT.getHeader(),
        APPLICATION_VERSION.getHeader(),
        SBOM_SPECIFICATION.getHeader());
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.ADVANCED_SEARCH_RESOURCE_PATH_V2);
  }

  protected void awaitIndexCompletion() {
    await().atMost(10, TimeUnit.SECONDS)
        .until(() -> !indexService.isFullIndexTriggered());
  }
}
