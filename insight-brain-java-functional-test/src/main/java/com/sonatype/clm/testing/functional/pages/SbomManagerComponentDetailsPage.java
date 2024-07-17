/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.sbommanager.componentdetails.ComponentDetailsSummaryTile;
import com.sonatype.clm.testing.functional.elements.sbommanager.componentdetails.DeleteAnnotationModal;
import com.sonatype.clm.testing.functional.elements.sbommanager.componentdetails.DependencyTreeTile;
import com.sonatype.clm.testing.functional.elements.sbommanager.componentdetails.VulnerabilitiesTableTile;
import com.sonatype.clm.testing.functional.elements.sbommanager.componentdetails.VexAnnotationDrawer;
import com.sonatype.clm.testing.functional.elements.sbommanager.componentdetails.VulnerabilityDetailsPopover;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class SbomManagerComponentDetailsPage
    extends BasicElement<SbomManagerComponentDetailsPage>
{
  ComponentDetailsSummaryTile componentDetailsSummaryTile;

  VulnerabilitiesTableTile vulnerabilitiesTableTile;

  VulnerabilitiesTableTile sonatypeVulnerabilitiesTile;

  DependencyTreeTile dependencyTreeTile;

  VulnerabilityDetailsPopover vulnerabilityDetailsPopover;

  VexAnnotationDrawer vexAnnotationDrawer;

  DeleteAnnotationModal deleteAnnotationModal;

  public SbomManagerComponentDetailsPage(
      final ComponentDetailsSummaryTile componentDetailsSummaryTile, final VulnerabilitiesTableTile
      vulnerabilitiesTableTile, final VulnerabilitiesTableTile sonatypeVulnerabilitiesTile,
      final DependencyTreeTile dependencyTreeTile, final VulnerabilityDetailsPopover vulnerabilityDetailsPopover,
      final VexAnnotationDrawer vexAnnotationDrawer, final DeleteAnnotationModal deleteAnnotationModal)
  {
    this.componentDetailsSummaryTile = componentDetailsSummaryTile;
    this.vulnerabilitiesTableTile = vulnerabilitiesTableTile;
    this.sonatypeVulnerabilitiesTile = sonatypeVulnerabilitiesTile;
    this.dependencyTreeTile = dependencyTreeTile;
    this.vulnerabilityDetailsPopover = vulnerabilityDetailsPopover;
    this.vexAnnotationDrawer = vexAnnotationDrawer;
    this.deleteAnnotationModal = deleteAnnotationModal;
  }

  public static String url(String applicationId, String versionId, String componentHash ) {
    return BaseUrl.resolvePageUrl("/sbomManager/application/{applicationId}/bom/{versionId}/{componentHash}/overview",
        applicationId, versionId, componentHash);
  }

  public ElementsCollection reportInfoItems() {
    return children(".component-details-header__reportinfo-item");
  }

  public ElementsCollection tags() {
    return children("label.nx-tag");
  }

  public SelenideElement pageTitle() {
    return child("#component-details-title");
  }

  public ComponentDetailsSummaryTile componentSummary() {
    return componentDetailsSummaryTile;
  }

  public VulnerabilitiesTableTile disclosedVulnerabilities() {
    return vulnerabilitiesTableTile;
  }

  public VulnerabilitiesTableTile sonatypeVulnerabilitiesTile() {
    return sonatypeVulnerabilitiesTile;
  }

  public DependencyTreeTile dependencyTreeTile() {
    return dependencyTreeTile;
  }

  public VulnerabilityDetailsPopover vulnerabilityDetailsPopover() {
    return vulnerabilityDetailsPopover;
  }

  public VexAnnotationDrawer vexAnnotationDrawer() {
    return vexAnnotationDrawer;
  }

  public DeleteAnnotationModal deleteAnnotationModal() {
    return deleteAnnotationModal;
  }
}
