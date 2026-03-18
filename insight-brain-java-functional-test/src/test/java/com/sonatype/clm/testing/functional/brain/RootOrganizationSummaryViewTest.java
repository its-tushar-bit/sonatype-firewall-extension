/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.ActionDropDown;
import com.sonatype.clm.testing.functional.elements.NxBreadcrumb;
import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.elements.SourceControlTile;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.ReportListPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.codeborne.selenide.Condition;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class RootOrganizationSummaryViewTest
    extends AbstractFunctionalTest
{
  private Organization rootOrg;

  @BeforeClass
  public static void boot() {
    refreshOrOpen(ReportListPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() {
    rootOrg = lookup(OrganizationDAO.class).getById(ROOT_ORGANIZATION_ID);
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
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(tile.getElement());

    tile.shouldBe(visible);
    tile.nxSubHeader()
        .shouldBe(visible)
        .shouldHave(
            Condition
                .text(String.format("Configures the integration with an external SCM for the %s", rootOrg.getName())));
    tile.rows().shouldHave(size(1));

    tile.itemSubText().shouldNotBe(visible);
    tile.itemText()
        .shouldBe(visible)
        .shouldHave(Condition.text("Source Control not configured"));

    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, "TEST_TOKEN", SourceControlProvider.GITHUB);
    refresh();

    SidebarNavigation.closeNavigationSidebar();
    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(tile.getElement());

    tile.shouldBe(visible);
    tile.nxSubHeader()
        .shouldBe(visible)
        .shouldHave(
            Condition
                .text(String.format("Configures the integration with an external SCM for the %s", rootOrg.getName())));
    tile.rows().shouldHave(size(1));

    // Verify valid source control record exists here
    tile.itemText().shouldBe(visible).shouldHave(Condition.text("GitHub"));
    tile.itemSubText()
        .shouldBe(visible)
        .shouldHave(Condition.text("Provides the default source control configuration settings"));

    // eyesWatcher.eyesCheck("Valid source control configured"); https://sonatype.atlassian.net/browse/CLM-30559
  }

  @Test
  public void testSourceControlTile_LicensingAwareNoLicense() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FOUNDATION);
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    SourceControlTile tile = OwnerSummaryPage.sourceControlTile();

    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(hidden);

    tile.shouldBe(hidden);

    // eyesWatcher.eyesCheck("Source Control No License");
  }

  @Test
  public void testSourceControlTile_LicensingAwareNotificationsAndSourceControlOnly() {
    setFeatures(LicensedFeature.NOTIFICATIONS, LicensedFeature.SOURCE_CONTROL);
    refresh();
    SidebarNavigation.closeNavigationSidebar();
    SourceControlTile tile = OwnerSummaryPage.sourceControlTile();

    OwnerSummaryPage.summaryTile().sourceControlButton().shouldBe(visible);
    ScrollUtil.scrollIntoViewInstantly(tile.getElement());

    tile.shouldBe(visible);
    tile.nxSubHeader()
        .shouldBe(visible)
        .shouldHave(Condition.text(String
            .format("Configures the integration with an external SCM for the %s", rootOrg.getName())));
    tile.content().shouldBe(visible);

    tile.itemText().shouldBe(visible);
  }

  @Test
  public void testActionsDropdownOptions() {
    ActionDropDown.actionButton().click();
    ActionDropDown.copyOrgIdButton().shouldBe(visible);
    ActionDropDown.editOwner().shouldBe(visible);
    ActionDropDown.importPoliciesButton().shouldBe(visible);
    ActionDropDown.actions().shouldHave(size(3));

    eyesWatcher.eyesCheck("root organization actions dropdown");
  }

  @Test
  public void testBreadcrumb() {
    NxBreadcrumb breadcrumb = new NxBreadcrumb();
    assertThat(breadcrumb.links()).isEmpty();
  }
}
