/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.report;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ReportCip;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.ReportPage;
import com.sonatype.clm.testing.functional.pages.ReportPolicyPage;
import com.sonatype.clm.testing.functional.pages.WaiverCip;
import com.sonatype.clm.testing.functional.pages.WaiverCip.AddWaiverDialog;
import com.sonatype.clm.testing.functional.pages.WaiverCip.ConfirmRemoveWaiverDialog;
import com.sonatype.clm.testing.functional.pages.WaiverCip.ExistingWaiver;
import com.sonatype.clm.testing.functional.pages.WaiverCip.ViewWaiversDialog;
import com.sonatype.clm.testing.functional.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.conditions.CoordinatesConditionType;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.ReportHelper;

import com.codeborne.selenide.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.appear;
import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public class WaiverTest
    extends AbstractFunctionalTest
{
  private static final String policyName = "All components";

  private static final String scanId = "306e0a923df34c64b836358182b1b902";

  private static final InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());

  public static final int numberOfComponents = 9;

  private Application app;

  private TestReportEvaluator evaluator;

  @BeforeClass
  public static void startup() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() {
    app = tempEntity.newApplicationWithParent(WaiverTest.class.getSimpleName(), "Waiver Test App", "Waiver Test Org");
    evaluator = new TestReportEvaluator(app, scanId, ReportHelper.zipReport("/canned-reports/small-report", tempDir),
        Configuration.baseUrl, work);
  }

  @Test
  public void testViewWaivedPolicyViolations() throws Exception {
    createGavViolatingPolicy(app.getOrganizationId());

    evaluator.evaluatePolicy();
    waiveComponent();
    AddWaiverDialog.saveButton().shouldBe(visible, enabled).click();
    AddWaiverDialog.root().should(disappear);

    List<PolicyViolation> policyViolations = new PolicyViolationDAO().getByApplicationId(app.getId());
    assertThat(policyViolations).hasSize(numberOfComponents - 1);

    List<PolicyWaiver> policyWaivers = new PolicyWaiverDAO().getActiveByOwnerId(app.getId());
    assertThat(policyWaivers).hasSize(1);

    PolicyWaiver policyWaiver = policyWaivers.get(0);
    PolicyViolation policyViolation = policyViolations.stream()
        .filter(violation -> policyWaiver.getConstraintFactsJson().equals(violation.getConstraintFactsJson()))
        .findFirst().get();

    assertThat(policyWaiver.getPolicyId()).isEqualTo(policyViolation.getPolicyId());
    assertThat(policyWaiver.getOwnerId()).isEqualTo(policyViolation.getApplicationId());
    assertThat(policyWaiver.getHash()).isEqualTo(policyViolation.getHash());

    evaluator.reevaluatePolicy();

    refreshOrOpen(ReportPage.url(app, scanId));
    ReportPage.policyTabButton().click();

    ReportPolicyPage.waivedView().shouldBe(visible).click();

    ReportPolicyPage.row(0).coordinates().shouldHave(text("ch.qos.logback : logback-access : 0.6"));
  }

  @Test
  public void testViewComponentPolicyWaivers() throws Exception {
    createGavViolatingPolicy(app.getOrganizationId());

    evaluator.evaluatePolicy();
    waiveComponent();
    AddWaiverDialog.comment().setValue("TEST COMMENT");
    AddWaiverDialog.saveButton().shouldBe(visible, enabled).click();
    AddWaiverDialog.root().should(disappear);

    WaiverCip.viewWaivers().click();

    ViewWaiversDialog.rows().shouldHaveSize(1);

    assertWaiver(ViewWaiversDialog.row(0), "TEST COMMENT");
    eyesWatcher.eyesCheck("Waivers list");

    ViewWaiversDialog.row(0).removeButton().click();
    ConfirmRemoveWaiverDialog.removeButton().should(visible).click();

    ViewWaiversDialog.rows().shouldHaveSize(0);
    ViewWaiversDialog.emptyText().shouldBe(visible);
    ViewWaiversDialog.closeButton().click();

    String longComment = StringUtils.repeat("Long text ", 101);
    String truncatedLongComment = longComment.substring(0, 1000);

    WaiverCip.row(0).waiveButton().click();
    AddWaiverDialog.comment().setValue(longComment);
    eyesWatcher.eyesCheck("Add waiver");
    AddWaiverDialog.saveButton().shouldBe(visible, enabled).click();
    AddWaiverDialog.root().should(disappear);

    WaiverCip.viewWaivers().shouldBe(visible).click();

    ViewWaiversDialog.rows().shouldHaveSize(1);
    ViewWaiversDialog.row(0).comment().shouldHave(text(truncatedLongComment));

    ViewWaiversDialog.row(0).removeButton().click();
    ConfirmRemoveWaiverDialog.cancelButton().shouldBe(visible).click();
    ConfirmRemoveWaiverDialog.cancelButton().shouldBe(hidden);

    ViewWaiversDialog.rows().shouldHaveSize(1);
    assertWaiver(ViewWaiversDialog.row(0), truncatedLongComment);

    ViewWaiversDialog.closeButton().click();

    ViewWaiversDialog.closeButton().shouldBe(hidden);
  }

  @Test
  public void testApplicationPolicyCanOnlyBeScopedToApplication() throws Exception {
    createGavViolatingPolicy(app.getId());

    evaluator.evaluatePolicy();

    waiveComponent();
    AddWaiverDialog.scopedWaiver().click();
    AddWaiverDialog.scopeContainer().shouldBe(visible);

    AddWaiverDialog.waiverOwner().shouldHave(text("Application - " + app.getName()));
    AddWaiverDialog.waiverOwnerOptions().shouldHaveSize(1);

  }

  @Test
  public void testOrganizationPolicyCanBeScopedToOrganization() throws Exception {
    createGavViolatingPolicy(app.getOrganizationId());

    evaluator.evaluatePolicy();

    waiveComponent();
    AddWaiverDialog.scopedWaiver().click();
    AddWaiverDialog.scopeContainer().shouldBe(visible);

    AddWaiverDialog.waiverOwnerOptions().shouldHaveSize(2);
    AddWaiverDialog.waiverOwnerOptions()
        .shouldHave(texts(new String[]{"Application - " + app.getName(), "Organization - Waiver Test Org"}));
  }

  @Test
  public void testWaiverCanBeAppliedToSelectedComponentForChildrenOfOrganization() throws Exception {
    createGavViolatingPolicy(app.getOrganizationId());

    evaluator.evaluatePolicy();

    waiveComponent();
    AddWaiverDialog.scopedWaiver().click();
    AddWaiverDialog.scopeContainer().shouldBe(visible);

    AddWaiverDialog.waiverOwner().selectOptionContainingText("Organization - Waiver Test Org");

    AddWaiverDialog.selectedComponent().shouldBe(selected);

    AddWaiverDialog.saveButton().shouldBe(visible, enabled).click();
    AddWaiverDialog.root().should(disappear);

    evaluator.reevaluatePolicy();

    refreshOrOpen(ReportPage.url(app, scanId));
    ReportPage.policyTabButton().click();

    ReportPolicyPage.rows().shouldHaveSize(numberOfComponents);
    ReportPolicyPage.resultsWithNoScore().shouldHaveSize(2);
  }

  @Test
  public void testWaiverCanBeAppliedToAllComponentsForApplication() throws Exception {
    createGavViolatingPolicy(app.getId());

    evaluator.evaluatePolicy();

    waiveComponent();

    AddWaiverDialog.scopedWaiver().click();
    AddWaiverDialog.allComponents().shouldBe(visible).shouldNotBe(selected).click();

    AddWaiverDialog.saveButton().shouldBe(visible, enabled).click();
    AddWaiverDialog.root().should(disappear);

    evaluator.reevaluatePolicy();

    refreshOrOpen(ReportPage.url(app, scanId));
    ReportPage.policyTabButton().click();

    ReportPolicyPage.rows().shouldHaveSize(numberOfComponents);
    ReportPolicyPage.resultsWithNoScore().shouldHaveSize(numberOfComponents);
  }

  private void assertWaiver(ExistingWaiver waiver, String comment) {
    waiver.policy().shouldHave(text(policyName));
    waiver.created().shouldHave(text(new SimpleDateFormat("yyyy-MM-dd").format(new Date())));
    waiver.owner().shouldHave(text(app.getName()));
    waiver.comment().shouldHave(text(comment));
  }

  private void waiveComponent() {
    refreshOrOpen(ReportPage.url(app, scanId));
    ReportPage.policyTabButton().shouldBe(visible).click();

    ReportPolicyPage.rows().shouldHaveSize(numberOfComponents);
    ReportPolicyPage.row(1).openCip();
    ReportCip.policyTab().should(appear).click();
    WaiverCip.row(0).waiveButton().shouldBe(visible).click();
    AddWaiverDialog.root().should(appear);
    AddWaiverDialog.comment().shouldBe(visible);
    AddWaiverDialog.waiveViolationOnly().shouldBe(visible);
    AddWaiverDialog.waiveViolationOnly().shouldBe(selected);
  }

  private void createGavViolatingPolicy(String ownerId) {
    // create policy
    Condition condition = new Condition(CoordinatesConditionType.ID, "match", "maven:*");
    Constraint constraint = new Constraint();
    constraint.setName("All coordinates");
    constraint.addCondition(condition);
    Policy policy = new Policy();
    policy.setName(policyName);
    policy.addConstraint(constraint);
    policy.setOwnerId(ownerId);

    // add policy
    tempEntity.newPolicy(policy);
  }
}
