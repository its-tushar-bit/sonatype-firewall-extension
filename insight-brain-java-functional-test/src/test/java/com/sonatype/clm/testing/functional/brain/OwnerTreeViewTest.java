/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView.OrganizationNode;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.interactions.Actions;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;

public class OwnerTreeViewTest
    extends AbstractFunctionalTest
{
  private List<Organization> organizations = new ArrayList<>();

  private List<Application> applications = new ArrayList<>();

  private final String browserName = System.getProperty("browser");

  @BeforeClass
  public static void startup() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Before
  public void before() {
    ImmutableMap<String, List<String>> organizations = ImmutableMap.<String, List<String>> builder()
        // At least one name alphabetically before and after Root Organization to test Root Organization is extracted
        .put("Silver Squadron", Arrays.asList("Garven Dreis", "Biggs Darklighter", "Luke Skywalker"))
        .put("Green Squadron", Arrays.asList("Arvel Crynyd", "Jake Farrell"))
        .put("Blue Squadron And Some Other Text To Force Overflow",
            Arrays.asList("Merrick Simms And Some Other Text To Force Overflow"))
        .put("Orange Squadron", Arrays.asList("PaulQuincyRandolph"))
        .build();

    for (Entry<String, List<String>> organizationMeta : organizations.entrySet()) {
      Organization organization = tempEntity.newOrganization(organizationMeta.getKey());
      this.organizations.add(organization);

      for (String applicationName : organizationMeta.getValue()) {
        Application application = tempEntity.newApplication(applicationName, applicationName.replaceAll("\\s", ""),
            organization.getId());
        this.applications.add(application);
      }
    }

    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    OwnerTreeView.organizationElements().shouldHaveSize(organizations.size());
  }

  @Test
  public void testInitialLoad() {
    assertOrganizationLoaded(OwnerTreeView.organization(0), "Blue Squadron And Some Other Text To Force Overflow",
        "Merrick Simms And Some Other Text To Force Overflow");
    assertOrganizationLoaded(OwnerTreeView.organization(1), "Green Squadron", "Arvel Crynyd", "Jake Farrell");
    assertOrganizationLoaded(OwnerTreeView.organization(2), "Orange Squadron", "PaulQuincyRandolph");
    assertOrganizationLoaded(OwnerTreeView.organization(3), "Silver Squadron", "Biggs Darklighter", "Garven Dreis",
        "Luke Skywalker");
  }

  private void assertOrganizationLoaded(OrganizationNode organizationNode,
                                        String organizationName,
                                        String... applicationNames)
  {
    SelenideElement twisty = organizationNode.twisty();
    SelenideElement organizationElement = organizationNode.treeViewElement();

    twisty.shouldBe(CLM.EXPANDED);
    organizationElement.shouldNotBe(CLM.SELECTED, CLM.DISABLED);
    organizationElement.shouldNotHave(OrganizationNode.DISABLED_TOOLTIP_ATTRIBUTE);
    organizationElement.shouldBe(visible);
    organizationNode.organizationName().shouldHave(text(organizationName)).hover();
    eyesWatcher.eyesCheck("Conditional tooltip rendering for organizations");
    checkTooltipRenderedOnlyOnOverflow(organizationName);

    twisty.click();
    twisty.shouldBe(CLM.COLLAPSED);
    organizationNode.applicationElements().shouldHaveSize(applicationNames.length);

    for (int i = 0; i < applicationNames.length; i++) {
      organizationNode.application(i).shouldNotBe(CLM.SELECTED).shouldHave(text(applicationNames[i])).hover();
      // For whatever reason the firefox driver misses the hover point. To work around this we nudge the cursor over a
      // bit so that the hover kicks in.
      if ("firefox".equals(browserName)) {
        new Actions(WebDriverRunner.getAndCheckWebDriver()).moveByOffset(1, 0).perform();
      }
      checkTooltipRenderedOnlyOnOverflow(applicationNames[i]);
    }

    twisty.click();
    twisty.shouldBe(CLM.EXPANDED);
  }

  @Test
  public void testSelectOrganization() {
    OrganizationNode organizationNode = OwnerTreeView.organization(0);
    SelenideElement twisty = organizationNode.twisty();
    SelenideElement treeViewElement = organizationNode.treeViewElement();

    // visual test with applitools - step 1
    eyesWatcher.eyesCheck();

    treeViewElement.click();
    treeViewElement.shouldBe(CLM.SELECTED);
    twisty.shouldBe(CLM.COLLAPSED);

    // visual test with applitools - step 2
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testSelectApplication() {
    OrganizationNode organizationNode = OwnerTreeView.organization(0);
    SelenideElement organizationTreeViewElement = organizationNode.treeViewElement();
    organizationTreeViewElement.click();
    organizationTreeViewElement.shouldBe(CLM.SELECTED);

    SelenideElement applicationNode = organizationNode.application(0);
    applicationNode.click();
    applicationNode.shouldBe(CLM.SELECTED);
    organizationTreeViewElement.shouldNotBe(CLM.SELECTED);
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testSelectedRepositories() {
    SelenideElement repositoriesTreeViewElement = OwnerTreeView.repositories();
    repositoriesTreeViewElement.shouldBe(visible).click();
    repositoriesTreeViewElement.shouldBe(CLM.SELECTED);
  }

  @Test
  public void testSelectedRepositoriesNoPermissions() {
    createUser();
    logout();
    login();

    refreshOrOpen(OwnerSummaryPage.url());
    SelenideElement repositoriesTreeViewElement = OwnerTreeView.repositories();
    repositoriesTreeViewElement.shouldBe(hidden);

    logout();
    loginAsAdmin();
  }

  @Test
  public void testOrganizationSubstringFilter() {
    OwnerTreeView.filter().setValue("Green Sq");
    assertSingleOrganizationVisible("Green Squadron", 2);
  }

  @Test
  public void testApplicationFuzzyFilter() {
    OwnerTreeView.filter().setValue("PaulQuincyRandalph");
    assertSingleApplicationVisible("Orange Squadron", "PaulQuincyRandolph");
    eyesWatcher.eyesCheck();
  }

  @Test
  public void testApplicationFilter() {
    Application queriedApplication = applications.get(0);
    Organization applicationOrganization = organizations.get(0);
    OwnerTreeView.filter().setValue(queriedApplication.getName());
    assertSingleApplicationVisible(applicationOrganization.getName(), queriedApplication.getName());
  }

  @Test
  public void testShowApplicationParentWithoutPermissions() {
    createUser();
    Organization organization = tempEntity.newOrganization("Unpermitted Parent Org");
    Application application = tempEntity.newApplication("No Parent Permissions", "No_Parent_Permissions",
        organization.getId());
    grantPermissions(getUsername(), application.getId(), Permission.READ);

    logout();
    login();

    refreshOrOpen(OwnerSummaryPage.url());
    OwnerTreeView.organizationElements().shouldHaveSize(1);
    OrganizationNode parentNode = OwnerTreeView.organization(0);
    parentNode.treeViewElement().shouldBe(CLM.DISABLED);
    parentNode.organizationName().shouldHave(text("Unpermitted Parent Org"));
    parentNode.organizationName().shouldHave(OrganizationNode.DISABLED_TOOLTIP_ATTRIBUTE);
    parentNode.twisty().click();
    parentNode.twisty().shouldBe(CLM.COLLAPSED);
    parentNode.organizationName().hover();
    Tooltip.get().shouldBe(visible).shouldHave(text(OrganizationNode.DISABLED_TOOLTIP_CONTENT));
    eyesWatcher.eyesCheck();

    parentNode.applicationElements().shouldHaveSize(1);
    parentNode.application(0).shouldHave(text("No Parent Permissions"));

    logout();
    loginAsAdmin();
  }

  @Test
  public void testOrgCantBeCollapsedWIthSelectedChild() {
    OrganizationNode organizationNode = OwnerTreeView.organization(0);
    organizationNode.twisty().shouldBe(CLM.EXPANDED);
    SelenideElement organizationTreeViewElement = organizationNode.treeViewElement();
    organizationTreeViewElement.click();
    organizationTreeViewElement.shouldBe(CLM.SELECTED);
    organizationTreeViewElement.shouldNotHave(OrganizationNode.CHILD_SELECTED);
    organizationNode.twisty().shouldBe(CLM.COLLAPSED);

    SelenideElement applicationNode = organizationNode.application(0);
    applicationNode.click();
    applicationNode.shouldBe(CLM.SELECTED);
    organizationTreeViewElement.shouldNotBe(CLM.SELECTED);
    organizationTreeViewElement.shouldHave(OrganizationNode.CHILD_SELECTED);

    organizationNode.twisty().click();
    organizationNode.twisty().shouldBe(CLM.COLLAPSED);
    applicationNode.shouldBe(Condition.visible);
  }

  private void assertSingleOrganizationVisible(String organizationName, int applicationCount) {
    OwnerTreeView.organizationElements().shouldHaveSize(1);
    OrganizationNode organizationNode = OwnerTreeView.organization(0);
    SelenideElement treeViewElement = organizationNode.treeViewElement();

    treeViewElement.shouldNotBe(CLM.SELECTED);
    organizationNode.organizationName().shouldHave(attribute("tooltip-text", organizationName));
    organizationNode.organizationName().shouldHave(text(organizationName));
    organizationNode.applicationElements().shouldHaveSize(applicationCount);
  }

  private void assertSingleApplicationVisible(String organizationName, String applicationName) {
    OwnerTreeView.organizationElements().shouldHaveSize(1);
    OrganizationNode organizationNode = OwnerTreeView.organization(0);
    SelenideElement treeViewElement = organizationNode.treeViewElement();

    treeViewElement.shouldNotBe(CLM.SELECTED);
    treeViewElement.shouldHave(text(organizationName));
    organizationNode.applicationElements().shouldHaveSize(1);

    SelenideElement applicationNode = organizationNode.application(0);
    applicationNode.shouldHave(attribute("tooltip-text", applicationName));
    applicationNode.shouldHave(text(applicationName));
  }

  /**
   * Check that tooltips only appear on overflow using arbitrarily chosen cut-over length.
   */
  private void checkTooltipRenderedOnlyOnOverflow(String ownerName) {
    if (ownerName.length() > 40) {
      Tooltip.get().shouldBe(visible).shouldHave(text(ownerName));
    }
    else {
      Tooltip.get().shouldNotBe(visible);
    }
  }
}
