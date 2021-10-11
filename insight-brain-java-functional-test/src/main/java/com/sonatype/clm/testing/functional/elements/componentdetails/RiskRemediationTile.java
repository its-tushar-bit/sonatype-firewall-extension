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

public class RiskRemediationTile
    extends BasicElement<RiskRemediationTile>
{
  private static final String TILE_SELECTOR = "#overview-component-risk-remediation-tile";

  private static final String TILE_CONTENT_SELECTOR = ".nx-tile-content";

  private static final String TILE_HEADER_TITLE_SELECTOR = ".nx-tile-header__title";

  private static final String LIST_SELECTOR = ".nx-list";

  private static final String LIST_ITEM_SELECTOR = ".nx-list__item";

  private static final String LIST_ITEM_CLICKABLE_SELECTOR = ".nx-list__item .nx-text-link";

  public static RiskRemediationTile getOverviewTileForParent(String parentSelector) {
    String combinedSelector = SelectorUtils.createSelector(parentSelector, TILE_SELECTOR);
    return new RiskRemediationTile(combinedSelector);
  }

  private RiskRemediationTile(String selectorStringWithParent) {
    super(selectorStringWithParent);
  }

  public SelenideElement getTitle() {
    return child(TILE_HEADER_TITLE_SELECTOR);
  }

  public DependencyInformationSection dependencyInformationSection() {
    return new DependencyInformationSection(
        this.childSelector(TILE_CONTENT_SELECTOR, ".iq-dependency-information"));
  }

  public VersionExplorerSection versionExplorerSection() {
    return new VersionExplorerSection(this.childSelector(TILE_CONTENT_SELECTOR, ".iq-version-explorer"));
  }

  public RecommendedVersionsSection recommendedVersionsSections() {
    return new RecommendedVersionsSection(
        this.childSelector(TILE_CONTENT_SELECTOR, ".iq-recommended-version"));
  }

  public SelenideElement compareVersionsTitle() {
    return child("#compare-versions-header .nx-grid-header__title");
  }

  public CompareVersionsTable compareVersionsTable() {
    return new CompareVersionsTable(this.childSelector("#compare-versions-table"));
  }

  public static class DependencyInformationSection
      extends BasicElement<RiskRemediationTile.DependencyInformationSection>
  {
    private DependencyInformationSection(String selector) {
      super(selector);
    }

    public SelenideElement getTitle() {
      return child(TILE_HEADER_TITLE_SELECTOR);
    }

    public SelenideElement content() {
      return child(TILE_CONTENT_SELECTOR);
    }

    public SelenideElement contentParagraph() {
      return child(".nx-tile-content>.nx-p");
    }

    public ElementsCollection contentAncestorsList() {
      SelenideElement child = child(LIST_SELECTOR);
      return child.findAll(LIST_ITEM_SELECTOR);
    }

    public ElementsCollection contentClickableAncestorsList() {
      SelenideElement child = child(LIST_SELECTOR);
      return child.findAll(LIST_ITEM_CLICKABLE_SELECTOR);
    }
  }

  public static class VersionExplorerSection
      extends BasicElement<RiskRemediationTile.VersionExplorerSection>
  {
    private VersionExplorerSection(String selector) {
      super(selector);
    }

    public SelenideElement getTitle() {
      return child(TILE_HEADER_TITLE_SELECTOR);
    }

    public SelenideElement content() {
      return child(TILE_CONTENT_SELECTOR);
    }
  }

  public static class RecommendedVersionsSection
      extends BasicElement<RiskRemediationTile.RecommendedVersionsSection>
  {
    private RecommendedVersionsSection(String selector) {
      super(selector);
    }

    public SelenideElement getTitle() {
      return child(TILE_HEADER_TITLE_SELECTOR);
    }

    public SelenideElement content() {
      return child(TILE_CONTENT_SELECTOR);
    }

    public ElementsCollection contentRecommendedVersionsList() {
      SelenideElement child = child(LIST_SELECTOR);
      return child.findAll(LIST_ITEM_SELECTOR);
    }

    public RecommendationElement getRecommendation(int index) {
      SelenideElement child = child(LIST_SELECTOR);
      ElementsCollection recommendations = child.findAll(LIST_ITEM_SELECTOR);
      return new RecommendationElement(recommendations.get(index), LIST_ITEM_SELECTOR);
    }
  }

  public static class RecommendationElement
      extends BasicElement<RecommendationElement>
  {
    private final SelenideElement element;

    private RecommendationElement(SelenideElement element, String selector) {
      super(selector);
      this.element = element;
    }

    public SelenideElement text() {
      return element.find(".nx-list__text");
    }

    public SelenideElement subText() {
      return element.find(".nx-list__subtext");
    }

    public ElementsCollection actions() {
      SelenideElement actions = element.find(".nx-list__actions");
      return actions.findAll(".nx-btn");
    }
  }

  public static class CompareVersionsTable
      extends BasicElement<CompareVersionsTable>
  {
    private CompareVersionsTable(String selector) {
      super(selector);
    }

    public ElementsCollection versionRow() {
      return children("#version .nx-cell");
    }

    public ElementsCollection highestPolicyThreatRow() {
      return children("#highestPolicyThreat .nx-cell");
    }

    public ElementsCollection highestCvssScoreRow() {
      return children("#highestCvssScore .nx-cell");
    }

    public ElementsCollection effectiveLicenseRow() {
      return children("#effectiveLicense .nx-cell");
    }

    public ElementsCollection hygieneRatingRow() {
      return children("#hygieneRating .nx-cell");
    }

    public ElementsCollection integrityRatingRow() {
      return children("#integrityRating .nx-cell");
    }
  }
}
