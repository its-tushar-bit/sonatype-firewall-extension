/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.applicationreport;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.pages.AddWaiverPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.CipModal;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.pages.WaiverCip;
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
import org.apache.commons.collections4.CollectionUtils;
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

public class PolicyCentricReportWaiverTest
    extends AbstractFunctionalTest
{
  private static final String policyName = "All components";

  private static final String scanId = "306e0a923df34c64b836358182b1b902";

  private static final InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());

  public static final int numberOfComponents = 4;

  private final ApplicationReportPage reportPage = new ApplicationReportPage();

  private Application app;

  private TestReportEvaluator evaluator;

  private final PolicyViolationDAO policyViolationDAO = new PolicyViolationDAO();

  @BeforeClass
  public static void startup() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void start() {
    app = tempEntity.newApplicationWithParent(PolicyCentricReportWaiverTest.class.getSimpleName(), "Waiver Test App",
        "Waiver Test Org");
    evaluator = new TestReportEvaluator(app, scanId, ReportHelper.zipReport("/canned-reports/small-report", tempDir),
        Configuration.baseUrl, work);
  }

  @Test
  public void testViewWaivedPolicyViolations() throws Exception {
    createGavViolatingPolicy(app.getOrganizationId());

    evaluator.evaluatePolicy();
    waiveComponent();
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.saveButton().shouldBe(visible, enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.should(disappear);

    List<PolicyViolation> policyViolations = new PolicyViolationDAO().getByApplicationId(app.getId());
    assertThat(policyViolations).hasSize(7);

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

    refreshOrOpen(ApplicationReportPage.url(app, scanId));

    reportPage.showAllViolationsRadio().click();

    reportPage.resultRow(2).shouldHave(text("ch.qos.logback : logback-access : 0.6"));
  }

  @Test
  public void testViewComponentPolicyWaivers() throws Exception {
    createGavViolatingPolicy(app.getOrganizationId());

    evaluator.evaluatePolicy();
    waiveComponent();
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.comments().setValue("TEST COMMENT");
    addWaiverPage.saveButton().shouldBe(visible, enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.should(disappear);

    waitUntilUrl(ApplicationReportPage.url(app, scanId));
    reportPage.shouldBe(visible);

    CipModal cipModal = reportPage.cipModal();
    cipModal.shouldBe(visible);

    switchCipToPolicyTab();

    WaiverCip.viewWaivers().shouldBe(visible).click();

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

    // get policy violation id
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "ch.qos.logback", "logback-access", "0.6", "", "jar");
    String policyViolationId = getViolationForPolicyComponent(policyName, componentIdentifier);
    waitUntilUrl(AddWaiverPage.url(policyViolationId));

    addWaiverPage.should(appear);
    addWaiverPage.comments().setValue(longComment);
    eyesWatcher.eyesCheck("Add waiver");
    addWaiverPage.saveButton().shouldBe(visible, enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.should(disappear);

    waitUntilUrl(ApplicationReportPage.url(app, scanId));
    reportPage.shouldBe(visible);

    cipModal.shouldBe(visible);
    switchCipToPolicyTab();

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

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHaveSize(1);
    addWaiverPage.scope(0).click();
    addWaiverPage.scope(0).shouldHave(text("Application - " + app.getName()));
  }

  @Test
  public void testOrganizationPolicyCanBeScopedToOrganization() throws Exception {
    createGavViolatingPolicy(app.getOrganizationId());

    evaluator.evaluatePolicy();

    waiveComponent();
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHaveSize(2);
    addWaiverPage.scope(0).click();
    addWaiverPage.scope(0).shouldHave(text("Application - " + app.getName()));
    addWaiverPage.scope(1).shouldHave(text("Organization - Waiver Test Org"));
  }

  @Test
  public void testWaiverCanBeAppliedToSelectedComponentForChildrenOfOrganization() throws Exception {
    createGavViolatingPolicy(app.getOrganizationId());

    evaluator.evaluatePolicy();

    waiveComponent();
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.availableScopes().shouldHaveSize(2);
    addWaiverPage.scope(1).shouldHave(text("Organization - Waiver Test Org"));
    addWaiverPage.scope(1).click();

    addWaiverPage.availableComponents().shouldHaveSize(2);
    addWaiverPage.component(0).label().shouldHave(text("ch.qos.logback : logback-access : 0.6"));
    addWaiverPage.component(0).shouldBe(selected);
    addWaiverPage.component(1).label().shouldHave(text("All Components"));
    addWaiverPage.component(1).shouldNotBe(selected);

    addWaiverPage.saveButton().shouldBe(visible, enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.should(disappear);

    evaluator.reevaluatePolicy();

    refreshOrOpen(ApplicationReportPage.url(app, scanId));

    reportPage.resultRows().shouldHave(texts("All Components", "All Components", "All Components", "All Components",
        "All Components", "All Components", "None", "None"));
  }

  @Test
  public void testWaiverCanBeAppliedToAllComponentsForApplication() throws Exception {
    createGavViolatingPolicy(app.getId());

    evaluator.evaluatePolicy();

    waiveComponent();
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.scope(0).click();
    addWaiverPage.availableComponents().shouldHaveSize(2);
    addWaiverPage.component(1).label().shouldHave(text("All Components"));
    addWaiverPage.component(1).shouldNotBe(selected).click();

    addWaiverPage.saveButton().shouldBe(visible, enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.should(disappear);

    evaluator.reevaluatePolicy();

    refreshOrOpen(ApplicationReportPage.url(app, scanId));

    reportPage.resultRows()
        .shouldHave(texts("Waived", "Waived", "Waived", "Waived", "Waived", "Waived", "Waived", "None"));
  }

  private void assertWaiver(ExistingWaiver waiver, String comment) {
    waiver.policy().shouldHave(text(policyName));
    waiver.created().shouldHave(text(new SimpleDateFormat("yyyy-MM-dd").format(new Date())));
    waiver.owner().shouldHave(text(app.getName()));
    waiver.comment().shouldHave(text(comment));
  }

  private void waiveComponent() {
    refreshOrOpen(ApplicationReportPage.url(app, scanId));

    reportPage.shouldBe(visible);

    CipModal cipModal = reportPage.cipModal();

    reportPage.resultRows().shouldHaveSize(8);
    reportPage.resultRow(2).click();
    cipModal.tabLink(2).click();
    WaiverCip.row(0).waiveButton().shouldBe(visible).click();

    // get policy violation id
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "ch.qos.logback", "logback-access", "0.6", "", "jar");
    String policyViolationId = getViolationForPolicyComponent(policyName, componentIdentifier);
    waitUntilUrl(AddWaiverPage.url(policyViolationId));

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.should(appear);
    addWaiverPage.comments().shouldBe(visible);
    addWaiverPage.scope(0).label().shouldHave(text("Application - " + app.getName()));
    addWaiverPage.scope(0).shouldBe(visible, selected);
  }

  private void switchCipToPolicyTab() {
    CipModal cipModal = reportPage.cipModal();
    cipModal.tabLink(2).click();
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

  private String getViolationForPolicyComponent(String policyName, ComponentIdentifier componentIdentifier) {
    List<PolicyViolation> policyViolations = policyViolationDAO.getByApplicationId(app.getId());

    if (CollectionUtils.isNotEmpty(policyViolations)) {
      return policyViolations.stream()
          .filter(pv -> pv.getPolicyName().equals(policyName)
              && pv.getComponentIdentifier().equals(componentIdentifier))
          .findFirst()
          .map(PolicyViolation::getId)
          .orElse(null);
    }

    return null;
  }
}
