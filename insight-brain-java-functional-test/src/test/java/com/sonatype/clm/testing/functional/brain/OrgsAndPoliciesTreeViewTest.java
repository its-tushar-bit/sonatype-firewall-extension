/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.List;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.functional.pages.OwnerTreeViewPage;
import com.sonatype.clm.testing.functional.utils.NameSupplierDictionary;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.text;

public class OrgsAndPoliciesTreeViewTest
    extends AbstractFunctionalTest
{
  private List<Organization> organizations;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(DashboardPage.url());
    loginAsAdmin();
  }

  @Before
  public void init() {
    organizations = tempEntity.newRelatedOrganizationsAsList(2, 3, 3, new NameSupplierDictionary());
    refreshOrOpen(OwnerTreeViewPage.url());

    // There is a delay in the organizations being available to the tree view so we need to wait for that before
    // refreshing the tree. We can't use shouldBe() here because its the backend data we are waiting on.
    try {
      Thread.sleep(1000L);
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    refresh();
  }

  @Test
  public void testOwnerTree() {
    ElementsCollection treeItems = OwnerTreeViewPage.tree().clickableTreeItems();
    treeItems.shouldHave(size(57));

    eyesWatcher.eyesCheck("owner tree view");
  }

  @Test
  public void testOwnerTree_linking() {
    ElementsCollection treeItems = OwnerTreeViewPage.tree().clickableTreeItems();
    SelenideElement itemToClick = treeItems.get(1);
    String organizationName = itemToClick.text();
    Organization organization =
        organizations.stream().filter(org -> org.getName().equals(organizationName)).findFirst().get();
    ScrollUtil.scrollIntoView(itemToClick);
    itemToClick.click();

    waitUntilUrl(OwnerSummaryPage.url(OwnerType.ORGANIZATION, organization.getId()));

    SelenideElement title = OwnerSummaryPage.summaryTile().name();
    title.shouldHave(text(organizationName));
  }

  @Test
  public void testOwnerTree_breadcrumbs() {
    ElementsCollection treeItems = OwnerTreeViewPage.tree().clickableTreeItems();
    SelenideElement itemToClick = treeItems.get(1);
    String organizationName = itemToClick.text();
    Organization organization =
        organizations.stream().filter(org -> org.getName().equals(organizationName)).findFirst().get();

    ScrollUtil.scrollIntoView(itemToClick);
    itemToClick.click();

    waitUntilUrl(OwnerSummaryPage.url(OwnerType.ORGANIZATION, organization.getId()));

    OwnerTreeViewPage.treeViewButton().click();
    waitUntilUrl(OwnerTreeViewPage.url());

    OwnerTreeViewPage.backButton().shouldHave(text("Back to " + organization.getName()));

    refresh();
    OwnerTreeViewPage.backButton().shouldHave(text("Back to Root Organization"));
  }

  @Test
  public void testOwnerTreeFilter() {
    ElementsCollection treeItems = OwnerTreeViewPage.tree().clickableTreeItems();
    treeItems.shouldHave(size(57));

    OwnerTreeViewPage.ownerFilterInput().setValue("12");
    treeItems = OwnerTreeViewPage.tree().clickableTreeItems();
    treeItems.shouldHave(size(10));

    OwnerTreeViewPage.ownerFilterInput().setValue("asdfg");
    treeItems = OwnerTreeViewPage.tree().clickableTreeItems();
    treeItems.shouldHave(size(0));
    OwnerTreeViewPage.emptyTreeMessage().shouldHave(text("No matching results"));
  }
}
