/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ElementsCollection;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class NamespaceConfusionProtectionTile
    extends BasicElement<NamespaceConfusionProtectionTile>
{
  private static final String CONFIGURATION_TILE_SELECTOR = "#namespace-confusion-protection-pill-configuration";

  private static final String ROW_SELECTOR = ".nx-table tbody .nx-table-row";

  public NamespaceConfusionProtectionTile() {
    super(CONFIGURATION_TILE_SELECTOR);
  }

  public SelenideElement tableBody() {
    return child("#iq-proprietary-table-body");
  }

  public ElementsCollection tableBodyRows() {
    return tableBody().findAll("tr");
  }

  public ElementsCollection resultRows() {
    return children(ROW_SELECTOR);
  }

  public ResultRow resultRow(int i) {
    return new ResultRow(childSelector(ROW_SELECTOR, nthChild(i)));
  }

  public SelenideElement emptyDescriptor() {
    return child(".nx-cell--meta-info");
  }

  public SelenideElement tablePagination() {
    return child(".nx-btn-bar--pagination");
  }

  public SelenideElement previousPageBtn() {
    return child(".nx-btn-bar--pagination button", nthChild(1));
  }

  public SelenideElement nextPageBtn() {
    return child(".nx-btn-bar--pagination button", nthChild(2));
  }

  public SelenideElement namespaceFilterInput() {
    return child("#nx-repository-name-space-filter");
  }

  public SelenideElement componentNamespaceHeaderSortBtn() {
    return child(".iq-repository-column--name-space .nx-cell__sort-btn");
  }

  public SelenideElement repositoryManagerHeaderSortBtn() {
    return child(".iq-repository-column--manager .nx-cell__sort-btn");
  }

  public SelenideElement hostedRepositoryNameHeaderSortBtn() {
    return child(".iq-repository-column--repository .nx-cell__sort-btn");
  }

  public SelenideElement enabledHeaderSortBtn() {
    return child(".iq-repository-column--enabled .nx-cell__sort-btn");
  }

  public ElementsCollection componentNamespaceColumnCells() {
    return children(".iq-repository-cell--name-space");
  }

  public ElementsCollection repositoryManagerIdColumnCells() {
    return children(".iq-repository-cell-manager");
  }

  public ElementsCollection hostedRepositoryNameColumnCells() {
    return children(".iq-repository-cell-repository");
  }

  public ElementsCollection enabledToggleIndicators() {
    return children("#iq-repository-component-enabled-toggle > input");
  }

  public static class ResultRow
      extends BasicElement<ResultRow>
  {
    public ResultRow(String selector) {
      super(selector);
    }
  }
}
