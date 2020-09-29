/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxCheckbox;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.SelenideElement;

public class AdvancedSearchConfigurationPage
    extends BasicElement<AdvancedSearchConfigurationPage>
{
  public static final String ROOT = "#advanced-search-config";

  public AdvancedSearchConfigurationPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/advancedSearchConfig");
  }

  public NxCheckbox isEnabledCheckbox() {
    return new NxCheckbox(child("#advanced-search-config-is-enabled-checkbox"));
  }

  public SelenideElement reIndexButton() {
    return child("#advanced-search-config-re-index-button");
  }

  public SelenideElement saveButton() {
    return child("#advanced-search-config-save");
  }

  public SelenideElement cancelButton() {
    return child("#advanced-search-config-cancel");
  }

  public SelenideElement lastIndexTime() {
    return child("#advanced-search-last-index-time");
  }
}
