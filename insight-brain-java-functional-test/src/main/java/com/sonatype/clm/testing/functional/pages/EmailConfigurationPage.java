/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.elements.ReactTextInput;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class EmailConfigurationPage
    extends BasicElement<EmailConfigurationPage>
{
  public static final String ROOT = "#email-configuration";

  public static class DeleteModal
      extends BasicElement<DeleteModal>
  {
    public static final String ROOT = "#mail-config-delete-modal";

    public DeleteModal() {
      super(ROOT);
    }

    public SelenideElement ok() {
      return child("#mail-config-delete-ok");
    }

    public SelenideElement cancel() {
      return child("#mail-config-delete-cancel");
    }
  }

  public DeleteModal deleteModal() {
    return new DeleteModal();
  }

  public EmailConfigurationPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/mailConfig");
  }

  public ReactTextInput hostName() {
    return new ReactTextInput(child("#email-config-hostname"));
  }

  public ReactTextInput port() {
    return new ReactTextInput(child("#email-config-port"));
  }

  public ReactTextInput username() {
    return new ReactTextInput(child("#email-config-username"));
  }

  public ReactTextInput password() {
    return new ReactTextInput(child("#email-config-password"));
  }

  public ReactTextInput systemEmail() {
    return new ReactTextInput(child("#email-config-systemEmail"));
  }

  public ReactTextInput testEmailRecipient() {
    return new ReactTextInput(child("#email-config-test-email-recipient"));
  }

  public SelenideElement testEmailSend() {
    return child("#email-config-test-email-send");
  }

  public NxCheckbox sslEnabled() {
    return new NxCheckbox(child("#email-config-ssl-enabled"));
  }

  public NxCheckbox startTlsEnabled() {
    return new NxCheckbox(child("#email-config-starttls-enabled"));
  }

  public SelenideElement save() {
    return child("#email-config-save");
  }

  public SelenideElement cancel() {
    return child("#email-config-cancel");
  }

  public SelenideElement delete() {
    return child("#email-config-delete");
  }

  public SelenideElement insufficientPermissionsError() {
    return child("#email-config-insufficient-permissions-error");
  }
}
