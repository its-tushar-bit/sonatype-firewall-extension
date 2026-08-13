/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages.sbom;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.NxTree;
import com.sonatype.clm.testing.functional.elements.sbom.ComponentsTile;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$$;

public class SbomManagerBillOfMaterialsPage
    extends BasicElement<SbomManagerBillOfMaterialsPage>
{
  public static String url(String applicationId, String versionId) {
    return BaseUrl.resolvePageUrl("/sbomManager/management/view/application/{applicationId}/bom/{versionId}/overview",
        applicationId, versionId);
  }

  public SelenideElement container() {
    return child("#sbom-manager-bom");
  }

  public SelenideElement title() {
    return child(".nx-h1");
  }

  public SelenideElement importedDate() {
    return child("#bill-of-materials-page-imported-date");
  }

  public SelenideElement errorAlert() {
    return child(".nx-alert--error");
  }

  public static ComponentsTile componentsTile() {
    return new ComponentsTile();
  }

  public SelenideElement filterDialog() {
    return child("dialog#components-filter-drawer");
  }

  public ElementsCollection vulnerabilityThreatLevelFilterCheckboxes() {
    return $$(".sbom-manager-components-filter-drawer__vulnerability-threat-level .nx-radio-checkbox");
  }

  public ElementsCollection dependencyTypeFilterChecboxes() {
    return $$(".sbom-manager-components-filter-drawer__dependency-type .nx-radio-checkbox");
  }

  public SelenideElement exportButton() {
    return child(".sbom-manager-bill-of-materials-page__export-button");
  }

  public SelenideElement exportButtonMenu() {
    return child(".nx-segmented-btn__dropdown-btn");
  }

  public ElementsCollection exportButtonMenuItems() {
    return $$(".nx-dropdown-button");
  }

  public SelenideElement additionalExportOptionsModal() {
    return child("#sbom-additional-export-options-modal");
  }

  public ElementsCollection sbomModalOptions() {
    return $$(".nx-fieldset");
  }

  public ElementsCollection sbomSpecificationOptions() {
    return sbomModalOptions().get(0).$$(".nx-radio-checkbox");
  }

  public ElementsCollection sbomsFormatOptions() {
    return sbomModalOptions().get(1).$$(".nx-radio-checkbox");
  }

  public SelenideElement cancelButtonModal() {
    return child("#sbom-additional-export-options-modal .nx-btn--secondary");
  }

  public SelenideElement exportSbomButtonModal() {
    return child("#sbom-additional-export-options-modal .nx-btn--primary");
  }

  public SelenideElement invalidSbomAlert() {
    return child("#invalid-sbom-alert");
  }

  public SelenideElement invalidSbomAlertCloseBtn() {
    return child("#invalid-sbom-alert .nx-btn--close");
  }

  public SelenideElement invalidSbomIndicator() {
    return child(".sbom-manager-invalid-sbom-indicator");
  }

  // Original BOM Viewer elements
  public ElementsCollection tabs() {
    return children(".nx-tab");
  }

  public SelenideElement originalBomViewerTree() {
    return child(".iq-original-bom-viewer__tree");
  }

  public NxTree originalBomTree() {
    return new NxTree(".iq-original-bom-viewer__tree");
  }

  public SelenideElement originalBomViewerInfo() {
    return child(".iq-original-bom-viewer__info");
  }

  public SelenideElement originalBomSearchInput() {
    return child("#original-bom-search");
  }

  public ElementsCollection treeItems() {
    return children(".nx-tree__item");
  }

  public ElementsCollection treeItemKeys() {
    return children("span.iq-original-bom-viewer__key");
  }

  public ElementsCollection treeItemPreviews() {
    return children(".iq-original-bom-viewer__preview");
  }

  public ElementsCollection searchHighlights() {
    return children(".iq-original-bom-viewer__highlight");
  }

  public SelenideElement searchResultsCount() {
    return child(".iq-original-bom-viewer__results-count");
  }
}
