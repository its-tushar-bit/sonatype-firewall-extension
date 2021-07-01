/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import java.util.List;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.ResetPasswordModal;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class UserManagementPage
    extends BasicElement<UserManagementPage>
{
  private static final String ROOT_SELECTOR = ".test-user-management";

  public static String url() {
    return BaseUrl.resolvePageUrl("/users");
  }

  public UserManagementPage() {
    super(ROOT_SELECTOR);
  }

  public Button newUserButton() {
    return new Button("#user-new");
  }

  public ElementsCollection headers() {
    return children("a.accordion-toggle");
  }

  public SelenideElement currentUser() {
    return child(".test-current-user");
  }

  public SummarySection summarySection() {
    return new SummarySection(ROOT_SELECTOR, ".accordion-inner");
  }

  public List<SelenideElement> resetPasswordButtons() {
    return children(".tm-iq-user-reset-password");
  }

  public List<SelenideElement> editUserButtons() {
    return children(".tm-iq-user-edit");
  }

  public List<SelenideElement> deleteUserButtons() {
    return children(".tm-iq-user-remove");
  }

  public ResetPasswordModal resetPasswordModal() {
    return new ResetPasswordModal();
  }

  public static class SummarySection
      extends BasicElement<SummarySection>
  {
    SummarySection(final String... selectors) {
      super(selectors);
    }

    public SelenideElement firstName() {
      return child(".test-user-first-name");
    }

    public SelenideElement lastName() {
      return child(".test-user-last-name");
    }

    public SelenideElement email() {
      return child(".test-user-email");
    }
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
    EditUserForm(final String ...selectors) {
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
  }
}
