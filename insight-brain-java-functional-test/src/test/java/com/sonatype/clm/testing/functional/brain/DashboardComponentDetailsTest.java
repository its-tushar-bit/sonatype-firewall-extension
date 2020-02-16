/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.DashboardComponents.ComponentsResults;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.DashboardComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.DashboardComponentDetailsPage.AccordionRow;
import com.sonatype.clm.testing.functional.pages.DashboardComponentDetailsPage.ApplicationRow;
import com.sonatype.clm.testing.functional.pages.DashboardComponentDetailsPage.ComponentDetailsRow;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.pages.DashboardComponentDetailsPage.ApplicationRow.appIconImageSource;
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;
import static java.util.Arrays.asList;

public class DashboardComponentDetailsTest
    extends AbstractFunctionalTest
{
  private Application app1;

  private Application app2;

  private Policy policy1;

  private Policy policy2;

  private Organization org;

  private PolicyEvaluation eval1;

  private PolicyEvaluation eval2;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @After
  public void cleanup() {
    reverseProxyServer.reset();
  }

  @Before
  public void init() {
    org = tempEntity.newOrganization("DashboardComponentDetailsTestOrg");

    policy1 = tempEntity.newPolicy(org.getId(), "DashboardComponentDetailsTestPolicy1");
    policy2 = tempEntity
        .newPolicy(org.getId(), "DashboardComponentDetailsTestPolicy2", 5, null, ReleaseStageType.ID, null);

    ComponentIdentifier component = ComponentIdentifier.createMavenCoordinates("Group", "Artifact", "Version");

    app1 = tempEntity.newApplication("App 1", "DashboardComponentDetailsTestApp1", org.getId());
    tempEntity.newApplicationComponent(app1.getId(), BuildStageType.ID, "hash", component);
    eval1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "DashboardComponentsTestEval1");
    tempEntity.newPolicyViolation(eval1, policy1, 9, LICENSE, component, "hash", FailActionType.ID);

    app2 = tempEntity.newApplication("App 2", "DashboardComponentDetailsTestApp2", org.getId());
    eval2 = tempEntity.newPolicyEvaluation(app2.getId(), ReleaseStageType.ID, "DashboardComponentsTestEval2");
    tempEntity.newApplicationComponent(app2.getId(), ReleaseStageType.ID, "hash", component);
    tempEntity.newPolicyViolation(eval2, policy2, 5, LICENSE, component, "hash", WarnActionType.ID);

    refreshOrOpen(DashboardPage.urlToComponents());
  }

  @Test
  public void testComponentDetails() {
    DashboardPage.dashboardContainer().shouldBe(visible);
    ComponentsResults table = DashboardPage.componentsView().results();

    table.components().shouldHaveSize(1).shouldHave(texts("Group : Artifact : Version"));

    table.firstComponent().name().click();
    DashboardComponentDetailsPage dashboardComponentDetailsPage = new DashboardComponentDetailsPage();
    waitUntilUrl(DashboardComponentDetailsPage.url("hash"));
    DashboardPage.dashboardContainer().shouldNotBe(visible);

    // header
    dashboardComponentDetailsPage.header().shouldHave(text("Group : Artifact : Version"));
    dashboardComponentDetailsPage.totalRisk().shouldHave(text("14"));

    eyesWatcher.eyesCheck("Initial state");

    // top row
    ApplicationRow app1Row = dashboardComponentDetailsPage.getApplicationRow(0);
    app1Row.accordion().shouldNotBe(visible);

    app1Row.appIcon().shouldHave(attribute("src", appIconImageSource(app1.getPublicId())));
    app1Row.name().shouldHave(text(org.getName() + " : " + app1.getName()));
    app1Row.twisty().shouldBe(CLM.EXPANDED).click();

    app1Row.accordion().shouldBe(visible);
    app1Row.accordion().entries().shouldHaveSize(1);
    AccordionRow accordion1Row = app1Row.accordion().entry(1);
    accordion1Row.threatBar().shouldHave(cssClass("critical"));
    accordion1Row.threat().shouldHave(text("9"));
    accordion1Row.policyName().shouldHave(text(policy1.getName()));

    for (ComponentDetailsRow row : asList(app1Row, accordion1Row)) {
      row.pie().shouldBe(visible);
      row.shareOfRisk().shouldHave(text("64%"));
      row.risk().shouldHave(text("9"));
      row.build().anchor().shouldHave(text("1min"), attribute("title", "View application report"),
          attribute("href", ApplicationReportPage.url(app1, eval1.getScanId())));
      row.stage().shouldBe(empty);
      row.release().shouldBe(empty);
      row.operate().shouldBe(empty);
    }

    // bottom row
    ApplicationRow app2Row = dashboardComponentDetailsPage.getApplicationRow(1);
    app2Row.accordion().shouldNotBe(visible);

    app2Row.appIcon().shouldHave(attribute("src", appIconImageSource(app2.getPublicId())));
    app2Row.name().shouldHave(text(org.getName() + " : " + app2.getName()));
    app2Row.twisty().shouldBe(CLM.EXPANDED).click();

    app2Row.accordion().shouldBe(visible);
    app2Row.accordion().entries().shouldHaveSize(1);
    AccordionRow accordion2Row = app2Row.accordion().entry(1);
    accordion2Row.threatBar().shouldHave(cssClass("severe"));
    accordion2Row.threat().shouldHave(text("5"));
    accordion2Row.policyName().shouldHave(text(policy2.getName()));

    for (ComponentDetailsRow row : asList(app2Row, accordion2Row)) {
      row.pie().shouldBe(visible);
      row.shareOfRisk().shouldHave(text("36%"));
      row.risk().shouldHave(text("5"));
      row.build().shouldBe(empty);
      row.stage().shouldBe(empty);
      row.release().anchor().shouldHave(text("1min"), attribute("title", "View application report"),
          attribute("href", ApplicationReportPage.url(app2, eval2.getScanId())));
      row.operate().shouldBe(empty);
    }

    eyesWatcher.eyesCheck("Accordions expanded");

    // collapse accordions
    app1Row.accordion().shouldBe(visible);
    app1Row.twisty().click();
    app1Row.accordion().shouldNotBe(visible);
    app2Row.accordion().shouldBe(visible);
    app2Row.twisty().click();
    app2Row.accordion().shouldNotBe(visible);

    dashboardComponentDetailsPage.breadCrumb().shouldHave(text("Dashboard/ Component Details"));
    dashboardComponentDetailsPage.breadCrumbLink().shouldHave(text("Dashboard")).click();
    DashboardPage.componentsView().results().shouldBe(visible);
  }
}
