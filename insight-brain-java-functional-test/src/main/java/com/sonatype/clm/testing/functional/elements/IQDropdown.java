/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;

public class IQDropdown
    extends BasicElement<IQDropdown>
{
  public IQDropdown(String selector) {
    super(selector);
  }

  public Button button() {
    return new Button(childSelector("button"));
  }

  public IQDropdownMenu menu() {
    return new IQDropdownMenu(childSelector(".iq-dropdown-menu"));
  }

  public class IQDropdownMenu
      extends BasicElement<IQDropdownMenu>
  {
    public IQDropdownMenu(String selector) {
      super(selector);
    }

    public ElementsCollection entries() {
      return children("a");
    }
  }
}
