/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class OrgsAndPoliciesSidebar
    extends BasicElement<OrgsAndPoliciesSidebar>
{
  private final NxCollapsible organizationList;

  private final NxCollapsible applicationList;

  private final NxCollapsible repoManagersList;

  private final NxCollapsible repositoryList;

  public OrgsAndPoliciesSidebar() {
    super(".iq-orgs-and-policies-summary-sidebar");
    organizationList = new NxCollapsible(childSelector("#organizations-collapsible"));
    applicationList = new NxCollapsible(childSelector("#applications-collapsible"));
    repoManagersList = new NxCollapsible(childSelector("#repository-managers-collapsible"));
    repositoryList = new NxCollapsible(childSelector("#repositories-collapsible"));
  }

  public SelenideElement selectedOrg() {
    return child(".iq-selected-org");
  }

  public SelenideElement repositories() {
    return child(".iq-repositories-link");
  }

  public SelenideElement filterInput() {
    return child("#owner-sidebar-filter");
  }

  public NxCollapsible getRepositoryList() {
    return repositoryList;
  }

  public NxCollapsible getRepoManagerList() {
    return repoManagersList;
  }

  public NxCollapsible getOrganizationList() {
    return organizationList;
  }

  public NxCollapsible getApplicationList() {
    return applicationList;
  }

  public SelenideElement getApplicationPlusIcon() {
    return applicationList.applicationsPlusIcon();
  }

  public SelenideElement getNewApplicationButton() {
    return applicationList.applicationsDropdownMenuItems(0);
  }

  public SelenideElement getOrganizationPlusIcon() {
    return organizationList.organizationPlusIcon();
  }

  public ElementsCollection getChildOrganizations() {
    return organizationList.children();
  }

  public SelenideElement getImportApplicationsButton() {
    return applicationList.applicationsDropdownMenuItems(1);
  }

  public OwnerItem getOrganizationLink(int index) {
    return new OwnerItem(organizationList.children().get(index));
  }

  public OwnerItem getApplicationLink(int index) {
    return new OwnerItem(applicationList.children().get(index));
  }

  public OwnerItem getRepositoryManagerLink(int index) {
    return new OwnerItem(repoManagersList.children().get(index));
  }

  public OwnerItem getRepositoryLink(int index) {
    return new OwnerItem(repositoryList.children().get(index));
  }

  public SelenideElement getLoadingSpinner() {
    return child(".nx-loading-spinner");
  }

  public class OwnerItem
      extends BasicElement<OwnerItem>
  {
    public OwnerItem(SelenideElement element) {
      super(".nx-collapsible-items__child");
      this.element = element;
    }

    public SelenideElement ownerName() {
      return element.find(".iq-owner-name");
    }

    public SelenideElement orgCounter() {
      return element.find(".iq-children-counter");
    }
  }
}
