/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.sbom.dashboard;

import com.sonatype.clm.testing.functional.BasicElement;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class HighPriorityVulnerabilitiesTile
    extends SbomDashboardTile
{
  private static final String ROOT = "#high-priority-vulnerabilities-tile";

  public HighPriorityVulnerabilitiesTile() {
    super(ROOT);
  }

  public VulnerabilityList vulnerabilityList() {
    return new VulnerabilityList();
  }

  public class VulnerabilityList
      extends BasicElement<VulnerabilityList>
  {
    VulnerabilityList() {
      super(ROOT, ".sbom-manager-high-priority-vulnerabilities-tile-list");
    }

    public ListItem listItem(int index) {
      return new ListItem(
          childSelector(
              createSelector(".sbom-manager-high-priority-vulnerabilities-tile-list-item", nthChild(index + 1))));
    }
  }

  public class ListItem
      extends BasicElement<ListItem>
  {
    public ListItem(String selector) {
      super(selector);
    }

    public SelenideElement severity() {
      return child(".sbom-manager-high-priority-vulnerabilities-tile-list-item__severity");
    }

    public SelenideElement vulnerabilityNameLink() {
      return child(".nx-text-link");
    }

    public SelenideElement creationDate() {
      return child(".sbom-manager-high-priority-vulnerabilities-tile-list-item__date");
    }

    public void shouldHaveCorrectSeverityAndName(int correctSeverity, String correctName, String correctDate) {
      this.severity().shouldHave(text(String.valueOf(correctSeverity)));
      this.vulnerabilityNameLink().shouldHave(text(correctName));
      this.creationDate().shouldHave(text(correctDate));
    }
  }
}
