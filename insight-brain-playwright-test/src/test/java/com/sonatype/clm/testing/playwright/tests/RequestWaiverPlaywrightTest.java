/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.RequestWaiverPage;
import com.sonatype.clm.testing.playwright.pages.RequestWaiverPageAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightWaitUtils;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Playwright test for the Request Waiver page.
 */
public class RequestWaiverPlaywrightTest
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

  private static final String PAGE_TITLE = "Request Waiver";

  private static final String CONSTRAINT_TEXT = "Test Constraint";

  private static final int EXPECTED_SCOPE_OPTIONS = 3;

  private static final int EXPECTED_COMPONENT_RADIOS = 3;

  private static final int EXPECTED_EXPIRY_OPTIONS = 8;

  private static final int EXPECTED_WAIVER_REASON_OPTIONS = 9;

  private static final String WAIVER_REASON = "Acknowledged violation";

  private static final String COMMENT = "Some comments";

  private static final String NOTE_TO_REVIEWER = "Some note to reviewer";

  private static final String DUPLICATE_COMMENT = "Other comments";

  private static final String DUPLICATE_NOTE_TO_REVIEWER = "Other note to reviewer";

  private static final String DUPLICATE_ERROR_TEXT = "This policy waiver request already exists";

  private PolicyViolation policyViolation;

  private User developerUser;

  @Before
  public void seedViolationAndNavigate() {
    Date twoDaysAgo = Date.from(Instant.now().minus(2, ChronoUnit.DAYS));

    Organization organization = tempEntity.newOrganization(ORG_NAME);
    Application application = tempEntity.newApplication(APP_NAME, APP_ID, organization.getId());
    Policy policy =
        tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, POLICY_NAME, POLICY_THREAT_LEVEL);

    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        application.getId(), StageTypes.BUILD.getId(), "scan1", false, false, twoDaysAgo);

    policyViolation = tempEntity.newPolicyViolation(evaluation, policy,
        COMPONENT_GROUP_ID, COMPONENT_ARTIFACT_ID, COMPONENT_VERSION,
        COMPONENT_HASH, COMPONENT_CVE_ID);

    developerUser = tempEntity.newUser();
    tempEntity.newMembershipMapping(
        Organization.ROOT_ORGANIZATION_ID,
        Role.DEVELOPER_ROLE_ID,
        developerUser.getUsername());

    playwrightLoginAt(DashboardPage.url(), developerUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);
  }

  @After
  public void logout() {
    playwrightLogout();
  }

  @Test
  @Category(SanityTest.class)
  public void testPageLayout() {
    playwrightRefreshOrOpen(RequestWaiverPage.url(policyViolation.getId()));

    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    RequestWaiverPageAssertions requestWaiverAssertions = new RequestWaiverPageAssertions(requestWaiverPage);
    requestWaiverAssertions.shouldBeVisible();
    requestWaiverAssertions.shouldShowPageLayout(
        PAGE_TITLE,
        COMPONENT_COORDS,
        POLICY_NAME,
        CONSTRAINT_TEXT,
        COMPONENT_CVE_ID,
        EXPECTED_SCOPE_OPTIONS,
        EXPECTED_COMPONENT_RADIOS,
        EXPECTED_EXPIRY_OPTIONS,
        EXPECTED_WAIVER_REASON_OPTIONS);
  }

  @Test
  @Category(SanityTest.class)
  public void testSubmitButton() {
    playwrightRefreshOrOpen(RequestWaiverPage.url(policyViolation.getId()));

    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    RequestWaiverPageAssertions requestWaiverAssertions = new RequestWaiverPageAssertions(requestWaiverPage);
    requestWaiverPage.selectWaiverReason(WAIVER_REASON);
    requestWaiverPage.fillComment(COMMENT);
    requestWaiverPage.fillNoteToReviewer(NOTE_TO_REVIEWER);
    requestWaiverPage.submit();
    waitForSubmitMask();
    requestWaiverAssertions.shouldHaveNoSubmitError();
    PlaywrightWaitUtils.waitForHidden(requestWaiverPage.container());
  }

  @Test
  @Category(SanityTest.class)
  public void testSubmitError() {
    playwrightRefreshOrOpen(RequestWaiverPage.url(policyViolation.getId()));

    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    RequestWaiverPageAssertions requestWaiverAssertions = new RequestWaiverPageAssertions(requestWaiverPage);
    requestWaiverPage.fillComment(COMMENT);
    requestWaiverPage.fillNoteToReviewer(NOTE_TO_REVIEWER);
    requestWaiverPage.submit();
    waitForSubmitMask();
    requestWaiverAssertions.shouldHaveNoSubmitError();

    playwrightRefreshOrOpen(RequestWaiverPage.url(policyViolation.getId()));
    requestWaiverPage.fillComment(DUPLICATE_COMMENT);
    requestWaiverPage.fillNoteToReviewer(DUPLICATE_NOTE_TO_REVIEWER);
    requestWaiverPage.submit();
    waitForSubmitMask();
    requestWaiverAssertions.shouldShowSubmitError(DUPLICATE_ERROR_TEXT);
  }
}
