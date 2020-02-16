/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.IqCheckbox;
import com.sonatype.clm.testing.functional.elements.NotificationsSection;
import com.sonatype.clm.testing.functional.elements.OwnerDetailTreeView;
import com.sonatype.clm.testing.functional.elements.PolicyTile;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.MonitoredStageEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.policy.StageTypeService;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.Condition;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;
import static com.sonatype.clm.testing.functional.elements.TileSimpleList.CLICKABLE;

public abstract class AbstractPolicyMonitoringEditorTest
    extends AbstractFunctionalTest
{
  private Owner currentOwner;

  private Organization parentOrg;

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private OrganizationDAO orgDao = new OrganizationDAO();

  @BeforeClass
  public static void boot() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  protected void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    this.parentOrg = orgDao.getById(currentOwner.getParentOwnerId());
    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    OwnerSummaryPage.summaryTile().shouldBe(visible).name().shouldHave(text(currentOwner.getName()));
  }

  @Test
  public void testEditMonitoredStage() {
    String inheritOptionText = MonitoredStageEditorPage.inheritFromParentDoNotMonitorText(parentOrg.getName());
    OwnerSummaryPage.policyTile().monitoredStage().shouldHave(text(inheritOptionText)).click();
    assertEditMonitoredStageStateIsCorrect(inheritOptionText);

    MonitoredStageEditorPage.getStageByName("Develop").click();
    MonitoredStageEditorPage.updateButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    assertEditMonitoredStageStateIsCorrect("Develop");

    eyesWatcher.eyesCheck();
  }

  @Test
  public void testNotLicensed() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    assertNotLicensed(false);
  }

  @Test
  public void testNotLicensed_Foundation() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    assertNotLicensed(true);
  }

  @Test
  public void testNotLicensed_Foundation_Firewall() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION, ProductLicenseDetails.PRODUCT_FIREWALL);
    assertNotLicensed(false);
  }
  
  public void assertNotLicensed(boolean notificationsReadOnly) {
    refresh();
    Condition notLicensedText = MonitoredStageEditorPage.unsupportedLicenseText();
    PolicyTile policyTile = OwnerSummaryPage.policyTile();
    policyTile.shouldBe(visible);
    policyTile.monitoredStage().shouldBe(visible).shouldHave(notLicensedText).shouldNotHave(CLICKABLE);

    // if the user gets there manually, show a warning
    refreshOrOpen(MonitoredStageEditorPage.url(currentOwner));
    MonitoredStageEditorPage.unsupportedLicenseWarning().shouldHave(notLicensedText);

    // disable the owner detail tree view item
    refreshOrOpen(PolicyEditorPage.urlToCreate(currentOwner));
    OwnerDetailTreeView.policyGroup().item(2).shouldBe(DISABLED).hover();
    int cmIndex = OwnerDetailTreeView.policyGroup().items().size() - 2;
    OwnerDetailTreeView.policyGroup().item(cmIndex).shouldBe(DISABLED).hover();
    Tooltip.get().shouldBe(visible).shouldHave(text("Policy Monitoring is not supported by your license"));
    // disable continuous monitoring checkboxes in notification area
    PolicyEditorPage.notificationsPill().click();
    NotificationsSection notificationsSection = PolicyEditorPage.notificationsSection();
    cmIndex = testCLMServer.getCLMServer().getInstance(StageTypeService.class).getLicensedStageTypes().size();
    notificationsSection.headers().get(cmIndex).shouldBe(DISABLED);
    if (notificationsReadOnly) {
      NotificationsSection.addNotification().email().shouldBe(disabled);
      NotificationsSection.addNotification().addButton().shouldBe(DISABLED);
    }
    else {
      NotificationsSection.addNotification().email().val("a@b");
      NotificationsSection.addNotification().addButton().shouldNotBe(DISABLED).click();
      IqCheckbox monitoringCheckbox = NotificationsSection.notificationFor("a@b").continuousMonitoring();
      monitoringCheckbox.input().shouldBe(disabled);
      monitoringCheckbox.hover();
      Tooltip.get().shouldBe(visible).shouldHave(text("Policy Monitoring is not supported by your license"));
      NotificationsSection.notificationFor("a@b").deleteButton().hover(); // tooltip obscures button, discard it
      NotificationsSection.notificationFor("a@b").deleteButton().click();
    }
  }

  private void assertEditMonitoredStageStateIsCorrect(String selectedStageText) {
    waitUntilUrl(MonitoredStageEditorPage.url(currentOwner));
    MonitoredStageEditorPage.title().shouldHave(text(MonitoredStageEditorPage.HEADER_TEXT));
    MonitoredStageEditorPage.selectedStage().shouldHave(text(selectedStageText));
    MonitoredStageEditorPage.updateButton().shouldHave(DISABLED);
  }
}
