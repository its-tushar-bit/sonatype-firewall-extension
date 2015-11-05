/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class VersionsCIP
{

  private static SelenideElement root() {
    return $("#version-graph");
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
}
