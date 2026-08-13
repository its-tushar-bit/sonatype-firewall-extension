/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import jakarta.ws.rs.core.UriBuilder;

import com.microsoft.playwright.Route;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.ListWaiversTablePage;
import com.sonatype.clm.testing.playwright.pages.ListWaiversTablePageAssertions;
import com.sonatype.clm.testing.playwright.pages.ViolationDetailsPage;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ListWaiversTablePlaywrightTest
    extends AbstractIqUiTest
{
  private static final Data DATA = TestDataManager.load("list-waivers-table", Data.class);

  private static final Pattern APPLICABLE_WAIVERS_URL_REGEX =
      Pattern.compile(DATA.applicableWaiversUrlPattern());

  private static final Pattern APPLICABLE_AUTO_WAIVER_URL_REGEX =
      Pattern.compile(DATA.applicableAutoWaiverUrlPattern());

  private ListWaiversTablePage listPage;

  private ListWaiversTablePageAssertions assertions;

  private PolicyViolationDAO policyViolationDAO;

  private PolicyViolation policyViolation;

  private Organization organization;

  private Application application;

  private Policy policy;

  private String newestWaiverDateFormatted;

  private String oldestWaiverDateFormatted;

  private String newestExpiredWaiverDateFormatted;

  private String oldestExpiredWaiverDateFormatted;

  @Before
  public void seedAndOpenAsAdmin() {
    listPage = new ListWaiversTablePage();
    assertions = new ListWaiversTablePageAssertions(listPage);

    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(true);

    policyViolationDAO = lookup(PolicyViolationDAO.class);

    seedDb();
    stubHds();

    playwrightRefreshOrOpen(DashboardPage.url());
    playwrightLogin();
  }

  @After
  public void resetAutoWaiversFeature() {
    SystemConfigurationPropertyFeature.AUTO_WAIVERS.setEnabled(false);
  }

  @Test
  @Category(RegressionTest.class)
  public void testAutoWaiverRow_appearsAtTopAboveActiveWaivers() {
    seedAutoWaiverAppliedToViolation();
    seedActiveWaivers();

    openApplicableWaiversTab();

    assertions.shouldShowAutoWaiverWithoutDeleteButton();
    assertions.shouldShowAutoWaiverBeforeActiveWaivers();
    assertions.shouldShowAutoWaiverTag(DATA.autoWaiverTagText());
    assertions.shouldShowActiveWaiverCount(DATA.activeWaiverCount());
  }

  @Test
  @Category(RegressionTest.class)
  public void testExpiredWaivers_shownBelowActiveWaivers() {
    seedActiveAndExpiredWaivers();

    openApplicableWaiversTab();

    assertThat(listPage.activeWaiverRows().first()).isVisible();
    assertions.shouldShowExpiredWaiverCount(DATA.expiredWaiverCount());
    assertThat(listPage.expiredWaiverRows().first()).isVisible();
    assertions.shouldShowExpiredWaiversAfterActiveWaivers();
    assertThat(listPage.waiverRowCreatedDate(1)).containsText(newestExpiredWaiverDateFormatted);
    assertThat(listPage.waiverRowCreatedDate(2)).containsText(oldestExpiredWaiverDateFormatted);
  }

  @Test
  @Category(RegressionTest.class)
  public void testActiveWaivers_sortedNewestFirst() {
    seedActiveWaiversWithDistinctTimes();

    openApplicableWaiversTab();

    assertions.shouldShowActiveWaiverCount(DATA.activeWaiverCount());
    assertThat(listPage.waiverRowCreatedDate(0)).containsText(newestWaiverDateFormatted);
    assertThat(listPage.waiverRowCreatedDate(2)).containsText(oldestWaiverDateFormatted);
  }

  @Test
  @Category(RegressionTest.class)
  public void testEmptyState_includesExternalDocsLink() {
    openApplicableWaiversTab();

    assertions.shouldShowEmptyState(DATA.emptyStateText(), DATA.emptyStateLinkText(), DATA.emptyStateLinkHref());
  }

  @Test
  @Category(RegressionTest.class)
  public void testLoadingState_shownWhileWaiversLoading() {
    page.route(APPLICABLE_WAIVERS_URL_REGEX, route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(500)
        .setContentType(DATA.jsonContentType())
        .setBody(DATA.errorResponseBody())));
    page.route(APPLICABLE_AUTO_WAIVER_URL_REGEX, route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(500)
        .setContentType(DATA.jsonContentType())
        .setBody(DATA.errorResponseBody())));

    openApplicableWaiversTab();
    assertThat(listPage.errorMessage()).isVisible();

    page.unrouteAll();
    page.route(APPLICABLE_WAIVERS_URL_REGEX, route -> {
      // Do not fulfill — keeps the request pending, holding loadingApplicableWaivers=true.
    });
    page.route(APPLICABLE_AUTO_WAIVER_URL_REGEX, route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(200)
        .setContentType(DATA.jsonContentType())
        .setBody(DATA.nullAutoWaiverResponse())));

    listPage.retryButton().click();
    listPage.loadingSpinner().waitFor();

    assertions.shouldShowLoadingState(DATA.loadingMetaRowCount());
  }

  @Test
  @Category(RegressionTest.class)
  public void testErrorState_shownWithRetryButton() {
    page.route(APPLICABLE_WAIVERS_URL_REGEX, route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(500)
        .setContentType(DATA.jsonContentType())
        .setBody(DATA.errorResponseBody())));
    page.route(APPLICABLE_AUTO_WAIVER_URL_REGEX, route -> route.fulfill(new Route.FulfillOptions()
        .setStatus(500)
        .setContentType(DATA.jsonContentType())
        .setBody(DATA.errorResponseBody())));

    openApplicableWaiversTab();
    assertions.shouldShowErrorWithRetry();

    page.unrouteAll();
    AtomicBoolean waiversCalled = new AtomicBoolean(false);
    AtomicBoolean autoWaiverCalled = new AtomicBoolean(false);
    page.route(APPLICABLE_WAIVERS_URL_REGEX, route -> {
      waiversCalled.set(true);
      route.fulfill(new Route.FulfillOptions()
          .setStatus(200)
          .setContentType(DATA.jsonContentType())
          .setBody(DATA.emptyWaiversResponse()));
    });
    page.route(APPLICABLE_AUTO_WAIVER_URL_REGEX, route -> {
      autoWaiverCalled.set(true);
      route.fulfill(new Route.FulfillOptions()
          .setStatus(200)
          .setContentType(DATA.jsonContentType())
          .setBody(DATA.nullAutoWaiverResponse()));
    });

    listPage.retryButton().click();
    listPage.emptyMessage().waitFor();

    Assertions.assertThat(waiversCalled.get())
        .as("Retry should call applicableWaivers endpoint")
        .isTrue();
    Assertions.assertThat(autoWaiverCalled.get())
        .as("Retry should call applicableAutoWaiver endpoint")
        .isTrue();
  }

  @Test
  @Category(RegressionTest.class)
  public void testDeleteWaiverModal_mountsOnlyWhenDeleteInitiated() {
    seedActiveWaivers();

    openApplicableWaiversTab();

    assertions.shouldNotShowDeleteModal();
    listPage.firstDeleteButton().click();
    assertions.shouldShowDeleteModal(DATA.deleteModalHeading());
  }

  private ListWaiversTablePage openApplicableWaiversTab() {
    playwrightRefreshOrOpen(ViolationDetailsPage.url(policyViolation.getId()));
    playwrightRefresh();
    ViolationDetailsPage detailsPage = new ViolationDetailsPage();
    return detailsPage.waitAndOpenApplicableWaiversTab(DATA.hdsWaitTimeoutMs());
  }

  private void seedDb() {
    String suffix = TemporaryEntity.uuid();
    String orgName = DATA.organizationNamePrefix() + "-" + suffix;
    String appName = DATA.applicationNamePrefix() + "-" + suffix;
    String appPublicId = DATA.applicationPublicIdPrefix() + "-" + suffix;

    organization = tempEntity.newOrganization(orgName);
    application = tempEntity.newApplication(appName, appPublicId, organization.getId());

    policy = tempEntity.newPolicy(
        Organization.ROOT_ORGANIZATION_ID, DATA.policyName(), DATA.policyThreatLevel());

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(
        application.getId(), StageTypes.BUILD.getId(), DATA.scanId());

    policyViolation = tempEntity.newPolicyViolation(
        policyEvaluation,
        policy,
        DATA.componentGroup(),
        DATA.componentArtifact(),
        DATA.componentVersion(),
        DATA.componentHash(),
        DATA.vulnerabilityRefId());
  }

  private void stubHds() {
    URI uri = UriBuilder.fromPath("rest/vulnerability/details/json/{refId}").build(DATA.vulnerabilityRefId());
    URI bulkUri = UriBuilder.fromPath("rest/vulnerability/details/json").build();
    testCLMServer.getHdsServer()
        .respondWith(getClass().getClassLoader().getResource("vulnerabilityDetails/vulnerabilityDetails2.json"))
        .atUri(uri);
    testCLMServer.getHdsServer()
        .respondWith(Collections.emptyMap())
        .atUri(bulkUri);
  }

  private void seedActiveWaivers() {
    List<String> ownerIds = activeWaiverOwnerIds();
    List<String> comments = List.of(DATA.waiver1Comment(), DATA.waiver2Comment(), DATA.waiver3Comment());
    List<ConstraintFact> constraintFacts = policyViolation.getConstraintFacts();
    for (int i = 0; i < ownerIds.size(); i++) {
      tempEntity.newWaiver(
          DATA.componentHash(),
          policy.getId(),
          ownerIds.get(i),
          constraintFacts,
          comments.get(i));
    }
  }

  private void seedActiveWaiversWithDistinctTimes() {
    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
    Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);

    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(ZoneOffset.UTC);

    newestWaiverDateFormatted = fmt.format(now);
    oldestWaiverDateFormatted = fmt.format(fiveDaysAgo);

    List<String> ownerIds = activeWaiverOwnerIds();
    List<String> comments = List.of(DATA.waiver1Comment(), DATA.waiver2Comment(), DATA.waiver3Comment());
    List<Date> createTimes = List.of(Date.from(now), Date.from(twoDaysAgo), Date.from(fiveDaysAgo));
    List<ConstraintFact> constraintFacts = policyViolation.getConstraintFacts();

    for (int i = 0; i < ownerIds.size(); i++) {
      tempEntity.newWaiver(
          DATA.componentHash(),
          policy.getId(),
          ownerIds.get(i),
          constraintFacts,
          comments.get(i),
          createTimes.get(i));
    }
  }

  private List<String> activeWaiverOwnerIds() {
    return List.of(Organization.ROOT_ORGANIZATION_ID, organization.getId(), application.getId());
  }

  private void seedActiveAndExpiredWaivers() {
    List<ConstraintFact> constraintFacts = policyViolation.getConstraintFacts();
    tempEntity.newWaiver(
        DATA.componentHash(),
        policy.getId(),
        application.getId(),
        constraintFacts,
        DATA.waiver1Comment());

    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
    Instant fiveDaysAgo = now.minus(5, ChronoUnit.DAYS);
    Instant pastExpiry = now.minus(1, ChronoUnit.DAYS);

    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(ZoneOffset.UTC);
    newestExpiredWaiverDateFormatted = fmt.format(twoDaysAgo);
    oldestExpiredWaiverDateFormatted = fmt.format(fiveDaysAgo);

    tempEntity.newWaiver(
        DATA.componentHash(),
        policy.getId(),
        organization.getId(),
        constraintFacts,
        DATA.expiredWaiverComment(),
        Date.from(twoDaysAgo),
        Date.from(pastExpiry));

    tempEntity.newWaiver(
        DATA.componentHash(),
        policy.getId(),
        Organization.ROOT_ORGANIZATION_ID,
        constraintFacts,
        DATA.expiredWaiver2Comment(),
        Date.from(fiveDaysAgo),
        Date.from(pastExpiry));
  }

  private void seedAutoWaiverAppliedToViolation() {
    AutoPolicyWaiver autoWaiver = tempEntity.newAutoPolicyWaiver(
        organization.getId(),
        DATA.policyThreatLevel(),
        true,
        false);
    policyViolation.setAutoPolicyWaiverId(autoWaiver.getId());
    policyViolationDAO.update(policyViolation);
  }

  private record Data(
      String organizationNamePrefix,
      String applicationNamePrefix,
      String applicationPublicIdPrefix,
      String policyName,
      int policyThreatLevel,
      String scanId,
      String componentGroup,
      String componentArtifact,
      String componentVersion,
      String componentHash,
      String vulnerabilityRefId,
      String waiver1Comment,
      String waiver2Comment,
      String waiver3Comment,
      String expiredWaiverComment,
      String expiredWaiver2Comment,
      int activeWaiverCount,
      int expiredWaiverCount,
      String emptyStateText,
      String emptyStateLinkText,
      String emptyStateLinkHref,
      String deleteModalHeading,
      String autoWaiverTagText,
      String applicableWaiversUrlPattern,
      String applicableAutoWaiverUrlPattern,
      String jsonContentType,
      String errorResponseBody,
      String nullAutoWaiverResponse,
      String emptyWaiversResponse,
      int loadingMetaRowCount,
      long hdsWaitTimeoutMs)
  {
  }
}
