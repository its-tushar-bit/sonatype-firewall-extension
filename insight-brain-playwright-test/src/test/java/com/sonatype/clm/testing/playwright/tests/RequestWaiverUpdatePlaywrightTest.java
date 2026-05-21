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
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.RequestWaiverUpdatePage;
import com.sonatype.clm.testing.playwright.pages.RequestWaiverUpdatePageAssertions;
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
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

/**
 * Playwright test for the Request Waiver Update page.
 */
public class RequestWaiverUpdatePlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME = "Org 1";

  private static final String APP_NAME = "App 1";

  private static final String APP_ID = "app1";

  private static final String POLICY_NAME = "Policy 1";

  private static final int POLICY_THREAT_LEVEL = 7;

  private static final String COMPONENT_GROUP_ID = "Group1";

  private static final String COMPONENT_ARTIFACT_ID = "Artifact1";

  private static final String COMPONENT_VERSION = "Version1";

  private static final String COMPONENT_HASH = "hash1";

  private static final String COMPONENT_CVE_ID = "sonatype-2017-0507";

  private static final String COMPONENT_COORDS =
      COMPONENT_GROUP_ID + " : " + COMPONENT_ARTIFACT_ID + " : " + COMPONENT_VERSION;

  private static final String WAIVER_REASON_ID_ACKNOWLEDGED_VIOLATION = "9b704ef5bc064fc29d7fe08a251ee9a6";

  private static final String INITIAL_COMMENT = "Comment 1";

  private static final String INITIAL_NOTE_TO_REVIEWER = "Note to Reviewer 1";

  private static final String PAGE_TITLE = "Request Waiver";

  private static final String UPDATED_COMMENT = "Updated comments for testing";

  private static final String UPDATED_NOTE_TO_REVIEWER = "Updated note to reviewer";

  private static final String REJECTION_REASON = "This component violates security policies";

  private static final String REJECTION_ALERT_TEXT = "This Waiver Request was rejected";

  private static final String REVIEWER_ID = "admin";

  private static final String REVIEWER_NAME = "Admin Reviewer";

  private PolicyWaiverRequest policyWaiverRequest;

  private PolicyWaiverRequestDAO policyWaiverRequestDAO;

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

    Organization organization = tempEntity.newOrganization(ORG_NAME);
    Application application = tempEntity.newApplication(APP_NAME, APP_ID, organization.getId());
    Policy policy =
        tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, POLICY_NAME, POLICY_THREAT_LEVEL);

    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        application.getId(), StageTypes.BUILD.getId(), "scan1", false, false, twoDaysAgo);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates(
            COMPONENT_GROUP_ID, COMPONENT_ARTIFACT_ID, COMPONENT_VERSION, "", "jar");
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();

    PolicyViolation violation = tempEntity.newPolicyViolation(
        evaluation, policy, componentIdentifier, COMPONENT_HASH, COMPONENT_CVE_ID);

    policyWaiverRequest = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash(COMPONENT_HASH)
        .setPolicyId(policy.getId())
        .setPolicyViolationId(violation.getId())
        .setOwnerId(application.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setWaiverReasonId(WAIVER_REASON_ID_ACKNOWLEDGED_VIOLATION)
        .setComment(INITIAL_COMMENT)
        .setNoteToReviewer(INITIAL_NOTE_TO_REVIEWER)
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
    updateAssertions.shouldShowUpdateLayout(PAGE_TITLE, COMPONENT_COORDS, POLICY_NAME);
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
    updatePage.fillComment(UPDATED_COMMENT);
    updatePage.fillNoteToReviewer(UPDATED_NOTE_TO_REVIEWER);
    updatePage.submit();
    waitForSubmitMask();

    playwrightRefreshOrOpen(RequestWaiverUpdatePage.url());
    updatePage.waitUntilSpinnersGone();
    PlaywrightWaitUtils.clickAndWaitForVisible(
        updatePage.firstWaiverRequestTile(), updatePage.container(), PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS,
        PlaywrightTiming.POLL_INTERVAL_MS);
    updateAssertions.shouldShowSavedCommentAndNote(UPDATED_COMMENT, UPDATED_NOTE_TO_REVIEWER);
  }

  @Test
  @Category(SanityTest.class)
  public void testErrorAlertWhenWaiverRequestIsRejected() {
    policyWaiverRequest.setStatus(PolicyWaiverRequestStatus.REJECTED);
    policyWaiverRequest.setRejectionReason(REJECTION_REASON);
    policyWaiverRequest.setReviewerId(REVIEWER_ID);
    policyWaiverRequest.setReviewerName(REVIEWER_NAME);
    policyWaiverRequestDAO.update(policyWaiverRequest);

    RequestWaiverUpdatePage updatePage = new RequestWaiverUpdatePage();
    RequestWaiverUpdatePageAssertions updateAssertions = new RequestWaiverUpdatePageAssertions(updatePage);
    playwrightRefreshOrOpen(RequestWaiverUpdatePage.url());
    updatePage.waitUntilSpinnersGone();

    PlaywrightWaitUtils.clickAndWaitForVisible(
        updatePage.firstWaiverRequestTile(), updatePage.container(), PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS,
        PlaywrightTiming.POLL_INTERVAL_MS);

    updateAssertions.shouldBeVisible();
    updateAssertions.shouldShowRejectionAlert(REJECTION_ALERT_TEXT, REJECTION_REASON);
  }
}
