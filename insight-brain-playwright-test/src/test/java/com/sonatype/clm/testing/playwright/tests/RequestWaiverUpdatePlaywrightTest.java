/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.RequestWaiverUpdatePage;
import com.sonatype.clm.testing.playwright.pages.RequestWaiverUpdatePageAssertions;

import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.utils.PlaywrightWaitUtils;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import org.junit.experimental.categories.Category;

public class RequestWaiverUpdatePlaywrightTest
    extends AbstractIqUiTest
{

  private record RequestWaiverUpdateData(
      String orgName,
      String appName,
      String appId,
      String policyName,
      int policyThreatLevel,
      String componentGroupId,
      String componentArtifactId,
      String componentVersion,
      String componentHash,
      String componentCveId,
      String scanId,
      String waiverReasonIdAcknowledgedViolation,
      String initialComment,
      String initialNoteToReviewer,
      String pageTitle,
      String updatedComment,
      String updatedNoteToReviewer,
      String rejectionReason,
      String rejectionAlertText,
      String reviewerId,
      String reviewerName)
  {
    String componentCoords() {
      return componentGroupId + " : " + componentArtifactId + " : " + componentVersion;
    }
  }

  private static final RequestWaiverUpdateData DATA =
      TestDataManager.load("request-waiver-update", RequestWaiverUpdateData.class);

  private PolicyWaiverRequestDAO policyWaiverRequestDAO;

  private PolicyWaiverRequest policyWaiverRequest;

  @Before
  public void seedWaiverRequestAndLoginAsDeveloper() {
    Instant now = Instant.now();
    Date twoDaysAgo = Date.from(now.minus(2, ChronoUnit.DAYS));
    Date threeDaysFromNow = Date.from(now.plus(3, ChronoUnit.DAYS));

    policyWaiverRequestDAO = lookup(PolicyWaiverRequestDAO.class);

    User developerUser = tempEntity.newUser();
    tempEntity.newMembershipMapping(
        Organization.ROOT_ORGANIZATION_ID,
        Role.DEVELOPER_ROLE_ID,
        developerUser.getUsername());

    Organization organization = tempEntity.newOrganization(DATA.orgName());
    Application application = tempEntity.newApplication(DATA.appName(), DATA.appId(), organization.getId());
    Policy policy =
        tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, DATA.policyName(), DATA.policyThreatLevel());

    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        application.getId(), StageTypes.BUILD.getId(), "scan1", false, false, twoDaysAgo);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates(
            DATA.componentGroupId(), DATA.componentArtifactId(), DATA.componentVersion(), "", "jar");
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();

    PolicyViolation violation = tempEntity.newPolicyViolation(
        evaluation, policy, componentIdentifier, DATA.componentHash(), DATA.componentCveId());

    policyWaiverRequest = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash(DATA.componentHash())
        .setPolicyId(policy.getId())
        .setPolicyViolationId(violation.getId())
        .setOwnerId(application.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setWaiverReasonId(DATA.waiverReasonIdAcknowledgedViolation())
        .setComment(DATA.initialComment())
        .setNoteToReviewer(DATA.initialNoteToReviewer())
        .setRequestTime(twoDaysAgo)
        .setExpiryTime(threeDaysFromNow)
        .setRequesterId(developerUser.getUsername())
        .setRequesterName(developerUser.getFirstName() + " " + developerUser.getLastName())
        .setComponentUpgradeAvailable(false));

    playwrightLoginAt(DashboardPage.url(), developerUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);
  }

  @After
  public void logoutDeveloper() {
    playwrightLogout();
  }

  @Test
  @Category(SanityTest.class)
  public void testRouteToUpdateFromDashboard() {
    RequestWaiverUpdatePage updatePage = new RequestWaiverUpdatePage();
    RequestWaiverUpdatePageAssertions updateAssertions = new RequestWaiverUpdatePageAssertions(updatePage);
    playwrightRefreshOrOpen(RequestWaiverUpdatePage.url());
    updatePage.waitUntilSpinnersGone();

    PlaywrightWaitUtils.clickAndWaitForVisible(
        updatePage.firstWaiverRequestTile(), updatePage.container(), PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS,
        PlaywrightTiming.POLL_INTERVAL_MS);

    updateAssertions.shouldBeVisible();
    updateAssertions.shouldShowUpdateLayout(
        DATA.pageTitle(), DATA.componentCoords(), DATA.policyName());
  }

  @Test
  @Category(SanityTest.class)
  public void testSubmitUpdatedValues() {
    RequestWaiverUpdatePage updatePage = new RequestWaiverUpdatePage();
    RequestWaiverUpdatePageAssertions updateAssertions = new RequestWaiverUpdatePageAssertions(updatePage);
    playwrightRefreshOrOpen(RequestWaiverUpdatePage.url());
    updatePage.waitUntilSpinnersGone();

    PlaywrightWaitUtils.clickAndWaitForVisible(
        updatePage.firstWaiverRequestTile(), updatePage.container(), PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS,
        PlaywrightTiming.POLL_INTERVAL_MS);

    updateAssertions.shouldBeVisible();
    updatePage.fillComment(DATA.updatedComment());
    updatePage.fillNoteToReviewer(DATA.updatedNoteToReviewer());
    updatePage.submit();
    waitForSubmitMask();

    playwrightRefreshOrOpen(RequestWaiverUpdatePage.url());
    updatePage.waitUntilSpinnersGone();
    PlaywrightWaitUtils.clickAndWaitForVisible(
        updatePage.firstWaiverRequestTile(), updatePage.container(), PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS,
        PlaywrightTiming.POLL_INTERVAL_MS);
    updateAssertions.shouldShowSavedCommentAndNote(DATA.updatedComment(), DATA.updatedNoteToReviewer());
  }

  @Test
  @Category(SanityTest.class)
  public void testErrorAlertWhenWaiverRequestIsRejected() {
    policyWaiverRequest.setStatus(PolicyWaiverRequestStatus.REJECTED);
    policyWaiverRequest.setRejectionReason(DATA.rejectionReason());
    policyWaiverRequest.setReviewerId(DATA.reviewerId());
    policyWaiverRequest.setReviewerName(DATA.reviewerName());
    policyWaiverRequestDAO.update(policyWaiverRequest);

    RequestWaiverUpdatePage updatePage = new RequestWaiverUpdatePage();
    RequestWaiverUpdatePageAssertions updateAssertions = new RequestWaiverUpdatePageAssertions(updatePage);
    playwrightRefreshOrOpen(RequestWaiverUpdatePage.url());
    updatePage.waitUntilSpinnersGone();

    PlaywrightWaitUtils.clickAndWaitForVisible(
        updatePage.firstWaiverRequestTile(), updatePage.container(), PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS,
        PlaywrightTiming.POLL_INTERVAL_MS);

    updateAssertions.shouldBeVisible();
    updateAssertions.shouldShowRejectionAlert(DATA.rejectionAlertText(), DATA.rejectionReason());
  }

  @Test
  @Category(RegressionTest.class)
  public void testApprovedStatusMakesFormReadOnly() {
    policyWaiverRequest.setStatus(PolicyWaiverRequestStatus.APPROVED);
    policyWaiverRequest.setReviewerId(DATA.reviewerId());
    policyWaiverRequest.setReviewerName(DATA.reviewerName());
    policyWaiverRequestDAO.update(policyWaiverRequest);

    RequestWaiverUpdatePage updatePage = new RequestWaiverUpdatePage();
    RequestWaiverUpdatePageAssertions updateAssertions = new RequestWaiverUpdatePageAssertions(updatePage);
    playwrightRefreshOrOpen(RequestWaiverUpdatePage.url());
    updatePage.waitUntilSpinnersGone();

    PlaywrightWaitUtils.clickAndWaitForVisible(
        updatePage.firstWaiverRequestTile(), updatePage.container(),
        PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS, PlaywrightTiming.POLL_INTERVAL_MS);

    updateAssertions.shouldBeVisible();
    updateAssertions.shouldShowApprovedReadOnlyState();
  }
}
