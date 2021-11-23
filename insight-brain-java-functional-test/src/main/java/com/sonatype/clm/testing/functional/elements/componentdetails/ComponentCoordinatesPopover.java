/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class ComponentCoordinatesPopover
    extends BasicElement<ComponentCoordinatesPopover>
{
  public static final String ROOT = "#iq-component-coordinates-popover";

  private static final String DEFINITION_ITEM_SELECTOR =
      ".nx-read-only > .nx-read-only__item";

  public ComponentCoordinatesPopover() {
    super(ROOT);
  }

  private ElementsCollection getDefinitionPairs() {
    return children(DEFINITION_ITEM_SELECTOR);
  }

  public SelenideElement closeButton() {
    return child("#iq-component-coordinates-popover-close-btn");
  }

  public SelenideElement typeDefinition() {
    ElementsCollection definitionPairs = getDefinitionPairs();
    return definitionPairs.get(0);
  }

  public ElementsCollection namingDefinitions() {
    ElementsCollection definitionPairs = getDefinitionPairs();
    return definitionPairs.last(definitionPairs.size() - 1);
  }

  public SelenideElement title() {
    return child(".iq-popover-header__title-text");
  }
}
