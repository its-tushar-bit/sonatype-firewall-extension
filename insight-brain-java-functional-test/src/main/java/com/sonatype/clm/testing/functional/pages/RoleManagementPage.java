/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class RoleManagementPage
    extends BasicElement<RoleManagementPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/roles");
  }

  public RoleManagementPage() {
    super("#role-management");
  }

  public SelenideElement componentTitle() {
    return child(".nx-tile-header__title h2");
  }

  public ElementsCollection builtinRoles() {
    return children("#builtin-roles .nx-list__item");
  }

  public ElementsCollection customRoles() {
    return children("#custom-roles .nx-list__item");
  }

  public SelenideElement customRolesDefaultMessage() {
    return child("#custom-roles .nx-list__item .nx-list__text");
  }

  public Role builtinRole(int num) {
    int elNum = num + 1;
    return new Role("#builtin-roles .nx-list__item:nth-child(" + elNum + ") .nx-list__link");
  }

  public Role customRole(int num) {
    int elNum = num + 1;
    return new Role("#custom-roles .nx-list__item:nth-child(" + elNum + ") .nx-list__link");
  }

  public class Role
      extends BasicElement<RoleManagementPage.Role>
  {
    Role(final String... selectors) {
      super(selectors);
    }

    public SelenideElement name() {
      return child(".nx-list__text");
    }

    public SelenideElement description() {
      return child(".nx-list__subtext");
    }
  }

  public SelenideElement createRole() {
    return child("#create-role");
  }
}
