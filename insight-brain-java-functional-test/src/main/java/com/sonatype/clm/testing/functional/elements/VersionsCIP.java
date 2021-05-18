/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.support.ui.Select;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.createSelector;
import static com.sonatype.clm.testing.functional.utils.SelectorUtils.nthChild;

public class VersionsCIP
{
  private static SelenideElement root() {
    return $("#version-graph");
  }

  public static SelenideElement error() {
    return root().find(".alert-error");
  }

  public static SelenideElement versionGraph() {
    return root().find("#aiVersionChartViz svg");
  }

  public static ElementsCollection versionGraphLabels() {
    return root().findAll("#aiVersionChartLabels text");
  }

  public static SelenideElement versionGraphLoading() {
    return root().find("i.icon-time:last-child");
  }

  public static SelenideElement artifactTable() {
    return root().find("#infoPanelArtifactTable");
  }

  public static SelenideElement componentType() {
    return artifactTable().find("#artifactInfoComponentTypeRow td:last-child");
  }

  public static SelenideElement groupId() {
    return root().find("#artifactInfo-Group");
  }

  public static SelenideElement artifactId() {
    return root().find("#artifactInfo-Artifact");
  }

  public static SelenideElement version() {
    return root().find("#artifactInfo-Version");
  }

  public static SelenideElement extension() {
    return root().find("#artifactInfo-Extension");
  }

  public static SelenideElement classifier() {
    return root().find("#artifactInfo-Classifier");
  }

  public static ElementsCollection declaredLicenses() {
    return artifactTable().findAll("#artifactInfoDeclaredLicenseRow .license");
  }

  public static ElementsCollection observedLicenses() {
    return artifactTable().findAll("#artifactInfoObservedLicenseRow .license");
  }

  public static ElementsCollection effectiveLicenses() {
    return artifactTable().findAll("#artifactInfoEffectiveLicenseRow .license");
  }

  public static SelenideElement highestPolicyThreat() {
    return artifactTable().find("#artifactInfoHighestPolicyThreat .clm-chiclet");
  }

  public static SelenideElement policyCount() {
    return artifactTable().find("#artifactInfoHighestPolicyThreat span");
  }

  public static SelenideElement highestSecurityThreat() {
    return artifactTable().find("#artifactInfoSecurityThreatRow td:last-child");
  }

  public static SelenideElement securityCount() {
    return artifactTable().find("#artifactInfoSecurityThreatRow span");
  }

  public static SelenideElement hygieneRating() {
    return artifactTable().find("#artifactHygieneRating td:last-child");
  }

  public static SelenideElement integrityRating() {
    return artifactTable().find("#artifactIntegrityRating span");
  }

  public static SelenideElement catalogDate() {
    return artifactTable().find("#artifactInfoCatalogDateRow td:last-child");
  }

  public static SelenideElement matchState() {
    return artifactTable().find("#artifactInfoSimilarityScoreRow td:last-child");
  }

  public static SelenideElement identificationSource() {
    return artifactTable().find("#artifactInfoIdentificatonSource td:last-child");
  }

  public static SelenideElement componentCategory() {
    return artifactTable().find("#artifactInfoCategory td:last-child");
  }

  public static SelenideElement selectComponentMessage() {
    return $("#select-component");
  }

  public static SelenideElement unknownComponentMessage() {
    return $("div[ng-if='isUnknown']");
  }

  public static SelenideElement addProprietaryMatchersButton() {
    return $("#add-proprietary-btn");
  }

  public static SelenideElement showDetailsLink() {
    return $$("#aiVersionChartLabels text").find(text("Details"));
  }

  public static SelenideElement hideDetailsLink() {
    return $$("#aiVersionChartLabels").find(text("Hide Details"));
  }

  public static SelenideElement versionBar(int i) {
    return $(createSelector("#aiVersionChartViz", "g", "rect[pointer-events=\"all\"]", nthChild(i)));
  }

  public static SelenideElement versionBarHoverText(int i) {
    return versionBar(i).parent().parent().find("text");
  }

  public static SelenideElement recommendedVersionsHeader() {
    return root().find("#recommended-versions-header");
  }

  public static SelenideElement nextNoViolationVersionLink() {
    return root().find("#next-no-violation-version-link");
  }

  public static SelenideElement nextNoFailVersionLink() {
    return root().find("#next-no-fail-version-link");
  }

  public static SelenideElement selectNoViolation() {
    return root().find("#select-no-violation");
  }

  public static SelenideElement viewDetailsButton() {
    return root().find("#view-details-button");
  }

  public static SelenideElement migrateButton() {
    return root().find("#migrate-button");
  }

  public static SelenideElement selectAnApplicationMessage() {
    return root().find("#select-application");
  }

  public static Select selectApplications() {
    return new Select(root().find("#selectApp"));
  }

  public static SelenideElement applicationsElement() {
    return root().find("#selectApp");
  }

  public static SelenideElement noVersionsAvailable() {
    return root().find("#no-versions-available");
  }

  public static SelenideElement rootAncestorsHeader() {
    return root().find("#cip-root-ancestors-header");
  }

  public static SelenideElement showMoreRootAncestorsToggle() {
    return root().find("#cip-root-ancestors-toggle-show-more");
  }

  public static ElementsCollection rootAncestorLinks() {
    return root().findAll("#cip-root-ancestors-links .cip-root-ancestors__link a");
  }

  public static SelenideElement rootAncestorLink(int i) {
    return $(createSelector("#cip-root-ancestors-links", ".cip-root-ancestors__link", "a", nthChild(i)));
  }
}
