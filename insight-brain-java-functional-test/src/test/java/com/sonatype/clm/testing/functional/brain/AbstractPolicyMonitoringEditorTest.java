/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.MonitoredStageEditorPage;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage.SummaryTile;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.open;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;

public abstract class AbstractPolicyMonitoringEditorTest
    extends AbstractFunctionalTest
{
  private Owner currentOwner;
  private Organization parentOrg;

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private OrganizationDAO orgDao = new OrganizationDAO();

  @BeforeClass
  public static void boot() {
    refreshOrOpen(OrganizationManagementPage.URL);
    loginAsAdmin();
  }

  protected void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    this.parentOrg = orgDao.getById(currentOwner.getParentOwnerId());
    open(OwnerSummaryPage.url(currentOwner.getType().toString(), currentOwner.getPublicId()));
  }

  @Test
  public void testEditMonitoredStage() {
    String inheritOptionText = MonitoredStageEditorPage.inheritFromParentDoNotMonitorText(parentOrg.getName());
    SummaryTile.monitoredStage().shouldHave(text(inheritOptionText)).click();
    assertEditMonitoredStageStateIsCorrect(inheritOptionText);

    MonitoredStageEditorPage.getStageByName("Develop").click();
    MonitoredStageEditorPage.updateButton().shouldNotHave(DISABLED).click();
    FormMask.root().shouldBe(visible).shouldNotBe(visible);
    assertEditMonitoredStageStateIsCorrect("Develop");
  }

  private void assertEditMonitoredStageStateIsCorrect(String selectedStageText) {
    waitUntilUrl(MonitoredStageEditorPage.url(currentOwner.getType().toString(), currentOwner.getPublicId()));
    MonitoredStageEditorPage.title().shouldHave(text(MonitoredStageEditorPage.HEADER_TEXT));
    MonitoredStageEditorPage.selectedStage().shouldHave(text(selectedStageText));
    MonitoredStageEditorPage.updateButton().shouldHave(DISABLED);
  }
}
