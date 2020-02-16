/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.labs;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionList;
import com.sonatype.clm.testing.functional.elements.ActionList.ActionListElement;
import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.AddSuccessMetricsModal;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportListPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportPage;
import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsReportDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsReportScopeDTO;
import com.sonatype.insight.json.store.JsonUtils;

import com.codeborne.selenide.Selenide;
import org.joda.time.LocalDate;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.pages.AddSuccessMetricsModal.FOOTER_ERROR_CLASS;
import static com.sonatype.clm.testing.functional.pages.AddSuccessMetricsModal.SUBMIT_BUTTON_DISABLED_CLASS;
import static com.sonatype.clm.testing.functional.pages.AddSuccessMetricsModal.SUBMIT_BUTTON_ERROR_CLASS;

public class SuccessMetricsReportListTest
    extends AbstractFunctionalTest
{
  private static Organization organization1;

  private static Organization organization2;

  private static Organization emptyOrganization;

  private static Application application1;

  private static Application application2;

  private static Application application3;

  @BeforeClass
  public static void createChartData() {
    organization1 = staticTempEntity.newOrganization("Test Org 1");
    organization2 = staticTempEntity.newOrganization("Test Org 2");
    emptyOrganization = staticTempEntity.newOrganization("Empty Org");
    application1 = staticTempEntity.newApplication("App1", "App1", organization1.getId());
    application2 = staticTempEntity.newApplication("App2", "App2", organization1.getId());
    application3 = staticTempEntity.newApplication("App3", "App3", organization2.getId());

    staticTempEntity.newPolicyEvaluation(application1.getId(), BuildStageType.ID,
        "scan1", new LocalDate().minusMonths(1).toDate());
    staticTempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID,
        "scan2", new LocalDate().minusMonths(1).toDate());
    staticTempEntity.newPolicyEvaluation(application3.getId(), BuildStageType.ID,
        "scan3", new LocalDate().minusMonths(1).toDate());
  }

  @Before
  public void before() {
    refreshOrOpen(SuccessMetricsReportListPage.url());
    loginAsAdmin();
  }

  @After
  public void cleanup() {
    SuccessMetricsReportDAO dao = new SuccessMetricsReportDAO();
    for (SuccessMetricsReport successMetricsReport : dao.getByUsername("admin")) {
      dao.delete(successMetricsReport);
    }

    Selenide.clearBrowserCookies();
  }

  @Test
  public void testSuccessMetricsReportList() {
    SuccessMetricsReportListPage successMetricsReportListPage = new SuccessMetricsReportListPage();
    successMetricsReportListPage.shouldBe(visible);

    ActionList reportList = successMetricsReportListPage.successMetricsChartActionItems();

    reportList.elements().shouldHaveSize(0);
    reportList.emptyDescriptor().shouldBe(visible).shouldHave(SuccessMetricsReportListPage.EMPTY_TEXT);

    tempEntity.newSuccessMetricsReport("admin", "Test Success Metric",
        JsonUtils.format(new SuccessMetricsReportScopeDTO()));
    
    refresh();

    reportList.emptyDescriptor().shouldBe(hidden);
    reportList.elements().shouldHaveSize(1);
    eyesWatcher.eyesCheck();

    ActionListElement row = reportList.element(0);
    row.chevron().shouldBe(visible);
    row.shouldBe(visible).shouldHave(text("Test Success Metric"));
  }

  @Test
  public void testAddSuccessMetrics() {
    SuccessMetricsReportListPage page = new SuccessMetricsReportListPage();
    page.addSuccessMetricsBtn().shouldBe(visible).click();

    AddSuccessMetricsModal modal = new AddSuccessMetricsModal();

    // First just test the cancel button.
    modal.shouldBe(visible);
    modal.name().shouldBe(visible); // ensure form is fully loaded and stable
    modal.name().setValue("To Be Cancelled");
    modal.cancelBtn().shouldBe(enabled).click();
    modal.shouldBe(hidden);
    refresh();
    page.successMetricsChartActionItems().elements().shouldHaveSize(0);

    page.addSuccessMetricsBtn().shouldBe(visible).click();

    // Add and test a Root Org SuccessMetricsReport.
    modal.name().setValue("Root Org Chart");
    modal.onlyForFullCalendarWeeksAndMonths().shouldBe(selected);
    modal.includingMostRecentEvaluations().shouldNotBe(selected);
    modal.allApplicationsRadioBtn().shouldHave(text("all applications")).shouldBe(selected);
    modal.customRadioBtn().shouldHave(text("custom")).shouldNotBe(selected);
    modal.createBtn().shouldHave(text("Create")).click();

    page.successMetricsChartActionItems().elements().shouldHaveSize(1);
    page.successMetricsChartActionItems().element(0).shouldHave(text("Root Org Chart")).click();

    SuccessMetricsReportPage chartPage = new SuccessMetricsReportPage();
    chartPage.shouldBe(visible);
    SuccessMetricsReportPage.SummaryStatementTile.activeApplicationsCount().shouldHave(text("3"));

    // Then add and test a SuccessMetricsReport with an org selection.
    chartPage.backButton().click();
    page.addSuccessMetricsBtn().click();
    modal.name().setValue("Organization Chart");
    modal.customRadioBtn().click();

    modal.appPicker().shouldBe(visible);
    modal.orgPicker().shouldBe(visible);
    modal.orgPickerCounter().shouldHave(text("3"));
    modal.appPickerCounter().shouldHave(text("3"));
    modal.orgPickerTrigger().click();
    modal.nthOrg(1).shouldHave(text("all/none"));
    modal.nthOrg(2).shouldHave(text(emptyOrganization.getName()));
    modal.nthOrg(3).shouldHave(text(organization1.getName())).click();
    modal.nthOrg(4).shouldHave(text(organization2.getName()));
    modal.orgPickerCounter().shouldHave(text("1 of 3"));
    modal.appPickerCounter().shouldHave(text("2 of 3"));
    modal.appPickerTrigger().scrollIntoView(false).click();
    modal.nthApp(1).shouldHave(text("all/none")).shouldNotBe(selected);
    modal.nthApp(2).shouldHave(text(application1.getName())).shouldBe(selected);
    modal.nthApp(3).shouldHave(text(application2.getName())).shouldBe(selected);
    modal.nthApp(4).shouldHave(text(application3.getName())).shouldNotBe(selected);
    eyesWatcher.eyesCheck();

    modal.createBtn().click();

    page.successMetricsChartActionItems().elements().shouldHaveSize(2);
    page.successMetricsChartActionItems().element(1).shouldHave(text("Organization Chart")).click();

    chartPage = new SuccessMetricsReportPage();
    chartPage.shouldBe(visible);
    SuccessMetricsReportPage.SummaryStatementTile.activeApplicationsCount().shouldHave(text("2"));

    // Then add and test a SuccessMetricsReport with an app selection.
    chartPage.backButton().click();
    page.addSuccessMetricsBtn().click();
    modal.name().setValue("Application Chart");
    modal.customRadioBtn().click();

    modal.appPickerCounter().shouldHave(text("3"));
    modal.appPickerTrigger().click();
    modal.nthApp(1).shouldHave(text("all/none"));
    modal.nthApp(2).shouldHave(text(application1.getName()));
    modal.nthApp(3).shouldHave(text(application2.getName())).click();
    modal.nthApp(4).shouldHave(text(application3.getName()));
    modal.appPickerCounter().shouldHave(text("1 of 3"));
    modal.orgPickerCounter().shouldHave(text("3"));
    modal.orgPickerTrigger().click();
    modal.nthOrg(1).shouldHave(text("all/none")).shouldNotBe(selected);
    modal.nthOrg(2).shouldHave(text(emptyOrganization.getName())).shouldNotBe(selected);
    modal.nthOrg(3).shouldHave(text(organization1.getName())).shouldNotBe(selected);
    modal.nthOrg(4).shouldHave(text(organization2.getName())).shouldNotBe(selected);
    modal.createBtn().click();

    page.successMetricsChartActionItems().elements().shouldHaveSize(3);
    page.successMetricsChartActionItems().element(2).shouldHave(text("Application Chart")).click();

    chartPage = new SuccessMetricsReportPage();
    chartPage.shouldBe(visible);
    SuccessMetricsReportPage.SummaryStatementTile.activeApplicationsCount().shouldHave(text("1"));

    // Delete this SuccessMetricsReport.
    chartPage.deleteBtn().shouldBe(visible).click();
    DeleteModal.body().shouldBe(visible).shouldHave(SuccessMetricsReportPage.confirmRemovalText("Application Chart"));
    DeleteModal.header().shouldHave(SuccessMetricsReportPage.CONFIRM_REMOVAL_HEADER_TEXT);
    DeleteModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    DeleteModal.body().shouldBe(hidden);
    page.successMetricsChartActionItems().elements().shouldHaveSize(2);

    // Then add and test a SuccessMetricsReport with only an empty Organization selected.
    page.addSuccessMetricsBtn().click();
    modal.name().setValue("Empty Org Chart");
    modal.customRadioBtn().click();

    modal.orgPickerCounter().shouldHave(text("3"));
    modal.orgPickerTrigger().click();
    modal.nthOrg(2).shouldHave(text(emptyOrganization.getName())).click();
    modal.orgPickerCounter().shouldHave(text("3"));
    modal.createBtn().click();

    page.successMetricsChartActionItems().elements().shouldHaveSize(3);
    page.successMetricsChartActionItems().element(2).shouldHave(text("Empty Org Chart")).click();

    chartPage = new SuccessMetricsReportPage();
    chartPage.shouldBe(visible);
    chartPage.noDataInfoPane().shouldBe(visible);

    // Now delete this empty SuccessMetricsReport.
    chartPage.deleteBtn().shouldBe(visible).click();
    DeleteModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();
    DeleteModal.body().shouldBe(hidden);
    page.successMetricsChartActionItems().elements().shouldHaveSize(2);
  }

  @Test
  public void testAddSuccessMetrics_Validation() {
    SuccessMetricsReportListPage page = new SuccessMetricsReportListPage();
    page.addSuccessMetricsBtn().click();

    AddSuccessMetricsModal modal = new AddSuccessMetricsModal();

    modal.createBtn().shouldHave(cssClass(SUBMIT_BUTTON_DISABLED_CLASS));

    modal.name().setValue("test");
    modal.createBtn().shouldNotHave(cssClass(SUBMIT_BUTTON_DISABLED_CLASS)).click();
    modal.shouldBe(hidden);

    page.addSuccessMetricsBtn().click();

    // duplicate checking
    modal.name().setValue("test");
    modal.createBtn().shouldHave(cssClass(SUBMIT_BUTTON_DISABLED_CLASS));

    // case-insensitive duplicate checking
    modal.name().setValue("Test");
    modal.createBtn().shouldHave(cssClass(SUBMIT_BUTTON_DISABLED_CLASS));

    modal.name().setValue("Test 1");
    modal.createBtn().shouldNotHave(cssClass(SUBMIT_BUTTON_DISABLED_CLASS));

    // doubled space checking
    modal.name().setValue("Test  1");
    modal.createBtn().shouldHave(cssClass(SUBMIT_BUTTON_DISABLED_CLASS));

    // tab checking
    modal.name().setValue("Test\t1");
    modal.createBtn().shouldHave(cssClass(SUBMIT_BUTTON_DISABLED_CLASS));

    // empty Custom selection checking
    modal.name().setValue("Test 1");
    modal.createBtn().shouldNotHave(cssClass(SUBMIT_BUTTON_DISABLED_CLASS));
    modal.customRadioBtn().click();
    modal.createBtn().shouldHave(cssClass(SUBMIT_BUTTON_DISABLED_CLASS));
    modal.orgPickerTrigger().click();
    modal.nthOrg(2).label().click();
    modal.createBtn().shouldNotHave(cssClass(SUBMIT_BUTTON_DISABLED_CLASS));
    modal.nthOrg(2).label().click();
    modal.orgPickerTrigger().click();
    modal.appPickerTrigger().click();
    modal.createBtn().shouldHave(cssClass(SUBMIT_BUTTON_DISABLED_CLASS));
    modal.nthApp(2).label().click();
    modal.createBtn().shouldNotHave(cssClass(SUBMIT_BUTTON_DISABLED_CLASS));
    modal.nthApp(2).label().click();
    modal.createBtn().shouldHave(cssClass(SUBMIT_BUTTON_DISABLED_CLASS));

    // whitespace-insensitive dup checking - only implemented on the server
    modal.allApplicationsRadioBtn().click();
    modal.createBtn().shouldNotHave(cssClass(SUBMIT_BUTTON_DISABLED_CLASS));
    modal.name().setValue("Tes t");
    modal.createBtn().shouldNotHave(cssClass(SUBMIT_BUTTON_DISABLED_CLASS));
    modal.createBtn().click();
    modal.footer().shouldHave(cssClass(FOOTER_ERROR_CLASS));
    modal.createBtn().shouldHave(cssClass(SUBMIT_BUTTON_ERROR_CLASS)).shouldHave(text("Retry"))
      .shouldNotHave(cssClass(SUBMIT_BUTTON_DISABLED_CLASS));
    modal.cancelBtn().shouldNotHave(cssClass(SUBMIT_BUTTON_DISABLED_CLASS));

    modal.cancelBtn().click();
  }
}
