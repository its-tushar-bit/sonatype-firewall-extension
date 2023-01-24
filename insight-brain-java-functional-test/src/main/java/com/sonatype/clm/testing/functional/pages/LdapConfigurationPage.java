/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.LdapConnectionForm;
import com.sonatype.clm.testing.functional.elements.LdapUserAndGroupSettingsForm;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class LdapConfigurationPage
{
  private static final String ROOT_SELECTOR = "#ldap-configuration-editor";

  public static String urlToEdit(String ldapId) {
    return BaseUrl.resolvePageUrl("/ldap/edit/{ldapId}", ldapId);
  }

  public static String urlToCreate() {
    return BaseUrl.resolvePageUrl("/ldap/create");
  }

  public static SelenideElement root() {
    return $(ROOT_SELECTOR);
  }

  public static NxBackButton backButton() {
    return new NxBackButton("");
  }

  public static SelenideElement connectionTab() {
    return $(".nx-tab-list li:nth-child(1)");
  }

  public static SelenideElement userAndGroupSettingsTab() {
    return $(".nx-tab-list li:nth-child(2)");
  }

  public static LdapConnectionForm ldapConnectionForm() {
    return new LdapConnectionForm(ROOT_SELECTOR, "#ldap-edit-connection");
  }

  public static LdapUserAndGroupSettingsForm ldapUserAndGroupSettingsForm() {
    return new LdapUserAndGroupSettingsForm(ROOT_SELECTOR, "#ldap-edit-usermapping");
  }

  public static CreateServer ldapNameEditor() {
    return new CreateServer("#ldap-create-server");
  }

  public static class CreateServer
      extends BasicElement<CreateServer>
  {
    CreateServer(String selector) {
      super(selector);
    }

    public SelenideElement serverNameInput() {
      return child("#serverName");
    }

    public SelenideElement save() {
      return child(".nx-form__submit-btn");
    }

    public SelenideElement cancel() {
      return child(".nx-form__cancel-btn");
    }
  }

  public static SelenideElement discardChangesModalButton() {
    return $("#unsaved-changes-modal-continue-button");
  }

  public static SelenideElement deleteButton() {
    return $("#remove-server");
  }

  public static SelenideElement deleteConfirmationButton() {
    return $("#delete-user-modal .nx-form__submit-btn");
  }

  public static SelenideElement getInputValidationElement(SelenideElement element) {
    return element.closest(".nx-form-group").find(".nx-field-validation-message");
  }
}
