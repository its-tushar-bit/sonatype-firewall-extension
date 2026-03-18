/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.ResetPasswordModal;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class UserManagementPage
    extends BasicElement<UserManagementPage>
{
  private static final String ROOT_SELECTOR = "#user-management";

  public static String url() {
    return BaseUrl.resolvePageUrl("/users");
  }

  public static String firewallUrl() {
    return BaseUrl.resolvePageUrl("/firewall/users");
  }

  public UserManagementPage() {
    super(ROOT_SELECTOR);
  }

  public Button newUserButton() {
    return new Button("#create-user");
  }

  public ElementsCollection userItems() {
    return children(".nx-list__item .nx-list__link");
  }

  public SelenideElement currentUser() {
    return child(".iq-user-list-item-current");
  }

  public SelenideElement loadError() {
    return child(".nx-alert--load-error");
  }

  public ResetPasswordModal resetPasswordModal() {
    return new ResetPasswordModal();
  }

  public NewUserForm newUserForm() {
    return new NewUserForm("#user-form");
  }

  public static class NewUserForm
      extends BasicElement<NewUserForm>
  {
    NewUserForm(final String... selectors) {
      super(selectors);
    }

    public Button saveButton() {
      return new Button(".nx-form__submit-btn");
    }

    public SelenideElement firstNameInput() {
      return child("#firstName");
    }

    public SelenideElement lastNameInput() {
      return child("#lastName");
    }

    public SelenideElement emailInput() {
      return child("#email");
    }

    public SelenideElement usernameInput() {
      return child("#username");
    }

    public SelenideElement passwordInput() {
      return child("#password");
    }

    public SelenideElement passwordValidateInput() {
      return child("#passwordValidate");
    }
  }

  public EditUserForm editUserForm() {
    return new EditUserForm("#user-edit");
  }

  public static class EditUserForm
      extends BasicElement<EditUserForm>
  {
    EditUserForm(final String... selectors) {
      super(selectors);
    }

    public Button saveButton() {
      return new Button(".nx-form__submit-btn");
    }

    public Button deleteButton() {
      return new Button("#delete-user");
    }

    public Button resetPasswordButton() {
      return new Button("#reset-password");
    }

    public SelenideElement firstNameInput() {
      return child("#firstName");
    }

    public SelenideElement lastNameInput() {
      return child("#lastName");
    }

    public SelenideElement emailInput() {
      return child("#email");
    }
  }

  public CopyToClipboardModal copyToClipboardModal() {
    return new CopyToClipboardModal();
  }

  public static class CopyToClipboardModal
      extends BasicElement<CopyToClipboardModal>
  {
    public CopyToClipboardModal() {
      super("#copy-password-modal");
    }

    public SelenideElement ok() {
      return child(".nx-form__submit-btn");
    }

    public SelenideElement newPassword() {
      return child(".nx-text-input__input");
    }
  }
}
