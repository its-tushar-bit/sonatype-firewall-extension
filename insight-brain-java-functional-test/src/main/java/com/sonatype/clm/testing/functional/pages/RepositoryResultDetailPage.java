/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.Button;
import com.sonatype.clm.testing.functional.elements.NxBackButton;
import com.sonatype.clm.testing.functional.elements.NxSmallThreatCounter;
import com.sonatype.clm.testing.functional.elements.NxTextInput;
import com.sonatype.clm.testing.functional.elements.NxTreeViewMultiSelect;
import com.sonatype.clm.testing.functional.elements.PolicyThreatLevelFilter;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class RepositoryResultDetailPage extends BasicElement<RepositoryResultDetailPage>
{
  public static String url(String repositoryId) {
    return BaseUrl.resolvePageUrl("/firewall/repository/{repositoryId}/result", repositoryId);
  }

  public static String firewallUrl(String repositoryId) {
    return BaseUrl.resolvePageUrl("/firewall/repository/{repositoryId}/result", repositoryId);
  }

  public static SelenideElement page() {
    return $("#repository-results-summary-page");
  }

  public static SelenideElement header() {
    return $("#repository-results-summary-page .nx-h1");
  }

  public static NxBackButton backButton() {
    return new NxBackButton();
  }

  public static SelenideElement filterPopoverButton() {
    return $("#repository-filter-popover-button");
  }

  public static SelenideElement reEvaluateReportButton() {
    return $("#iq-repository-results-summary-page__reevaluate-button");
  }

  public static SelenideElement reEvaluateModalButton() {
    return $(".iq-reevaluate-modal-btn-bar a");
  }

  public static SelenideElement reEvaluateModalCancelButton() {
    return $(".iq-reevaluate-modal-btn-bar .nx-btn");
  }

  public static RepositoryFilterPopover filterPopover() {
    return new RepositoryFilterPopover();
  }

  public static IndicatorHeaderRow indicatorRow() {
    return new IndicatorHeaderRow();
  }

  public static RepositoryResultTable table() {
    return new RepositoryResultTable();
  }

  public ElementsCollection paginationButtons() {
    return children(".nx-btn-bar--pagination .nx-btn");
  }

  public static SelenideElement aggregateToggle() {
    return $("#repository-report-aggregate-by-component-toggle");
  }

  public static class RepositoryFilterPopover extends BasicElement<RepositoryFilterPopover>
  {
    static final String POPOVER_SELECTOR = "#iq-summary-page-components-filter";

    public RepositoryFilterPopover() {
      super(POPOVER_SELECTOR);
    }

    public PolicyThreatLevelFilter policyThreatLevelFilter() {
      return new PolicyThreatLevelFilter("#repository-threat-level-filter");
    }

    public static NxTreeViewMultiSelect violationsFilter() {
      return new NxTreeViewMultiSelect("#components-violations-filter");
    }

    public SelenideElement closeButton() {
      return child(".nx-drawer-header__close-button");
    }

    public SelenideElement clearButton() {
      return child("#iq-summary-page-components-filter__clear");
    }

    public SelenideElement applyButton() {
      return child("#iq-summary-page-components-filter__apply");
    }
  }

  public static class IndicatorHeaderRow
      extends BasicElement<IndicatorHeaderRow>
  {
    public IndicatorHeaderRow() {
      super(".iq-indicator-row");
    }

    public NxSmallThreatCounter counts() {
      return new NxSmallThreatCounter(".nx-small-threat-counter-container");
    }

    public SelenideElement coverageCaptionText() {
      return child(".iq-coverage-indicator .iq-caption__text");
    }

    public SelenideElement coverageCaptionSubtext() {
      return child(".iq-coverage-indicator .iq-caption__sub-text");
    }

    public SelenideElement quarantineCaptionText() {
      return child(".iq-quarantine-indicator .iq-caption__text");
    }

    public SelenideElement quarantineCaptionSubtext() {
      return child(".iq-quarantine-indicator .iq-caption__sub-text");
    }
  }

  public static class RepositoryResultTable
      extends BasicElement<RepositoryResultTable>
  {
    public RepositoryResultTable() {
      super(".nx-table");
    }

    public RepositoryResultTableRow row(int num) {
      return new RepositoryResultTableRow("tbody tr.nx-table-row", nthChild(num + 1));
    }

    public ElementsCollection rows() {
      return children("tbody tr.nx-table-row");
    }

    public RepositoryResultTableRow header() {
      return new RepositoryResultTableRow("thead tr.nx-table-row", nthChild(1));
    }

    public NxTextInput policyName() {
      return new NxTextInput(child("#nx-repository-policy-filter"));
    }

    public Button policyNameClearFilterButton() {
      return new Button(".iq-repository-filter--policy button");
    }

    public NxTextInput quarantineTime() {
      return new NxTextInput(child("#nx-repository-quarantine-filter"));
    }

    public Button quarantineTimeClearFilterButton() {
      return new Button(".iq-repository-filter--quarantine button");
    }

    public NxTextInput componentName() {
      return new NxTextInput(child("#nx-repository-component-filter"));
    }

    public Button componentNameClearFilterButton() {
      return new Button(".iq-repository-filter--component button");
    }

    public Button quarantinedHeaderSortButton() {
      return new Button(".iq-repository-column--quarantine-date button");
    }
  }

  public static class RepositoryResultTableRow extends BasicElement<RepositoryResultTableRow>
  {
    public RepositoryResultTableRow(String... selectors) {
      super(selectors);
    }

    public SelenideElement threat() {
      return children(".nx-cell").get(0);
    }

    public SelenideElement policy() {
      return children(".nx-cell").get(1);
    }

    public SelenideElement quarantined() {
      return children(".nx-cell").get(2);
    }

    public SelenideElement component() {
      return children(".nx-cell").get(3);
    }
  }
}
