/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView.ApplicationNode;
import com.sonatype.clm.testing.functional.elements.OwnerTreeView.OrganizationNode;
import com.sonatype.clm.testing.functional.pages.OrganizationManagementPage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

import com.codeborne.selenide.SelenideElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.cssClass;
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
    for (int i = 0; i < 5; i++) {
      Organization organization = tempEntity.newOrganization("Organization " + i + UUID.randomUUID());
      organizations.add(organization);
      for (int j = 0; j < 5; j++) {
        Application application = tempEntity
            .newApplication("Application " + j + UUID.randomUUID(), "id_" + UUID.randomUUID(), organization.getId());
        applications.add(application);
      }
    }
    refreshOrOpen(OrganizationManagementPage.URL);
    OwnerTreeView.organizationElements().shouldHaveSize(5);
  }

  @Test
  public void testInitialLoad() {
    List<OrganizationNode> organizationNodes = OwnerTreeView.organizations();
    for (int i = 0; i < 5; i++) {
      Organization organization = organizations.get(i);
      OrganizationNode organizationNode = organizationNodes.get(i);

      SelenideElement twisty = organizationNode.twisty();
      SelenideElement organizationElement = organizationNode.treeViewElement();

      twisty.shouldHave(cssClass(OrganizationNode.EXPAND_CLASS));
      organizationElement.shouldNotHave(cssClass(OrganizationNode.SELECTED_CLASS));
      organizationElement.isDisplayed();
      organizationElement.shouldHave(text(organization.getName()));

      twisty.click();
      twisty.shouldHave(cssClass(OrganizationNode.COLLAPSE_CLASS));
      organizationNode.applicationElements().shouldHaveSize(5);

      List<ApplicationNode> applicationNodes = organizationNode.applications();
      for (int j = 0; j < 5; j++) {
        Application application = applications.get(5 * i + j);
        ApplicationNode applicationNode = applicationNodes.get(j);

        SelenideElement applicationElement = applicationNode.treeViewElement();
        applicationElement.isDisplayed();
        applicationElement.shouldNotHave(cssClass(ApplicationNode.SELECTED_CLASS));
        applicationElement.shouldHave(text(application.getName()));
      }

      twisty.click();
      twisty.shouldHave(cssClass(OrganizationNode.EXPAND_CLASS));
    }
  }

  @Test
  public void testSelectOrganization() {
    OrganizationNode organizationNode = OwnerTreeView.organizations().get(0);
    SelenideElement twisty = organizationNode.twisty();
    SelenideElement treeViewElement = organizationNode.treeViewElement();

    treeViewElement.click();
    treeViewElement.shouldHave(cssClass(OrganizationNode.SELECTED_CLASS));
    twisty.shouldHave(cssClass(OrganizationNode.COLLAPSE_CLASS));
  }

  @Test
  public void testSelectApplication() {
    OrganizationNode organizationNode = OwnerTreeView.organizations().get(0);
    organizationNode.twisty().click();

    ApplicationNode applicationNode = organizationNode.applications().get(0);
    SelenideElement treeViewElement = applicationNode.treeViewElement();
    treeViewElement.click();
    treeViewElement.shouldHave(cssClass(ApplicationNode.SELECTED_CLASS));
  }

  @Test
  public void testOrganizationFilter() {
    Organization queriedOrganization = organizations.get(0);
    OwnerTreeView.filter().setValue(queriedOrganization.getName());
    assertSingleOrganizationVisible(queriedOrganization);

    queriedOrganization = organizations.get(1);
    OwnerTreeView.filter().setValue(queriedOrganization.getId());
    assertSingleOrganizationVisible(queriedOrganization);
  }

  @Test
  public void testApplicationFilter() {
    Application queriedApplication = applications.get(0);
    Organization applicationOrganization = organizations.get(0);
    OwnerTreeView.filter().setValue(queriedApplication.getName());
    assertSingleApplicationVisible(applicationOrganization, queriedApplication);

    queriedApplication = applications.get(5);
    applicationOrganization = organizations.get(1);
    OwnerTreeView.filter().setValue(queriedApplication.getPublicId());
    assertSingleApplicationVisible(applicationOrganization, queriedApplication);

    queriedApplication = applications.get(10);
    applicationOrganization = organizations.get(2);
    OwnerTreeView.filter().setValue(queriedApplication.getId());
    assertSingleApplicationVisible(applicationOrganization, queriedApplication);
  }

  private void assertSingleOrganizationVisible(Organization organization) {
    OwnerTreeView.organizationElements().shouldHaveSize(1);
    OrganizationNode organizationNode = OwnerTreeView.organizations().get(0);
    SelenideElement treeViewElement = organizationNode.treeViewElement();

    treeViewElement.shouldNotHave(cssClass(OrganizationNode.SELECTED_CLASS));
    treeViewElement.shouldHave(text(organization.getName()));
    organizationNode.applicationElements().shouldHaveSize(5);
  }

  private void assertSingleApplicationVisible(Organization organization, Application application) {
    OwnerTreeView.organizationElements().shouldHaveSize(1);
    OrganizationNode organizationNode = OwnerTreeView.organizations().get(0);
    SelenideElement treeViewElement = organizationNode.treeViewElement();

    treeViewElement.shouldNotHave(cssClass(OrganizationNode.SELECTED_CLASS));
    treeViewElement.shouldHave(text(organization.getName()));
    organizationNode.applicationElements().shouldHaveSize(1);

    ApplicationNode applicationNode = organizationNode.applications().get(0);
    applicationNode.treeViewElement().shouldHave(text(application.getName()));
  }
}
