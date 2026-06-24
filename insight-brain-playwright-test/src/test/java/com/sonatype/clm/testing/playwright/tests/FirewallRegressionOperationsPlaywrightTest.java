/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.EnterpriseReportingDashboardPage;
import com.sonatype.clm.testing.playwright.pages.EnterpriseReportingPage;
import com.sonatype.clm.testing.playwright.pages.FirewallAutoUnquarantinePage;
import com.sonatype.clm.testing.playwright.pages.FirewallComponentDetailsRegressionPage;
import com.sonatype.clm.testing.playwright.pages.FirewallPage;
import com.sonatype.clm.testing.playwright.pages.FirewallRegressionPage;
import com.sonatype.clm.testing.playwright.pages.FirewallRepositoryResultsRegressionPage;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.model.repository.QuarantinedComponentAccess;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryComponent;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.license.model.LicensedFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.assertions.LocatorAssertions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Regression tests covering Firewall operations across ER dashboard, component-details tabs, bulk waive, container
 * waiver, auto-unquarantine, onboarding guard, repo results, and quarantined component report.
 */
public class FirewallRegressionOperationsPlaywrightTest
    extends AbstractIqUiTest
{
  private record HdsStubs(JsonNode currentVersion, JsonNode dashboards)
  {
  }

  private record AutoUnquarantineHistoryStubs(
      JsonNode defaultBody,
      JsonNode quarAscBody,
      JsonNode quarDescBody,
      JsonNode clearAscBody,
      JsonNode clearDescBody)
  {
  }

  private record ContainerWaiverStubs(JsonNode policyThreats, JsonNode dataGlobals, JsonNode metadata)
  {
  }

  private static final Data DATA = TestDataManager.load("firewall-regression-operations", Data.class);

  private static final HdsStubs ENTERPRISE_REPORTING_HDS =
      TestDataManager.load("enterprise-reporting-hds-stubs", HdsStubs.class);

  private static final AutoUnquarantineHistoryStubs AUTO_UNQUARANTINE_HISTORY_STUBS =
      TestDataManager.load("firewall-auto-unquarantine-history-stubs", AutoUnquarantineHistoryStubs.class);

  private static final ContainerWaiverStubs CONTAINER_WAIVER_STUBS =
      TestDataManager.load("firewall-container-waiver-stubs", ContainerWaiverStubs.class);

  private static final String FIREWALL_DASHBOARD_HASH = "/firewall/dashboard";

  private static final int POLICY_THREAT_LEVEL = 5;

  private static final int FIRST_CONDITION_TOGGLE = 1;

  private static final int SECOND_CONDITION_TOGGLE = 2;

  private Set<LicensedFeature> savedLicenseFeatures;

  @Before
  public void setUp() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
    savedLicenseFeatures = productLicenseManager.getFeatures();
    enableIntegratedEnterpriseReportingOnLicense();
    stubEnterpriseReportingHds();
    playwrightHardreset();
    playwrightRefreshOrOpen("/");
    playwrightLogin();
  }

  @After
  public void tearDown() {
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(false);
    if (savedLicenseFeatures != null) {
      setFeatures(savedLicenseFeatures.toArray(new LicensedFeature[0]));
      savedLicenseFeatures = null;
    }
  }

  /**
   * Clicking "Open Dashboard" navigates to the embed route; the Looker iframe is not asserted (requires external
   * network unavailable in test).
   */
  @Test
  @Category(RegressionTest.class)
  public void testFirewallEnterpriseReporting_clickDashboardCard_embedPageRenders() {
    playwrightRefreshOrOpen(EnterpriseReportingPage.url());

    FirewallRegressionPage landingPage = new FirewallRegressionPage();
    EnterpriseReportingDashboardPage dashboardPage = new EnterpriseReportingDashboardPage();

    assertThat(landingPage.enterpriseReportingContainer()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(landingPage.enterpriseDashboardCard(DATA.dashboardId())).isVisible();

    landingPage.openDashboardButton(DATA.dashboardId()).click();

    assertThat(page).hasURL(Pattern.compile(".*" + DATA.expectedDashboardUrlFragment() + ".*"));
    assertThat(dashboardPage.container()).isVisible();
    assertThat(dashboardPage.pageHeading()).containsText(DATA.dashboardTitle());
  }

  /**
   * Bulk Waive three-step flow: selects two of three seeded components individually, configures
   * expiry, submits on the Confirmation page, and asserts the wizard completes back to the repo
   * results page.
   */
  @Test
  @Category(RegressionTest.class)
  public void testFirewallBulkWaive_threeStepFlow() {
    String suffix = "-" + TemporaryEntity.uuid();
    Repository repo = seedDbForBulkWaiveTest(suffix);

    FirewallRepositoryResultsRegressionPage repoPage = new FirewallRepositoryResultsRegressionPage();
    FirewallRegressionPage bulkWaivePage = new FirewallRegressionPage();

    playwrightRefreshOrOpen(FirewallRepositoryResultsRegressionPage.url(repo.getId()));

    assertThat(repoPage.container()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(repoPage.bulkWaiveButton()).isEnabled();
    repoPage.bulkWaiveButton().click();

    // Step 1: BulkWaivePage — three components available; select two individually
    assertThat(page).hasURL(Pattern.compile(DATA.bulkWaiveUrlFragment()));
    assertThat(bulkWaivePage.bulkWaivePageContainer()).isVisible();
    assertThat(bulkWaivePage.bulkWaivePageNextButton()).isDisabled();

    assertThat(bulkWaivePage.bulkWaiveComponentRows()).hasCount(3,
        new LocatorAssertions.HasCountOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    bulkWaivePage.bulkWaiveComponentCheckbox(0).click();
    bulkWaivePage.bulkWaiveComponentCheckbox(1).click();
    assertThat(bulkWaivePage.bulkWaivePageNextButton()).isEnabled();
    bulkWaivePage.bulkWaivePageNextButton().click();

    // Step 2: ConfigurationPage — Next disabled until expiry is configured
    assertThat(page).hasURL(Pattern.compile(DATA.bulkWaiveConfigUrlFragment()));
    assertThat(bulkWaivePage.bulkWaiveConfigPageContainer()).isVisible();
    assertThat(bulkWaivePage.bulkWaiveConfigPageNextButton()).isDisabled();

    bulkWaivePage.bulkWaiveExpirySelect().selectOption(DATA.bulkWaiveExpiryOption());
    assertThat(bulkWaivePage.bulkWaiveConfigPageNextButton()).isEnabled();
    bulkWaivePage.bulkWaiveConfigPageNextButton().click();

    // Step 3: ConfirmationPage — verify heading and submit
    assertThat(page).hasURL(Pattern.compile(DATA.bulkWaiveConfirmationUrlFragment()));
    assertThat(bulkWaivePage.bulkWaiveConfirmationPageContainer()).isVisible();
    assertThat(bulkWaivePage.bulkWaiveConfirmationHeading()).containsText(DATA.confirmationHeadingText());
    assertThat(bulkWaivePage.bulkWaiveConfirmButton()).isVisible();
    bulkWaivePage.bulkWaiveConfirmButton().click();

    // Post-submit: wizard completes and returns to the repository results page
    assertThat(repoPage.container()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  private Repository seedDbForBulkWaiveTest(String suffix) {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repoManager, "bw-repo" + suffix, true, false);
    Date now = Date.from(Instant.now());
    Policy policy = tempEntity.newPolicy();
    for (int i = 1; i <= 3; i++) {
      RepositoryComponent component = tempEntity.newRepositoryComponent(
          repo.getId(), MatchState.EXACT, "bw/path-" + i + suffix, "bw-hash-00" + i,
          ComponentIdentifier.createMavenCoordinates(
              "bw-grp-" + i + suffix, "bw-art-" + i + suffix, DATA.componentVersion()),
          now, now);
      tempEntity.newRepositoryPolicyViolation(
          component.getRepositoryId(), POLICY_THREAT_LEVEL, component.getPathname(), false,
          FailActionType.ID, policy.getId(), "bw-policy-" + i + suffix, component.getComponentIdentifier());
    }
    return repo;
  }

  /**
   * Covers the 4-step navigation path to {@code AddContainerImageWaiverPage}:
   * (1) Solution Switcher → "Repository Firewall", (2) Containers &gt; Quarantine tab,
   * (3) click a container row to open the container report,
   * (4) click "Waive All Fail Policy Violations" to reach the waiver page.
   * Asserts waiver configuration fields render.
   */
  @Test
  @Category(RegressionTest.class)
  public void testFirewallAddContainerImageWaiver_pageRenders() {
    String suffix = "-" + TemporaryEntity.uuid();
    seedDbForContainerNavigationTest(suffix);
    enableContainerImagesEvalOnLicense();
    SystemConfigurationPropertyFeature.CONTAINER_IMAGES_EVAL_ENABLED.setEnabled(true);
    // Hard-reset forces browser to re-fetch product features; cookies are cleared so re-login below.
    playwrightHardreset();

    String firewallDashboardUrl = baseUrlFromTest + FirewallPage.url();
    page.route(Pattern.compile(".*/api/v2/solutions/licensed.*"), route -> route.fulfill(new Route.FulfillOptions()
        .setContentType("application/json")
        .setBody("[{\"id\":\"firewall\",\"url\":\"" + firewallDashboardUrl + "\"}]")));

    // Mock report endpoints: the container report page loads JSON files that only exist after a real
    // scan; since test data is seeded via DB, we provide minimal valid responses so the page can
    // compute activeProxyFailedViolationCount > 0 and enable the waiver button.
    page.route(Pattern.compile(".*\\/rest\\/report\\/.*"), route -> {
      String url = route.request().url();
      if (url.contains("/browseReport/policythreats.json")) {
        route.fulfill(new Route.FulfillOptions().setStatus(200)
            .setContentType("application/json")
            .setBody(CONTAINER_WAIVER_STUBS.policyThreats().toString()));
      }
      else if (url.contains("/browseReport/bom.json") || url.contains("/browseReport/partialmatched.json")) {
        route.fulfill(new Route.FulfillOptions().setStatus(200)
            .setContentType("application/json")
            .setBody("{\"aaData\":[]}"));
      }
      else if (url.contains("/browseReport/data.json")) {
        route.fulfill(new Route.FulfillOptions().setStatus(200)
            .setContentType("application/json")
            .setBody(CONTAINER_WAIVER_STUBS.dataGlobals().toString()));
      }
      else if (url.contains("/browseReport/dependencies.json")) {
        route.fulfill(new Route.FulfillOptions().setStatus(200)
            .setContentType("application/json")
            .setBody("{\"dependencyTree\":null}"));
      }
      else if (url.contains("/metadata")) {
        route.fulfill(new Route.FulfillOptions().setStatus(200)
            .setContentType("application/json")
            .setBody(CONTAINER_WAIVER_STUBS.metadata().toString()));
      }
      else {
        route.resume();
      }
    });
    playwrightLoginAdminAt("/");

    FirewallRegressionPage regressionPage = new FirewallRegressionPage();
    FirewallPage firewallPage = new FirewallPage();

    // Step 1: click Solution Switcher → select Repository Firewall
    clickFirewallSolutionSwitcherLink(regressionPage);

    // Step 2: click the Containers top-level tab, then the Quarantine sub-tab
    assertThat(firewallPage.container()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    firewallPage.tab("containers").click();
    regressionPage.containerQuarantineTab().click();

    // Step 3: wait for a container quarantine row and click its container-report link
    assertThat(regressionPage.containerQuarantineTableRows()).hasCount(1,
        new LocatorAssertions.HasCountOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    Locator firstRow = regressionPage.containerQuarantineTableRows().first();
    regressionPage.containerReportLinkInRow(firstRow).click();

    // Step 4: wait for the container report to load and click "Waive All Fail Policy Violations"
    assertThat(regressionPage.addContainerImageWaiverButton()).isEnabled(
        new LocatorAssertions.IsEnabledOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    regressionPage.addContainerImageWaiverButton().click();

    assertThat(regressionPage.addContainerImageWaiverPageContainer()).isVisible();
    assertThat(regressionPage.containerWaiverConfigHeading()).containsText(DATA.waiverConfigHeadingText());
    assertThat(regressionPage.addContainerImageWaiverExpirationSelect()).isVisible();
    assertThat(regressionPage.addContainerImageWaiverReasonSelect()).isVisible();
  }

  private void seedDbForContainerNavigationTest(String suffix) {
    Organization org = tempEntity.newOrgWithRepoManagerAndProxyRepo(
        "cn-org" + suffix, "cn-repo" + suffix, "docker", true, true);
    Application app = tempEntity.newApplication("cn-app" + suffix, "cn-pub" + suffix, org.getId());
    Policy policy = tempEntity.newPolicy();
    PolicyEvaluation policyEvaluation =
        tempEntity.newPolicyEvaluation(app.getId(), ProxyStageType.ID, "cn-scan" + suffix);
    tempEntity.newPolicyViolation(policyEvaluation, policy, 9, PolicyThreatCategory.OTHER,
        "cn-grp" + suffix, "cn-art" + suffix, "v1", "cn-hash", FailActionType.ID);
  }

  private void enableContainerImagesEvalOnLicense() {
    Set<LicensedFeature> baseline = productLicenseManager.getFeatures();
    EnumSet<LicensedFeature> merged =
        baseline != null && !baseline.isEmpty()
            ? EnumSet.copyOf(baseline)
            : EnumSet.allOf(LicensedFeature.class);
    merged.add(LicensedFeature.CONTAINER_IMAGES_EVALUATION);
    setFeatures(merged.toArray(new LicensedFeature[0]));
  }

  /**
   * Auto-Unquarantine config modal renders toggles, info alert, and Save button; toggles are interactive; Cancel closes
   * the modal.
   */
  @Test
  @Category(RegressionTest.class)
  public void testFirewallAutoUnquarantineConfigModal_contentTogglesAndSave() {
    navigateAndWaitForUrl(FirewallAutoUnquarantinePage.url(), "autoReleaseQuarantine");

    FirewallAutoUnquarantinePage autoPage = new FirewallAutoUnquarantinePage();

    assertThat(autoPage.container()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    autoPage.openConfigurationModal();

    assertThat(autoPage.modalInfoAlert()).isVisible();
    assertThat(autoPage.modalReadMoreLink()).isVisible();

    assertThat(autoPage.modalIntegrityRatingToggle()).isVisible();
    assertThat(autoPage.modalConditionToggle(FIRST_CONDITION_TOGGLE)).isVisible();
    assertThat(autoPage.modalConditionToggle(SECOND_CONDITION_TOGGLE)).isVisible();
    assertThat(autoPage.modalSaveButton()).containsText(DATA.modalSaveButtonText());
    // NxStatefulForm never adds the HTML disabled attribute — it shows a form-level validation
    // error alert instead. Check that alert is visible before any changes are made.
    assertThat(autoPage.modalValidationErrors()).containsText(DATA.modalNoChangesText());

    autoPage.modalConditionToggle(FIRST_CONDITION_TOGGLE).click();
    assertThat(autoPage.configurationModal()).isVisible();
    assertThat(autoPage.modalSaveButton()).isEnabled();

    autoPage.cancelConfigurationModal();
  }

  /** Auto-Unquarantine page renders back navigation, month-to-date/year-to-date metric cards, and status card. */
  @Test
  @Category(RegressionTest.class)
  public void testFirewallAutoUnquarantine_layoutCardsAndBackNavigation() {
    navigateAndWaitForUrl(FirewallAutoUnquarantinePage.url(), "autoReleaseQuarantine");

    FirewallAutoUnquarantinePage autoPage = new FirewallAutoUnquarantinePage();
    FirewallRegressionPage regressionPage = new FirewallRegressionPage();
    FirewallPage firewallPage = new FirewallPage();

    assertThat(autoPage.container()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(regressionPage.autoUnquarantineBackButton()).isVisible();

    // MTD and YTD cards are visible; assert visibility only — a freshly-seeded org may render
    // the initial "-" placeholder instead of a numeric count, which would fail a \d+ regex.
    assertThat(regressionPage.autoUnquarantineMtdCard()).isVisible();
    assertThat(autoPage.mtdCallOut()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(regressionPage.autoUnquarantineYtdCard()).isVisible();
    assertThat(autoPage.ytdCallOut()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    // Status card: Active/Inactive indicator, status body text, Configure link.
    // statusReleasingText() text depends on backend config state — asserting visibility only
    // to avoid fragility when no auto-unquarantine config has been seeded.
    assertThat(autoPage.statusCard()).isVisible();
    assertThat(autoPage.statusText()).isVisible();
    assertThat(autoPage.statusReleasingText()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(autoPage.configureLink()).isVisible();

    // Back button returns to the Firewall dashboard
    regressionPage.autoUnquarantineBackButton().click();
    assertThat(firewallPage.container()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
  }

  /**
   * History table shows all columns and sortable headers; sorts by quarantine date and date cleared
   * with first-row content verification; clicking a row navigates to the component details page;
   * empty state shown when no data.
   *
   * <p>
   * Auto-release history records cannot be seeded via {@code TemporaryEntity} — the list API
   * is mocked before navigation. Three components with distinct quarantine/release dates are used
   * so that each sort direction produces a different first row, making the sort assertions
   * meaningful:
   * <ul>
   * <li>A ({@code aru-art-1:1.0}) — oldest quarantine date, oldest date cleared</li>
   * <li>B ({@code aru-art-2:1.0}) — middle</li>
   * <li>C ({@code aru-art-3:1.0}) — newest quarantine date, newest date cleared</li>
   * </ul>
   * The mock handler inspects the request URL to return the appropriate ordering per sort params.
   * Five additional mocks cover all APIs loaded by {@code FirewallComponentDetailsPage} on mount
   * ({@code loadComponentDetails}, {@code loadComponentPolicyViolations},
   * {@code loadExistingWaiversData}, applicable labels, component labels) — without them the
   * fake {@code aru-repo-id} returns 404s, and the {@code loadComponentDetails} failure sets
   * {@code componentDetailsError}, causing {@code NxLoadWrapper} to show an error overlay.
   *
   * <p>
   * The mocks are registered first, then we navigate directly to the auto-unquarantine page
   * (full-page load). Navigating via the Firewall dashboard first must be avoided because
   * {@code loadFirewallData()} dispatches in-flight API calls (e.g. {@code loadQuarantineSummary},
   * {@code loadReleaseQuarantineSummary}) that may still be in-flight when we navigate away. When
   * those late-arriving responses fail, their failure handlers set
   * {@code firewall.viewState.loadError}, which causes {@code LoadWrapper} to show an error overlay
   * even after the auto-unquarantine component has reset Redux state via
   * {@code FIREWALL_AUTO_UNQUARANTINE_DATA_REQUESTED}. Navigating directly avoids all of this.
   * The empty-state check is done after unrouting the mock and navigating back from the component
   * details page (a genuine UI Router transition that remounts the component and calls the real,
   * empty API).
   */
  @Test
  @Category(RegressionTest.class)
  public void testFirewallAutoUnquarantine_historyTableColumnsAndSorting() {
    FirewallAutoUnquarantinePage autoPage = new FirewallAutoUnquarantinePage();
    FirewallRegressionPage regressionPage = new FirewallRegressionPage();

    // Three components with distinct quarantine/release dates (A=oldest, B=middle, C=newest).
    // Each sort direction produces a different first row, making the sort assertions meaningful.
    // Stub data is in firewall-auto-unquarantine-history-stubs.json.
    page.route(Pattern.compile(".*/api/v2/firewall/components/autoReleasedFromQuarantine.*"), route -> {
      String url = route.request().url();
      String body;
      if (url.contains("sortBy=quarantineTime") && url.contains("asc=true")) {
        body = AUTO_UNQUARANTINE_HISTORY_STUBS.quarAscBody().toString();
      }
      else if (url.contains("sortBy=quarantineTime") && url.contains("asc=false")) {
        body = AUTO_UNQUARANTINE_HISTORY_STUBS.quarDescBody().toString();
      }
      else if (url.contains("sortBy=releaseQuarantineTime") && url.contains("asc=true")) {
        body = AUTO_UNQUARANTINE_HISTORY_STUBS.clearAscBody().toString();
      }
      else if (url.contains("sortBy=releaseQuarantineTime") && url.contains("asc=false")) {
        body = AUTO_UNQUARANTINE_HISTORY_STUBS.clearDescBody().toString();
      }
      else {
        body = AUTO_UNQUARANTINE_HISTORY_STUBS.defaultBody().toString();
      }
      route.fulfill(new Route.FulfillOptions().setStatus(200)
          .setContentType("application/json")
          .setBody(body));
    });

    // Component-details page mocks (step 4): all five APIs called by FirewallComponentDetailsPage
    // on mount use the fake aru-repo-id. Without these mocks the server returns 404s; the
    // loadComponentDetails failure sets componentDetailsError, causing NxLoadWrapper to show
    // "error occurred while loading the data" instead of rendering the page.
    page.route(Pattern.compile(".*/rest/ci/componentDetails/repository/aru-repo-id.*"),
        route -> route.fulfill(new Route.FulfillOptions().setStatus(200)
            .setContentType("application/json")
            .setBody("{\"matchState\":\"exact\",\"displayName\":{\"parts\":[]},"
                + "\"componentIdentifier\":{\"format\":\"maven\",\"coordinates\":{}},"
                + "\"metadata\":{}}")));
    page.route(Pattern.compile(".*/rest/repositories/aru-repo-id/policyViolations/.*"),
        route -> route.fulfill(new Route.FulfillOptions().setStatus(200)
            .setContentType("application/json")
            .setBody("[]")));
    page.route(Pattern.compile(".*/rest/policyWaiver/repository/aru-repo-id/component/.*"),
        route -> route.fulfill(new Route.FulfillOptions().setStatus(200)
            .setContentType("application/json")
            .setBody("[]")));
    page.route(Pattern.compile(".*/api/v2/labels/repository/aru-repo-id/applicable.*"),
        route -> route.fulfill(new Route.FulfillOptions().setStatus(200)
            .setContentType("application/json")
            .setBody("{\"labelsByOwner\":[]}")));
    page.route(Pattern.compile(".*/rest/label/component/repository/aru-repo-id/.*"),
        route -> route.fulfill(new Route.FulfillOptions().setStatus(200)
            .setContentType("application/json")
            .setBody("{\"labelsByOwner\":[]}")));

    // Navigate directly — avoids in-flight dashboard API calls contaminating viewState.loadError.
    navigateAndWaitForUrl(FirewallAutoUnquarantinePage.url(), "autoReleaseQuarantine");
    assertThat(autoPage.container()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(regressionPage.autoUnquarantineTableFirstRow()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS));
    assertThat(regressionPage.autoUnquarantineTableRows()).hasCount(3,
        new LocatorAssertions.HasCountOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    // Step 1: all five column headers are present
    assertThat(regressionPage.autoUnquarantineComponentHeader()).isVisible();
    assertThat(regressionPage.autoUnquarantineQuarantineDateHeader()).isVisible();
    assertThat(regressionPage.autoUnquarantineRepositoryHeader()).isVisible();
    assertThat(regressionPage.autoUnquarantineDateClearedHeader()).isVisible();
    assertThat(regressionPage.autoUnquarantineChevronHeader()).isVisible();

    // Step 2: Quarantine Date sorts ascending then descending; first-row component reflects order
    regressionPage.autoUnquarantineQuarantineDateSortButton().click();
    assertThat(regressionPage.autoUnquarantineQuarantineDateHeader()).hasAttribute(DATA.ariaSortAttribute(),
        DATA.ariaSortAscending());
    assertThat(regressionPage.autoUnquarantineTableFirstRow()).containsText("aru-art-1:1.0",
        new LocatorAssertions.ContainsTextOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    regressionPage.autoUnquarantineQuarantineDateSortButton().click();
    assertThat(regressionPage.autoUnquarantineQuarantineDateHeader()).hasAttribute(DATA.ariaSortAttribute(),
        DATA.ariaSortDescending());
    assertThat(regressionPage.autoUnquarantineTableFirstRow()).containsText("aru-art-3:1.0",
        new LocatorAssertions.ContainsTextOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    // Step 3: Date Cleared sorts ascending then descending; first-row component reflects order
    regressionPage.autoUnquarantineDateClearedSortButton().click();
    assertThat(regressionPage.autoUnquarantineDateClearedHeader()).hasAttribute(DATA.ariaSortAttribute(),
        DATA.ariaSortAscending());
    assertThat(regressionPage.autoUnquarantineTableFirstRow()).containsText("aru-art-1:1.0",
        new LocatorAssertions.ContainsTextOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    regressionPage.autoUnquarantineDateClearedSortButton().click();
    assertThat(regressionPage.autoUnquarantineDateClearedHeader()).hasAttribute(DATA.ariaSortAttribute(),
        DATA.ariaSortDescending());
    assertThat(regressionPage.autoUnquarantineTableFirstRow()).containsText("aru-art-3:1.0",
        new LocatorAssertions.ContainsTextOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    // Step 4: clicking a row navigates to the component details page and renders without error
    regressionPage.autoUnquarantineTableFirstRow().click();
    assertThat(page).hasURL(Pattern.compile(".*/firewall/repository/aru-repo-id/component/.*"));
    assertThat(new FirewallComponentDetailsRegressionPage().container()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));

    // Step 5: empty state — unroute the mock; navigating from the component-details URL back to
    // autoReleaseQuarantine is a genuine UI Router transition so the component remounts and the
    // real (empty) API is called, rendering "No data found."
    page.unroute(Pattern.compile(".*/api/v2/firewall/components/autoReleasedFromQuarantine.*"));
    navigateAndWaitForUrl(FirewallAutoUnquarantinePage.url(), "autoReleaseQuarantine");
    assertThat(autoPage.container()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(regressionPage.autoUnquarantineTableEmptyMessage()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(regressionPage.autoUnquarantineTableEmptyMessage()).containsText(DATA.emptyStateText());
  }

  /**
   * Firewall Onboarding page renders via the Solution Switcher on a fresh installation;
   * navigating away while {@code isConfiguring=true} triggers the unsaved-changes modal,
   * which presents Cancel and Continue buttons.
   * Note: {@code route.js} registers {@code unsavedChangesModal: IncompleteConfigurationModal} in route data,
   * but the transition guard in {@code main.js} always opens the generic {@code UnsavedChangesModal} —
   * the custom component is never rendered via that path.
   */
  @Test
  @Category(RegressionTest.class)
  public void testFirewallOnboarding_pageRendersAndNavigationAwayTriggersModal() {
    // Mock solutions/licensed so the Solution Switcher shows a "Repository Firewall" link
    // pointing at the onboarding page, simulating a fresh installation where the user arrives
    // at onboarding by selecting Firewall from the Solution Switcher.
    String onboardingUrl = baseUrlFromTest + FirewallRegressionPage.firewallOnboardingUrl();
    page.route(Pattern.compile(".*/api/v2/solutions/licensed.*"), route -> route.fulfill(new Route.FulfillOptions()
        .setContentType("application/json")
        .setBody("[{\"id\":\"firewall\",\"url\":\"" + onboardingUrl + "\"}]")));

    playwrightRefreshOrOpen("/");

    FirewallRegressionPage regressionPage = new FirewallRegressionPage();

    // Enter via Solution Switcher → "Repository Firewall" → lands on onboarding page
    clickFirewallSolutionSwitcherLink(regressionPage);

    assertThat(regressionPage.firewallOnboardingPageContainer()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(regressionPage.firewallOnboardingGetStartedButton()).isVisible();

    // Navigate away while isConfiguring=true — IncompleteConfigurationModal must appear
    page.evaluate("window.location.hash = '" + FIREWALL_DASHBOARD_HASH + "'");

    assertThat(regressionPage.unsavedChangesModal()).isVisible();
    assertThat(regressionPage.incompleteConfigModalHeading()).containsText(DATA.incompleteConfigModalHeadingText());
    assertThat(regressionPage.incompleteConfigModalWarningAlert())
        .containsText(DATA.incompleteConfigModalWarningText());
    assertThat(regressionPage.unsavedChangesModalCancelButton()).isVisible();
    assertThat(regressionPage.unsavedChangesModalContinueButton()).isVisible();

    regressionPage.unsavedChangesModalCancelButton().click();
  }

  /** Repository Results Summary page renders the stats bar with violation, coverage, and quarantine indicators. */
  @Test
  @Category(RegressionTest.class)
  public void testFirewallRepositoryResultsSummary_pageRendersWithStatsBar() {
    String suffix = "-" + TemporaryEntity.uuid();
    seedDbForRepoResultsSummaryTest(suffix);

    // Setup: mock solutions/licensed so the Solution Switcher shows a Repository Firewall entry
    String firewallDashboardUrl = baseUrlFromTest + FirewallPage.url();
    page.route(Pattern.compile(".*/api/v2/solutions/licensed.*"), route -> route.fulfill(new Route.FulfillOptions()
        .setContentType("application/json")
        .setBody("[{\"id\":\"firewall\",\"url\":\"" + firewallDashboardUrl + "\"}]")));

    playwrightRefreshOrOpen("/");

    FirewallRegressionPage regressionPage = new FirewallRegressionPage();
    FirewallPage firewallPage = new FirewallPage();
    FirewallRepositoryResultsRegressionPage repoPage = new FirewallRepositoryResultsRegressionPage();

    // Step 1: click Solution Switcher → select Repository Firewall
    clickFirewallSolutionSwitcherLink(regressionPage);

    // Step 2: click the Quarantine sub-tab
    assertThat(firewallPage.container()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    firewallPage.tab("quarantine").click();

    // Step 3: click the repository name link in the quarantine table
    assertThat(firewallPage.quarantineTableRows()).hasCount(1,
        new LocatorAssertions.HasCountOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    regressionPage.quarantineTableRepoLink(0).click();

    // RepositoryResultsSummaryPage renders with aggregated stats
    assertThat(repoPage.container()).isVisible();
    assertThat(repoPage.statsBar()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS));
    assertThat(repoPage.violationsIndicator()).isVisible();
    assertThat(repoPage.coverageIndicator()).isVisible();
    assertThat(repoPage.quarantineIndicator()).isVisible();
  }

  /** Quarantined Component Report renders the page heading and component overview tile for a valid token URL. */
  @Test
  @Category(RegressionTest.class)
  public void testFirewallQuarantinedComponentReport_pageRendersWithComponentDetailsAndQuarantineReason() {
    String suffix = "-" + TemporaryEntity.uuid();
    QuarantinedComponentAccess access = seedDbForQuarantinedComponentReportTest(suffix);

    String token = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(access.getId().getBytes(StandardCharsets.UTF_8));

    FirewallRegressionPage regressionPage = new FirewallRegressionPage();

    playwrightRefreshOrOpen(FirewallRegressionPage.quarantinedComponentReportUrl(token));

    assertThat(regressionPage.quarantinedComponentReportContainer()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    assertThat(regressionPage.quarantinedComponentReportHeading()).containsText(DATA.quarantinedComponentHeadingText());
    assertThat(regressionPage.quarantinedComponentOverviewTile()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    // Waiver option present — navigates to component details page where a waiver can be created
    assertThat(regressionPage.quarantinedComponentViewDetailsButton()).isVisible();
  }

  private QuarantinedComponentAccess seedDbForQuarantinedComponentReportTest(String suffix) {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repoManager, "qcr-repo" + suffix, true, true);
    ComponentIdentifier identifier =
        ComponentIdentifier.createMavenCoordinates("qcr-grp" + suffix, "qcr-art" + suffix, DATA.componentVersion());
    Date now = Date.from(Instant.now());
    RepositoryComponent component = new RepositoryComponent(
        repo.getId(), "qcr/path" + suffix, now, "qcr-hash-001",
        identifier, MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), now);
    component.setDisplayName("qcr-art" + suffix + ":1.0");
    component.setQuarantineTime(now);
    tempEntity.newRepositoryComponent(component);
    Policy policy = tempEntity.newPolicy();
    tempEntity.newRepositoryPolicyViolation(
        component.getRepositoryId(), POLICY_THREAT_LEVEL, component.getPathname(), false,
        FailActionType.ID, policy.getId(), "qcr-policy" + suffix, component.getComponentIdentifier());
    return tempEntity.newQuarantinedComponentAccess(repo.getId(), component.getId());
  }

  private Repository seedDbForRepoResultsSummaryTest(String suffix) {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repoManager, "rrs-repo" + suffix, true, false);
    Date now = Date.from(Instant.now());
    RepositoryComponent component = tempEntity.newRepositoryComponent(
        repo.getId(), MatchState.EXACT, "rrs/path" + suffix, "rrs-hash-001",
        ComponentIdentifier.createMavenCoordinates("rrs-grp" + suffix, "rrs-art" + suffix, DATA.componentVersion()),
        now, now);
    Policy policy = tempEntity.newPolicy();
    tempEntity.newRepositoryPolicyViolation(
        component.getRepositoryId(), POLICY_THREAT_LEVEL, component.getPathname(), false,
        FailActionType.ID, policy.getId(), "rrs-policy" + suffix, component.getComponentIdentifier());
    return repo;
  }

  /**
   * Solution Switcher → "Repository Firewall" navigates to the Firewall dashboard;
   * clicking the Components tab → Quarantine sub-tab shows the quarantine table;
   * clicking the component name link opens the component details page; and all
   * details tabs (Overview, Violations, Security, Legal, Labels) render correctly.
   */
  @Test
  @Category(RegressionTest.class)
  public void testFirewallComponentDetails_allTabsVisible() {
    String suffix = "-" + TemporaryEntity.uuid();
    seedDbForNavigationTest(suffix);

    // Intercept the licensed-solutions API to inject a Repository Firewall entry pointing at the
    // local test server's Firewall dashboard URL (license in test env may not include the product).
    String firewallDashboardUrl = baseUrlFromTest + FirewallPage.url();
    page.route(Pattern.compile(".*/api/v2/solutions/licensed.*"), route -> route.fulfill(new Route.FulfillOptions()
        .setContentType("application/json")
        .setBody("[{\"id\":\"firewall\",\"url\":\"" + firewallDashboardUrl + "\"}]")));

    // Intercept component-details so the page receives matchState='exact', which causes all 5 tabs
    // (Overview, Violations, Security, Legal, Labels) to render. Without HDS data, IQ returns
    // matchState='unknown' (createEmptyComponentDetails), hiding Security/Legal/Labels tabs.
    // route.fetch() lets the real IQ request complete so the response includes a valid
    // componentIdentifier; we only replace the matchState value in the JSON.
    // Pattern matches /rest/ci/componentDetails/{ownerType}/{ownerId}?... but NOT /allVersions.
    page.route(Pattern.compile(".*/rest/ci/componentDetails/[^/?]+/[^/?]+\\?.*"), route -> {
      try {
        APIResponse response = route.fetch();
        String body = response.text();
        String modified = body.contains("\"matchState\"")
            ? body.replaceFirst("\"matchState\"\\s*:\\s*\"[^\"]*\"", "\"matchState\":\"exact\"")
            // body is always a non-empty JSON object from IQ; leading-brace replace is safe
            : body.replaceFirst("^\\{", "{\"matchState\":\"exact\",");
        route.fulfill(new Route.FulfillOptions()
            .setStatus(response.status())
            .setContentType("application/json")
            .setBody(modified));
      }
      catch (Exception e) {
        route.resume();
      }
    });

    // The allVersions endpoint triggers IQ to call HDS at rest/ci/componentDetails/list, which has
    // no stub in the test HDS mock server. Intercept it at the browser level so the Overview tab's
    // RiskRemediation tile loads cleanly rather than showing an error banner.
    page.route(Pattern.compile(".*/rest/ci/componentDetails/.*/allVersions.*"),
        route -> route.fulfill(new Route.FulfillOptions()
            .setContentType("application/json")
            .setBody(
                "{\"allVersions\":[],\"remediation\":null,\"sourceResponse\":null,\"automatedRemediationStatus\":null}")));

    // Reload so the Solution Switcher fetches from the intercepted API
    playwrightRefreshOrOpen("/");

    FirewallRegressionPage regressionPage = new FirewallRegressionPage();
    FirewallPage firewallPage = new FirewallPage();
    FirewallComponentDetailsRegressionPage detailsPage = new FirewallComponentDetailsRegressionPage();

    // Step 1: click Solution Switcher → select Repository Firewall
    clickFirewallSolutionSwitcherLink(regressionPage);

    // Step 2: click the Quarantine sub-tab
    assertThat(firewallPage.container()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    firewallPage.tab("quarantine").click();

    // Step 3: click the component name link in the quarantine table
    assertThat(firewallPage.quarantineTableRows()).hasCount(1,
        new LocatorAssertions.HasCountOptions().setTimeout(PlaywrightTiming.ELEMENT_TIMEOUT_MS));
    firewallPage.componentDetailsLink(0).click();

    // Step 4: all component-details tabs render
    assertThat(detailsPage.container()).isVisible(
        new LocatorAssertions.IsVisibleOptions().setTimeout(PlaywrightTiming.SLOW_ELEMENT_TIMEOUT_MS));
    assertThat(detailsPage.overviewTab()).isVisible();
    assertThat(detailsPage.violationsTab()).isVisible();
    assertThat(detailsPage.securityTab()).isVisible();
    assertThat(detailsPage.legalTab()).isVisible();
    assertThat(detailsPage.labelsTab()).isVisible();
  }

  private RepositoryComponent seedDbForNavigationTest(String suffix) {
    Policy policy = tempEntity.newPolicy();
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newRepository(repoManager, "nav-repo" + suffix, true, false);
    Date now = Date.from(Instant.now());
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates(
        "nav-grp" + suffix, "nav-art" + suffix, DATA.componentVersion());
    RepositoryComponent component = new RepositoryComponent(
        repo.getId(), "nav/path" + suffix, now, "nav-hash-001",
        identifier, MatchState.EXACT.getId(), IdentificationSource.SONATYPE.getId(), now);
    component.setDisplayName("nav-art" + suffix + ":" + DATA.componentVersion());
    component.setQuarantineTime(now);
    tempEntity.newRepositoryComponent(component);
    tempEntity.newRepositoryPolicyViolation(
        component.getRepositoryId(), POLICY_THREAT_LEVEL, component.getPathname(), false,
        FailActionType.ID, policy.getId(), "nav-policy" + suffix, component.getComponentIdentifier());
    return component;
  }

  private void clickFirewallSolutionSwitcherLink(FirewallRegressionPage regressionPage) {
    regressionPage.solutionSwitcherToggle().click();
    Locator firewallLink = regressionPage.solutionSwitcherFirewallLink();
    assertThat(firewallLink).isVisible();
    firewallLink.evaluate("link => link.removeAttribute('target')");
    firewallLink.click();
  }

  private void enableIntegratedEnterpriseReportingOnLicense() {
    Set<LicensedFeature> baseline = productLicenseManager.getFeatures();
    EnumSet<LicensedFeature> merged =
        baseline != null && !baseline.isEmpty()
            ? EnumSet.copyOf(baseline)
            : EnumSet.allOf(LicensedFeature.class);
    merged.add(LicensedFeature.INTEGRATED_ENTERPRISE_REPORTING);
    setFeatures(merged.toArray(new LicensedFeature[0]));
  }

  private void stubEnterpriseReportingHds() {
    testCLMServer.getHdsServer()
        .respondWith(ENTERPRISE_REPORTING_HDS.currentVersion().toString())
        .atUri("rest/enterpriseReporting/currentVersion");
    testCLMServer.getHdsServer()
        .respondWith(ENTERPRISE_REPORTING_HDS.dashboards().toString())
        .atUri("rest/enterpriseReporting/dashboards");
  }

  /** Typed view of {@code src/test/resources/test-data/firewall-regression-operations.json}. */
  public record Data(
      String dashboardId,
      String dashboardTitle,
      String expectedDashboardUrlFragment,
      String componentVersion,
      String bulkWaiveExpiryOption,
      String confirmationHeadingText,
      String waiverConfigHeadingText,
      String modalSaveButtonText,
      String modalNoChangesText,
      String emptyStateText,
      String quarantinedComponentHeadingText,
      String bulkWaiveUrlFragment,
      String bulkWaiveConfigUrlFragment,
      String bulkWaiveConfirmationUrlFragment,
      String ariaSortAttribute,
      String ariaSortAscending,
      String ariaSortDescending,
      String incompleteConfigModalHeadingText,
      String incompleteConfigModalWarningText)
  {
  }
}
