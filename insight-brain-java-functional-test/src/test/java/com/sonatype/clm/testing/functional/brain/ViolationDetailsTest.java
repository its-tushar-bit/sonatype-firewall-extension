/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;

public class ViolationDetailsTest
    extends AbstractFunctionalTest
{
  private static Application application;

  private static Policy policy;

  private static PolicyViolation policyViolation;

  @BeforeClass
  public static void startup() {
    PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
    Instant oneDayAgo = now.minus(1, ChronoUnit.DAYS);

    Organization organization = staticTempEntity.newOrganization("Org 1");
    application = staticTempEntity.newApplication("App 1", "app1", organization.getId());
    policy = staticTempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7);

    PolicyEvaluation policyEvaluation1 = staticTempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.BUILD.getId(), "scan1", false, false, Date.from(twoDaysAgo));

    PolicyEvaluation policyEvaluation2 = staticTempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.RELEASE.getId(), "scan2", false, false, Date.from(oneDayAgo));

    PolicyEvaluation policyEvaluation3 = staticTempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.OPERATE.getId(), "scan3", false, false, Date.from(oneDayAgo));

    policyViolation = staticTempEntity.newPolicyViolation(policyEvaluation1, policy);
    policyViolation.setActionTypeId(Action.ID_FAIL);
    policyViolationDAO.update(policyViolation);

    staticTempEntity.newPolicyViolation(policyEvaluation2, policy);

    PolicyViolation policyViolation3 = staticTempEntity.newPolicyViolation(policyEvaluation3, policy);
    policyViolation3.setActionTypeId(Action.ID_WARN);
    policyViolationDAO.update(policyViolation3);

    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void openPage() {
    refreshOrOpen(ViolationDetailsPage.url(policyViolation.getId()));
  }

  @Test
  public void testDetails() {
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();

    eyesWatcher.eyesCheck();

    tile.headerTitle().shouldHave(text("Violation of Policy 1"));
    tile.headerSubtitle().shouldHave(text("Org 1App 1Group1 : Artifact1 : Version1"));
    tile.firstReported().shouldHave(text("2 days ago"));
    tile.lastReported().shouldHave(text("1 day ago"));
    tile.policyType().shouldHave(text("Security"));
    tile.threatLevel().shouldHave(text("7"));
    tile.policyOwner().shouldHave(text("Root Organization"));

    tile.stages().shouldHaveSize(4);

    tile.stage(0).shouldHave(text("Build 2d"));
    tile.stage(0).icon().should(exist);
    tile.stage(0).shouldNotBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(1).shouldHave(text("Stage"));
    tile.stage(1).icon().should(exist);
    tile.stage(1).shouldBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(2).shouldHave(text("Release 1d"));
    tile.stage(2).icon().shouldNot(exist);
    tile.stage(2).shouldNotBe(ViolationDetailsPage.ViolationDetailsStage.unused());

    tile.stage(3).shouldHave(text("Operate 1d"));
    tile.stage(3).icon().should(exist);
    tile.stage(3).shouldNotBe(ViolationDetailsPage.ViolationDetailsStage.unused());
  }

  @Test
  public void testStageLink() {
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();

    tile.stage(0).link().shouldHave(text("Build")).click();
    waitUntilUrl(ApplicationReportPage.url(application, "scan1"));

    refreshOrOpen(ViolationDetailsPage.url(policyViolation.getId()));

    tile.stage(2).link().shouldHave(text("Release")).click();
    waitUntilUrl(ApplicationReportPage.url(application, "scan2"));

    refreshOrOpen(ViolationDetailsPage.url(policyViolation.getId()));
    tile.stage(1).link().shouldNot(exist);
    tile.stage(3).link().should(exist);
  }

  @Test
  public void testPolicyOwnerLink() {
    ViolationDetailsPage.ViolationDetailsTile tile = new ViolationDetailsPage().detailsTile();

    tile.policyOwner().click();
    waitUntilUrl(OwnerSummaryPage.urlToRootOrg());
  }

  @Test
  public void testBackButton() {
    ViolationDetailsPage violationDetailsPage = new ViolationDetailsPage();

    // for now, this is hard-coded to go to the dashboard
    violationDetailsPage.backButton().click();
    waitUntilUrl(DashboardPage.urlToViolations());
  }
}
