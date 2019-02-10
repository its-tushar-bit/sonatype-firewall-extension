/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

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
    return root().find("svg");
  }

  public static SelenideElement artifactTable() {
    return root().find("#infoPanelArtifactTable");
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
    return artifactTable().find("#artifactInfoSecurityThreatRow .clm-chiclet");
  }

  public static SelenideElement securityCount() {
    return artifactTable().find("#artifactInfoSecurityThreatRow span");
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
}
