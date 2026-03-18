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

public class FirewallRepositoryList
    extends BasicElement<FirewallRepositoryList>
{
  public FirewallRepositoryList(String... selectors) {
    super(selectors);
  }

  public ElementsCollection rows() {
    return children(".nx-table .firewall-repositories-entries .nx-table-row");
  }

  public FirewallRepositoryListItem row(int i) {
    return new FirewallRepositoryListItem(
        selector,
        ".nx-table .firewall-repositories-entries .nx-table-row",
        nthChild(i + 1));
  }

  public SelenideElement title() {
    return child(".firewall-repository-list__title");
  }

  public static class FirewallRepositoryListItem
      extends BasicElement<FirewallRepositoryListItem>
  {
    public FirewallRepositoryListItem(String... selectors) {
      super(selectors);
    }

    public NxCheckbox checkbox() {
      return new NxCheckbox(column(0));
    }

    public SelenideElement name() {
      return column(1);
    }

    public SelenideElement column(int num) {
      return child("td", nthChild(num + 1));
    }
  }

  public HeaderColumn checkAllHeaderColumn() {
    return this.header(0);
  }

  public HeaderColumn nameHeaderColumn() {
    return this.header(1);
  }

  private HeaderColumn header(int num) {
    return new HeaderColumn(child("thead th", nthChild(num + 1)));
  }

  public static class HeaderColumn
  {
    public SelenideElement root;

    public static final String NX_UP_SELECTED = ".fa-sort-up";

    public static final String NX_DOWN_SELECTED = ".fa-sort-down";

    public HeaderColumn(SelenideElement root) {
      this.root = root;
    }

    public SelenideElement sort(String sortClass) {
      return root.$(".nx-cell__sort-icons " + sortClass);
    }

    public SelenideElement nxAnchor() {
      return root.$("button");
    }

    public SelenideElement name() {
      return root.$("button > span");
    }

    public NxCheckbox selectAllCheckbox() {
      return new NxCheckbox(root.$(".nx-checkbox"));
    }
  }
}
