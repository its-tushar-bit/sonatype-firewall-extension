/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

public class PublicDataSourcesEditorPage
{
  public static String urlToRootOrg() {
    return url("organization", "ROOT_ORGANIZATION_ID");
  }

  public static String url(String ownerType, String ownerId) {
    return BaseUrl.resolvePageUrl(
        "/management/edit/" + ownerType.toLowerCase() + "/" + ownerId + "/publicDataSourcesEditor");
  }

  public static SelenideElement title() {
    return $("#public-data-sources-title h1");
  }

  public static ElementsCollection radioInputs() {
    return $$(".nx-radio");
  }

  public static SelenideElement errorMessage() {
    return $(".nx-load-error__message");
  }

  public static SelenideElement allowOverridesCheckbox() {
    return $("#allow-public-data-override");
  }

  public static SelenideElement submitButton() {
    return $(".nx-form__submit-btn");
  }
}
