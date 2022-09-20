/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.utils.SelectorUtils;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class LicenseThreatGroupSummaryTile
    extends OwnerTile
{
  private static final String TILE_ID_SELECTOR = "#owner-pill-ltgs";

  private static final String TILE_ADD_LTG_BUTTON_ID_SELECTOR = "#add-ltg-button";

  private static final String TILE_CONTENT_SELECTOR = ".nx-tile-content";

  private static final String TILE_CONTENT_SUBSECTION_SELECTOR = ".nx-tile-subsection";

  private static final String TILE_ACTIONS_SELECTOR = ".nx-tile-header__actions";

  private static final String TILE_SUBSECTION_HEADER = ".nx-tile-subsection__header";

  private static final String TABLE_SELECTOR = ".nx-table";

  private static final String TABLE_HEADERS_ROW_SELECTOR = ".nx-table-row--header";

  private static final String TABLE_CONTENT_ROW_SELECTOR = ".nx-table-row";

  private static final String TABLE_CONTENT_EMPTY_DESCRIPTOR_SELECTOR = ".nx-cell--meta-info";

  private static final String TABLE_CELL_SELECTOR = ".nx-cell";

  private static final String TABLE_CELL_ROW_BUTTON = ".nx-cell__row-btn";

  public LicenseThreatGroupSummaryTile() {
    super(TILE_ID_SELECTOR);
  }

  public SelenideElement getActions() {
    return child(TILE_ACTIONS_SELECTOR);
  }

  public SelenideElement addLTGButton() {
    return child(TILE_ADD_LTG_BUTTON_ID_SELECTOR);
  }

  public static Condition inheritedText(String parent) {
    return Condition.text("inherited from " + parent);
  }

  public ElementsCollection getAllApplicableLicenseThreatGroupSection() {

    SelenideElement child = getElement().find(TILE_CONTENT_SELECTOR);
    return child.findAll(TILE_CONTENT_SUBSECTION_SELECTOR);
  }

  public ApplicableLicenseThreatGroupSection getApplicableLicenseThreatGroupSection(int index) {
    SelenideElement child = child(TILE_CONTENT_SELECTOR);
    ElementsCollection contentSubsections = child.findAll(TILE_CONTENT_SUBSECTION_SELECTOR);
    assert contentSubsections.size() > 0;

    return new ApplicableLicenseThreatGroupSection(contentSubsections.get(index), TILE_CONTENT_SUBSECTION_SELECTOR);
  }

  public ApplicableLicenseThreatGroupSection getLocalLTGSection() {
    return getApplicableLicenseThreatGroupSection(0);
  }

  public static class ApplicableLicenseThreatGroupSection
      extends BasicElement<ApplicableLicenseThreatGroupSection>
  {
    private final SelenideElement element;

    private ApplicableLicenseThreatGroupSection(SelenideElement element, String selector) {
      super(selector);
      this.element = element;
    }

    public SelenideElement getTitle() {
      return element.find(TILE_SUBSECTION_HEADER);
    }

    public SelenideElement getEmptyDescriptor() {
      return element.find(TABLE_CONTENT_EMPTY_DESCRIPTOR_SELECTOR);
    }

    public ElementsCollection getTableHeader() {
      SelenideElement child = element.find(TABLE_SELECTOR);
      return child.findAll(SelectorUtils.createSelector("thead", TABLE_HEADERS_ROW_SELECTOR));
    }

    public ElementsCollection getTableContent() {
      SelenideElement child = element.find(TABLE_SELECTOR);
      return child.findAll(SelectorUtils.createSelector("tbody", TABLE_CONTENT_ROW_SELECTOR));
    }

    public SelenideElement getLTG(String ltgName) {
      ElementsCollection elements = getTableContent();
      return elements.findBy(text(ltgName));
    }

    public SelenideElement getLTG(int index) {
      ElementsCollection elements = getTableContent();
      return elements.get(index);
    }

    public List<LicenseThreatGroupElement> getLicenseThreatGroupElements() {
      ElementsCollection elements = getTableContent();
      return getLicenseThreatGroupElements(elements);
    }

    public List<LicenseThreatGroupElement> getLicenseThreatGroupElements(ElementsCollection elements) {
      assert elements != null;
      return elements.stream()
        .map(e -> new LicenseThreatGroupElement(e,"tbody", TABLE_CONTENT_ROW_SELECTOR)).collect(Collectors.toList());
    }

    public LicenseThreatGroupElement getLicenseThreatGroupElement(SelenideElement element) {
      assert element != null;
      return new LicenseThreatGroupElement(element, TABLE_CONTENT_ROW_SELECTOR);
    }

    public Optional<LicenseThreatGroupElement> getLicenseThreatGroupElement(String ltgName) {
      return Optional.of(getLicenseThreatGroupElement(getLTG(ltgName)));
    }

    public Optional<LicenseThreatGroupElement> getLicenseThreatGroupElement(int index) {
      return Optional.of(getLicenseThreatGroupElement(getLTG(index)));
    }
  }

  public static class LicenseThreatGroupElement
      extends BasicElement<LicenseThreatGroupElement>
  {
    private final SelenideElement element;

    private LicenseThreatGroupElement(SelenideElement element, String... selectors) {
      super(SelectorUtils.createSelector(selectors));
      this.element = element;
    }

    public SelenideElement getThreatLevelValue() {
      return element.find(SelectorUtils.createSelector(TABLE_CELL_SELECTOR, ":first-child"));
    }

    public SelenideElement getThreatLevelIndicator() {
      SelenideElement threatLevel = getThreatLevelValue();
      return threatLevel.find(".nx-icon");
    }

    public static Condition threatLevel(int threatLevel) {
      return Condition.cssClass("nx-threat-indicator--" + convertToCssClass(threatLevel));
    }

    public SelenideElement getName() {
      return element.find(SelectorUtils.createSelector(TABLE_CELL_SELECTOR, nthChild(2)));
    }

    public SelenideElement getChevron() {
      return element.find(TABLE_CELL_ROW_BUTTON);
    }
  }

  private static String convertToCssClass(Integer threatLevel) {
    if (threatLevel == null) {
      return "unspecified";
    }
    else if (threatLevel > 7) {
      return "critical";
    }
    else if (threatLevel > 3) {
      return "severe";
    }
    else if (threatLevel > 1) {
      return "moderate";
    }
    else if (threatLevel == 1) {
      return "low";
    }
    else {
      return "none";
    }
  }
}
