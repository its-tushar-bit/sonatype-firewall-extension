/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.pages;

import com.codeborne.selenide.SelenideElement;
import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

public class SbomManagerContinuousMonitoringPage
    extends BasicElement<SbomManagerContinuousMonitoringPage>
{
  public static String url(String ownerId, boolean organization) {
    return BaseUrl.resolvePageUrl(
        "/sbomManager/management/edit/" + (organization ? "organization" : "application") + "/{ownerId}/monitoring",
        ownerId);
  }

  public SelenideElement container() {
    return child("#sbom-manager-continuous-monitoring");
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public SelenideElement errorAlert() {
    return child(".nx-alert--error");
  }

  public SelenideElement submitButton() {
    return child(".nx-form__submit-btn");
  }

  public SelenideElement toggleInput() {
    return child(".nx-toggle__input");
  }

  public SelenideElement toggleButton() {
    return child("#enable-continuous-monitoring");
  }

  public SelenideElement stageStatusLabel() {
    return child("#sbom-continuous-monitoring-status-label");
  }
}
