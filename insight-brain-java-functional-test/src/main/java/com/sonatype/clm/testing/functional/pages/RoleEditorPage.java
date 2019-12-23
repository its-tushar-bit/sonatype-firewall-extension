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

import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class RoleEditorPage
    extends BasicElement<RoleEditorPage>
{
  public static final String url() {
    return BaseUrl.resolvePageUrl("/roles");
  }

  public RoleEditorPage() {
    super("#role-editor");
  }

  public SelenideElement pageTitle() {
    return child("#role-title");
  }

  public SelenideElement deleteRole() {
    return child("#delete-role");
  }

  public SelenideElement deleteConfirm() {
    return $(".modal.in .btn-primary");
  }

  public SelenideElement nameEditor() {
    return child("#roleName");
  }

  public SelenideElement namePopover() {
    return child("#roleName-popover");
  }

  public SelenideElement descriptionEditor() {
    return child("#roleDescription");
  }

  public SelenideElement cancel() {
    return child("#role-details-cancel");
  }

  public SelenideElement save() {
    return child("#role-details-save");
  }

  public ElementsCollection permissionCategories() {
    return children(".iq-list--role-permissions");
  }

  public PermissionCategory permissionCategory(String categoryName) {
    return new PermissionCategory(".test-permission-category-" + categoryName);
  }

  public class PermissionCategory
      extends BasicElement<PermissionCategory>
  {
    PermissionCategory(final String... selectors) {
      super(selectors);
    }
  }

  public ElementsCollection permissions(String categoryName) {
    return children(".test-permissions-" + categoryName, ".iq-list__item");
  }

  public Permission permission(String categoryName, int num) {
    return new Permission(".test-permissions-" + categoryName, ".iq-list__item", nthChild(num + 1));
  }

  public class Permission
      extends BasicElement<Permission>
  {
    Permission(final String... selectors) {
      super(selectors);
    }

    public SelenideElement name() {
      return child("label > span");
    }

    public SelenideElement description() {
      return child(".iq-role-description");
    }

    public ToggleSwitch toggleSwitch() {
      return new ToggleSwitch(childSelector(".toggle-checkbox"));
    }
  }

  public class ToggleSwitch
      extends BasicElement<ToggleSwitch>
  {
    ToggleSwitch(final String... selectors) {
      super(selectors);
    }

    public SelenideElement toggle() {
      return child(".toggle");
    }

    public SelenideElement toggleCheckbox() {
      return toggle().find("input");
    }

    public SelenideElement label() {
      return child("span:not(.toggle-handle)");
    }
  }
}
