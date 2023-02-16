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

  public OrgsAndPoliciesSidebar() {
    super(".iq-orgs-and-policies-summary-sidebar");
    organizationList = new NxCollapsible(childSelector("#organizations-collapsible"));
    applicationList = new NxCollapsible(childSelector("#applications-collapsible"));
  }

  public SelenideElement selectedOrg() {
    return child(".iq-selected-org");
  }

  public SelenideElement repositories() {
    return child(".iq-repositories-link");
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
