/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ZscalerConfigPage
    extends BasicElement<ZscalerConfigPage>
{
  public static final String ROOT = "#zscaler-config-page-container";

  public ZscalerConfigPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/firewall/zscalerConfig");
  }

  public SelenideElement zscalerConfigHeader() {
    return child("#zscaler-config-header");
  }

  public SelenideElement zscalerCustomUrlsHeader() {
    return child("#zscaler-custom-urls-header");
  }

  public SelenideElement zscalerFormSection() {
    return child("#zscaler-configuration");
  }

  public SelenideElement username() {
    return child("#zscaler-config-username");
  }

  public SelenideElement password() {
    return child("#zscaler-config-password");
  }

  public SelenideElement hostname() {
    return child("#zscaler-config-hostname");
  }

  public SelenideElement apiKey() {
    return child("#zscaler-config-api-key");
  }

  public SelenideElement formatDropdownButton() {
    return child(".nx-dropdown .nx-dropdown__toggle");
  }

  public NxCheckbox getFormatCheckboxAt(int i ) {
    return new NxCheckbox(child(".nx-dropdown-menu .nx-radio-checkbox", nthChild(i + 1)));
  }

  public SelenideElement formatTooltipIcon() {
    return child(".config-format-tooltip__icon");
  }

  public SelenideElement formatValidationError() {
    return child("#zscaler-config-format .nx-field-validation-message");
  }
  
  public NxCheckbox eulaCheckbox() {
    return new NxCheckbox(child("#zscaler-eula-checkbox"));
  }

  public SelenideElement save() {
    return child(".nx-form__submit-btn");
  }

  public SelenideElement cancel() {
    return child("#zscaler-config-cancel");
  }

  public SelenideElement delete() {
    return child("#zscaler-config-delete");
  }

  public SelenideElement testConfig() {
    return child("#zscaler-config-test");
  }

  public SelenideElement loadError() {
    return child(".nx-alert--load-error");
  }

  public ElementsCollection gridHeaders() {
    return children(".nx-grid-header__title");
  }

  public SelenideElement indicator() {
    return child(".nx-status-indicator");
  }

  public DeleteModal deleteModal() {
    return new DeleteModal();
  }

  public static class DeleteModal
      extends BasicElement<ZscalerConfigPage.DeleteModal>
  {
    public static final String ROOT = "#zscaler-config-delete-modal";

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
}
