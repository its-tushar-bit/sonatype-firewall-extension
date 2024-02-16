/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class NxBreadcrumb
    extends BasicElement<NxBreadcrumb>
{
  public NxBreadcrumb() {
    super(".nx-breadcrumb");
  }

  public ElementsCollection links() {
    return children(".nx-breadcrumb__link");
  }

  public ElementsCollection listItems() {
    return children(".nx-breadcrumb__list-item");
  }

  public SelenideElement current() {
    return child(".nx-breadcrumb__link--current");
  }

  public SelenideElement currentOwnerForEditPage() {
    return child(".nx-breadcrumb__list-item:nth-child(2)");
  }
}
