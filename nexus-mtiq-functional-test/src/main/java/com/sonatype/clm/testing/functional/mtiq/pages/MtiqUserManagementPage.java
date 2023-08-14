/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.pages.UserManagementPage;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class MtiqUserManagementPage
    extends UserManagementPage
{
  @Override
  public Button newUserButton() {
    return new Button("#invite-user");
  }

  @Override
  public ElementsCollection userItems() {
    return children(".nx-list__item");
  }

  public UserItem userItem(int i) {
    return new UserItem(childSelector(".nx-list__item:nth-child(" + (i + 1) + ")"));
  }

  public static class UserItem
      extends BasicElement<UserItem>
  {
    public UserItem(String selector) {
      super(selector);
    }

    public SelenideElement deleteBtn() {
      return child(".iq-user-list-item__delete-btn");
    }
  }
}
