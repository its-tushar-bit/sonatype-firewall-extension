/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxToggle;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class RoleEditorPage
    extends BasicElement<RoleEditorPage>
{
  public static String url() {
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
    return $(".nx-modal .nx-btn--primary");
  }

  public SelenideElement nameEditor() {
    return child("#roleName");
  }

  public SelenideElement nameAlert() {
    return child(".nx-field-validation-message");
  }

  public SelenideElement descriptionEditor() {
    return child("#roleDescription");
  }

  public SelenideElement cancel() {
    return child("#role-details-cancel");
  }

  public SelenideElement save() {
    return child(".nx-form__submit-btn");
  }

  public ElementsCollection permissionCategories() {
    return children(".nx-tile-subsection");
  }

  public PermissionCategory permissionCategory(String categoryName) {
    return new PermissionCategory("#test-permission-category-" + categoryName);
  }

  public class PermissionCategory
      extends BasicElement<PermissionCategory>
  {
    PermissionCategory(final String... selectors) {
      super(selectors);
    }

    public SelenideElement title() {
      return child(".nx-tile-subsection__header", ".nx-h3");
    }
  }

  public ElementsCollection permissions(String categoryName) {
    return children(".test-permissions-" + categoryName, ".iq-role-editor-permission-group__col", ".nx-toggle--no-gap");
  }

  public NxToggle permission(String categoryName, int num, boolean firstColumn) {
    return new NxToggle(childSelector(".test-permissions-" + categoryName,
        ".iq-role-editor-permission-group__col", nthChild(firstColumn ? 1 : 2),
        ".nx-toggle--no-gap", nthChild(num + 1)));
  }

  public class Permission
      extends BasicElement<Permission>
  {
    Permission(final String... selectors) {
      super(selectors);
    }

    public SelenideElement name() {
      return child("span");
    }

    public SelenideElement description() {
      return child(".nx-toggle__content");
    }

    public NxToggle toggleSwitch() {
      return new NxToggle(childSelector(".toggle-checkbox"));
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
