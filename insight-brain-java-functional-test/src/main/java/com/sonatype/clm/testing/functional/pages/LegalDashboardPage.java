/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class LegalDashboardPage extends BasicElement<LegalDashboardPage>
{
  public static final String ROOT = "#legal-dashboard-container";

  public LegalDashboardPage() {
    super(ROOT);
  }

  public static String url() {
    return BaseUrl.resolvePageUrl("/legal/dashboard");
  }

  public static String url(boolean enableComponentDetails) {
    if (!enableComponentDetails) {
      return url();
    }
    return BaseUrl.resolvePageUrl("/legal/dashboard?legalComponentsTabEnabled");
  }

  public SelenideElement componentsTab() {
    return child("#nx-tabs-0-tab-1");
  }

  public ElementsCollection componentItems() {
    return children(".nx-table-row.nx-clickable");
  }

  public ElementsCollection pageButtons() {
    return children(".nx-btn--pagination");
  }
}
