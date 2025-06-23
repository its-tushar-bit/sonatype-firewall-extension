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
    return $("#ui-view");
  }

  public static SelenideElement error() {
    return root().find(".nx-alert");
  }

  public static SelenideElement versionGraph() {
    return root().find("#aiVersionChartViz svg");
  }

  public static ElementsCollection versionGraphLabels() {
    return root().findAll("#aiVersionChartLabels text");
  }

  public static SelenideElement versionGraphLoading() {
    return root().find(".nx-loading-spinner");
  }

  public static SelenideElement artifactTable() {
    return root().find(".iq-version-graph-component-details__list");
  }

  public static SelenideElement componentType() {
    return artifactTable().find("#iq-version-graph-component-details-type dd");
  }

  public static SelenideElement groupId() {
    return root().find("#iq-version-graph-component-details-group dd");
  }

  public static SelenideElement artifactId() {
    return root().find("#iq-version-graph-component-details-artifact dd");
  }

  public static SelenideElement version() {
    return root().find("#iq-version-graph-component-details-version dd");
  }

  public static SelenideElement extension() {
    return root().find("#iq-version-graph-component-details-extension dd");
  }

  public static SelenideElement classifier() {
    return root().find("#iq-version-graph-component-details-classifier dd");
  }

  public static ElementsCollection declaredLicenses() {
    return artifactTable()
        .findAll("#iq-version-graph-component-details-declared-license .iq-version-graph-component-details__license");
  }

  public static ElementsCollection observedLicenses() {
    return artifactTable()
        .findAll("#iq-version-graph-component-details-observed-license .iq-version-graph-component-details__license");
  }

  public static ElementsCollection effectiveLicenses() {
    return artifactTable()
        .findAll("#iq-version-graph-component-details-effective-license .iq-version-graph-component-details__license");
  }

  public static SelenideElement highestPolicyThreat() {
    return artifactTable().find("#iq-version-graph-component-details-highest-policy-threat dd");
  }

  public static SelenideElement highestPolicyThreatIndicator() {
    return artifactTable().find("#iq-version-graph-component-details-highest-policy-threat .nx-threat-indicator");
  }

  public static SelenideElement policyCount() {
    return artifactTable().find("#iq-version-graph-component-details-policy-count");
  }

  public static SelenideElement highestSecurityThreat() {
    return artifactTable().find("#iq-version-graph-component-details-highest-cvss dd");
  }

  public static SelenideElement securityCount() {
    return artifactTable().find("#iq-version-graph-component-details-vuln-count");
  }

  public static SelenideElement hygieneRating() {
    return artifactTable().find("#iq-version-graph-component-details-hygiene-rating dd");
  }

  public static SelenideElement integrityRating() {
    return artifactTable().find("#iq-version-graph-component-details-integrity-rating dd");
  }

  public static SelenideElement catalogDate() {
    return artifactTable().find("#iq-version-graph-component-details-catalog-date dd");
  }

  public static SelenideElement componentCategory() {
    return artifactTable().find("#iq-version-graph-component-details-category dd");
  }

  public static SelenideElement showDetailsLink() {
    return $$("#aiVersionChartLabels text").find(text("Details"));
  }

  public static SelenideElement versionBar(int i) {
    return $(createSelector("#aiVersionChartViz", "g", "rect[pointer-events=\"all\"]", nthChild(i)));
  }

  public static SelenideElement versionBarHoverText(int i) {
    return versionBar(i).parent().parent().find("text");
  }

  public static SelenideElement viewDetailsButton() {
    return root().find("#iq-version-graph-view-details-btn");
  }

  public static SelenideElement selectAnApplicationMessage() {
    return root().find(".iq-version-graph-app-selector");
  }

  public static Select selectApplications() {
    return new Select(applicationsElement());
  }

  public static SelenideElement applicationsElement() {
    return root().find(".iq-version-graph-app-selector__form-select select");
  }
}
