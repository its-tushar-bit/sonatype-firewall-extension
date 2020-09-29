/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class ListWaiversPage
    extends BasicElement<ListWaiversPage>
{
  public static final String ROOT = "#list-waivers-page";

  public static String url(String violationId) {
    return BaseUrl.resolvePageUrl("/waivers/{id}", violationId);
  }

  public ListWaiversPage() {
    super(ROOT);
  }

  public NxBackButton backButton() {
    return new NxBackButton(ROOT);
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public SelenideElement waiverDetailsTile() {
    return child(".nx-tile", nthChild(0));
  }

  public SelenideElement waiverDetailsTitle() {
    return child(".nx-tile-header--hrule h2");
  }

  public SelenideElement waiverListTile() {
    return child(".nx-tile", nthChild(1));
  }

  public SelenideElement waiverListTitle() {
    return child(".nx-tile-header__title h2");
  }

  public SelenideElement addWaiverButton() {
    return child(".nx-btn--tertiary");
  }

  public SelenideElement waiverListTable() {
    return child(".nx-table");
  }

  public SelenideElement policyName() {
    return child(".list-waivers--threat-indicator .iq-threat-level");
  }

  public SelenideElement constraintName() {
    return child(".list-waivers--constraint div");
  }

  public ElementsCollection conditions() {
    return children(".list-waivers--conditions span");
  }

  public SelenideElement condition(int index) {
    return child(".list-waivers--conditions span", nthChild(index));
  }

  public SelenideElement componentName() {
    return child(".list-waivers--component-name div");
  }
}
