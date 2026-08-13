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

public class BaseUrlConfigurationPage
    extends BasicElement<BaseUrlConfigurationPage>
{
  public static String url() {
    return BaseUrl.resolvePageUrl("/baseUrl");
  }

  public String getUrl() {
    return BaseUrlConfigurationPage.url();
  }

  public SelenideElement baseUrlAttribute() {
    return child("#config-base-url");
  }

  public Button saveButton() {
    return new Button(childSelector(".iq-base-url-configuration-save-button"));
  }

  public Button cancelButton() {
    return new Button(childSelector("#base-url-cancel"));
  }

  public Button deleteButton() {
    return new Button(childSelector("#base-url-config-delete-button"));
  }
}
