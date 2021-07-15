/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.utils;

import com.sonatype.clm.testing.functional.elements.NxSubmitMask;
import com.sonatype.clm.testing.functional.pages.AddWaiverPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage;
import com.sonatype.clm.testing.functional.pages.ApplicationReportPage.CipModal;
import com.sonatype.clm.testing.functional.pages.WaiverCip;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.visible;

public class WaiverApplierForReport
{
  public static void waiveReportRow(ApplicationReportPage reportPage, int rowNumber) {
    ElementsCollection violations = reportPage.resultRows();
    SelenideElement firstViolation = violations.get(rowNumber);
    firstViolation.click();

    CipModal cipModal = reportPage.cipModal();
    cipModal.tabLink(2).click();
    WaiverCip.row(0).waiveButton().shouldBe(visible).click();

    AddWaiverPage addWaiverPage = new AddWaiverPage();
    addWaiverPage.shouldBe(visible);
    addWaiverPage.saveButton().shouldBe(visible, enabled).click();
    NxSubmitMask.seeAndWaitForDismissal();
    addWaiverPage.should(disappear);

    // We are back to the context of the report at this point
    cipModal.closeButton().click();
    cipModal.shouldNotBe(visible);
  }
}
