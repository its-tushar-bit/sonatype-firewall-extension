/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ElementsCollection;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class ProductLicensePage
{
  public static final String url() {
    return BaseUrl.resolvePageUrl("/productlicense");
  }

  public static SelenideElement expiryDate() {
    return $("#license-expiry-date");
  }

  public static SelenideElement daysToExpiration() {
    return $("#license-days-to-expiration");
  }

  public static SelenideElement fingerprint() {
    return $("#license-fingerprint");
  }

  public static SelenideElement contactName() {
    return $("#license-contact-name");
  }

  public static SelenideElement contactCompany() {
    return $("#license-contact-company");
  }

  public static SelenideElement contactEmail() {
    return $("#license-contact-email");
  }

  public static SelenideElement licensedUsers() {
    return $("#license-licensed-users");
  }

  public static SelenideElement firewallLicensedUsers() {
    return $("#license-firewall-licensed-users");
  }

  public static SelenideElement applicationLimit() {
    return $("#license-application-limit");
  }

  public static SelenideElement installLicenseBtn() {
    return $("label[for=license-input]");
  }

  public static SelenideElement installLicenseFileUpload() {
    return $("#license-input");
  }

  public static SelenideElement uninstallLicenseBtn() {
    return $("#uninstall-license");
  }

  public static ElementsCollection products() {
    return $$(".license-product");
  }

  public static class ProductLicenseEulaModal
      extends BasicElement<ProductLicenseEulaModal>
  {
    public ProductLicenseEulaModal() {
      super("#eulaModal");
    }

    public SelenideElement eula() {
      return child(".modal-body .well");
    }

    public SelenideElement acceptBtn() {
      return child(".btn-primary");
    }
  }

  public static class ProductLicenseInstalledModal
      extends BasicElement<ProductLicenseInstalledModal>
  {
    public ProductLicenseInstalledModal() {
      super("#licenseInstalledModal");
    }

    public SelenideElement refreshBtn() {
      return child("button");
    }
  }

  public static class ProductLicenseUninstallModal
      extends BasicElement<ProductLicenseUninstallModal>
  {
    public ProductLicenseUninstallModal() {
      super("#license-uninstall-modal");
    }

    public SelenideElement uninstallBtn() {
      return child(".btn-primary");
    }
  }

  public static class ProductLicenseUninstalledModal
      extends BasicElement<ProductLicenseUninstalledModal>
  {
    public ProductLicenseUninstalledModal() {
      super("#licenseUninstalledModal");
    }

    public SelenideElement refreshBtn() {
      return child("button");
    }
  }
}
