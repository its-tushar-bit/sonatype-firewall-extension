/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;

public class NxDropdown
    extends BasicElement<NxDropdown>
{
  public NxDropdown(String selector) {
    super(selector);
  }

  public Button button() {
    return new Button(childSelector("button"));
  }

  public NxDropdown.NxDropdownMenu menu() {
    return new NxDropdownMenu(childSelector(".nx-dropdown-menu"));
  }

  public static class NxDropdownMenu
      extends BasicElement<NxDropdown.NxDropdownMenu>
  {
    public NxDropdownMenu(String selector) {
      super(selector);
    }

    public ElementsCollection entries() {
      return children("a");
    }
  }
}
