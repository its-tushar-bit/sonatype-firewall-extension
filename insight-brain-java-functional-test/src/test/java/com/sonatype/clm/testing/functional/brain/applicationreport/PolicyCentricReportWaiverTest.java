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
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationDetailPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationsTable;
import com.sonatype.clm.testing.functional.pages.AddWaiverPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage;
import com.sonatype.clm.testing.functional.pages.DeleteWaiverModal;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage.WaiverListRow;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage.WaiverListTable;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
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
import com.codeborne.selenide.SelenideElement;
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
import static com.sonatype.clm.testing.functional.utils.ScrollUtil.scrollIntoView;
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
    assertThat(policyViolations).hasSize(12);

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

    reportPage.aggregateByComponentToggle().shouldBeOn().click();
    reportPage.aggregateByComponentToggle().shouldBeOff();

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

    ListWaiversPage listWaiversPage = new ListWaiversPage();
    listWaiversPage.shouldBe(visible);
    WaiverListTable waiverListTable = listWaiversPage.waiverListTable();
    waiverListTable.shouldBe(visible);
    waiverListTable.rows().shouldHaveSize(1);
    WaiverListRow existingWaiver = waiverListTable.row(1);

    assertWaiver(existingWaiver, "TEST COMMENT");

    eyesWatcher.eyesCheck("Waivers list");

    existingWaiver.deleteButton().shouldBe(visible).click();
    DeleteWaiverModal deleteWaiverModal = new DeleteWaiverModal();
    deleteWaiverModal.root().shouldBe(visible);
    deleteWaiverModal.yesButton().shouldBe(visible).click();
    NxSubmitMask.seeAndWaitForDismissal();

    waiverListTable.rows().shouldHaveSize(1);
    waiverListTable.noWaiversMessage().shouldBe(visible);

    listWaiversPage.addWaiverButton().shouldBe(visible).click();

    // get policy violation id
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates(
        "ch.qos.logback", "logback-access", "0.6", "", "jar");
    String policyViolationId = getViolationForPolicyComponent(policyName, componentIdentifier);
    waitUntilUrl(AddWaiverPage.url(policyViolationId));

    addWaiverPage.should(appear);

    String longComment = StringUtils.repeat("Long text ", 101);
    String truncatedLongComment = longComment.substring(0, 1000);
    addWaiverPage.shouldBe(visible);
    addWaiverPage.comments().setValue(longComment);
    scrollIntoView(addWaiverPage.artifactName());
    eyesWatcher.eyesCheck("Add waiver");
    addWaiverPage.saveButton().shouldBe(visible, enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.should(disappear);

    waiverListTable.shouldBe(visible);
    waiverListTable.rows().shouldHaveSize(1);
    assertWaiver(waiverListTable.row(1), truncatedLongComment);

    existingWaiver.deleteButton().click();
    deleteWaiverModal.cancelButton().shouldBe(visible).click();
    deleteWaiverModal.cancelButton().shouldBe(hidden);

    waiverListTable.rows().shouldHaveSize(1);
    assertWaiver(waiverListTable.row(1), truncatedLongComment);
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

    addWaiverPage.availableComponents().shouldHaveSize(3);
    addWaiverPage.component(0).label().shouldHave(text("ch.qos.logback : logback-access : 0.6"));
    addWaiverPage.component(0).shouldBe(selected);
    addWaiverPage.component(1).label().shouldHave(text("ch.qos.logback : logback-access (all versions)"));
    addWaiverPage.component(1).shouldNotBe(selected);
    addWaiverPage.component(2).label().shouldHave(text("All Components"));
    addWaiverPage.component(2).shouldNotBe(selected);

    addWaiverPage.saveButton().shouldBe(visible, enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.should(disappear);

    evaluator.reevaluatePolicy();

    refreshOrOpen(ApplicationReportPage.url(app, scanId));
    reportPage.resultRows().shouldHave(texts("All Components", "All Components", "All Components", "All Components",
        "All Components", "All Components", "All Components", "All Components", "All Components", "All Components",
        "All Components", "None", "None"));
  }

  @Test
  public void testWaiverCanBeAppliedToAllComponentsForApplication() throws Exception {
    createGavViolatingPolicy(app.getId());

    evaluator.evaluatePolicy();

    waiveComponent();
    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.scope(0).click();
    addWaiverPage.availableComponents().shouldHaveSize(3);
    addWaiverPage.component(2).label().shouldHave(text("All Components"));
    addWaiverPage.component(2).shouldNotBe(selected).click();

    addWaiverPage.saveButton().shouldBe(visible, enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.should(disappear);

    evaluator.reevaluatePolicy();

    refreshOrOpen(ApplicationReportPage.url(app, scanId));
    reportPage.resultRows()
        .shouldHave(
            texts("Waived", "Waived", "Waived", "Waived", "Waived", "Waived", "Waived", "Waived", "Waived", "Waived",
                "Waived", "Waived", "None"));
  }

  private void assertWaiver(WaiverListRow waiver, String comment) {
    waiver.dateCreated().shouldHave(text(new SimpleDateFormat("MM/dd/yyyy").format(new Date())));
    waiver.scope().shouldHave(text(app.getName()));
    waiver.comments().shouldHave(text(comment));
  }

  private void waiveComponent() {
    refreshOrOpen(ApplicationReportPage.url(app, scanId));

    reportPage.shouldBe(visible);
    reportPage.resultRows().shouldHaveSize(13);
    reportPage.resultRow(2).click();

    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.violationsTab().shouldBe(visible).click();
    componentDetailsPage.violationsTabContent().shouldBe(visible);

    PolicyViolationsTable violationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    violationsTable.shouldBe(visible);
    SelenideElement row = violationsTable.getRow(1);
    row.click();
    PolicyViolationDetailPopover violationDetailPopover = new PolicyViolationDetailPopover();
    violationDetailPopover.shouldBe(visible);
    SelenideElement manageWaiversButton = violationDetailPopover.getManageWaiversButton();
    manageWaiversButton.click();
    ListWaiversPage waiversForViolationPage = new ListWaiversPage();
    waiversForViolationPage.shouldBe(visible);
    waiversForViolationPage.addWaiverButton().click();

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

