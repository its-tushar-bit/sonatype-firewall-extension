/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.AdvancedSearchPage;
import com.sonatype.clm.testing.playwright.pages.AdvancedSearchPageAssertions;
import com.sonatype.clm.testing.playwright.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.playwright.pages.ComponentDetailsPageAssertions;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.dependency.ComponentDependenciesDTO;

import com.microsoft.playwright.Download;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

public class AdvancedSearchLucenePlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME = "Playwright Lucene Org";

  private static final String APP_NAME = "Playwright Lucene App";

  private static final String APP_PUBLIC_ID = "pw-lucene-app";

  private static final String FIELD_APPLICATION_NAME = "Application Name";

  private static final String FIELD_ORGANIZATION_NAME = "Organization Name";

  private static final String FIELD_COMPONENT_NAME = "Component Name";

  private static final String MATCH_EXACT = "Exact Match";

  private static final String MATCH_PARTIAL = "Partial Match";

  private static final String COMPONENT_VALUE = "commons-fileupload";

  private static final String REPORT_STAGE = "build";

  /** Scan ID embedded in the large-report canned fixture — fixed by the canned report binary. */
  private static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private static final String CANNED_REPORT_CLASSPATH_DIR = "/canned-reports/large-report";

  // hash of commons-fileupload:1.2.2 in the large-report canned fixture
  private static final String SECURITY_COMPONENT_HASH = "1e48256a2341047e7d72";

  private static final String OPERATOR_AND = "AND";

  private static final String QUERY_FIELD_APPLICATION_NAME = "applicationName";

  private static final String QUERY_FIELD_ORGANIZATION_NAME = "organizationName";

  private static final int FIRST_ROW = 0;

  private static final int SECOND_ROW = 1;

  private static final int ONE_ROW = 1;

  private static final int TWO_ROWS = 2;

  private static final String HDS_COMPONENT_DETAILS = "/componentDetails/javancssComponentDetails-29.50.json";

  private static final String HDS_COMPONENT_DETAILS_LIST = "/componentDetails/javancssComponentDetailsList.json";

  private static final String URI_COMPONENT_DETAILS = "rest/ci/componentDetails";

  private static final String URI_COMPONENT_DETAILS_LIST = "rest/ci/componentDetails/list";

  private static final String URI_COMPONENT_DEPENDENCIES = "rest/component/dependencies";

  private static final String URI_VULNERABILITY_DETAILS = "rest/vulnerability/details/json";

  private Application app;

  private AdvancedSearchPage advancedSearch;

  private AdvancedSearchPageAssertions assertions;

  @BeforeEach
  public void setup() throws IOException {
    app = tempEntity.newApplicationWithParent(APP_PUBLIC_ID, APP_NAME, ORG_NAME);
    seedCannedReport(); // must precede createSearchIndex() so the PolicyEvaluation record exists for component docs
    awaitNoExecutingSchedulerJobs(); // guard against in-flight SearchIndexUpdate still holding the Lucene write lock
    lookup(IndexService.class).createSearchIndex();
    enableAdvancedSearch(); // set after DB seeding so @After disableAdvancedSearch() runs only when the flag was
                            // actually enabled
    playwrightRefreshOrOpen(AdvancedSearchPage.url());
    playwrightLogin();
    advancedSearch = new AdvancedSearchPage();
    assertions = new AdvancedSearchPageAssertions(advancedSearch);
    assertions.shouldBeLoaded();
  }

  @Test
  @Tag("regression")
  public void testLuceneSearch_ApplicationNameQuery_ReturnsRealResultCard() {
    searchByExactAppName();
    assertions.shouldHaveResultGroup(APP_NAME);
    assertions.shouldHaveApplicationLinkVisible(APP_NAME);
  }

  @Test
  @Tag("regression")
  public void testLuceneSearch_MultipleTermsWithAndOperator_ReturnsResults() {
    advancedSearch.openQueryBuilder();
    assertions.shouldHaveQueryRowCount(ONE_ROW);

    advancedSearch.selectFieldForRow(FIRST_ROW, FIELD_APPLICATION_NAME);
    advancedSearch.setMatchTypeForRow(FIRST_ROW, MATCH_EXACT);
    advancedSearch.setValueForRow(FIRST_ROW, APP_NAME);

    advancedSearch.clickAddSearchItem();
    assertions.shouldHaveQueryRowCount(TWO_ROWS);

    advancedSearch.setOperatorForRow(SECOND_ROW, OPERATOR_AND);
    assertions.shouldHaveOperatorForRow(SECOND_ROW, OPERATOR_AND);
    advancedSearch.selectFieldForRow(SECOND_ROW, FIELD_ORGANIZATION_NAME);
    advancedSearch.setMatchTypeForRow(SECOND_ROW, MATCH_EXACT);
    advancedSearch.setValueForRow(SECOND_ROW, ORG_NAME);

    assertions.shouldHaveQueryContaining(QUERY_FIELD_APPLICATION_NAME);
    assertions.shouldHaveQueryContaining(APP_NAME);
    assertions.shouldHaveQueryContaining(OPERATOR_AND);
    assertions.shouldHaveQueryContaining(QUERY_FIELD_ORGANIZATION_NAME);
    assertions.shouldHaveQueryContaining(ORG_NAME);

    advancedSearch.clickSearchButton();
    assertions.shouldShowSearchResultCount();
    assertions.shouldHaveResultGroup(APP_NAME);
  }

  @Test
  @Tag("regression")
  public void testLuceneSearch_ExportResults_TriggersCsvDownload() {
    searchByExactAppName();
    assertions.shouldHaveExportButtonEnabled();

    Download download = advancedSearch.clickExportResultsAndWaitForDownload();
    assertions.shouldHaveCsvDownload(download);
  }

  @Test
  @Tag("regression")
  public void testLuceneSearch_ComponentNameSearch_NavigatesToComponentDetailsPage() {
    // MATCH_PARTIAL required: componentName is indexed as a single token by LowerCaseKeywordAnalyzer
    advancedSearch.buildSingleTermQuery(FIELD_COMPONENT_NAME, MATCH_PARTIAL, COMPONENT_VALUE);
    advancedSearch.clickSearchButton();

    assertions.shouldShowSearchResultCount();
    assertions.shouldHaveExactlyOneResultGroup();
    assertions.shouldHaveResultGroup(COMPONENT_VALUE);

    advancedSearch.clickFirstResultCardReportLinkAndWaitForNavigation(REPORT_STAGE);

    // navigate directly — no reference policies imported, so violations table is empty
    playwrightRefreshOrOpen(ComponentDetailsPage.urlToOverview(app, SCAN_ID, SECURITY_COMPONENT_HASH));
    new ComponentDetailsPageAssertions(new ComponentDetailsPage()).shouldShowHeaderTitle();
  }

  @Test
  @Tag("regression")
  public void testLuceneSearch_RemoveSearchTermRow_ClearsTermFromQuery() {
    advancedSearch.buildSingleTermQuery(FIELD_APPLICATION_NAME, MATCH_EXACT, APP_NAME);

    advancedSearch.clickAddSearchItem();
    assertions.shouldHaveQueryRowCount(TWO_ROWS);
    advancedSearch.selectFieldForRow(SECOND_ROW, FIELD_ORGANIZATION_NAME);
    advancedSearch.setValueForRow(SECOND_ROW, ORG_NAME);
    assertions.shouldHaveQueryContaining(QUERY_FIELD_ORGANIZATION_NAME);

    advancedSearch.removeRow(SECOND_ROW);
    assertions.shouldHaveQueryRowCount(ONE_ROW);

    assertions.shouldHaveQueryContaining(QUERY_FIELD_APPLICATION_NAME);
    assertions.shouldNotHaveQueryContaining(QUERY_FIELD_ORGANIZATION_NAME);

    advancedSearch.clickSearchButton();
    assertions.shouldShowSearchResultCount();
  }

  @Test
  @Tag("regression")
  public void testLuceneSearch_ExactMatchGeneratesQuotedQuery() {
    advancedSearch.buildSingleTermQuery(FIELD_APPLICATION_NAME, MATCH_EXACT, APP_NAME);

    assertions.shouldHaveQueryContaining("\"" + APP_NAME + "\"");
    assertions.shouldNotHaveWildcardWrapForValue(APP_NAME);

    advancedSearch.clickSearchButton();
    assertions.shouldShowSearchResultCount();
  }

  @Test
  @Tag("regression")
  public void testLuceneSearch_ClearAllTermsResetsQueryAndDisablesSearch() {
    advancedSearch.buildSingleTermQuery(FIELD_APPLICATION_NAME, MATCH_PARTIAL, APP_NAME);

    advancedSearch.clickAddSearchItem();
    assertions.shouldHaveQueryRowCount(TWO_ROWS);
    advancedSearch.selectFieldForRow(SECOND_ROW, FIELD_ORGANIZATION_NAME);
    advancedSearch.setValueForRow(SECOND_ROW, ORG_NAME);
    assertions.shouldHaveQueryContaining(QUERY_FIELD_APPLICATION_NAME);
    assertions.shouldHaveQueryContaining(QUERY_FIELD_ORGANIZATION_NAME);

    advancedSearch.removeRow(SECOND_ROW);
    advancedSearch.removeRow(FIRST_ROW);

    assertions.shouldHaveQueryRowCount(0);
    assertions.shouldHaveQueryBuilderEmptyState();
    assertions.shouldHaveEmptyQuery();
    assertions.shouldHaveSearchButtonDisabled();
  }

  /** Field-validity check; seeded fixture has no violations so count is legitimately zero. */
  @Test
  @Tag("regression")
  public void testLuceneSearch_PolicyNameQuery_FieldIsSearchable() {
    advancedSearch.runKeywordSearch("policyName:*");

    assertions.shouldShowSearchResultCount();
  }

  @Test
  @Tag("regression")
  public void testLuceneSearch_VulnerabilitySeverityRange_ReturnsResults() {
    // Expects the canned report to contain at least one component whose vulnerabilitySeverity
    // is in [7, 10] — verified against fixtures in src/test/resources/reportWithComponentRef
    // on 2026-06-30. If this assertion starts failing after a fixture change, regenerate the
    // expected component list rather than weakening the assertion.
    advancedSearch.runKeywordSearch("vulnerabilitySeverity:[7 TO 10]");

    assertions.shouldShowSearchResultCount();
    assertions.shouldHaveAtLeastOneResult();
  }

  @Test
  @Tag("regression")
  public void testLuceneSearch_NonsenseQuery_ShowsNoResults() {
    advancedSearch.runKeywordSearch(
        "applicationName:nonexistent-app-name-" + TemporaryEntity.uuid());

    assertions.shouldShowSearchResultCount();
    assertions.shouldHaveNoResults();
  }

  // afterTest() calls standby() which pauses new firings but does not interrupt in-flight jobs;
  // wait here so the job releases the IndexWriter write lock before createSearchIndex() acquires it.
  private void awaitNoExecutingSchedulerJobs() {
    Scheduler quartz = lookup(TaskScheduler.class).getScheduler();
    if (quartz == null) {
      return;
    }
    Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> {
      try {
        return quartz.getCurrentlyExecutingJobs().isEmpty();
      }
      catch (SchedulerException e) {
        return true;
      }
    });
  }

  private void seedCannedReport() throws IOException {
    stubComponentDetailsEndpoints();
    URL zippedReport = ReportHelper.zipReport(CANNED_REPORT_CLASSPATH_DIR, tempDir);
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work).evaluatePolicy();
  }

  private void stubComponentDetailsEndpoints() {
    var hds = testCLMServer.getHdsServer();
    hds.respondWith(getClass().getResource(HDS_COMPONENT_DETAILS)).atUri(URI_COMPONENT_DETAILS);
    hds.respondWith(getClass().getResource(HDS_COMPONENT_DETAILS_LIST)).atUri(URI_COMPONENT_DETAILS_LIST);
    hds.respondWith(new ComponentDependenciesDTO(Collections.emptyMap(), Collections.emptyMap()))
        .atUri(URI_COMPONENT_DEPENDENCIES);
    hds.respondWith(Collections.emptyMap())
        .atUri(URI_VULNERABILITY_DETAILS);
  }

  private void searchByExactAppName() {
    advancedSearch.buildSingleTermQuery(FIELD_APPLICATION_NAME, MATCH_EXACT, APP_NAME);
    advancedSearch.clickSearchButton();
    assertions.shouldShowSearchResultCount();
  }

  @AfterEach
  public void disableAdvancedSearch() {
    // Always runs — safe to call even if enableAdvancedSearch() was never reached in @Before.
    lookup(SystemConfigurationPropertyDAO.class)
        .set(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "false");
  }

  private void enableAdvancedSearch() {
    lookup(SystemConfigurationPropertyDAO.class)
        .set(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED, "true");
  }
}
