/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.brain.legal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.ComponentNoticeDetailsPage;
import com.sonatype.clm.testing.functional.pages.ComponentNoticeDetailsPage.NoticeList;
import com.sonatype.clm.testing.functional.pages.ComponentNoticeDetailsPage.NoticeOverview;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentLegalPartStatus;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;
import com.sonatype.insight.brain.model.legal.LegalFileType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import com.codeborne.selenide.SelenideElement;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;

public class NoticeDetailsTest
    extends AbstractFunctionalTest
{
  private Application app;

  private Organization rootOrg;

  ComponentIdentifier componentId;

  ComponentLegalFile componentNoticeFile;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() throws IOException {
    rootOrg = new OrganizationDAO().getById(Organization.ROOT_ORGANIZATION_ID);
    app = tempEntity.newApplicationWithParent(NoticeDetailsTest.class.getSimpleName());
    componentId = ComponentIdentifier.createMavenCoordinates("g", "a", "v");
    tempEntity.newApplicationComponent(app.getId(), BuildStageType.ID, "033e7a20b23ea284d474",
        componentId);

    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalLicenseMetadataHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/license/metadata");
    testCLMServer.getHdsServer()
        .respondWith("[]")
        .atUri("/rest/legal/comment");
    testCLMServer.getHdsServer()
        .respondWith(IOUtils
            .toString(this.getClass().getResourceAsStream("/legal/legalFileHdsResponse.json"),
                StandardCharsets.UTF_8))
        .atUri("/rest/legal/file");

    refreshOrOpen(ComponentNoticeDetailsPage.urlToApplicationScope(
        app.getPublicId(), "033e7a20b23ea284d474", 0));
  }

  @Test
  public void testNoticeOverview() {
    refreshOrOpen(ComponentNoticeDetailsPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474", 0));
    final NoticeOverview noticeOverview = ComponentNoticeDetailsPage.noticeOverview();

    noticeOverview.getAttributionReportStatus().shouldHave(text("Included"));
    noticeOverview.getScope().shouldHave(text("Root Organization"));
    noticeOverview.getSource().shouldHave(text("Sonatype Scan"));

    noticeOverview.getNoticeText().shouldHave(text("Apache ServiceComb Copyright 2017-2021"));
  }

  @Test
  public void testNoticeList() {
    refreshOrOpen(ComponentNoticeDetailsPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474", 0));
    final NoticeList noticeList = ComponentNoticeDetailsPage.noticeList();

    noticeList.shouldHave(text("notice"));
    noticeList.attributionInclusion(1).shouldHave(text("Included in attribution report"));
    noticeList.attributionInclusion(2).shouldHave(text("Included in attribution report"));
  }

  @Test
  public void changeSelectedNotice() {
    refreshOrOpen(ComponentNoticeDetailsPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474", 0));

    final NoticeOverview noticeOverview = ComponentNoticeDetailsPage.noticeOverview();
    final NoticeList noticeList = ComponentNoticeDetailsPage.noticeList();

    noticeOverview.shouldHave(text("Apache Servicecomb"));
    noticeOverview.shouldNotHave(text("content"));
    SelenideElement secondNotice = noticeList.itemAt(2);
    secondNotice.lastChild().click();
    noticeOverview.shouldHave(text("content"));
    noticeOverview.shouldNotHave(text("Apache Servicecomb"));
  }

  @Test
  public void addVerifyNotice() {
    refreshOrOpen(ComponentNoticeDetailsPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474", 0));
    final NoticeOverview noticeOverview = ComponentNoticeDetailsPage.noticeOverview();

    noticeOverview.shouldHave(text("Apache Servicecomb"));
    noticeOverview.shouldNotHave(text("content"));

    componentNoticeFile =
        tempEntity.newComponentLegalFile(componentId, rootOrg.getId(), LegalFileType.NOTICE, "noticeContentHash");
    LegalFileOverride noticeFileOverride = tempEntity.newLegalFileOverride(
        "ceeb94cfb8ad27ae26ad0703a3e46babb828499fee29ff036b7eb9c80cd659e4", "hash", "added notice",
        ComponentLegalPartStatus.ENABLED, componentNoticeFile.getId());

    refreshOrOpen(ComponentNoticeDetailsPage.urlToApplicationScope(app.getPublicId(), "033e7a20b23ea284d474", 0));
    noticeOverview.shouldHave(text(noticeFileOverride.getContent()));
  }
}
