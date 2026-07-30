/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiSearchResultsDTOV2;
import com.sonatype.insight.brain.dataaccess.OwnerComponentDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoader;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
  private OwnerComponentDAO applicationComponentDAO;

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
    OwnerComponent appComponent1 = tempEntity
        .newApplicationComponent(application.getId(), BuildStageType.ID, "2b8e230d2ab644e4ecaa",
            ComponentIdentifier.createMavenCoordinates("xmlpull", "xmlpull", "1.1.3.1"));
    OwnerComponent appComponent2 = tempEntity
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
    OwnerComponent appComponent = tempEntity
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
    OwnerComponent appComponent = tempEntity
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
      OwnerComponentDAO spyOwnerComponentDAO = spy(applicationComponentDAO);
      PolicyViolationDAO spyPolicyViolationDAO = spy(policyViolationDAO);
      ReportService spyReportService = spy(reportService);

      // Constructed directly (bypassing the @AuthzFilter proxy) so the spied DAOs can observe the query pattern;
      // authorization behavior is covered by ApiSearchServiceV2AuthzTest.
      ApiSearchServiceV2 service = new ApiSearchServiceV2(baseUrl, applicationDAO, spyPolicyEvaluationDAO,
          spyOwnerComponentDAO, spyPolicyViolationDAO, spyReportService, componentLoaderFactory);

      ApiSearchResultsDTOV2 result = service.searchComponent(BuildStageType.ID, null,
          ComponentIdentifier.createMavenCoordinates("", "", ""), null);

      assertThat(result.results).hasSize(4);

      // Batched: one query each regardless of the number of applications/components.
      verify(spyPolicyEvaluationDAO, times(1)).getLastPrimaryByOwnerIdsAndStageId(anySet(),
          eq(BuildStageType.ID));
      verify(spyOwnerComponentDAO, times(1)).getByOwnerIdsAndStageTypeId(anySet(), eq(BuildStageType.ID));
      verify(spyPolicyViolationDAO, times(1))
          .getActiveByOwnerIdsAndStageIdAndHashes(anySet(), eq(BuildStageType.ID), anySet());

      // The per-application / per-component query variants must not be used.
      verify(spyPolicyEvaluationDAO, never()).getLastPrimaryByOwnerIdAndStageId(anyString(), anyString());
      verify(spyOwnerComponentDAO, never()).getByOwnerIdAndStageTypeId(anyString(), anyString());
      verify(spyPolicyViolationDAO, never())
          .getActiveByOwnerIdAndStageIdAndHash(anyString(), anyString(), anyString());

      // Each application's report is loaded exactly once even though two of its components matched, via the
      // no-recovery getReportIfPresent path; neither recovery getReport overload is used (CLM-41473).
      verify(spyReportService, times(2)).getReportIfPresent(any(Application.class), anyString());
      verify(spyReportService, never()).getReport(anyString(), anyString());
      verify(spyReportService, never()).getReport(any(Application.class), anyString());
    }
    finally {
      SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.setEnabled(originalInnerSourceEnabled);
    }
  }

  /**
   * CLM-41473 regression: when a matched application's report is not stored on disk (the SaaS norm, where reports are
   * purged), the dependency-data path must not fall back to the per-application recovery {@code getReport(appId,
   * scanId)} -- which issued ~4 DB queries per matched application ({@code application}, two {@code policy_evaluation}
   * and {@code proxy_repository_component}) and reintroduced an N+1 that scaled with the number of results. It must use
   * the
   * no-recovery {@code getReportIfPresent} path and return empty dependency data.
   */
  @Test
  public void testSearchComponent_missingReports_skipsPerApplicationRecovery() throws Exception {
    boolean originalInnerSourceEnabled =
        SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.isEnabled();
    SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.setEnabled(true);
    try {
      // Two evaluated applications with matched components but no report stored on disk.
      newAppWithTwoComponentsNoReport();
      newAppWithTwoComponentsNoReport();

      ReportService spyReportService = spy(reportService);
      ApiSearchServiceV2 service = new ApiSearchServiceV2(baseUrl, applicationDAO, policyEvaluationDAO,
          applicationComponentDAO, policyViolationDAO, spyReportService, componentLoaderFactory);

      ApiSearchResultsDTOV2 result = service.searchComponent(BuildStageType.ID, null,
          ComponentIdentifier.createMavenCoordinates("", "", ""), null);

      // Results are still returned; only the dependency-data enrichment is skipped when no report is present.
      assertThat(result.results).hasSize(4);
      assertThat(result.results).allSatisfy(r -> {
        assertThat(r.dependencyData).isNotNull();
        assertThat(r.dependencyData.innerSourceData).isNull();
        assertThat(r.dependencyData.parentComponentPurls).isNull();
        assertThat(r.dependencyData.directDependency).isNull();
      });

      // The report is probed once per matched application via the no-recovery path...
      verify(spyReportService, times(2)).getReportIfPresent(any(Application.class), anyString());
      // ...and neither recovery overload (the ~4-query-per-app N+1) is ever taken.
      verify(spyReportService, never()).getReport(anyString(), anyString());
      verify(spyReportService, never()).getReport(any(Application.class), anyString());
    }
    finally {
      SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.setEnabled(originalInnerSourceEnabled);
    }
  }

  /**
   * CLM-41473: a mix of matched applications -- one whose report is present on disk and one whose report is absent --
   * must enrich the present application's results from its report while returning empty (but present) dependency data
   * for the absent one, all without taking the per-application recovery {@code getReport} path.
   */
  @Test
  public void testSearchComponent_mixedReportPresence_enrichesPresentSkipsAbsent() throws Exception {
    boolean originalInnerSourceEnabled =
        SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.isEnabled();
    SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.setEnabled(true);
    try {
      Application withReport = newAppWithTwoComponentsAndReport();
      Application withoutReport = newAppWithTwoComponentsNoReport();

      ReportService spyReportService = spy(reportService);
      ApiSearchServiceV2 service = new ApiSearchServiceV2(baseUrl, applicationDAO, policyEvaluationDAO,
          applicationComponentDAO, policyViolationDAO, spyReportService, componentLoaderFactory);

      ApiSearchResultsDTOV2 result = service.searchComponent(BuildStageType.ID, null,
          ComponentIdentifier.createMavenCoordinates("", "", ""), null);

      assertThat(result.results).hasSize(4);
      // The application whose report exists is enriched from it...
      assertThat(result.results)
          .filteredOn(r -> r.applicationId.equals(withReport.getPublicId()))
          .hasSize(2)
          .allSatisfy(r -> assertThat(r.dependencyData.parentComponentPurls).isNotEmpty());
      // ...while the application with no report yields present-but-empty dependency data (no recovery).
      assertThat(result.results)
          .filteredOn(r -> r.applicationId.equals(withoutReport.getPublicId()))
          .hasSize(2)
          .allSatisfy(r -> {
            assertThat(r.dependencyData).isNotNull();
            assertThat(r.dependencyData.parentComponentPurls).isNull();
          });

      // One no-recovery probe per matched application; the per-application recovery overloads are never used.
      verify(spyReportService, times(2)).getReportIfPresent(any(Application.class), anyString());
      verify(spyReportService, never()).getReport(anyString(), anyString());
      verify(spyReportService, never()).getReport(any(Application.class), anyString());
    }
    finally {
      SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.setEnabled(originalInnerSourceEnabled);
    }
  }

  /**
   * CLM-41473: when reports ARE present on disk, the dependency-data path must parse them via the DB-free
   * {@code getDependencyComponents} rather than {@code ComponentLoader.getAll}. {@code getAll} performs per-owner
   * license/label/vulnerability-group enrichment (several DAO queries) that is discarded here, and a new
   * {@code ComponentLoader} is built per matched application -- so using {@code getAll} re-ran that enrichment once
   * per result (a second N+1 on top of the report-load one). This verifies the heavy {@code getAll} overloads are
   * never invoked while the InnerSource dependency data is still resolved correctly.
   */
  @Test
  public void testSearchComponent_reportsPresent_usesDbFreeDependencyParseNotGetAll() throws Exception {
    boolean originalInnerSourceEnabled =
        SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.isEnabled();
    SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.setEnabled(true);
    try {
      newAppWithTwoComponentsAndReport();
      newAppWithTwoComponentsAndReport();

      // Spy every ComponentLoader the factory hands out so the parse method actually used can be verified.
      List<ComponentLoader> createdLoaders = new ArrayList<>();
      ComponentLoaderFactory spyFactory = spy(componentLoaderFactory);
      doAnswer(invocation -> {
        ComponentLoader loader = spy((ComponentLoader) invocation.callRealMethod());
        createdLoaders.add(loader);
        return loader;
      }).when(spyFactory).createComponentLoader(any());

      ApiSearchServiceV2 service = new ApiSearchServiceV2(baseUrl, applicationDAO, policyEvaluationDAO,
          applicationComponentDAO, policyViolationDAO, reportService, spyFactory);

      ApiSearchResultsDTOV2 result = service.searchComponent(BuildStageType.ID, null,
          ComponentIdentifier.createMavenCoordinates("", "", ""), null);

      // Dependency data is still resolved from each present report.
      assertThat(result.results).hasSize(4);
      assertThat(result.results).allSatisfy(r -> assertThat(r.dependencyData).isNotNull());
      assertThat(result.results).anySatisfy(r -> assertThat(r.dependencyData.parentComponentPurls).isNotEmpty());

      // One ComponentLoader per matched application, each used only via the DB-free getDependencyComponents path;
      // neither heavy getAll overload (which issues the per-owner enrichment queries) is ever invoked.
      assertThat(createdLoaders).hasSize(2);
      for (ComponentLoader loader : createdLoaders) {
        verify(loader, times(1)).getDependencyComponents(any(), any());
        verify(loader, never()).getAll(any(), any(), any(), any());
        verify(loader, never()).getAll(any(), anyBoolean(), any(), any(), any());
      }
    }
    finally {
      SystemConfigurationPropertyFeature.COMPONENT_SEARCH_API_WITH_INNERSOURCE.setEnabled(originalInnerSourceEnabled);
    }
  }

  /**
   * Guards the CLM-40023 memory fix: a hash search must filter the components in SQL (loading only the matching rows)
   * via {@code getMapByOwnerIdsAndStageTypeIdsAndHashes} rather than materialising every inspected application's
   * components through the full {@code getByOwnerIdsAndStageTypeId} scan. An upper-case hash is used to confirm
   * matching stays case-insensitive after moving the filter into SQL.
   */
  @Test
  public void testSearchComponent_byHash_filtersComponentsInSqlAndIsCaseInsensitive() throws Exception {
    // Both applications contain a component with hash "2b8e230d2ab644e4ecaa".
    newAppWithTwoComponentsAndReport();
    newAppWithTwoComponentsAndReport();

    OwnerComponentDAO spyOwnerComponentDAO = spy(applicationComponentDAO);
    PolicyEvaluationDAO spyPolicyEvaluationDAO = spy(policyEvaluationDAO);
    PolicyViolationDAO spyPolicyViolationDAO = spy(policyViolationDAO);

    ApiSearchServiceV2 service = new ApiSearchServiceV2(baseUrl, applicationDAO, spyPolicyEvaluationDAO,
        spyOwnerComponentDAO, spyPolicyViolationDAO, reportService, componentLoaderFactory);

    ApiSearchResultsDTOV2 result = service.searchComponent(BuildStageType.ID, "2B8E230D2AB644E4ECAA", null, null);

    assertThat(result.results).hasSize(2);
    assertThat(result.results).allSatisfy(r -> assertThat(r.hash).isEqualTo("2b8e230d2ab644e4ecaa"));

    // Hash searches filter in SQL: only the matching rows are loaded, so the full component scan must not be used.
    verify(spyOwnerComponentDAO, times(1))
        .getMapByOwnerIdsAndStageTypeIdsAndHashes(anySet(), anySet(), anySet());
    verify(spyOwnerComponentDAO, never()).getByOwnerIdsAndStageTypeId(anySet(), anyString());

    // The evaluation and violation queries stay batched (no per-application variants).
    verify(spyPolicyEvaluationDAO, times(1)).getLastPrimaryByOwnerIdsAndStageId(anySet(), eq(BuildStageType.ID));
    verify(spyPolicyEvaluationDAO, never()).getLastPrimaryByOwnerIdAndStageId(anyString(), anyString());
    verify(spyPolicyViolationDAO, times(1))
        .getActiveByOwnerIdsAndStageIdAndHashes(anySet(), eq(BuildStageType.ID), anySet());
  }

  private Application newAppWithTwoComponentsAndReport() throws URISyntaxException, IOException {
    Application application = newAppWithTwoComponentsNoReport();
    createReportFile(application.getId(), "scan-id", zipReportDir("/ApiSearchServiceV2Test/report-1", tempDir),
        insightWork);
    return application;
  }

  private Application newAppWithTwoComponentsNoReport() {
    Application application = tempEntity.newApplication(ROOT_ORGANIZATION_ID);
    tempEntity.newApplicationComponent(application.getId(), BuildStageType.ID, "2b8e230d2ab644e4ecaa",
        ComponentIdentifier.createMavenCoordinates("xmlpull", "xmlpull", "1.1.3.1"));
    tempEntity.newApplicationComponent(application.getId(), BuildStageType.ID, "e3fd8ced1f52c7574af9",
        ComponentIdentifier.createMavenCoordinates("org.apache.httpcomponents", "httpcore", "4.4.6"));
    tempEntity.newPolicyEvaluation(application.getId(), BuildStageType.ID, "scan-id");
    return application;
  }
}
