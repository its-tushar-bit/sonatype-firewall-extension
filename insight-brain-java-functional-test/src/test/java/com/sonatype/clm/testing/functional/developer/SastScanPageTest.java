/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.developer;

import java.sql.Date;
import java.util.stream.IntStream;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.SastScanPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.sast.SastFinding;
import com.sonatype.insight.brain.model.sast.SastFindingConfidence;
import com.sonatype.insight.brain.model.sast.SastScan;
import com.sonatype.insight.license.model.LicensedFeature;

import com.codeborne.selenide.ElementsCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.utils.ScrollUtil.scrollIntoView;

public class SastScanPageTest
    extends AbstractFunctionalTest
{
  private static final String APPLICATION_ID = "appId";

  private static final String APPLICATION_NAME = "appName";

  @Before
  public void before() {
    setFeatures(LicensedFeature.DEVELOPER_DASHBOARD, LicensedFeature.DASHBOARD);
  }

  @After
  public void after() {
    logout();
  }

  @Test
  public void testSastScanPage() {
    final Application application = tempEntity.newApplicationWithParent(APPLICATION_ID, APPLICATION_NAME);
    SastScan sastScan = tempEntity.newSastScanWithCustomTimestamp(application.getId(), Date.valueOf("2020-01-01"));
    setUpSastFindings(sastScan);
    refreshOrOpen(SastScanPage.urlToCreate(application.getPublicId(), sastScan.getId()));
    loginAsAdmin();

    scrollIntoView(SastScanPage.findingsTable());

    SastScanPage.title().shouldBe(visible);
    SastScanPage.triggeredOnDate().shouldBe(visible);
    SastScanPage.filterBySeverityDropdown().shouldBe(visible);
    SastScanPage.findingsTable().shouldBe(visible);

    // test sast finding table
    SastScanPage.sastFindingTableDataRows().shouldHave(size(4));
    SastScanPage.sastFindingTableDataRows().get(0).shouldHave(text("CWE"));
    SastScanPage.sastFindingTableDataRows().get(0).shouldHave(text("someDescription"));
    SastScanPage.sastFindingTableDataRows().get(0).shouldHave(text("someRuleName"));
    SastScanPage.sastFindingTableDataRows().get(0).shouldHave(text("Medium"));
    SastScanPage.sastFindingTableDataRows().get(0).shouldHave(text("Critical"));

    // test sast finding table filter
    SastScanPage.filterBySeverityDropdown().click();
    ElementsCollection filterOptions = SastScanPage.filterBySeverityDropdown().findAll(".nx-checkbox");
    filterOptions.shouldHave(size(4));
    filterOptions.get(0).shouldHave(text("CRITICAL"));
    filterOptions.get(1).shouldHave(text("HIGH"));
    filterOptions.get(2).shouldHave(text("MEDIUM"));
    filterOptions.get(3).shouldHave(text("LOW"));

    filterOptions.get(0).click();
    SastScanPage.sastFindingTableDataRows().shouldHave(size(1));

    filterOptions.get(0).click();
    SastScanPage.sastFindingTableDataRows().shouldHave(size(4));

    SastScanPage.filterBySeverityDropdown().click();

    eyesWatcher.eyesCheck();

  }

  private void setUpSastFindings(SastScan sastScan) {
    IntStream.range(1, 5).forEach(i -> {

      final SastFinding sastFinding = new SastFinding();
      sastFinding.setSastScanId(sastScan.getId());
      sastFinding.setCwe("CWE");
      sastFinding.setConfidence(SastFindingConfidence.MEDIUM);
      sastFinding.setSeverityId(i);
      sastFinding.setDescription("someDescription");
      sastFinding.setCoordinate("{\"namespace\":\"namespace\",\"name\":\"CWE\",\"methodName\":\"method\"}");
      sastFinding.setLineNumber(null);
      sastFinding.setRuleName("someRuleName");
      tempEntity.newSastFinding(sastFinding);
    });
  }
}
