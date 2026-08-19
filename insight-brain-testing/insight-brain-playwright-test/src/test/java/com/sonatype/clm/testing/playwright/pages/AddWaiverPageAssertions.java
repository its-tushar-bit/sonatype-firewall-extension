/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class AddWaiverPageAssertions
{
  private final AddWaiverPage page;

  public AddWaiverPageAssertions(AddWaiverPage page) {
    this.page = page;
  }

  public void shouldShowPageLayout(
      String artifactId,
      String componentCoords,
      String policyName,
      String constraintName,
      String cveId,
      String vulnerabilityLinkText,
      int scopeCount,
      int radioCount,
      int expiryCount,
      int reasonCount,
      String createdByName)
  {
    assertThat(page.artifactName()).containsText(artifactId);
    assertThat(page.componentName()).containsText(componentCoords);
    assertThat(page.policyName()).containsText(policyName);
    assertThat(page.constraintName()).containsText(constraintName);
    assertThat(page.conditions()).hasCount(1);
    assertThat(page.conditions().first()).containsText(cveId);
    assertThat(page.vulnerabilityDetailsLink()).containsText(vulnerabilityLinkText);
    assertThat(page.currentUserName()).containsText(createdByName);
    assertThat(page.scopeOptions()).hasCount(scopeCount);
    assertThat(page.componentRadios()).hasCount(radioCount);
    assertThat(page.comments()).isEmpty();
    page.assertSharedFormShellLayout(expiryCount, reasonCount);
    assertThat(page.saveButton()).isVisible();
  }

  public void shouldShowScopeOptions(String... expectedLabels) {
    assertThat(page.scopeOptions()).hasCount(expectedLabels.length);
    for (int i = 0; i < expectedLabels.length; i++) {
      assertThat(page.scopeOptions().nth(i)).containsText(expectedLabels[i]);
    }
  }

  public void shouldShowComponentRadioLabels(String... expectedLabels) {
    assertThat(page.componentRadios()).hasCount(expectedLabels.length);
    for (int i = 0; i < expectedLabels.length; i++) {
      assertThat(page.componentRadioLabel(i)).containsText(expectedLabels[i]);
    }
  }

  public void shouldHaveNoSubmitError() {
    assertThat(page.submitError()).isHidden();
  }

  public void shouldShowSubmitError() {
    assertThat(page.submitError()).isVisible();
  }

  public void shouldShowCustomExpiryDateInput() {
    assertThat(page.customExpiryTime()).isVisible();
  }

  public void shouldNotShowVulnerabilityDetailsLink() {
    assertThat(page.vulnerabilityDetailsLink()).hasCount(0);
  }

  public void shouldShowScopeAsRequired() {
    assertThat(page.scopeFieldset()).isVisible();
    assertThat(page.scopeFieldsetLabel()).isVisible();
    assertThat(page.scopeFieldsetLabel()).not().containsText("Optional");
    assertThat(page.scopesDropdown()).isEnabled();
  }

  public void shouldHaveScopePreSelected() {
    assertThat(page.scopesDropdown()).not().hasValue("");
  }

  public void shouldShowAllVersionsRadioDisabled() {
    assertThat(page.allVersionsRadio()).isVisible();
    assertThat(page.allVersionsRadioInput()).isDisabled();
  }
}
