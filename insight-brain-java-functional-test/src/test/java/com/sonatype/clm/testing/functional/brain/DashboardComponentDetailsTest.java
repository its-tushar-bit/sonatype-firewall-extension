/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.Date;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.DashboardComponents.ComponentsResults;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.DashboardComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.DashboardComponentDetailsPage.ApplicationCard;
import com.sonatype.clm.testing.functional.pages.DashboardComponentDetailsPage.ApplicationCardTable;
import com.sonatype.clm.testing.functional.pages.DashboardComponentDetailsPage.ApplicationCardTotals;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.actions.WarnActionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.ReleaseStageType;

import org.apache.commons.lang3.time.DateUtils;
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
import static com.sonatype.insight.brain.model.policy.PolicyThreatCategory.LICENSE;

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
    eval1 = tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, "DashboardComponentsTestEval1",
        DateUtils.addMinutes(new Date(), -1));
    tempEntity.newPolicyViolation(eval1, policy1, 9, LICENSE, component, "hash", FailActionType.ID);

    app2 = tempEntity.newApplication("App 2", "DashboardComponentDetailsTestApp2", org.getId());
    eval2 = tempEntity.newPolicyEvaluation(app2.getId(), ReleaseStageType.ID, "DashboardComponentsTestEval2",
        DateUtils.addMinutes(new Date(), -1));
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

    // top row
    ApplicationCard app1Row = dashboardComponentDetailsPage.getApplicationRow(0).shouldBe(visible);

    app1Row.name().shouldHave(text(app1.getName()));

    ApplicationCardTotals app1Totals = app1Row.totals().shouldBe(visible);
    app1Totals.pie().shouldBe(visible);
    app1Totals.shareOfRisk().shouldHave(text("64%"));
    app1Totals.risk().shouldHave(text("9"));
    app1Totals.source().shouldBe(empty);
    app1Totals.build().anchor().shouldHave(attribute("href", ApplicationReportPage.url(app1, eval1.getScanId())));
    app1Totals.build().anchorText().shouldHave(text("1min"));
    app1Totals.stage().shouldBe(empty);
    app1Totals.release().shouldBe(empty);
    app1Totals.operate().shouldBe(empty);

    app1Row.accordionRow().shouldBe(visible).click();
    ApplicationCardTable app1Table = app1Row.table().shouldBe(visible);

    app1Table.threatIndicator().shouldHave(cssClass("nx-threat-indicator--critical"));
    app1Table.threat().shouldHave(text("9"));
    app1Table.policyName().shouldHave(text(policy1.getName()));
    app1Table.pie().shouldBe(visible);
    app1Table.shareOfRisk().shouldHave(text("64%"));
    app1Table.risk().shouldHave(text("9"));
    app1Table.source().shouldBe(empty);
    app1Table.build().anchor().shouldHave(attribute("href", ApplicationReportPage.url(app1, eval1.getScanId())));
    app1Table.build().anchorText().shouldHave(text("1min"));
    app1Table.stage().shouldBe(empty);
    app1Table.release().shouldBe(empty);
    app1Table.operate().shouldBe(empty);

    // bottom row
    ApplicationCard app2Row = dashboardComponentDetailsPage.getApplicationRow(1).shouldBe(visible);

    app2Row.name().shouldHave(text(app2.getName()));

    ApplicationCardTotals app2Totals = app2Row.totals().shouldBe(visible);
    app2Totals.pie().shouldBe(visible);
    app2Totals.shareOfRisk().shouldHave(text("36%"));
    app2Totals.risk().shouldHave(text("5"));
    app2Totals.source().shouldBe(empty);
    app2Totals.build().shouldBe(empty);
    app2Totals.stage().shouldBe(empty);
    app2Totals.release().anchor().shouldHave(attribute("href", ApplicationReportPage.url(app2, eval2.getScanId())));
    app2Totals.release().anchorText().shouldHave(text("1min"));
    app2Totals.operate().shouldBe(empty);

    app2Row.accordionRow().shouldBe(visible).click();
    ApplicationCardTable app2Table = app2Row.table().shouldBe(visible);

    app2Table.threatIndicator().shouldHave(cssClass("nx-threat-indicator--severe"));
    app2Table.threat().shouldHave(text("5"));
    app2Table.policyName().shouldHave(text(policy2.getName()));
    app2Table.pie().shouldBe(visible);
    app2Table.shareOfRisk().shouldHave(text("36%"));
    app2Table.risk().shouldHave(text("5"));
    app2Table.source().shouldBe(empty);
    app2Table.build().shouldBe(empty);
    app2Table.stage().shouldBe(empty);
    app2Table.release().anchor().shouldHave(attribute("href", ApplicationReportPage.url(app2, eval2.getScanId())));
    app2Table.release().anchorText().shouldHave(text("1min"));
    app2Table.operate().shouldBe(empty);

    eyesWatcher.eyesCheck("Accordions expanded");
  }
}
