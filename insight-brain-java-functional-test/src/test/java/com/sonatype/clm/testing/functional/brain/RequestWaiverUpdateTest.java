/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardWaiverRequests.WaiverRequestTile;
import com.sonatype.clm.testing.functional.elements.DashboardWaiverRequests.WaiverRequestsResults;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.RequestWaiverPage;
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

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.WebElementCondition;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.DashboardViolations.SEVERE;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.ALL_COMPONENTS;
import static com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver.EXACT_COMPONENT;
import static org.assertj.core.api.Assertions.assertThat;

public class RequestWaiverUpdateTest
    extends AbstractFunctionalTest
{
  private static final String SCAN_ID = "scan1";

  private Organization organization;

  private Application application;

  private PolicyViolation policyViolation;

  private PolicyWaiverRequest policyWaiverRequest;

  private PolicyWaiverRequestDAO policyWaiverRequestDAO;

  private final Date now = new Date();

  private static final WaiverRequestsResults dashboardWaiverRequestTable = DashboardPage.waiverRequestsView().results();

  private Policy securityPolicy;

  private Date threeDaysFromNow;

  @Before
  public void init() {
    Date twoDaysAgo = DateUtils.addDays(now, -2);
    threeDaysFromNow = DateUtils.addDays(now, 3);

    policyWaiverRequestDAO = lookup(PolicyWaiverRequestDAO.class);

    organization = tempEntity.newOrganization("Org 1");
    application = tempEntity.newApplication("App 1", "app1", organization.getId());
    securityPolicy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.BUILD.getId(), SCAN_ID, false, false, twoDaysAgo);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("Group1", "Artifact1", "Version1", "", "jar");
    String purl = PackageUrlIdentifier.fromComponentIdentifier(componentIdentifier).getPackageUrl();
    policyViolation = tempEntity.newPolicyViolation(policyEvaluation, securityPolicy, componentIdentifier, "hash1",
        "sonatype-2017-0507");

    String waiverReasonIdOfAcknowledgedViolation = "9b704ef5bc064fc29d7fe08a251ee9a6";

    policyWaiverRequest = tempEntity.newPolicyWaiverRequest(new PolicyWaiverRequest()
        .setHash("hash1")
        .setPolicyId(securityPolicy.getId())
        .setPolicyViolationId(policyViolation.getId())
        .setOwnerId(application.getId())
        .setAssociatedPackageUrl(purl)
        .setComponentMatchStrategy(EXACT_COMPONENT)
        .setWaiverReasonId(waiverReasonIdOfAcknowledgedViolation)
        .setComment("Comment 1")
        .setNoteToReviewer("Note to Reviewer 1")
        .setRequestTime(twoDaysAgo)
        .setExpiryTime(threeDaysFromNow)
        .setRequesterId("testuser1")
        .setRequesterName("Test User 1")
        .setComponentUpgradeAvailable(false));
    refreshOrOpen(DashboardPage.url());
    loginAsLimitedUser();
  }

  @After
  public void cleanUp() {
    logout();
  }

  @Test
  public void testRouteToUpdateFromDashboard() {
    refreshOrOpen(DashboardPage.urlToWaiverRequests());
    DashboardPage.waitUntilSpinnersGone();

    DashboardPage.dashboardContainer().shouldBe(visible);
    dashboardWaiverRequestTable.firstWaiverRequest().click();

    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();

    verifyWaiverRequestValues(
        requestWaiverPage,
        "Application - App 1",
        "Acknowledged violation",
        "Custom",
        threeDaysFromNow,
        "Comment 1",
        "Note to Reviewer 1");
  }

  @Test
  public void testSubmitUpdatedValues() {
    refreshOrOpen(DashboardPage.urlToWaiverRequests());
    DashboardPage.waitUntilSpinnersGone();

    DashboardPage.dashboardContainer().shouldBe(visible);
    dashboardWaiverRequestTable.firstWaiverRequest().click();

    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();

    verifyWaiverRequestValues(
        requestWaiverPage,
        "Application - App 1",
        "Acknowledged violation",
        "Custom",
        threeDaysFromNow,
        "Comment 1",
        "Note to Reviewer 1");

    String updatedScope = "Organization - Org 1";
    String updatedComponentMatcher = "All Components";
    String updatedExpiration = "Never";
    String updatedReason = "Other";
    String updatedComments = "Updated comments for testing";
    String updatedNoteToReviewer = "Updated note to reviewer";

    updateWaiverRequestFormFields(
        requestWaiverPage,
        updatedScope,
        updatedComponentMatcher,
        updatedExpiration,
        updatedReason,
        updatedComments,
        updatedNoteToReviewer);

    requestWaiverPage.saveButton().click();

    DashboardPage.waitUntilSpinnersGone();

    DashboardPage.dashboardContainer().shouldBe(visible);

    dashboardWaiverRequestTable.firstWaiverRequest().click();

    requestWaiverPage = new RequestWaiverPage();
    requestWaiverPage.root().shouldBe(visible);

    verifyWaiverRequestValues(
        requestWaiverPage,
        updatedScope,
        updatedReason,
        updatedExpiration,
        null,
        updatedComments,
        updatedNoteToReviewer);

    PolicyWaiverRequest updatedRequest = policyWaiverRequestDAO.getById(policyWaiverRequest.getId());

    assertThat(updatedRequest.getComment()).isEqualTo(updatedComments);
    assertThat(updatedRequest.getNoteToReviewer()).isEqualTo(updatedNoteToReviewer);
    assertThat(updatedRequest.getOwnerId()).isEqualTo(organization.getId());
    assertThat(updatedRequest.getExpiryTime()).isNull();
    String waiverReasonIdOfOther = "c991ef95866d4903ad0c6c217ac47c07";
    assertThat(updatedRequest.getWaiverReasonId()).isEqualTo(waiverReasonIdOfOther);
    assertThat(updatedRequest.getComponentMatchStrategy()).isEqualTo(ALL_COMPONENTS);
  }

  @Test
  public void testErrorAlertWhenWaiverRequestIsRejected() {
    String rejectionReason = "This component violates security policies";
    String reviewerName = "Admin Reviewer";

    policyWaiverRequest.setStatus(PolicyWaiverRequestStatus.REJECTED);
    policyWaiverRequest.setRejectionReason(rejectionReason);
    policyWaiverRequest.setReviewerId("admin");
    policyWaiverRequest.setReviewerName(reviewerName);
    policyWaiverRequestDAO.update(policyWaiverRequest);

    refreshOrOpen(DashboardPage.urlToWaiverRequests());
    DashboardPage.waitUntilSpinnersGone();

    DashboardPage.dashboardContainer().shouldBe(visible);
    WaiverRequestTile waiverRequest = verifyDashboardWaiverRequests(
        0,
        SEVERE,
        "7",
        policyWaiverRequest.getRequestTime(),
        "Test User 1",
        "Policy 1",
        "Application - App 1",
        "Group1 : Artifact1 : Version1");
    waiverRequest.status().shouldHave(text("Rejected"));

    dashboardWaiverRequestTable.firstWaiverRequest().click();

    RequestWaiverPage requestWaiverPage = new RequestWaiverPage();

    verifyWaiverRequestValues(
        requestWaiverPage,
        "Application - App 1",
        "Acknowledged violation",
        "Custom",
        threeDaysFromNow,
        "Comment 1",
        "Note to Reviewer 1");

    requestWaiverPage.rejectionErrorAlert().shouldBe(visible);

    String expectedText = "This Waiver Request was rejected by " + reviewerName + " for the following reason:";
    requestWaiverPage.rejectionErrorAlert().shouldHave(text(expectedText));
    requestWaiverPage.rejectionErrorAlert().shouldHave(text(rejectionReason));
  }

  private void loginAsLimitedUser() {
    User developerUser = tempEntity.newUser();
    tempEntity.newMembershipMapping(
        Organization.ROOT_ORGANIZATION_ID,
        Role.DEVELOPER_ROLE_ID,
        developerUser.getUsername());
    login(developerUser.getUsername(), developerUser.getPassword());
  }

  private WaiverRequestTile verifyDashboardWaiverRequests(
      int waiverIndex,
      WebElementCondition threatLevel,
      String threatNumber,
      Date requestTime,
      String requesterName,
      String policyName,
      String scopeName,
      String componentName)
  {

    DashboardPage.dashboardContainer().shouldBe(visible);
    WaiverRequestTile waiverRequest = dashboardWaiverRequestTable.waiverRequest(waiverIndex);
    waiverRequest.threatIndicator().shouldHave(threatLevel);
    waiverRequest.threatNumber().shouldHave(text(threatNumber));
    waiverRequest.createTime().shouldHave(text(formatDate(requestTime)));
    waiverRequest.requester().shouldHave(text(requesterName));
    waiverRequest.policy().shouldHave(text(policyName));
    waiverRequest.scope().shouldHave(text(scopeName));
    waiverRequest.component().shouldHave(text(componentName));

    return waiverRequest;
  }

  private String formatDate(Date date) {
    return DateFormatUtils.format(date, "yyyy-MM-dd");
  }

  private void verifyWaiverRequestValues(
      RequestWaiverPage requestWaiverPage,
      String selectedScope,
      String selectedWaiverReason,
      String waiverExpirationSelection,
      Date waiverExpirationDate,
      String comments,
      String noteToReviewer)
  {
    requestWaiverPage.root().shouldBe(visible);
    requestWaiverPage.requestWaiverHeader().shouldBe(visible);

    requestWaiverPage.requestWaiverScope().shouldBe(visible);
    requestWaiverPage.requestWaiverScopeOptions().findBy(text(selectedScope)).shouldBe(selected);

    requestWaiverPage.requestWaiverReasonOptions().findBy(text(selectedWaiverReason)).shouldBe(visible);

    if ("Custom".equals(waiverExpirationSelection)) {
      requestWaiverPage.requestWaiverDateInput().shouldBe(visible);

      if (waiverExpirationDate != null) {
        String formattedDate = formatDate(waiverExpirationDate);
        requestWaiverPage.requestWaiverDateInput().shouldHave(Condition.value(formattedDate));
      }
    }
    else {
      requestWaiverPage.requestWaiverExpiryTimeOptions().findBy(text(waiverExpirationSelection)).shouldBe(visible);
    }

    if (comments != null && !comments.isEmpty()) {
      requestWaiverPage.requestWaiverComments().shouldHave(text(comments));
    }

    if (noteToReviewer != null && !noteToReviewer.isEmpty()) {
      requestWaiverPage.requestWaiverNoteToReviewer().shouldHave(text(noteToReviewer));
    }

    requestWaiverPage.saveButton().shouldBe(visible);
    requestWaiverPage.cancelButton().shouldBe(visible);
  }

  private void updateWaiverRequestFormFields(
      RequestWaiverPage requestWaiverPage,
      String scopeText,
      String componentMatcherStrategy,
      String expirationText,
      String reasonText,
      String commentsText,
      String noteToReviewerText)
  {
    if (scopeText != null && !scopeText.isEmpty()) {
      requestWaiverPage.requestWaiverScopeSelect().selectOptionContainingText(scopeText);
    }

    if (componentMatcherStrategy != null && !componentMatcherStrategy.isEmpty()) {
      requestWaiverPage.requestWaiverComponentsOptions()
          .findBy(text(componentMatcherStrategy))
          .click();
    }

    if (expirationText != null && !expirationText.isEmpty()) {
      requestWaiverPage.waiverExpirationSelect().selectOptionContainingText(expirationText);
    }

    if (reasonText != null && !reasonText.isEmpty()) {
      requestWaiverPage.waiverReasonSelect().selectOptionContainingText(reasonText);
    }

    if (commentsText != null && !commentsText.isEmpty()) {
      requestWaiverPage.requestWaiverComments().setValue(commentsText);
    }

    if (noteToReviewerText != null && !noteToReviewerText.isEmpty()) {
      requestWaiverPage.requestWaiverNoteToReviewer().setValue(noteToReviewerText);
    }
  }
}
