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

public class VulnerabilitiesThreatLevelTile
    extends SbomDashboardTile
{
  private static final String ROOT = "#vulnerabilities-by-threat-level-tile";

  private static final String TABLE = ".sbom-manager-vulnerabilities-by-threat-level-table";

  public VulnerabilitiesThreatLevelTile() {
    super(ROOT);
  }

  public TileLabels tileLabels() {
    return new TileLabels();
  }

  public TileTable tileTable() {
    return new TileTable();
  }

  public SelenideElement link() {
    return child(".sbom-manager-vulnerabilities-by-threat-level-tile__link");
  }

  public class TileLabels
      extends BasicElement<TileLabels>
  {
    TileLabels() {
      super(ROOT, ".sbom-manager-vulnerabilities-by-threat-level-tile__list");
    }

    public SelenideElement label(int index) {
      return child(
          createSelector(".sbom-manager-vulnerabilities-by-threat-level-tile__list__item", nthChild(index + 1)));
    }
  }

  public SelenideElement tilePieChart() {
    return child(".sbom-manager-vulnerability-by-threat-level-pie-chart");
  }

  public class TileTable
      extends BasicElement<TileTable>
  {
    TileTable() {
      super(ROOT, TABLE);
    }

    public TableRow tableRow(int index) {
      return new TableRow(childSelector(createSelector("tr", nthChild(index + 1))));
    }

    public TableHeaders tableHeaders() {
      return new TableHeaders();
    }
  }

  public class TableHeaders
      extends BasicElement<TableHeaders>
  {
    public TableHeaders() {
      super(TABLE);
    }

    public SelenideElement header(int index) {
      return child(createSelector("th", nthChild(index + 1)));
    }
  }

  public class TableRow
      extends BasicElement<TableRow>
  {
    public TableRow(String selector) {
      super(selector);
    }

    public SelenideElement threatLevel() {
      return child("td:nth-child(1)");
    }

    public SelenideElement unannotated() {
      return child("td:nth-child(2)");
    }

    public SelenideElement annotated() {
      return child("td:nth-child(3)");
    }

    public SelenideElement total() {
      return child("td:nth-child(4)");
    }

    public void shouldHaveCorrectThreatLevelAndMetrics(String threatLevel, int unannotated, int annotated, int total) {
      this.threatLevel().shouldHave(text(threatLevel));
      this.unannotated().shouldHave(text(String.valueOf(unannotated)));
      this.annotated().shouldHave(text(String.valueOf(annotated)));
      this.total().shouldHave(text(String.valueOf(total)));
    }
  }
}
