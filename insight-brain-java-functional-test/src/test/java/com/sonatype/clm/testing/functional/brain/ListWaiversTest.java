/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.AddWaiverPage;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage;
import com.sonatype.clm.testing.functional.pages.ViolationDetailsPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.text;

public class ListWaiversTest
    extends AbstractFunctionalTest
{
  private static Organization organization;

  private static Application application;

  private static PolicyViolation policyViolation;

  @BeforeClass
  public static void startup() {
    Instant now = Instant.now();
    Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

    organization = staticTempEntity.newOrganization("Org 1");
    application = staticTempEntity.newApplication("App 1", "app1", organization.getId());
    Policy securityPolicy1 = staticTempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID, "Policy 1", 7);

    PolicyEvaluation policyEvaluation1 = staticTempEntity.newPolicyEvaluation(application.getId(),
        StageTypes.BUILD.getId(), "scan1", false, false, Date.from(twoDaysAgo));

    policyViolation = staticTempEntity.newPolicyViolation(policyEvaluation1, securityPolicy1, "Group1",
        "Artifact1", "Version1", "hash1", "sonatype-2017-0507");

    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Test
  public void testPageLayout() {
    refreshOrOpen(ListWaiversPage.url(policyViolation.getId()));

    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.title().shouldHave(text("Waivers for Violation"));
    listWaiversPage.waiverDetailsTile().exists();
    listWaiversPage.waiverDetailsTitle().shouldHave(text("Waiver Details"));
    listWaiversPage.policyName().shouldHave(text("Policy 1"));
    listWaiversPage.policyName().shouldHave(cssClass("iq-threat-level--severe"));
    listWaiversPage.constraintName().shouldHave(text("Test Constraint"));
    listWaiversPage.conditions().shouldHaveSize(1);
    listWaiversPage.condition(1).shouldHave(text("sonatype-2017-0507"));
    listWaiversPage.componentName().shouldHave(text("Group1 : Artifact1 : Version1"));

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testWaiverListTable() {
    refreshOrOpen(ListWaiversPage.url(policyViolation.getId()));

    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.waiverListTile().exists();
    listWaiversPage.waiverListTitle().shouldHave(text("Waiver List Table"));
    listWaiversPage.waiverListTable().exists();
    listWaiversPage.addWaiverButton().click();
    waitUntilUrl(AddWaiverPage.url(policyViolation.getId()));
  }

  @Test
  public void testBackButton() {
    refreshOrOpen(ListWaiversPage.url(policyViolation.getId()));
    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.backButton().shouldHave(text("Back to Violation Details")).click();
    waitUntilUrl(ViolationDetailsPage.url(policyViolation.getId()));
  }
}
