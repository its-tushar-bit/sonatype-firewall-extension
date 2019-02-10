/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class AccessTileList
    extends BasicElement<AccessTileList>
{
  public AccessTileList(String... selector) {
    super(selector);
  }

  public ElementsCollection elements() {
    return children("tr");
  }

  public AccessTileListElement element(int num) {
    return new AccessTileListElement(selector, "tr", nthChild(num + 1));
  }

  public SelenideElement ownerName() {
    return child(".iq-list__title");
  }

  public SelenideElement emptyDescriptor() {
    return child(".iq-list__item--empty");
  }

  public static class AccessTileListElement
      extends BasicElement<AccessTileListElement>
  {
    public SelenideElement root;

    public AccessTileListElement(String... selectors) {
      super(selectors);
    }

    public SelenideElement chevron() {
      return child(".fa-chevron-right");
    }

    public SelenideElement role() {
      return child(".iq-cell--access-role");
    }

    public SelenideElement members() {
      return child(".iq-cell--access-members");
    }

    public SelenideElement userIcon() {
      return child(".fa-user");
    }

    public SelenideElement groupIcon() {
      return child(".fa-users");
    }
  }
}
