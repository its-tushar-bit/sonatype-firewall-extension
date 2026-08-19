/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.sbom.dashboard;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxSortingHeader;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class RecentlyImportedSBOMsTile
    extends SbomDashboardTile
{
  private static final String ROOT = "#recently-imported-sboms-tile";

  private static final String TABLE = ".sbom-manager-recently-imported-sboms-tile-table";

  public RecentlyImportedSBOMsTile() {
    super(ROOT);
  }

  public SbomTable sbomTable() {
    return new SbomTable();
  }

  public class SbomTable
      extends BasicElement<SbomTable>
  {
    SbomTable() {
      super(ROOT, ".sbom-manager-recently-imported-sboms-tile-table");
    }

    public NxSortingHeader applicationNameTableHeader() {
      return new NxSortingHeader(childSelector(createSelector(".nx-cell--header", nthChild(1))));
    }

    public NxSortingHeader tableHeader(int index) {
      return new NxSortingHeader(childSelector(createSelector(".nx-cell--header", nthChild(index + 1))));
    }

    public ElementsCollection allTableRows() {
      return children(".nx-table-row");
    }

    public TableRow tableRow(int index) {
      return new TableRow(childSelector(createSelector(".nx-table-row", nthChild(index + 1))));
    }

    public TableRow firstRow() {
      return new TableRow(childSelector(".nx-table-row:first-child"));
    }
  }

  public class TableRow
      extends BasicElement<TableRow>
  {
    public TableRow(String selector) {
      super(selector);
    }

    public SelenideElement applicationName() {
      return child(".sbom-manager-recently-imported-sboms-tile-table__application-name");
    }

    public SelenideElement sbomVersion() {
      return child(".sbom-manager-recently-imported-sboms-tile-table__sbom-version");
    }

    public SelenideElement bomFormat() {
      return child("td:nth-child(3)");
    }

    public SelenideElement importDate() {
      return child("td:nth-child(4)");
    }

    public ThreatCounters threatCounters() {
      return new ThreatCounters();
    }
  }

  public class ThreatCounters
      extends BasicElement<ThreatCounters>
  {
    public ThreatCounters() {
      super(TABLE);
    }

    public SelenideElement criticalThreatCounter() {
      return child(".nx-small-threat-counter.nx-small-threat-counter--critical");
    }

    public SelenideElement severeThreatCounter() {
      return child(".nx-small-threat-counter.nx-small-threat-counter--severe");
    }

    public SelenideElement moderateThreatCounter() {
      return child(".nx-small-threat-counter.nx-small-threat-counter--moderate");
    }

    public SelenideElement lowThreatCounter() {
      return child(".nx-small-threat-counter.nx-small-threat-counter--low");
    }
  }
}
