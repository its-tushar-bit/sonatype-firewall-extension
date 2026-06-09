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
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.DashboardPageAssertions;
import com.sonatype.clm.testing.playwright.pages.DashboardWaiversComponent;
import com.sonatype.clm.testing.playwright.pages.DashboardWaiversComponentAssertions;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryContainer;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_VERSIONS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import com.sonatype.clm.testing.playwright.pages.WaiverDetailsPage;
import com.sonatype.clm.testing.playwright.pages.WaiverDetailsPageAssertions;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

public class DashboardWaiversPlaywrightTest
    extends AbstractIqUiTest
{

  private static final Data DATA = TestDataManager.load("dashboard-waivers", Data.class);

  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

  private final Instant now = Instant.now();

  private Organization rootOrg;

  private Organization organization;

  private Organization parentOrganization;

  private Application application;

  private Application application2;

  private Repository repository1;

  private List<Policy> securityPolicies;

  private String componentPurl;

  @Before
  public void openDashboardWaiversAsAdmin() {
    rootOrg = lookup(OrganizationDAO.class).getById(Organization.ROOT_ORGANIZATION_ID);
    playwrightRefreshOrOpen(DashboardPage.urlToWaivers());
    playwrightLogin();
    new DashboardPage().waitUntilSpinnersGone();
  }

  @After
  public void cleanup() {
    reverseProxyServer.reset();
  }

  @Test
  @Category(SanityTest.class)
  public void testWaiversTable_noDataMessage() {
    DashboardWaiversComponent table = new DashboardWaiversComponent();
    new DashboardWaiversComponentAssertions(table).shouldShowNoDataMessage(DATA.noDataMessage());
  }

  @Test
  @Category(SanityTest.class)
  public void testWaiversTable_loadsAllWaiversWithoutFilters() {
    seed();

    playwrightRefresh();

    new DashboardPageAssertions(new DashboardPage()).shouldBeLoaded();

    DashboardWaiversComponent table = new DashboardWaiversComponent();
    new DashboardWaiversComponentAssertions(table).shouldHaveWaiverCount(DATA.expectedWaiversCount());
    assertAllWaiverRows(table);
  }

  @Test
  @Category(SanityTest.class)
  public void testWaiversTableRowClick() {
    seed();

    assertClickNavigatesToWaiverDetail(DATA.waivers().get(0).rowIndex());
    assertClickNavigatesToWaiverDetail(DATA.waivers().get(1).rowIndex());
  }

  @Test
  @Category(RegressionTest.class)
  public void testWaiversTab_showsSubTabsAndNavigatesToWaiverRequests() {
    DashboardWaiversComponent table = new DashboardWaiversComponent();
    DashboardWaiversComponentAssertions assertions = new DashboardWaiversComponentAssertions(table);
    assertions.shouldShowExistingWaiversTab();
    assertions.shouldShowRequestedWaiversTab();

    table.requestedWaiversTab().click();
    playwrightWaitUntilUrlContains("/dashboard/waiverRequests");
    assertions.shouldShowWaiverRequestsTable();
  }

  @Test
  @Category(RegressionTest.class)
  public void testAutoWaiver_expiryColumnShowsAutoLabel() {
    seed();
    seedAutoWaiver();

    playwrightRefresh();
    new DashboardPageAssertions(new DashboardPage()).shouldBeLoaded();

    DashboardWaiversComponent table = new DashboardWaiversComponent();
    DashboardWaiversComponentAssertions assertions = new DashboardWaiversComponentAssertions(table);
    int expectedCount = DATA.expectedWaiversCount() + 1;
    assertions.shouldHaveWaiverCount(expectedCount);

    assertions.shouldShowExpiryTime(expectedCount - 1, DATA.autoWaiverExpiryLabel());
  }

  @Test
  @Category(RegressionTest.class)
  public void testDeleteWaiver_fromDashboardWaiverList() {
    seed();

    playwrightRefresh();
    new DashboardPageAssertions(new DashboardPage()).shouldBeLoaded();

    DashboardWaiversComponent table = new DashboardWaiversComponent();
    DashboardWaiversComponentAssertions assertions = new DashboardWaiversComponentAssertions(table);
    assertions.shouldHaveWaiverCount(DATA.expectedWaiversCount());
    table.waiver(0).click();
    playwrightWaitUntilUrlContains("/waiver/");

    WaiverDetailsPage detailsPage = new WaiverDetailsPage();
    assertThat(detailsPage.container()).isVisible();
    new WaiverDetailsPageAssertions(detailsPage).shouldHideDeleteWaiverModal();
    detailsPage.deleteWaiverAndConfirm();

    playwrightRefreshOrOpen(DashboardPage.urlToWaivers());
    new DashboardPage().waitUntilSpinnersGone();
    DashboardWaiversComponent tableAfterDelete = new DashboardWaiversComponent();
    DashboardWaiversComponentAssertions afterDeleteAssertions =
        new DashboardWaiversComponentAssertions(tableAfterDelete);
    tableAfterDelete.waivers()
        .first()
        .waitFor();
    afterDeleteAssertions.shouldHaveWaiverCount(DATA.expectedWaiversCount() - 1);
  }

  private void assertClickNavigatesToWaiverDetail(int rowIndex) {
    playwrightRefreshOrOpen(DashboardPage.urlToWaivers());
    new DashboardPage().waitUntilSpinnersGone();
    DashboardWaiversComponent table = new DashboardWaiversComponent();
    table.waivers()
        .first()
        .waitFor();
    new DashboardWaiversComponentAssertions(table).shouldHaveWaiverCount(DATA.expectedWaiversCount());
    table.waiver(rowIndex).click();
    playwrightWaitUntilUrlContains("/waiver/");
  }

  private void seed() {
    seedOrgsAppsAndRepo();
    seedPolicies();
    seedPolicyViolations();
    seedWaiverRecords();
  }

  private void seedOrgsAppsAndRepo() {
    parentOrganization = tempEntity.newOrganization(DATA.parentOrgName());
    organization = tempEntity.newOrganization(DATA.orgName(), parentOrganization);
    application = tempEntity.newApplication(DATA.app1Name(), DATA.app1Id(), organization.getId());
    application2 = tempEntity.newApplication(DATA.app2Name(), DATA.app2Id(), organization.getId());
    repository1 = tempEntity.newRepository(DATA.repositoryName());
  }

  private void seedPolicies() {
    securityPolicies = new ArrayList<>();
    for (Data.PolicyData pd : DATA.policies()) {
      securityPolicies.add(
          tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, pd.name(), pd.threatLevel()));
    }
  }

  private void seedPolicyViolations() {
    Data.WaiverData firstWaiver = DATA.waivers().get(0);
    PolicyEvaluation evaluation1 = tempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.BUILD.getId(), "scan1", false, false, daysAgo(firstWaiver.createDaysAgo()));
    PolicyEvaluation evaluation2 = tempEntity.newPolicyEvaluation(application2.getId(),
        StageTypes.BUILD.getId(), "scan1", false, false, daysAgo(firstWaiver.createDaysAgo()));

    tempEntity.newPolicyViolation(evaluation1, securityPolicies.get(0), "Group1", "Artifact1", "Version1", "hash1",
        "sonatype-2017-0507");
    tempEntity.newPolicyViolation(evaluation1, securityPolicies.get(1), "Group2", "Artifact2", "Version2", "hash2",
        "sonatype-2017-8912");
    tempEntity.newPolicyViolation(evaluation2, securityPolicies.get(2), "Group3", "Artifact3", "Version3", "hash3",
        "sonatype-2017-7848");

    componentPurl = PackageUrlIdentifier.fromComponentIdentifier(
        ComponentIdentifier.createMavenCoordinates(
            DATA.componentGroupId(), DATA.componentArtifactId(), DATA.componentVersion(), "", "jar"))
        .getPackageUrl();
  }

  private void seedWaiverRecords() {
    for (Data.WaiverData w : DATA.waivers()) {
      PolicyWaiver waiver = new PolicyWaiver()
          .setHash(w.hash())
          .setPolicyId(securityPolicies.get(w.policyIndex()).getId())
          .setOwnerId(ownerIdFor(w))
          .setAssociatedPackageUrl(componentPurl)
          .setComponentMatchStrategy(strategyFor(w))
          .setComment(w.comment())
          .setCreateTime(daysAgo(w.createDaysAgo()))
          .setCreatorId(DATA.creatorId())
          .setCreatorName(DATA.creatorName());

      if (w.expiryDaysFromNow() != null) {
        waiver.setExpiryTime(daysFromNow(w.expiryDaysFromNow()));
      }
      if (w.upgradeAvailable()) {
        waiver.setComponentUpgradeAvailable(true);
      }

      tempEntity.newWaiver(waiver);
    }
  }

  private String ownerIdFor(Data.WaiverData w) {
    return switch (w.ownerKey()) {
      case "org" -> organization.getId();
      case "app1" -> application.getId();
      case "app2" -> application2.getId();
      case "rootOrg" -> rootOrg.getId();
      case "repo" -> repository1.getId();
      case "repoContainer" -> RepositoryContainer.REPOSITORY_CONTAINER_ID;
      case "parentOrg" -> parentOrganization.getId();
      default -> throw new IllegalArgumentException("Unknown ownerKey: " + w.ownerKey());
    };
  }

  private ComponentMatcherStrategyForWaiver strategyFor(Data.WaiverData w) {
    return switch (w.matchStrategy()) {
      case "EXACT_COMPONENT" -> EXACT_COMPONENT;
      case "ALL_COMPONENTS" -> ALL_COMPONENTS;
      case "ALL_VERSIONS" -> ALL_VERSIONS;
      default -> throw new IllegalArgumentException("Unknown matchStrategy: " + w.matchStrategy());
    };
  }

  private void assertAllWaiverRows(DashboardWaiversComponent table) {
    DashboardWaiversComponentAssertions assertions = new DashboardWaiversComponentAssertions(table);
    for (Data.WaiverData w : DATA.waivers()) {
      int row = w.rowIndex();
      Data.PolicyData policyData = DATA.policies().get(w.policyIndex());

      assertions.shouldShowThreatLevel(row, String.valueOf(policyData.threatLevel()));
      assertions.shouldShowCreateTime(row, DATE_FMT.format(now.minus(w.createDaysAgo(), ChronoUnit.DAYS)));
      String expectedExpiry = w.expiryDaysFromNow() != null
          ? DATE_FMT.format(now.plus(w.expiryDaysFromNow(), ChronoUnit.DAYS))
          : DATA.neverExpiryText();
      assertions.shouldShowExpiryTime(row, expectedExpiry);
      assertions.shouldShowPolicy(row, policyData.name());
      assertions.shouldShowScope(row, scopeFor(w));
      assertions.shouldShowComponent(row, componentFor(w));
      if (w.upgradeAvailable()) {
        assertions.shouldShowUpgradeAvailable(row, DATA.upgradeAvailableText());
      }
    }
  }

  private String scopeFor(Data.WaiverData w) {
    return switch (w.ownerKey()) {
      case "org" -> "Organization - " + DATA.orgName();
      case "app1" -> "Application - " + DATA.app1Name();
      case "app2" -> "Application - " + DATA.app2Name();
      case "rootOrg" -> rootOrg.getName();
      case "repo" -> "Repository - " + DATA.repositoryName();
      case "repoContainer" -> DATA.repositoryContainerName();
      case "parentOrg" -> "Organization - " + DATA.parentOrgName();
      default -> throw new IllegalArgumentException("Unknown ownerKey: " + w.ownerKey());
    };
  }

  private String componentFor(Data.WaiverData w) {
    return switch (w.matchStrategy()) {
      case "EXACT_COMPONENT" -> DATA.componentGroupId() + " : " + DATA.componentArtifactId() + " : "
          + DATA.componentVersion();
      case "ALL_COMPONENTS" -> DATA.allComponentsLabel();
      case "ALL_VERSIONS" -> DATA.componentGroupId() + " : " + DATA.componentArtifactId() + " (all versions)";
      default -> throw new IllegalArgumentException("Unknown matchStrategy: " + w.matchStrategy());
    };
  }

  private void seedAutoWaiver() {
    tempEntity.newAutoPolicyWaiver(application.getId());
  }

  private Date daysAgo(int days) {
    return Date.from(now.minus(days, ChronoUnit.DAYS));
  }

  private Date daysFromNow(int days) {
    return Date.from(now.plus(days, ChronoUnit.DAYS));
  }

  public record Data(
      String noDataMessage,
      String parentOrgName,
      String orgName,
      String app1Name,
      String app1Id,
      String app2Name,
      String app2Id,
      String repositoryName,
      String repositoryContainerName,
      List<PolicyData> policies,
      String componentGroupId,
      String componentArtifactId,
      String componentVersion,
      String componentExtension,
      String componentSeparator,
      String creatorId,
      String creatorName,
      String allComponentsLabel,
      String upgradeAvailableText,
      String neverExpiryText,
      String autoWaiverExpiryLabel,
      int autoWaiverThreatLevel,
      int expectedWaiversCount,
      String scanId,
      String dateFormat,
      String allVersionsSuffix,
      String applicationScopePrefix,
      String organizationScopePrefix,
      String repositoryScopePrefix,
      String existingWaiversTabLabel,
      String requestedWaiversTabLabel,
      List<ViolationData> violations,
      List<WaiverData> waivers)
  {
    public record PolicyData(String name, int threatLevel)
    {
    }

    public record ViolationData(
        String groupId,
        String artifactId,
        String version,
        String hash,
        String refId,
        int policyIndex,
        int evaluationIndex)
    {
    }

    public record WaiverData(
        int rowIndex,
        String hash,
        int policyIndex,
        String ownerKey,
        String matchStrategy,
        String comment,
        int createDaysAgo,
        Integer expiryDaysFromNow,
        boolean upgradeAvailable)
    {
    }
  }
}
