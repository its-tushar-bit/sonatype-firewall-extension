/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

public class ComponentInformationTile
    extends BasicElement<ComponentInformationTile>
{
  private static final String TILE_SELECTOR = "#overview-component-information-tile";

  private static final String SECTION_DEFINITION_ITEM_SELECTOR =
      ".nx-read-only__item";

  public static ComponentInformationTile getOverviewTileForParent(String parentSelector) {
    String combinedSelector = SelectorUtils.createSelector(parentSelector, TILE_SELECTOR);
    return new ComponentInformationTile(combinedSelector);
  }

  private ComponentInformationTile(String selectorStringWithParent) {
    super(selectorStringWithParent);
  }

  public IdentificationDefinitionList identificationDefinitionList() {
    return new IdentificationDefinitionList(
        this.childSelector(".iq-identification-info-definition-list"));
  }

  public SelenideElement componentCoordinatesButton() {
    return child(".component-coordinates-button");
  }

  public static class IdentificationDefinitionList
      extends BasicElement<IdentificationDefinitionList>
  {
    private IdentificationDefinitionList(String selector) {
      super(selector);
    }

    public SelenideElement getMatchStateItem() {
      ElementsCollection definitionPairs = getDefinitionPairs();
      return definitionPairs.get(0);
    }

    public SelenideElement getSimilarMatchesLink() {
      SelenideElement matchStateItem = getMatchStateItem();
      return matchStateItem.find(By.tagName("a"));
    }

    public SelenideElement getIdentificationSourceItem() {
      ElementsCollection definitionPairs = getDefinitionPairs();
      return definitionPairs.get(1);
    }

    public SelenideElement getOccurrencesItem() {
      ElementsCollection definitionPairs = getDefinitionPairs();
      return definitionPairs.get(2);
    }

    public SelenideElement getOccurrencesLink() {
      return child(".iq-identification-info-definition-list__occurrences-link");
    }

    public SelenideElement getWebsiteItem() {
      ElementsCollection definitionPairs = getDefinitionPairs();
      return definitionPairs.get(3);
    }

    public SelenideElement getCategoryItem() {
      ElementsCollection definitionPairs = getDefinitionPairs();
      return definitionPairs.get(4);
    }

    private ElementsCollection getDefinitionPairs() {
      return children(SECTION_DEFINITION_ITEM_SELECTOR);
    }
  }
}
