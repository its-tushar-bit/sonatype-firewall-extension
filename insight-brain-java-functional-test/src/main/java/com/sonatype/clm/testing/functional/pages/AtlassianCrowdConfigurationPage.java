/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class AtlassianCrowdConfigurationPage
    extends BasicElement<AtlassianCrowdConfigurationPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/crowd");
  }

  public SelenideElement serverUrlAttribute() {
    return child("#crowd-config-server-url");
  }

  public SelenideElement applicationNameAttribute() {
    return child("#crowd-config-app-name");
  }

  public SelenideElement applicationPasswordAttribute() {
    return child("#crowd-config-app-password");
  }

  public Button saveButton() {
    return new Button(childSelector(".iq-crowd-configuration-save-button"));
  }

  public Button cancelButton() {
    return new Button(childSelector("#crowd-config-form .nx-form__cancel-btn"));
  }

  public Button deleteButton() {
    return new Button(childSelector("#crowd-config-delete-button"));
  }

  public Button testButton() {
    return new Button(childSelector("#test-crowd-configuration"));
  }

  public SelenideElement successAlertBox() {
    return child(".nx-alert--success");
  }
}
