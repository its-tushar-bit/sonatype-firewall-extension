/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$;

public class RootOrgMigrateModal
{
  public static SelenideElement root() {
    return $("#root-organization-migrate-modal");
  }

  public static IqRadio selectOrgRadioButton() {
    return new IqRadio($("#select-organization-radio-button"));
  }

  public static IqRadio blankRootRadioButton() {
    return new IqRadio($("#empty-root-radio-button"));
  }

  public static Dropdown organizationSelect() {
    return new Dropdown("#migrate-organization");
  }

  public static SelenideElement continueButton() {
    return $("#root-org-migrate-continue");
  }

  public static SelenideElement retryButton() {
    return $("#root-org-migrate-retry");
  }

  public static SelenideElement cancelButton() {
    return $("#root-org-migrate-cancel");
  }

  public static SelenideElement reloadAppLink() {
    return $("#reload-app-link");
  }
}
