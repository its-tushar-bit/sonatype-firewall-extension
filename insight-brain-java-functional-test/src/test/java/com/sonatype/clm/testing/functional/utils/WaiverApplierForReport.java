/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationDetailPopover;
import com.sonatype.clm.testing.functional.elements.componentdetails.PolicyViolationsTable;
import com.sonatype.clm.testing.functional.pages.AddWaiverPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ComponentDetailsPage;
import com.sonatype.clm.testing.functional.pages.ListWaiversPage;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class WaiverApplierForReport
{
  public static void waiveViolationFromTable(PolicyViolationsTable violationsTable, int rowNumber) {
    SelenideElement row = violationsTable.getRow(rowNumber);
    row.shouldBe(visible);
    row.click();
    PolicyViolationDetailPopover violationDetailPopover = new PolicyViolationDetailPopover();
    violationDetailPopover.shouldBe(visible);

    SelenideElement manageWaiversButton = violationDetailPopover.getManageWaiversButton();
    manageWaiversButton.click();

    ListWaiversPage waiversForViolationPage = new ListWaiversPage();
    waiversForViolationPage.shouldBe(visible);
    waiversForViolationPage.addWaiverButton().click();

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.shouldBe(visible);
    addWaiverPage.saveButton().shouldBe(visible).click();

    waiversForViolationPage.backButton().shouldBe(visible).click();
    violationDetailPopover.getCloseButton().click();
  }

  public static void waiveReportRow(ApplicationReportPage reportPage, int rowNumber) {
    ElementsCollection violations = reportPage.resultRows();
    SelenideElement violation = violations.get(rowNumber);
    violation.click();

    ComponentDetailsPage componentDetailsPage = new ComponentDetailsPage();
    componentDetailsPage.violationsTab().shouldBe(visible).click();
    componentDetailsPage.violationsTabContent().shouldBe(visible);
    PolicyViolationsTable violationsTable = componentDetailsPage.violationsTabContent().policyViolationsTable();
    violationsTable.shouldBe(visible);

    waiveViolationFromTable(violationsTable, 1);

    SelenideElement backButton = componentDetailsPage.backButton();
    backButton.shouldBe(visible);
    backButton.shouldHave(text("Back to Application Report"));
    backButton.click();
  }
}
