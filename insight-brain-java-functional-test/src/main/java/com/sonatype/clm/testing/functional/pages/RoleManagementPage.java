/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class RoleManagementPage
    extends BasicElement<RoleManagementPage>
{
  public static final String url() {
    return BaseUrl.resolvePageUrl("/roles");
  }

  public RoleManagementPage() {
    super("#role-management");
  }

  public SelenideElement pageTitle() {
    return child(".iq-tile-header__title h2");
  }

  public ElementsCollection builtinRoles() {
    return children("#builtin-roles .iq-list__item");
  }

  public ElementsCollection customRoles() {
    return children("#custom-roles .role-name-list-item");
  }

  public Role builtinRole(int num) {
    return new Role("#builtin-roles .iq-list__item", nthChild(num + 1));
  }

  public Role customRole(int num) {
    return new Role("#custom-roles .role-name-list-item", nthChild(num + 1));
  }

  public class Role
      extends BasicElement<RoleManagementPage.Role>
  {
    Role(final String... selectors) {
      super(selectors);
    }

    public SelenideElement name() {
      return child(".role-name");
    }

    public SelenideElement description() {
      return child(".iq-list__subtext");
    }
  }

  public Button createRole() {
    return new Button("#create-role");
  }
}
