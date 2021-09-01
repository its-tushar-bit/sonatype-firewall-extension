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

public class ComponentInformationTile
    extends BasicElement<ComponentInformationTile>
{
  private static final String TILE_SELECTOR = "#overview-component-information-tile";

  private static final String TILE_CONTENT_SELECTOR = ".nx-tile-content";

  private static final String SECTION_DEFINITION_ITEM_SELECTOR =
      ".nx-read-only > .nx-read-only__item";

  public static ComponentInformationTile getOverviewTileForParent(String parentSelector) {
    String combinedSelector = SelectorUtils.createSelector(parentSelector, TILE_SELECTOR);
    return new ComponentInformationTile(combinedSelector);
  }

  private ComponentInformationTile(String selectorStringWithParent) {
    super(selectorStringWithParent);
  }

  public GeneralInfoSection generalInfoSection() {
    return new GeneralInfoSection(this.childSelector(TILE_CONTENT_SELECTOR, "section:nth-child(1)"));
  }

  public IdentificationInfoSection identificationInfoSection() {
    return new IdentificationInfoSection(this.childSelector(TILE_CONTENT_SELECTOR, "section:nth-child(2)"));
  }

  public static class GeneralInfoSection
      extends BasicElement<GeneralInfoSection>
  {
    private GeneralInfoSection(String selector) {
      super(selector);
    }

    public SelenideElement getTypeItem() {
      ElementsCollection definitionPairs = getDefinitionPairs();
      return definitionPairs.get(0);
    }

    public ElementsCollection getNamingItems() {
      ElementsCollection definitionPairs = getDefinitionPairs();
      return definitionPairs.last(definitionPairs.size() - 1);
    }

    private ElementsCollection getDefinitionPairs() {
      return children(SECTION_DEFINITION_ITEM_SELECTOR);
    }
  }

  public static class IdentificationInfoSection
      extends BasicElement<IdentificationInfoSection>
  {
    private IdentificationInfoSection(String selector) {
      super(selector);
    }

    public SelenideElement getCatalogedDateItem() {
      ElementsCollection definitionPairs = getDefinitionPairs();
      return definitionPairs.get(0);
    }

    public SelenideElement getMatchStateItem() {
      ElementsCollection definitionPairs = getDefinitionPairs();
      return definitionPairs.get(1);
    }

    public SelenideElement getOccurrencesItem() {
      ElementsCollection definitionPairs = getDefinitionPairs();
      return definitionPairs.get(2);
    }

    public SelenideElement getIdentificationSourceItem() {
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
