/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class ProxyConfigurationPage
    extends BasicElement<ProxyConfigurationPage>
{
  public static final String ROOT = "#proxy-config-container";

  public static class DeleteModal
      extends BasicElement<DeleteModal>
  {
    public static final String ROOT = "#proxy-config-delete-modal";

    public DeleteModal() {
      super(ROOT);
    }

    public SelenideElement ok() {
      return child("#proxy-config-delete-ok");
    }

    public SelenideElement cancel() {
      return child("#proxy-config-delete-cancel");
    }
  }

  public Tooltip saveTooltip() {
    return new Tooltip("#save-button-tooltip");
  }

  public DeleteModal deleteModal() {
    return new DeleteModal();
  }

  public ProxyConfigurationPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/proxyConfig");
  }

  public SelenideElement hostName() {
    return child("#proxy-config-hostname");
  }

  public SelenideElement port() {
    return child("#proxy-config-port");
  }

  public SelenideElement username() {
    return child("#proxy-config-username");
  }

  public SelenideElement password() {
    return child("#proxy-config-password");
  }

  public SelenideElement excludeHosts() {
    return child("#proxy-config-exclude-hosts");
  }

  public SelenideElement save() {
    return child("#proxy-config-save");
  }

  public SelenideElement cancel() {
    return child("#proxy-config-cancel");
  }

  public SelenideElement delete() {
    return child("#proxy-config-delete");
  }

  public SelenideElement loadError() {
    return child(".nx-alert--load-error");
  }

  public SelenideElement productLicenseNavigation() {
    return child("#proxy-config-product-license-navigation");
  }
}
