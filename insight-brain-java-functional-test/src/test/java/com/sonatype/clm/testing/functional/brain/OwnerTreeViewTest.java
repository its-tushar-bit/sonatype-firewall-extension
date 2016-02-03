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
import java.util.UUID;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView.ApplicationNode;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView.OrganizationNode;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.open;

public class OwnerTreeViewTest
    extends AbstractFunctionalTest
{
  private List<Organization> organizations = new ArrayList<>();
  private List<Application> applications = new ArrayList<>();

  @BeforeClass
  public static void startup() {
    open(OrganizationManagementPage.URL);
    loginAsAdmin();
  }

  @Before
  public void before() {
    ImmutableMap<String, List<String>> organizations = ImmutableMap.<String, List<String>> builder()
        .put("Red Squadron", Arrays.asList("Garven Dreis", "Biggs Darklighter", "Luke Skywalker"))
        .put("Green Squadron", Arrays.asList("Arvel Crynyd", "Jake Farrell"))
        .put("Blue Squadron", Arrays.asList("Merrick Simms")).build();

    for (Entry<String, List<String>> organizationMeta : organizations.entrySet()) {
      Organization organization = tempEntity.newOrganization(organizationMeta.getKey());
      this.organizations.add(organization);

      for (String applicationName : organizationMeta.getValue()) {
        Application application = tempEntity.newApplication(applicationName, UUID.randomUUID().toString(),
            organization.getId());
        this.applications.add(application);
      }
    }

    refreshOrOpen(OrganizationManagementPage.URL);
    OwnerTreeView.organizationElements().shouldHaveSize(organizations.size());
  }

  @Test
  public void testInitialLoad() {
    List<OrganizationNode> organizationNodes = OwnerTreeView.organizations();
    assertOrganizationLoaded(organizationNodes.get(0), "Blue Squadron", "Merrick Simms");
    assertOrganizationLoaded(organizationNodes.get(1), "Green Squadron", "Arvel Crynyd", "Jake Farrell");
    assertOrganizationLoaded(organizationNodes.get(2), "Red Squadron", "Biggs Darklighter", "Garven Dreis",
        "Luke Skywalker");
  }

  private void assertOrganizationLoaded(OrganizationNode organizationNode,
                                        String organizationName,
                                        String... applicationNames)
  {
    SelenideElement twisty = organizationNode.twisty();
    SelenideElement organizationElement = organizationNode.treeViewElement();

    twisty.shouldBe(CLM.EXPANDED);
    organizationElement.shouldNotBe(CLM.SELECTED);
    organizationElement.shouldNotBe(CLM.DISABLED);
    organizationElement.shouldNotHave(OrganizationNode.DISABLED_TOOLTIP_ATTRIBUTE);
    organizationElement.isDisplayed();
    organizationNode.organizationName().shouldHave(text(organizationName));

    twisty.click();
    twisty.shouldBe(CLM.COLLAPSED);
    organizationNode.applicationElements().shouldHaveSize(applicationNames.length);

    List<ApplicationNode> applicationNodes = organizationNode.applications();
    for (int i = 0; i < applicationNames.length; i++) {
      ApplicationNode applicationNode = applicationNodes.get(i);

      SelenideElement applicationElement = applicationNode.treeViewElement();
      applicationElement.isDisplayed();
      applicationElement.shouldNotBe(CLM.SELECTED);
      applicationElement.shouldHave(text(applicationNames[i]));
    }

    twisty.click();
    twisty.shouldBe(CLM.EXPANDED);
  }

  @Test
  public void testSelectOrganization() {
    OrganizationNode organizationNode = OwnerTreeView.organizations().get(0);
    SelenideElement twisty = organizationNode.twisty();
    SelenideElement treeViewElement = organizationNode.treeViewElement();

    treeViewElement.click();
    treeViewElement.shouldBe(CLM.SELECTED);
    twisty.shouldBe(CLM.COLLAPSED);
  }

  @Test
  public void testSelectApplication() {
    OrganizationNode organizationNode = OwnerTreeView.organizations().get(0);
    SelenideElement organizationTreeViewElement = organizationNode.treeViewElement();
    organizationTreeViewElement.click();
    organizationTreeViewElement.shouldBe(CLM.SELECTED);

    ApplicationNode applicationNode = organizationNode.applications().get(0);
    SelenideElement applicationTreeViewElement = applicationNode.treeViewElement();
    applicationTreeViewElement.click();
    applicationTreeViewElement.shouldBe(CLM.SELECTED);
    organizationTreeViewElement.shouldNotBe(CLM.SELECTED);
  }

  @Test
  public void testOrganizationSubstringFilter() {
    OwnerTreeView.filter().setValue("Green Sq");
    assertSingleOrganizationVisible("Green Squadron", 2);
  }

  @Test
  public void testApplicationFuzzyFilter() {
    OwnerTreeView.filter().setValue("Skiwalkr");
    assertSingleApplicationVisible("Red Squadron", "Luke Skywalker");
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
    Organization organization = tempEntity.newOrganization("Parent Organization No Permission");
    Application application = tempEntity.newApplication("No Parent Permissions", "No_Parent_Permissions",
        organization.getId());
    grantPermissions(getUsername(), application.getId(), Permission.READ);

    logout();
    login();

    refreshOrOpen(OrganizationManagementPage.URL);
    OwnerTreeView.organizationElements().shouldHaveSize(1);
    OrganizationNode parentNode = OwnerTreeView.organizations().get(0);
    parentNode.treeViewElement().shouldBe(CLM.DISABLED);
    parentNode.organizationName().shouldHave(text("Parent Organization No Permission"));
    parentNode.organizationName().shouldHave(OrganizationNode.DISABLED_TOOLTIP_ATTRIBUTE);
    parentNode.organizationName().hover();
    parentNode.popup().isDisplayed();
    parentNode.popup().shouldHave(text(OrganizationNode.DISABLED_TOOLTIP_CONTENT));
    parentNode.twisty().click();
    parentNode.twisty().shouldBe(CLM.COLLAPSED);

    parentNode.applicationElements().shouldHaveSize(1);
    ApplicationNode childNode = parentNode.applications().get(0);
    childNode.treeViewElement().shouldHave(text("No Parent Permissions"));

    logout();
    loginAsAdmin();
  }

  @Test
  public void testOrgCantBeCollapsedWIthSelectedChild() {
    OrganizationNode organizationNode = OwnerTreeView.organizations().get(0);
    organizationNode.twisty().shouldBe(CLM.EXPANDED);
    SelenideElement organizationTreeViewElement = organizationNode.treeViewElement();
    organizationTreeViewElement.click();
    organizationTreeViewElement.shouldBe(CLM.SELECTED);
    organizationTreeViewElement.shouldNotHave(OrganizationNode.CHILD_SELECTED);
    organizationNode.twisty().shouldBe(CLM.COLLAPSED);

    ApplicationNode applicationNode = organizationNode.applications().get(0);
    SelenideElement applicationTreeViewElement = applicationNode.treeViewElement();
    applicationTreeViewElement.click();
    applicationTreeViewElement.shouldBe(CLM.SELECTED);
    organizationTreeViewElement.shouldNotBe(CLM.SELECTED);
    organizationTreeViewElement.shouldHave(OrganizationNode.CHILD_SELECTED);

    organizationNode.twisty().click();
    organizationNode.twisty().shouldBe(CLM.COLLAPSED);
    applicationTreeViewElement.shouldBe(Condition.visible);
  }

  private void assertSingleOrganizationVisible(String organizationName, int applicationCount) {
    OwnerTreeView.organizationElements().shouldHaveSize(1);
    OrganizationNode organizationNode = OwnerTreeView.organizations().get(0);
    SelenideElement treeViewElement = organizationNode.treeViewElement();

    treeViewElement.shouldNotBe(CLM.SELECTED);
    treeViewElement.shouldNotHave(OrganizationNode.DISABLED_TOOLTIP_ATTRIBUTE);
    organizationNode.organizationName().shouldHave(text(organizationName));
    organizationNode.applicationElements().shouldHaveSize(applicationCount);
  }

  private void assertSingleApplicationVisible(String organizationName, String applicationName) {
    OwnerTreeView.organizationElements().shouldHaveSize(1);
    OrganizationNode organizationNode = OwnerTreeView.organizations().get(0);
    SelenideElement treeViewElement = organizationNode.treeViewElement();

    treeViewElement.shouldNotBe(CLM.SELECTED);
    treeViewElement.shouldHave(text(organizationName));
    organizationNode.applicationElements().shouldHaveSize(1);

    ApplicationNode applicationNode = organizationNode.applications().get(0);
    SelenideElement applicationTreeViewElement = applicationNode.treeViewElement();
    applicationTreeViewElement.isDisplayed();
    applicationTreeViewElement.shouldHave(text(applicationName));
  }
}
