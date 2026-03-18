/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain.labs;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.pages.AddSuccessMetricsModal;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportListPage;
import com.sonatype.clm.testing.functional.pages.SuccessMetricsReportListPage.SuccessMetricsListItem;
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
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class SuccessMetricsReportListTest
    extends AbstractFunctionalTest
{
  private Organization organization1;

  private Organization organization2;

  private Organization emptyOrganization;

  private Application application1;

  private Application application2;

  private Application application3;

  private void createChartData() {
    organization1 = tempEntity.newOrganization("Test Org 1");
    organization2 = tempEntity.newOrganization("Test Org 2");
    emptyOrganization = tempEntity.newOrganization("Empty Org");
    application1 = tempEntity.newApplication("App1", "App1", organization1.getId());
    application2 = tempEntity.newApplication("App2", "App2", organization1.getId());
    application3 = tempEntity.newApplication("App3", "App3", organization2.getId());

    tempEntity.newPolicyEvaluation(application1.getId(), BuildStageType.ID,
        "scan1", new LocalDate().minusMonths(1).toDate());
    tempEntity.newPolicyEvaluation(application2.getId(), BuildStageType.ID,
        "scan2", new LocalDate().minusMonths(1).toDate());
    tempEntity.newPolicyEvaluation(application3.getId(), BuildStageType.ID,
        "scan3", new LocalDate().minusMonths(1).toDate());
  }

  @Before
  public void before() {
    createChartData();
    refreshOrOpen(SuccessMetricsReportListPage.url());
    loginAsAdmin();
  }

  @After
  public void cleanup() {
    SuccessMetricsReportDAO dao = lookup(SuccessMetricsReportDAO.class);
    for (SuccessMetricsReport successMetricsReport : dao.getByUsername("admin")) {
      dao.delete(successMetricsReport);
    }

    Selenide.clearBrowserCookies();
  }

  @Test
  public void testSuccessMetricsReportList() {
    SuccessMetricsReportListPage successMetricsReportListPage = new SuccessMetricsReportListPage();
    successMetricsReportListPage.shouldBe(visible);

    successMetricsReportListPage.reports().shouldHave(size(0));
    successMetricsReportListPage.emptyDescriptor()
        .shouldBe(visible)
        .shouldHave(SuccessMetricsReportListPage.EMPTY_TEXT);

    tempEntity.newSuccessMetricsReport("admin", "Test Success Metric",
        JsonUtils.format(new SuccessMetricsReportScopeDTO()));

    refresh();

    successMetricsReportListPage.emptyDescriptor().shouldBe(hidden);
    successMetricsReportListPage.reports().shouldHave(size(1));
    eyesWatcher.eyesCheck();

    SuccessMetricsListItem row = successMetricsReportListPage.report(0);
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
    page.reports().shouldHave(size(0));

    page.addSuccessMetricsBtn().shouldBe(visible).click();

    // Add and test a Root Org SuccessMetricsReport.
    modal.name().setValue("Root Org Chart");
    modal.onlyForFullCalendarWeeksAndMonths().shouldBe(selected);
    modal.includingMostRecentEvaluations().shouldNotBe(selected);
    modal.allApplicationsRadioBtn().shouldHave(text("all applications")).shouldBe(selected);
    modal.customRadioBtn().shouldHave(text("custom")).shouldNotBe(selected);
    modal.createBtn().shouldHave(text("Submit")).click();

    page.reports().shouldHave(size(1));
    page.report(0).shouldHave(text("Root Org Chart")).link().click();

    SuccessMetricsReportPage chartPage = new SuccessMetricsReportPage();
    chartPage.shouldBe(visible);
    SuccessMetricsReportPage.Header.description().shouldHave(text("3 applications"));

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

    page.reports().shouldHave(size(2));
    page.report(1).shouldHave(text("Organization Chart")).link().click();

    chartPage = new SuccessMetricsReportPage();
    chartPage.shouldBe(visible);
    SuccessMetricsReportPage.Header.description().shouldHave(text("2 applications"));

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

    page.reports().shouldHave(size(3));
    page.report(2).shouldHave(text("Application Chart")).link().click();

    chartPage = new SuccessMetricsReportPage();
    chartPage.shouldBe(visible);
    SuccessMetricsReportPage.Header.description().shouldHave(text("data for 1 application"));

    // Delete this SuccessMetricsReport.
    chartPage.deleteBtn().shouldBe(visible).click();
    NxDeleteModal deleteModal = new NxDeleteModal("#delete-modal");
    deleteModal.alertContent()
        .shouldBe(visible)
        .shouldHave(
            SuccessMetricsReportPage.confirmRemovalText("Application Chart"));
    deleteModal.header().shouldHave(SuccessMetricsReportPage.CONFIRM_REMOVAL_HEADER_TEXT);
    deleteModal.submitButton().click();
    FormMask.seeAndWaitForDismissal();
    deleteModal.alertContent().shouldBe(hidden);
    page.reports().shouldHave(size(2));

    // Then add and test a SuccessMetricsReport with only an empty Organization selected.
    page.addSuccessMetricsBtn().click();
    modal.name().setValue("Empty Org Chart");
    modal.customRadioBtn().click();

    modal.orgPickerCounter().shouldHave(text("3"));
    modal.orgPickerTrigger().click();
    modal.nthOrg(2).shouldHave(text(emptyOrganization.getName())).click();
    modal.orgPickerCounter().shouldHave(text("3"));
    modal.createBtn().click();

    page.reports().shouldHave(size(3));
    page.report(2).shouldHave(text("Empty Org Chart")).link().click();

    chartPage = new SuccessMetricsReportPage();
    chartPage.shouldBe(visible);
    chartPage.noDataInfoPane().shouldBe(visible);

    // Now delete this empty SuccessMetricsReport.
    chartPage.deleteBtn().shouldBe(visible).click();
    deleteModal.submitButton().click();
    FormMask.seeAndWaitForDismissal();
    deleteModal.alertContent().shouldBe(hidden);
    page.reports().shouldHave(size(2));
  }
}
