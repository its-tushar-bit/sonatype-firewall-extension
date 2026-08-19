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
import com.sonatype.clm.testing.playwright.pages.DashboardPage;
import com.sonatype.clm.testing.playwright.pages.RequestWaiverPage;
import com.sonatype.clm.testing.playwright.pages.RequestWaiverPageAssertions;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.clm.testing.playwright.utils.PlaywrightWaitUtils;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class RequestWaiverPlaywrightTest
    extends AbstractIqUiTest
{

  private record RequestWaiverData(
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
      String pageTitle,
      String constraintText,
      int expectedScopeOptionCount,
      int expectedComponentRadioCount,
      int expectedExpiryOptionCount,
      int expectedWaiverReasonOptionCount,
      String waiverReason,
      String comment,
      String noteToReviewer,
      String duplicateComment,
      String duplicateNoteToReviewer,
      String duplicateErrorText,
      String enterpriseAlertText,
      String enterpriseGoBackLinkText,
      String noteToReviewerLabel,
      String noteToReviewerSublabel,
      String noteToReviewerMaxLength)
  {
    String componentCoords() {
      return componentGroupId + " : " + componentArtifactId + " : " + componentVersion;
    }
  }

  private static final RequestWaiverData DATA =
      TestDataManager.load("request-waiver", RequestWaiverData.class);

  private PolicyViolation policyViolation;

  private User developerUser;

  @BeforeEach
  public void seedViolationAndNavigate() {
    Date twoDaysAgo = Date.from(Instant.now().minus(2, ChronoUnit.DAYS));

    Organization organization = tempEntity.newOrganization(DATA.orgName());
    Application application = tempEntity.newApplication(DATA.appName(), DATA.appId(), organization.getId());
    Policy policy =
        tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, DATA.policyName(), DATA.policyThreatLevel());

    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        application.getId(), StageTypes.BUILD.getId(), "scan1", false, false, twoDaysAgo);

    policyViolation = tempEntity.newPolicyViolation(evaluation, policy,
        DATA.componentGroupId(), DATA.componentArtifactId(), DATA.componentVersion(),
        DATA.componentHash(), DATA.componentCveId());

    developerUser = tempEntity.newUser();
    tempEntity.newMembershipMapping(
        Organization.ROOT_ORGANIZATION_ID,
        Role.DEVELOPER_ROLE_ID,
        developerUser.getUsername());

    playwrightLoginAt(DashboardPage.url(), developerUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);
  }

  @AfterEach
  public void logout() {
    playwrightLogout();
  }

  private String requestWaiverUrl() {
    return RequestWaiverPage.url(policyViolation.getId());
  }

  @Test
  @Tag("sanity")
  public void testPageLayout() {
    playwrightRefreshOrOpen(RequestWaiverPage.url(policyViolation.getId()));

    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    RequestWaiverPageAssertions requestWaiverAssertions = new RequestWaiverPageAssertions(requestWaiverPage);
    requestWaiverAssertions.shouldBeVisible();
    requestWaiverAssertions.shouldShowPageLayout(
        DATA.pageTitle(),
        DATA.componentCoords(),
        DATA.policyName(),
        DATA.constraintText(),
        DATA.componentCveId(),
        DATA.expectedScopeOptionCount(),
        DATA.expectedComponentRadioCount(),
        DATA.expectedExpiryOptionCount(),
        DATA.expectedWaiverReasonOptionCount());
  }

  @Test
  @Tag("sanity")
  public void testSubmitButton() {
    playwrightRefreshOrOpen(requestWaiverUrl());

    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    RequestWaiverPageAssertions requestWaiverAssertions = new RequestWaiverPageAssertions(requestWaiverPage);
    requestWaiverPage.selectWaiverReason(DATA.waiverReason());
    requestWaiverPage.fillComment(DATA.comment());
    requestWaiverPage.fillNoteToReviewer(DATA.noteToReviewer());
    requestWaiverPage.submit();
    waitForSubmitMask();
    requestWaiverAssertions.shouldHaveNoSubmitError();
    PlaywrightWaitUtils.waitForHidden(requestWaiverPage.container());
  }

  @Test
  @Tag("sanity")
  public void testSubmitError() {
    playwrightRefreshOrOpen(RequestWaiverPage.url(policyViolation.getId()));

    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    RequestWaiverPageAssertions requestWaiverAssertions = new RequestWaiverPageAssertions(requestWaiverPage);
    requestWaiverPage.fillComment(DATA.comment());
    requestWaiverPage.fillNoteToReviewer(DATA.noteToReviewer());
    requestWaiverPage.submit();
    waitForSubmitMask();
    requestWaiverAssertions.shouldHaveNoSubmitError();

    playwrightRefreshOrOpen(requestWaiverUrl());
    requestWaiverPage.fillComment(DATA.duplicateComment());
    requestWaiverPage.fillNoteToReviewer(DATA.duplicateNoteToReviewer());
    requestWaiverPage.submit();
    waitForSubmitMask();
    requestWaiverAssertions.shouldShowSubmitError(DATA.duplicateErrorText());
  }

  @Test
  @Tag("regression")
  public void testEnterprisePreviewBlocksSave() {
    setMissingFeature(LicensedFeature.WAIVER_REQUEST_WORKFLOW);

    playwrightLogout();
    playwrightLoginAt(requestWaiverUrl(), developerUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    RequestWaiverPageAssertions requestWaiverAssertions = new RequestWaiverPageAssertions(requestWaiverPage);
    requestWaiverAssertions.shouldBeVisible();
    requestWaiverAssertions.shouldShowEnterprisePreviewMode(
        DATA.enterpriseAlertText(), DATA.enterpriseGoBackLinkText());
  }

  @Test
  @Tag("regression")
  public void testNoteToReviewerField() {
    playwrightRefreshOrOpen(requestWaiverUrl());

    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();
    RequestWaiverPageAssertions requestWaiverAssertions = new RequestWaiverPageAssertions(requestWaiverPage);
    requestWaiverAssertions.shouldBeVisible();
    requestWaiverAssertions.shouldShowNoteToReviewerField(
        DATA.noteToReviewerLabel(), DATA.noteToReviewerSublabel(), DATA.noteToReviewerMaxLength());
  }
}
