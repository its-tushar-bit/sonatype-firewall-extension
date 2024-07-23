/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NotificationsSection;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.elements.OwnerDetailSidebar;
import com.sonatype.clm.testing.functional.elements.PolicyTile;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.MonitoredStageEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.PolicyEditorPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.codeborne.selenide.Condition;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.elements.CLM.DISABLED;

public abstract class AbstractPolicyMonitoringEditorTest
    extends AbstractFunctionalTest
{
  private Owner currentOwner;

  private Organization parentOrg;

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  private OrganizationDAO orgDao;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    orgDao = lookup(OrganizationDAO.class);
  }

  protected void init(Owner currentOwner) {
    this.currentOwner = currentOwner;
    this.parentOrg = orgDao.getById(currentOwner.getParentOwnerId());
    refreshOrOpen(OwnerSummaryPage.url(currentOwner));
    OwnerSummaryPage.summaryTile().shouldBe(visible).name().shouldHave(text(currentOwner.getName()));
  }

  void testEditMonitoredStage() {
    String inheritOptionText = MonitoredStageEditorPage.inheritFromParentDoNotMonitorText(parentOrg.getName());
    OwnerSummaryPage.monitoredStage().shouldHave(text(inheritOptionText)).click();
    assertEditMonitoredStageStateIsCorrect(inheritOptionText);

    MonitoredStageEditorPage.getStageByName("Develop").click();
    MonitoredStageEditorPage.updateButton().shouldNotHave(DISABLED).click();
    FormMask.seeAndWaitForDismissal();
    assertEditMonitoredStageStateIsCorrect("Develop");
  }

  @Test
  public void testNotLicensed() {
    setFeatures(LicensedFeature.NOTIFICATIONS, LicensedFeature.POLICY_MANAGEMENT, LicensedFeature.POLICY_READ_ONLY);
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
    OwnerSummaryPage.monitoredStage()
        .shouldBe(hidden);

    // if the user gets there manually, show a warning
    refreshOrOpen(MonitoredStageEditorPage.url(currentOwner));
    MonitoredStageEditorPage.unsupportedLicenseWarning().shouldHave(notLicensedText);

    // disable the owner detail sidebar item
    refreshOrOpen(PolicyEditorPage.urlToCreate(currentOwner));
    OwnerDetailSidebar.continuousMonitoring().shouldBe(hidden);
    Tooltip.get().shouldBe(hidden);
    // disable continuous monitoring checkboxes in notification area
    ScrollUtil.scrollIntoView(PolicyEditorPage.notificationsSection().header());
    if (notificationsReadOnly) {
      NotificationsSection.addNotification().email().shouldBe(disabled);
      NotificationsSection.addNotification().addButton().shouldBe(disabled);
    }
    else {
      NotificationsSection.addNotification().email().val("a@b");
      NotificationsSection.addNotification().addButton().shouldNotBe(DISABLED).click();
      NxCheckbox monitoringCheckbox = NotificationsSection.notificationFor("a@b").continuousMonitoring();
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
    MonitoredStageEditorPage.updateButton().shouldBe(visible);
  }
}
