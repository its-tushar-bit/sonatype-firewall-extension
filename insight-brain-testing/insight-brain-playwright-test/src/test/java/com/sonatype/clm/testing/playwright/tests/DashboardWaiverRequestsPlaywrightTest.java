/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardFiltersComponent;
import com.sonatype.clm.testing.playwright.pages.DashboardFiltersComponentAssertions;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.DashboardWaiverRequestsComponent;
import com.sonatype.clm.testing.playwright.pages.DashboardWaiverRequestsComponent.ExpectedRow;
import com.sonatype.clm.testing.playwright.pages.DashboardWaiverRequestsComponentAssertions;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Playwright migration of the Selenide {@code DashboardWaiverRequestsTest}.
 *
 * <p>
 * Each test follows a Given / When / Then shape:
 * <ul>
 * <li>{@link #openWaiverRequestsTabAsAdmin()} navigates to the Waiver Requests dashboard tab,
 * logs in as admin and waits for the spinner to clear.</li>
 * <li>The seeder ({@link DashboardWaiverRequestsSeeder}) — instantiated lazily inside each
 * test — owns all DB writes for org / app / repository / policy / waiver-request rows.
 * Each {@code @Test} keeps its own seeder instance per the per-test isolation contract
 * (authoring guide §3c, §7b).</li>
 * <li>UI assertions live on {@link DashboardWaiverRequestsComponent} so this class only
 * expresses intent (open ➜ assert no-data, or seed-9 ➜ assert nine rows).</li>
 * </ul>
 *
 * <p>
 * String literals (entity names, expected row contents, the no-data message and the canonical
 * waiver-reason filter options) are loaded from
 * {@code src/test/resources/test-data/dashboard-waiver-requests.json} via {@link TestDataManager}.
 * See {@code PLAYWRIGHT_TEST_AUTHORING_GUIDE.md}.
 */
public class DashboardWaiverRequestsPlaywrightTest
    extends AbstractIqUiTest
{
  private static final DashboardWaiverRequestsData DATA =
      TestDataManager.load("dashboard-waiver-requests", DashboardWaiverRequestsData.class);

  private static final ZoneId ZONE = ZoneId.systemDefault();

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZONE);

  private DashboardWaiverRequestsComponent waiverRequestsTable;

  // --------------- @Before / @After ---------------

  @BeforeEach
  public void openWaiverRequestsTabAsAdmin() {
    playwrightRefreshOrOpen(DashboardPage.urlToWaiverRequests());
    playwrightLogin();

    waiverRequestsTable = new DashboardWaiverRequestsComponent();
    waiverRequestsTable.waitUntilLoaded();
  }

  @AfterEach
  public void resetReverseProxy() {
    reverseProxyServer.reset();
  }

  // --------------- @Test methods ---------------

  /**
   * With no waiver requests in the DB, the table renders the "no data in last 30 days"
   * placeholder message.
   */
  @Test
  @Tag("sanity")
  public void testWaiverRequestsTable_NoDataMessage() {
    // Then: placeholder row renders with the canonical empty-state copy.
    new DashboardWaiverRequestsComponentAssertions(waiverRequestsTable).shouldShowNoDataMessage(DATA.noDataMessage());
  }

  /**
   * Nine waiver requests across orgs, apps, root org, repository and repository-container scopes
   * render in the table — newest first, with the per-row threat / requester / policy / scope /
   * component / status text matching the rendered cells.
   */
  @Test
  @Tag("sanity")
  public void testWaiverRequestsTable_LoadsAllWaiverRequestsWithoutFilters() {
    // Given: nine waiver requests of varying scope and component-match strategy.
    seedAllWaiverRequests();

    // When: the dashboard is reloaded so the freshly-seeded rows are queried.
    playwrightRefresh();
    waiverRequestsTable.waitUntilLoaded();
    assertThat(new DashboardPage().dashboardContainer()).isVisible();

    // Then: the table holds exactly nine rows in the expected order.
    DashboardWaiverRequestsComponentAssertions waiverAssertions =
        new DashboardWaiverRequestsComponentAssertions(waiverRequestsTable);
    waiverAssertions.shouldHaveRequestCount(DATA.expectedTotalRequestCount());

    List<ExpectedRow> expected = buildExpectedRows();
    for (int i = 0; i < expected.size(); i++) {
      waiverAssertions.shouldShowRequestRow(i, expected.get(i));
    }
  }

  /**
   * The dashboard filter drawer renders the canonical set of waiver-reason options, in order.
   */
  @Test
  @Tag("sanity")
  public void testShowsReasonsFilter() {
    // Given: the dashboard is on the waiver-requests tab (see @Before).
    DashboardPage dashboard = new DashboardPage();

    // When: the filter drawer is opened and the policy-waiver-reason section expanded.
    dashboard.expandFilter();
    DashboardFiltersComponent filters = new DashboardFiltersComponent();
    filters.expandWaiverReasonFilter();

    // Then: every canonical option is listed in the documented order.
    new DashboardFiltersComponentAssertions(filters).shouldShowWaiverReasonOptions(DATA.expectedWaiverReasonOptions());
  }

  // --------------- Row-expectation builder ---------------

  /**
   * Translate the {@code expectedRows[]} JSON into table-row expectations. Done in Java rather
   * than the JSON itself so the root-organization name (a runtime value owned by the harness)
   * can be substituted for the {@code "ROOT_ORG_NAME"} placeholder in the fixture.
   */
  private List<ExpectedRow> buildExpectedRows() {
    Instant now = Instant.now();
    OrganizationDAO organizationDAO = lookup(OrganizationDAO.class);
    Organization rootOrg = organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID);

    List<ExpectedRow> rows = new ArrayList<>();
    for (DashboardWaiverRequestsData.RowSpec spec : DATA.expectedRows()) {
      String scope = "ROOT_ORG_NAME".equals(spec.expectedScope()) ? rootOrg.getName() : spec.expectedScope();
      rows.add(new ExpectedRow(
          spec.expectedThreat(),
          DATE_FORMAT.format(requestInstant(now, spec.requestDayOffset())),
          spec.expectedRequester(),
          spec.expectedPolicy(),
          scope,
          spec.expectedComponent(),
          spec.expectedStatus()));
    }
    return rows;
  }

  private static Instant requestInstant(Instant anchor, int dayOffset) {
    return anchor.plus(dayOffset, ChronoUnit.DAYS);
  }

  private static Date toDate(Instant instant) {
    return Date.from(instant);
  }

  // --------------- Backend seed fields ---------------

  private Organization seederParentOrg;

  private Organization seederChildOrg;

  private Application seederApp1;

  private Application seederApp2;

  private Repository seederRepository;

  private List<Policy> seederPolicies;

  private final List<PolicyWaiverRequest> seederRequests = new ArrayList<>();

  // --------------- Backend seed methods ---------------

  private void seedAllWaiverRequests() {
    seedHierarchy();
    seedPolicies();

    Instant now = Instant.now();
    String purl = mavenPurl(DATA.componentGroupId(), DATA.componentArtifactId(),
        DATA.componentVersion(), DATA.componentClassifier(), DATA.componentExtension());

    for (DashboardWaiverRequestsData.RowSpec spec : DATA.expectedRows()) {
      seederRequests.add(insertWaiverRequest(spec, now, purl));
    }
  }

  private void seedHierarchy() {
    seederParentOrg = tempEntity.newOrganization(DATA.parentOrgName());
    seederChildOrg = tempEntity.newOrganization(DATA.orgName(), seederParentOrg);
    seederApp1 = tempEntity.newApplication(DATA.app1Name(), DATA.app1Id(), seederChildOrg.getId());
    seederApp2 = tempEntity.newApplication(DATA.app2Name(), DATA.app2Id(), seederChildOrg.getId());
    seederRepository = tempEntity.newRepository(DATA.repositoryName());
  }

  private void seedPolicies() {
    seederPolicies = new ArrayList<>();
    for (DashboardWaiverRequestsData.PolicySpec p : DATA.policies()) {
      seederPolicies.add(tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, p.name(), p.threatLevel()));
    }
  }

  private PolicyWaiverRequest insertWaiverRequest(
      DashboardWaiverRequestsData.RowSpec spec,
      Instant now,
      String purl)
  {
    PolicyWaiverRequest request = new PolicyWaiverRequest()
        .setHash("hash" + (seederRequests.size() + 1))
        .setPolicyId(seederPolicies.get(spec.policyIndex()).getId())
        .setOwnerId(resolveOwnerId(spec.scopeOwner()))
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(ComponentMatcherStrategyForWaiver.valueOf(spec.componentMatch()))
        .setComment(spec.comment())
        .setRequestTime(toDate(requestInstant(now, spec.requestDayOffset())))
        .setRequesterId(DATA.requesterId())
        .setRequesterName(DATA.requesterName())
        .setComponentUpgradeAvailable(spec.componentUpgradeAvailable());
    if (spec.expiryDayOffset() != null) {
      request.setExpiryTime(toDate(requestInstant(now, spec.expiryDayOffset())));
    }
    return tempEntity.newPolicyWaiverRequest(request);
  }

  private String resolveOwnerId(String scopeOwner) {
    return switch (scopeOwner) {
      case "ROOT_ORG" -> Organization.ROOT_ORGANIZATION_ID;
      case "PARENT_ORG" -> seederParentOrg.getId();
      case "ORG" -> seederChildOrg.getId();
      case "APP_1" -> seederApp1.getId();
      case "APP_2" -> seederApp2.getId();
      case "REPOSITORY" -> seederRepository.getId();
      case "REPOSITORY_CONTAINER" -> RepositoryContainer.REPOSITORY_CONTAINER_ID;
      default -> throw new IllegalArgumentException("Unknown scopeOwner: " + scopeOwner);
    };
  }

  private String mavenPurl(
      String groupId,
      String artifactId,
      String version,
      String classifier,
      String extension)
  {
    ComponentIdentifier id = ComponentIdentifier.createMavenCoordinates(
        groupId, artifactId, version, classifier, extension);
    return PackageUrlIdentifier.fromComponentIdentifier(id).getPackageUrl();
  }

  // --------------- Test data record ---------------

  /** Typed view of {@code src/test/resources/test-data/dashboard-waiver-requests.json}. */
  private record DashboardWaiverRequestsData(
      String parentOrgName,
      String orgName,
      String app1Name,
      String app1Id,
      String app2Name,
      String app2Id,
      String repositoryName,
      String componentGroupId,
      String componentArtifactId,
      String componentVersion,
      String componentExtension,
      String componentClassifier,
      String requesterId,
      String requesterName,
      List<PolicySpec> policies,
      String noDataMessage,
      int expectedTotalRequestCount,
      List<RowSpec> expectedRows,
      List<String> expectedWaiverReasonOptions)
  {
    public record PolicySpec(String name, int threatLevel)
    {
    }

    public record RowSpec(
        int policyIndex,
        String scopeOwner,
        String componentMatch,
        int requestDayOffset,
        Integer expiryDayOffset,
        boolean componentUpgradeAvailable,
        String comment,
        String expectedThreat,
        String expectedRequester,
        String expectedPolicy,
        String expectedScope,
        String expectedComponent,
        String expectedStatus)
    {
    }
  }
}
