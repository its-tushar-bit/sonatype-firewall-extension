/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.SelenideElement;
import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

public class EmailConfigurationPage
    extends BasicElement<EmailConfigurationPage>
{
  public static final String ROOT = "#mail-config-page-container";

  public static class DeleteModal
      extends BasicElement<DeleteModal>
  {
    public static final String ROOT = "#mail-config-delete-modal";

    public DeleteModal() {
      super(ROOT);
    }

    public SelenideElement ok() {
      return child(".nx-form__submit-btn");
    }

    public SelenideElement cancel() {
      return child(".nx-form__cancel-btn");
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

  public SelenideElement title() {
    return child(".nx-h2");
  }

  public SelenideElement hostName() {
    return child("#email-config-hostname");
  }

  public SelenideElement port() {
    return child("#email-config-port");
  }

  public SelenideElement username() {
    return child("#email-config-username");
  }

  public SelenideElement password() {
    return child("#email-config-password");
  }

  public SelenideElement systemEmail() {
    return child("#email-config-systemEmail");
  }

  public SelenideElement testEmailRecipient() {
    return child("#email-config-test-email-recipient");
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
    return child(".nx-form__submit-btn");
  }

  public SelenideElement cancel() {
    return child("#email-config-cancel");
  }

  public SelenideElement delete() {
    return child("#email-config-delete");
  }

  public SelenideElement loadError() {
    return child(".nx-alert--load-error");
  }
}
