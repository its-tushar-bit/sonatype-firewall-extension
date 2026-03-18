/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.pages;

import com.sonatype.clm.testing.functional.BasicElement;
import com.sonatype.clm.testing.functional.elements.sbom.componentdetails.ComponentDetailsSummaryTile;
import com.sonatype.clm.testing.functional.elements.sbom.componentdetails.CopyAnnotationModal;
import com.sonatype.clm.testing.functional.elements.sbom.componentdetails.DeleteAnnotationModal;
import com.sonatype.clm.testing.functional.elements.sbom.componentdetails.PolicyViolationsTile;
import com.sonatype.clm.testing.functional.elements.sbom.componentdetails.PolicyViolationDetailsDrawer;
import com.sonatype.clm.testing.functional.elements.sbom.componentdetails.VexAnnotationDrawer;
import com.sonatype.clm.testing.functional.elements.sbom.componentdetails.VulnerabilitiesTableTile;
import com.sonatype.clm.testing.functional.elements.sbom.componentdetails.VulnerabilityDetailsPopover;
import com.sonatype.clm.testing.functional.utils.BaseUrl;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

public class SbomManagerComponentDetailsPage
    extends BasicElement<SbomManagerComponentDetailsPage>
{
  ComponentDetailsSummaryTile componentDetailsSummaryTile;

  VulnerabilitiesTableTile vulnerabilitiesTableTile;

  VulnerabilitiesTableTile sonatypeVulnerabilitiesTile;

  PolicyViolationsTile policyViolationsTile;

  public PolicyViolationDetailsDrawer policyViolationDetailsDrawer;

  VulnerabilityDetailsPopover vulnerabilityDetailsPopover;

  VexAnnotationDrawer vexAnnotationDrawer;

  DeleteAnnotationModal deleteAnnotationModal;

  CopyAnnotationModal copyAnnotationModal;

  public SbomManagerComponentDetailsPage(
      final ComponentDetailsSummaryTile componentDetailsSummaryTile,
      final VulnerabilitiesTableTile vulnerabilitiesTableTile,
      final VulnerabilitiesTableTile sonatypeVulnerabilitiesTile,
      final VulnerabilityDetailsPopover vulnerabilityDetailsPopover,
      final VexAnnotationDrawer vexAnnotationDrawer,
      final DeleteAnnotationModal deleteAnnotationModal,
      final CopyAnnotationModal copyAnnotationModal,
      final PolicyViolationsTile policyViolationsTile,
      final PolicyViolationDetailsDrawer policyViolationDetailsDrawer)
  {
    this.componentDetailsSummaryTile = componentDetailsSummaryTile;
    this.vulnerabilitiesTableTile = vulnerabilitiesTableTile;
    this.sonatypeVulnerabilitiesTile = sonatypeVulnerabilitiesTile;
    this.vulnerabilityDetailsPopover = vulnerabilityDetailsPopover;
    this.vexAnnotationDrawer = vexAnnotationDrawer;
    this.deleteAnnotationModal = deleteAnnotationModal;
    this.copyAnnotationModal = copyAnnotationModal;
    this.policyViolationsTile = policyViolationsTile;
    this.policyViolationDetailsDrawer = policyViolationDetailsDrawer;
  }

  public static String url(String applicationId, String versionId, String componentHash) {
    return BaseUrl.resolvePageUrl(
        "/sbomManager/application/{applicationId}/bom/{versionId}/componentDetails/{componentHash}/overview",
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

  public VulnerabilityDetailsPopover vulnerabilityDetailsPopover() {
    return vulnerabilityDetailsPopover;
  }

  public VexAnnotationDrawer vexAnnotationDrawer() {
    return vexAnnotationDrawer;
  }

  public DeleteAnnotationModal deleteAnnotationModal() {
    return deleteAnnotationModal;
  }

  public CopyAnnotationModal copyAnnotationModal() {
    return copyAnnotationModal;
  }

  public ElementsCollection tabs() {
    return children(".nx-tab");
  }

  public SelenideElement vulnerabilityTab() {
    return this.tabs().get(0);
  }

  public SelenideElement policyViolationsTab() {
    return this.tabs().get(1);
  }

  public PolicyViolationsTile policyViolationsTile() {
    return policyViolationsTile;
  }
}
