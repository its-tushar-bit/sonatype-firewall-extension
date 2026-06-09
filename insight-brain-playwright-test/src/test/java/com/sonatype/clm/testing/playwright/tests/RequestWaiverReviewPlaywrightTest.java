/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.RequestWaiverReviewPage;
import com.sonatype.clm.testing.playwright.pages.RequestWaiverReviewPageAssertions;
import com.sonatype.clm.testing.playwright.testdatamanager.TestDataManager;
import com.sonatype.clm.testing.playwright.utils.PlaywrightWaitUtils;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static org.assertj.core.api.Assertions.assertThat;

public class RequestWaiverReviewPlaywrightTest
    extends AbstractIqUiTest
{
  private record RequestWaiverReviewData(
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
      String constraintName,
      String waiverReasonId,
      String initialComment,
      String noteToReviewer,
      String pageTitle,
      String requesterName,
      String rejectionModalTitle,
      String rejectionReason,
      String rejectionAlertTextPrefix,
      String defaultRejectionReason,
      String expiryNeverOptionValue,
      String reviewerId,
      String reviewerName,
      String scopeOptionTextPrefix,
      String rejectionTextareaPlaceholder,
      String rejectionTextareaMaxLength,
      String rejectionSendButtonLabel,
      String allVersionsDisabledTooltip)
  {
    String componentCoords() {
      return componentGroupId + " : " + componentArtifactId + " : " + componentVersion;
    }
  }

  private static final RequestWaiverReviewData DATA =
      TestDataManager.load("request-waiver-review", RequestWaiverReviewData.class);

  private RequestWaiverReviewPage reviewPage;

  private RequestWaiverReviewPageAssertions assertions;

  private PolicyWaiverRequestDAO policyWaiverRequestDAO;

  private PolicyWaiverDAO policyWaiverDAO;

  private Application application;

  private User developerUser;

  private PolicyWaiverRequest primaryRequest;

  private String componentHash;

  @Before
  public void seedWaiverRequestAndLoginAsAdmin() {
    reviewPage = new RequestWaiverReviewPage();
    assertions = new RequestWaiverReviewPageAssertions(reviewPage);

    policyWaiverRequestDAO = lookup(PolicyWaiverRequestDAO.class);
    policyWaiverDAO = lookup(PolicyWaiverDAO.class);

    seed();

    playwrightLoginAdminAt(reviewPageUrl());
  }

  @After
  public void logout() {
    playwrightLogout();
  }

  private String reviewPageUrl() {
    return RequestWaiverReviewPage.url("application", application.getId(), primaryRequest.getId());
  }

  @Test
  @Category(RegressionTest.class)
  public void testPageLayout() {
    playwrightRefreshOrOpen(reviewPageUrl());

    assertions.shouldBeVisible();
    assertions.shouldShowPageTitle(DATA.pageTitle());
    assertions.shouldShowRequesterInfo(requesterFullName(), DATA.noteToReviewer());
    assertions.shouldShowWaiverConfiguration(
        DATA.componentCoords(), DATA.policyName(), DATA.constraintName(), DATA.componentCveId());
    assertions.shouldShowApproveAndRejectButtons();
  }

  @Test
  @Category(RegressionTest.class)
  public void testApproveWaiverRequest() {
    playwrightRefreshOrOpen(reviewPageUrl());

    assertions.shouldBeVisible();
    reviewPage.clickApprove();
    waitForSubmitMask();

    PlaywrightWaitUtils.waitForCondition(
        () -> {
          PolicyWaiverRequest updated = policyWaiverRequestDAO.getById(primaryRequest.getId());
          return PolicyWaiverRequestStatus.APPROVED == updated.getStatus();
        },
        15000, 200,
        "Timed out waiting for waiver request to be approved");

    PolicyWaiverRequest updated = policyWaiverRequestDAO.getById(primaryRequest.getId());
    assertThat(updated.getStatus()).isEqualTo(PolicyWaiverRequestStatus.APPROVED);
    assertThat(updated.getReviewerId()).isNotBlank();
    assertThat(updated.getReviewerName()).isNotBlank();

    List<PolicyWaiver> waivers =
        policyWaiverDAO.getApplicableToComponent(application.getId(), componentHash);
    assertThat(waivers).isNotEmpty();
  }

  @Test
  @Category(RegressionTest.class)
  public void testRejectWaiverRequest() {
    playwrightRefreshOrOpen(reviewPageUrl());

    assertions.shouldBeVisible();
    reviewPage.clickReject();
    assertions.shouldShowRejectionModal(DATA.rejectionModalTitle());

    reviewPage.fillRejectionReason(DATA.rejectionReason());
    reviewPage.clickSendRejection();
    waitForSubmitMask();

    PlaywrightWaitUtils.waitForCondition(
        () -> {
          PolicyWaiverRequest updated = policyWaiverRequestDAO.getById(primaryRequest.getId());
          return PolicyWaiverRequestStatus.REJECTED == updated.getStatus();
        },
        15000, 200,
        "Timed out waiting for waiver request to be rejected");

    PolicyWaiverRequest updated = policyWaiverRequestDAO.getById(primaryRequest.getId());
    assertThat(updated.getStatus()).isEqualTo(PolicyWaiverRequestStatus.REJECTED);
    assertThat(updated.getRejectionReason()).isEqualTo(DATA.rejectionReason());
    assertThat(updated.getReviewerId()).isNotBlank();
    assertThat(updated.getReviewerName()).isNotBlank();
  }

  @Test
  @Category(RegressionTest.class)
  public void testReadOnlyWithoutPermission() {
    playwrightLogout();
    playwrightLoginAt(reviewPageUrl(), developerUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    assertions.shouldBeVisible();
    assertions.shouldShowReadOnlyState();
  }

  @Test
  @Category(RegressionTest.class)
  public void testScopeOptionTextFormat() {
    playwrightRefreshOrOpen(reviewPageUrl());

    assertions.shouldShowScopeOptionText(DATA.scopeOptionTextPrefix());
  }

  @Test
  @Category(RegressionTest.class)
  public void testExpiryNeverDispatchesNull() {
    playwrightRefreshOrOpen(reviewPageUrl());

    assertions.shouldBeVisible();
    reviewPage.selectExpiryTime(DATA.expiryNeverOptionValue());
    reviewPage.clickApprove();
    waitForSubmitMask();

    PlaywrightWaitUtils.waitForCondition(
        () -> {
          PolicyWaiverRequest updated = policyWaiverRequestDAO.getById(primaryRequest.getId());
          return PolicyWaiverRequestStatus.APPROVED == updated.getStatus();
        },
        15000, 200,
        "Timed out waiting for waiver request to be approved with never expiry");

    List<PolicyWaiver> waivers =
        policyWaiverDAO.getApplicableToComponent(application.getId(), componentHash);
    assertThat(waivers).isNotEmpty();
    assertThat(waivers.get(0).getExpiryTime()).isNull();
  }

  @Test
  @Category(RegressionTest.class)
  public void testRejectedStatusDisplay() {
    markRequestRejectedWithoutReason();

    playwrightRefreshOrOpen(reviewPageUrl());

    assertions.shouldBeVisible();
    assertions.shouldShowRejectedStatusAlert(DATA.rejectionAlertTextPrefix());
    assertions.shouldShowRejectedStatusAlert(DATA.defaultRejectionReason());
  }

  @Test
  @Category(RegressionTest.class)
  public void testRejectModalFieldsAndSuccessfulRejection() {
    playwrightRefreshOrOpen(reviewPageUrl());

    assertions.shouldBeVisible();
    reviewPage.clickReject();
    assertions.shouldShowRejectionModal(DATA.rejectionModalTitle());
    assertions.shouldShowRejectionTextareaPlaceholder(DATA.rejectionTextareaPlaceholder());
    assertions.shouldShowRejectionTextareaMaxLength(DATA.rejectionTextareaMaxLength());
    assertions.shouldShowSendButtonLabel(DATA.rejectionSendButtonLabel());

    reviewPage.clickRejectionModalCancel();
    assertions.shouldShowRejectionModalDismissed();

    reviewPage.clickReject();
    assertions.shouldShowRejectionModal(DATA.rejectionModalTitle());
    reviewPage.fillRejectionReason(DATA.rejectionReason());
    reviewPage.clickSendRejection();
    waitForSubmitMask();

    PlaywrightWaitUtils.waitForCondition(
        () -> {
          PolicyWaiverRequest updated = policyWaiverRequestDAO.getById(primaryRequest.getId());
          return PolicyWaiverRequestStatus.REJECTED == updated.getStatus();
        },
        15000, 200,
        "Timed out waiting for waiver request to be rejected after valid reason");

    PolicyWaiverRequest updated = policyWaiverRequestDAO.getById(primaryRequest.getId());
    assertThat(updated.getStatus()).isEqualTo(PolicyWaiverRequestStatus.REJECTED);
    assertThat(updated.getRejectionReason()).isEqualTo(DATA.rejectionReason());
  }

  @Test
  @Category(RegressionTest.class)
  public void testReadOnlyFormFieldsDisabled() {
    playwrightLogout();
    playwrightLoginAt(reviewPageUrl(), developerUser.getUsername(), TemporaryEntity.USER_PASSWORD_CLEAR);

    assertions.shouldBeVisible();
    assertions.shouldShowFormFieldsDisabled();
    assertions.shouldDisableApproveAndRejectButtons();
  }

  private void seed() {
    Instant now = Instant.now();
    Date twoDaysAgo = Date.from(now.minus(2, ChronoUnit.DAYS));
    Date threeDaysFromNow = Date.from(now.plus(3, ChronoUnit.DAYS));

    developerUser = tempEntity.newUser();
    tempEntity.newMembershipMapping(
        Organization.ROOT_ORGANIZATION_ID,
        Role.DEVELOPER_ROLE_ID,
        developerUser.getUsername());

    String suffix = TemporaryEntity.uuid();
    Organization organization = tempEntity.newOrganization(DATA.orgName() + "-" + suffix);
    application =
        tempEntity.newApplication(DATA.appName() + "-" + suffix, DATA.appId() + "-" + suffix, organization.getId());
    Policy policy =
        tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, DATA.policyName() + "-" + suffix,
            DATA.policyThreatLevel());

    String scanId = DATA.scanId() + "-" + suffix;
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(
        application.getId(), StageTypes.BUILD.getId(), scanId, false, false, twoDaysAgo);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates(
            DATA.componentGroupId(), DATA.componentArtifactId(), DATA.componentVersion(), "", "jar");
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();

    componentHash = TemporaryEntity.uuid().replace("-", "").substring(0, 20);
    PolicyViolation violation = tempEntity.newPolicyViolation(
        evaluation, policy, componentIdentifier, componentHash, DATA.componentCveId());

    String requesterFullName = developerUser.getFirstName() + " " + developerUser.getLastName();

    primaryRequest = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash(componentHash)
        .setPolicyId(policy.getId())
        .setPolicyViolationId(violation.getId())
        .setOwnerId(application.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setWaiverReasonId(DATA.waiverReasonId())
        .setComment(DATA.initialComment())
        .setNoteToReviewer(DATA.noteToReviewer())
        .setRequestTime(twoDaysAgo)
        .setExpiryTime(threeDaysFromNow)
        .setRequesterId(developerUser.getUsername())
        .setRequesterName(requesterFullName)
        .setComponentUpgradeAvailable(false));
  }

  private void markRequestRejectedWithoutReason() {
    primaryRequest.setStatus(PolicyWaiverRequestStatus.REJECTED);
    primaryRequest.setReviewerId(DATA.reviewerId());
    primaryRequest.setReviewerName(DATA.reviewerName());
    policyWaiverRequestDAO.update(primaryRequest);
  }

  private String requesterFullName() {
    return developerUser.getFirstName() + " " + developerUser.getLastName();
  }
}
