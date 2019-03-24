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

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

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

  public SummarySection summarySection(int visibleIndex) {
    return new SummarySection(ROOT_SELECTOR, ".accordion-inner", nthChild(visibleIndex));
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

  public EditPanelForm editPanelForm() {
    return new EditPanelForm(".accordion-body", "form");
  }

  public static class EditPanelForm
      extends BasicElement<EditPanelForm>
  {
    EditPanelForm(final String... selectors) {
      super(selectors);
    }

    public SelenideElement firstName() {
      return child("input[name=firstName]");
    }

    public SelenideElement lastName() {
      return child("input[name=lastName]");
    }

    public SelenideElement email() {
      return child("input[name=email]");
    }

    public Button saveButton() {
      return new Button("button[id$='user-form-save']");
    }
  }

  public static class SummarySection
      extends BasicElement<SummarySection>
  {
    SummarySection(final String... selectors) {
      super(selectors);
    }

    public SelenideElement firstName() {
      return children("td", valueTd()).get(0);
    }

    public SelenideElement lastName() {
      return children("td", valueTd()).get(1);
    }

    public SelenideElement email() {
      return children("td", valueTd()).get(2);
    }

    private String valueTd() {
      return nthChild(2);
    }
  }

  public NewUserForm newUserForm() {
    return new NewUserForm("form[id$='user-form']");
  }

  public static class NewUserForm
      extends BasicElement<NewUserForm>
  {
    NewUserForm(final String... selectors) {
      super(selectors);
    }

    public Button saveButton() {
      return new Button("button[id$='user-form-save']");
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
}
