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
  public static String url() {
    return BaseUrl.resolvePageUrl("/productlicense");
  }

  public static String firewallUrl() {
    return BaseUrl.resolvePageUrl("/firewall/productlicense");
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

  public static SelenideElement licensedApplications() {
    return $("#license-application-limit");
  }

  public static SelenideElement licensedDevelopers() {
    return $("#license-licensed-developers");
  }

  public static ElementsCollection licensedDevelopersRows() {
    return $$("#license-licensed-developers > dd");
  }

  public static SelenideElement licensedSboms() {
    return $("#license-sbom-limit");
  }

  public static SelenideElement installLicenseBtn() {
    return $("#install-license-btn");
  }

  public static SelenideElement installLicenseFileUpload() {
    return $("#license-input");
  }

  public static SelenideElement uninstallLicenseBtn() {
    return $("#uninstall-license");
  }

  public static SelenideElement licenseInstallGuideline() {
    return $("#license-install-guideline");
  }

  public static SelenideElement licenseProxyGuideline() {
    return $("#license-proxy-guideline");
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

    public SelenideElement header() {
      return child(".nx-modal-header");
    }

    public SelenideElement eula() {
      return child(".nx-modal-content");
    }

    public SelenideElement acceptBtn() {
      return child(".nx-btn--primary");
    }
  }

  public static class ProductLicenseUninstallModal
      extends BasicElement<ProductLicenseUninstallModal>
  {
    public ProductLicenseUninstallModal() {
      super("#license-uninstall-modal");
    }

    public SelenideElement uninstallBtn() {
      return child(".nx-form__submit-btn");
    }

    public SelenideElement retryBtn() {
      return child(".nx-load-error__retry");
    }

    public SelenideElement errorMessage() {
      return child(".nx-alert--error");
    }
  }
}
