/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.elements.SourceControlTile;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.codeborne.selenide.Condition;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;

public class RootOrganizationSummaryViewTest extends AbstractFunctionalTest
{
  private Organization rootOrg;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() {
    rootOrg = new OrganizationDAO().getById(ROOT_ORGANIZATION_ID);
    refreshOrOpen(OwnerSummaryPage.url(rootOrg));
    OwnerSummaryPage.summaryTile().name().shouldHave(text(rootOrg.getName()));
  }

  @Test
  public void testDeleteRootOrg() {
    // Delete action should not be available to the root org
    ActionDropDown.actionButton().click();
    ActionDropDown.deleteOwnerButton().shouldNot(exist);
  }

  @Test
  public void testSourceControlTile() {
    SourceControlTile tile = OwnerSummaryPage.sourceControlTile();

    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    tile.subHeader().shouldBe(visible).shouldHave(
        Condition.text(String.format("Configures the integration with an external SCM for the %s", rootOrg.getName())));
    tile.rows().shouldHaveSize(1);

    tile.itemText().shouldNotBe(visible);
    tile.itemSubText().shouldBe(visible)
        .shouldHave(Condition.text("Source Control not configured"));

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, "TEST_TOKEN", SourceControlProvider.GITHUB);
    refresh();

    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    tile.subHeader().shouldBe(visible).shouldHave(
        Condition.text(String.format("Configures the integration with an external SCM for the %s", rootOrg.getName())));
    tile.rows().shouldHaveSize(1);

    //Verify valid source control record exists here
    tile.itemText().shouldBe(visible).shouldHave(Condition.text("GitHub"));
    tile.itemSubText().shouldBe(visible)
        .shouldHave(Condition.text("Provides the default source control configuration settings"));

    eyesWatcher.eyesCheck("Valid source control configured");
  }

  @Test
  public void testSourceControlTile_LicensingAwareNoLicense() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    SourceControlTile tile = OwnerSummaryPage.sourceControlTile();

    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    tile.subHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s", rootOrg.getName())));
    tile.notSupported().shouldBe(visible);
    tile.content().shouldNotBe(visible);
    tile.notSupported().shouldHave(text("Source Control is not supported by your license"));

    tile.itemText().shouldNotBe(visible);
    tile.itemSubText().shouldNotBe(visible);

    eyesWatcher.eyesCheck("Source Control No License");
  }

  @Test
  public void testSourceControlTile_LicensingAwareNotificationOnly() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_NEXUS);
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    SourceControlTile tile = OwnerSummaryPage.sourceControlTile();

    OwnerSummaryPage.summaryTile().dropdownButton().click();
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible).click();

    tile.shouldBe(visible);
    tile.subHeader().shouldBe(visible).shouldHave(Condition.text(String
        .format("Configures the integration with an external SCM for the %s", rootOrg.getName())));
    tile.notSupported().shouldNotBe(visible);
    tile.content().shouldBe(visible);

    tile.itemSubText().shouldBe(visible);
  }

  @Test
  public void testActionsDropdownOptions() {
    ActionDropDown.actionButton().click();
    ActionDropDown.editOwner().shouldBe(visible);
    ActionDropDown.importPoliciesButton().shouldBe(visible);
    ActionDropDown.actions().shouldHaveSize(2);

    eyesWatcher.eyesCheck("root organization actions dropdown");
  }
}
