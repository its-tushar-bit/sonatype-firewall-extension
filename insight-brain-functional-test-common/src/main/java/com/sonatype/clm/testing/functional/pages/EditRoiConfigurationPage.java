/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.SelenideElement;
import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

public class EditRoiConfigurationPage
    extends BasicElement<EditRoiConfigurationPage>
{
  public static final String ROOT = "#edit-roi-configuration-page";

  public EditRoiConfigurationPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/roiConfiguration/edit");
  }

  public static String firewallUrl() {
    return BaseUrl.resolvePageUrl("/firewall/roiConfiguration/edit");
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public SelenideElement lifecycleTitle() {
    return child("#edit-roi-configuration-page__lifecycle-title");
  }

  public SelenideElement baselineDaysToResolveViolation() {
    return child("#edit-roi-configuration-page__input__baseline-days-to-resolve-violation");
  }

  public SelenideElement dailyRiskCostOfUnfixedViolation() {
    return child("#edit-roi-configuration-page__input__daily-risk-cost-of-unfixed-violation");
  }

  public SelenideElement firewallTitle() {
    return child("#edit-roi-configuration-page__firewall-title");
  }

  public SelenideElement malwareAttacksPrevented() {
    return child("#edit-roi-configuration-page__input__malware-attacks-prevented");
  }

  public SelenideElement namespaceAttacksPrevented() {
    return child("#edit-roi-configuration-page__input__namespace-attacks-prevented");
  }

  public SelenideElement safeComponentsAutoSelected() {
    return child("#edit-roi-configuration-page__input__safe-components-auto-selected");
  }

  public SelenideElement validationError() {
    return child("#edit-roi-configuration-page__alert__validation-error");
  }

  public SelenideElement loadError() {
    return child(".nx-alert--load-error");
  }

  /*
   * public Button cancelButton() {
   * return new Button(child("#edit-roi-configuration-page__button__cancel"));
   * }
   *
   * public Button restoreDefaultsButton() {
   * return new Button(child("#edit-roi-configuration-page__button__restore-defaults"));
   * }
   *
   * public Button updateButton() {
   * return new Button(child("#edit-roi-configuration-page__button__update"));
   * }
   */
  /*
   * public static class RestoreDefaultsModal
   * extends BasicElement<RestoreDefaultsModal>
   * {
   * public RestoreDefaultsModal(String selector) {
   * super(selector);
   * }
   *
   * public SelenideElement modal() {
   * return child("#edit-roi-configuration-page__restore-defaults-modal");
   * }
   *
   * public Button cancelButton() {
   * return new Button(child(".restore-defaults-modal__cancel-button"));
   * }
   *
   * public Button restoreDefaultsButton() {
   * return new Button(child(".restore-defaults-modal__restore-button"));
   * }
   * }
   */
}
