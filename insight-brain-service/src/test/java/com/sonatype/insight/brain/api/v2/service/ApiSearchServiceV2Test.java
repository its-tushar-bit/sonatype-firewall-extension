/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.net.URISyntaxException;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;
import com.sonatype.insight.brain.dataaccess.ApplicationComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.component.InnerSourceData;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.report.ReportTestUtils.createReportFile;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ApiSearchServiceV2Test
    extends AbstractComponentTest
{
  @Inject
  private ApiSearchServiceV2 apiSearchServiceV2;

  @Inject
  private InsightWork insightWork;

  @Inject
  private BaseUrl baseUrl;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  private ApplicationComponentDAO applicationComponentDAO;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private ReportService reportService;

  @Inject
  private ComponentLoaderFactory componentLoaderFactory;

  @Before
  public void before() {
    setBaseUrl("http://localhost:8070");
  }

  @Test
  public void testSearchComponent_InnerSourceData_WithEnabledComponentSearchApiWithInnerSource() throws URISyntaxException, IOException {
    Application application = tempEntity.newApplication(ROOT_ORGANIZATION_ID);
    ApplicationComponent appComponent1 = tempEntity
        .newApplicationComponent(application.getId(), BuildStageType.ID, "2b8e230d2ab644e4ecaa",
            ComponentIdentifier.createMavenCoordinates("xmlpull", "xmlpull", "1.1.3.1"));
    ApplicationComponent appComponent2 = tempEntity
        .newApplicationComponent(application.getId(), BuildStageType.ID, "e3fd8ced1f52c7574af9",
            ComponentIdentifier.createMavenCoordinates("org.apache.httpcomponents", "httpcore", "4.4.6"));
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-id");

    createReportFile(application.getId(), "scan-id", zipReportDir("/ApiSearchServiceV2Test/report-1", tempDir),
        insightWork);

    ApiSearchResultsDTOV2 result = apiSearchServiceV2
        .searchComponent(BuildStageType.ID, appComponent1.getHash(), appComponent1.getComponentIdentifier(),
            "pkg:maven/xmlpull/xmlpull@1.1.3.1");

    assertThat(result).isNotNull();
    assertThat(result.results.get(0).dependencyData.directDependency).isFalse();
    assertThat(result.results.get(0).dependencyData.parentComponentPurls)
        .containsExactly("pkg:maven/com.sonatype.insight.scan/insight-module-model@1.0.0-SNAPSHOT?type=jar");
    assertThat(result.results.get(0).dependencyData.innerSource).isFalse();
    assertThat(result.results.get(0).dependencyData).hasFieldOrProperty(ComponentLoader.INNER_SOURCE_DATA_FIELD);
    assertThat(result.results.get(0).dependencyData.innerSourceData).containsExactly(
        new InnerSourceData("insight-module-model", "7509f572645749eba3e19b826e111c8b",
            "pkg:maven/com.sonatype.insight.scan/insight-module-model@1.0.0-SNAPSHOT?type=jar"));

    result = apiSearchServiceV2
        .searchComponent(BuildStageType.ID, appComponent2.getHash(), appComponent2.getComponentIdentifier(),
            "pkg:maven/org.apache.httpcomponents/httpcore@4.4.6");
    assertThat(result).isNotNull();
    assertThat(result.results.get(0).dependencyData.directDependency).isFalse();
    assertThat(result.results.get(0).dependencyData.parentComponentPurls)
        .containsExactly("pkg:maven/com.sonatype.insight.scan/insight-client-utils@1.0.0-SNAPSHOT?type=jar");
    assertThat(result.results.get(0).dependencyData.innerSource).isFalse();
    assertThat(JsonUtils.writeUnformatted(result)).doesNotContain(ComponentLoader.INNER_SOURCE_DATA_FIELD);
  }

  @Test
  public void testSearchComponent_InnerSourceData_WithEnabledComponentSearchApiWithInnerSource_MultipleParentPurls() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    ApplicationComponent appComponent = tempEntity
        .newApplicationComponent(application.getId(), BuildStageType.ID, "0f5a654e4675769c716e",
            ComponentIdentifier.createMavenCoordinates("com.fasterxml.jackson.core", "jackson-core", "2.9.8"));
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-id");
    createReportFile(application.getId(), "scan-id", zipReportDir("/ApiSearchServiceV2Test/report-2", tempDir),
        insightWork);

    ApiSearchResultsDTOV2 result = apiSearchServiceV2
        .searchComponent(BuildStageType.ID, appComponent.getHash(), appComponent.getComponentIdentifier(),
            "pkg:maven/com.fasterxml.jackson.core/jackson-core@2.9.8");

    assertThat(result).isNotNull();
    assertThat(result.results).hasSize(1);
    assertThat(result.results.get(0).dependencyData).isNotNull();
    assertThat(result.results.get(0).dependencyData.parentComponentPurls).containsExactlyInAnyOrder(
        "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.8?type=jar",
        "pkg:maven/com.fasterxml.jackson.core/jackson-databind@2.9.9?type=jar");
  }

  @Test
  public void testSearchComponent_InnerSourceData_WithDisabledComponentSearchApiWithInnerSource() throws URISyntaxException, IOException {
    SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.setEnabled(false);
    Application application = tempEntity.newApplication(ROOT_ORGANIZATION_ID);
    ApplicationComponent appComponent = tempEntity
        .newApplicationComponent(application.getId(), BuildStageType.ID, "2b8e230d2ab644e4ecaa",
            ComponentIdentifier.createMavenCoordinates("xmlpull", "xmlpull", "1.1.3.1"));
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-id");
    createReportFile(application.getId(), "scan-id", zipReportDir("/ApiSearchServiceV2Test/report-1", tempDir),
        insightWork);

    ApiSearchResultsDTOV2 result = apiSearchServiceV2
        .searchComponent(BuildStageType.ID, appComponent.getHash(), appComponent.getComponentIdentifier(),
            "pkg:maven/xmlpull/xmlpull@1.1.3.1");

    assertThat(result).isNotNull();
    assertThat(JsonUtils.writeUnformatted(result.results.get(0))).doesNotContain("dependencyData");
  }

  /**
   * Guards against reintroducing the N+1 query pattern fixed in CLM-40023: the per-application evaluation, component
   * and per-component violation queries must be replaced by a single batch query each, and each matched application's
   * report must be loaded only once even when several of its components match.
   */
  @Test
  public void testSearchComponent_batchesQueriesAndLoadsEachReportOnce() throws Exception {
    // The report-load-once assertion below depends on the dependency-data path being active. Capture and restore
    // the original value so this test does not leak feature-flag state to other tests.
    boolean originalInnerSourceEnabled =
        SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.isEnabled();
    SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.setEnabled(true);
    try {
      // Two applications, each with two components covered by the same report, so an all-wildcard search matches
      // multiple components per application.
      newAppWithTwoComponentsAndReport();
      newAppWithTwoComponentsAndReport();

      PolicyEvaluationDAO spyPolicyEvaluationDAO = spy(policyEvaluationDAO);
      ApplicationComponentDAO spyApplicationComponentDAO = spy(applicationComponentDAO);
      PolicyViolationDAO spyPolicyViolationDAO = spy(policyViolationDAO);
      ReportService spyReportService = spy(reportService);

      // Constructed directly (bypassing the @AuthzFilter proxy) so the spied DAOs can observe the query pattern;
      // authorization behavior is covered by ApiSearchServiceV2AuthzTest.
      ApiSearchServiceV2 service = new ApiSearchServiceV2(baseUrl, applicationDAO, spyPolicyEvaluationDAO,
          spyApplicationComponentDAO, spyPolicyViolationDAO, spyReportService, componentLoaderFactory);

      ApiSearchResultsDTOV2 result = service.searchComponent(BuildStageType.ID, null,
          ComponentIdentifier.createMavenCoordinates("", "", ""), null);

      assertThat(result.results).hasSize(4);

      // Batched: one query each regardless of the number of applications/components.
      verify(spyPolicyEvaluationDAO, times(1)).getLastPrimaryByApplicationIdsAndStageId(anySet(),
          eq(BuildStageType.ID));
      verify(spyApplicationComponentDAO, times(1)).getByApplicationIdsAndStageTypeId(anySet(), eq(BuildStageType.ID));
      verify(spyPolicyViolationDAO, times(1))
          .getActiveByApplicationIdsAndStageIdAndHashes(anySet(), eq(BuildStageType.ID), anySet());

      // The per-application / per-component query variants must not be used.
      verify(spyPolicyEvaluationDAO, never()).getLastPrimaryByApplicationIdAndStageId(anyString(), anyString());
      verify(spyApplicationComponentDAO, never()).getByApplicationIdAndStageTypeId(anyString(), anyString());
      verify(spyPolicyViolationDAO, never())
          .getActiveByApplicationIdAndStageIdAndHash(anyString(), anyString(), anyString());

      // Each application's report is loaded exactly once even though two of its components matched.
      verify(spyReportService, times(2)).getReport(anyString(), anyString());
    }
    finally {
      SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.setEnabled(originalInnerSourceEnabled);
    }
  }

  /**
   * Guards the CLM-40023 memory fix: a hash search must filter the components in SQL (loading only the matching rows)
   * via {@code getMapByApplicationIdsAndStageTypeIdsAndHashes} rather than materialising every inspected application's
   * components through the full {@code getByApplicationIdsAndStageTypeId} scan. An upper-case hash is used to confirm
   * matching stays case-insensitive after moving the filter into SQL.
   */
  @Test
  public void testSearchComponent_byHash_filtersComponentsInSqlAndIsCaseInsensitive() throws Exception {
    // Both applications contain a component with hash "2b8e230d2ab644e4ecaa".
    newAppWithTwoComponentsAndReport();
    newAppWithTwoComponentsAndReport();

    ApplicationComponentDAO spyApplicationComponentDAO = spy(applicationComponentDAO);
    PolicyEvaluationDAO spyPolicyEvaluationDAO = spy(policyEvaluationDAO);
    PolicyViolationDAO spyPolicyViolationDAO = spy(policyViolationDAO);

    ApiSearchServiceV2 service = new ApiSearchServiceV2(baseUrl, applicationDAO, spyPolicyEvaluationDAO,
        spyApplicationComponentDAO, spyPolicyViolationDAO, reportService, componentLoaderFactory);

    ApiSearchResultsDTOV2 result = service.searchComponent(BuildStageType.ID, "2B8E230D2AB644E4ECAA", null, null);

    assertThat(result.results).hasSize(2);
    assertThat(result.results).allSatisfy(r -> assertThat(r.hash).isEqualTo("2b8e230d2ab644e4ecaa"));

    // Hash searches filter in SQL: only the matching rows are loaded, so the full component scan must not be used.
    verify(spyApplicationComponentDAO, times(1))
        .getMapByApplicationIdsAndStageTypeIdsAndHashes(anySet(), anySet(), anySet());
    verify(spyApplicationComponentDAO, never()).getByApplicationIdsAndStageTypeId(anySet(), anyString());

    // The evaluation and violation queries stay batched (no per-application variants).
    verify(spyPolicyEvaluationDAO, times(1)).getLastPrimaryByApplicationIdsAndStageId(anySet(), eq(BuildStageType.ID));
    verify(spyPolicyEvaluationDAO, never()).getLastPrimaryByApplicationIdAndStageId(anyString(), anyString());
    verify(spyPolicyViolationDAO, times(1))
        .getActiveByApplicationIdsAndStageIdAndHashes(anySet(), eq(BuildStageType.ID), anySet());
  }

  private Application newAppWithTwoComponentsAndReport() throws URISyntaxException, IOException {
    Application application = tempEntity.newApplication(ROOT_ORGANIZATION_ID);
    tempEntity.newApplicationComponent(application.getId(), BuildStageType.ID, "2b8e230d2ab644e4ecaa",
        ComponentIdentifier.createMavenCoordinates("xmlpull", "xmlpull", "1.1.3.1"));
    tempEntity.newApplicationComponent(application.getId(), BuildStageType.ID, "e3fd8ced1f52c7574af9",
        ComponentIdentifier.createMavenCoordinates("org.apache.httpcomponents", "httpcore", "4.4.6"));
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-id");
    createReportFile(application.getId(), "scan-id", zipReportDir("/ApiSearchServiceV2Test/report-1", tempDir),
        insightWork);
    return application;
  }
}
