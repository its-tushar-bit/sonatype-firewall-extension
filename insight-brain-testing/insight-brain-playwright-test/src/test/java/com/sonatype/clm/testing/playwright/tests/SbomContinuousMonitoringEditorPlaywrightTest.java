/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.util.regex.Pattern;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.SbomContinuousMonitoringEditorPage;
import com.sonatype.clm.testing.playwright.pages.SbomContinuousMonitoringEditorPageAssertions;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/** Regression tests for the SBOM Continuous Monitoring editor. */
public class SbomContinuousMonitoringEditorPlaywrightTest
    extends AbstractIqUiTest
{
  // The short link redirects 301 to help.sonatype.com — assert the post-redirect host.
  private static final Pattern LEARN_MORE_URL_PATTERN = Pattern.compile("^https://help\\.sonatype\\.com/.*");

  private Organization org;

  @BeforeEach
  public void enableSbomManagerLicense() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_SBOM_MANAGER);
    setFeatures(LicensedFeature.SBOM_MANAGER, LicensedFeature.POLICY_MONITORING);
    org = tempEntity.newOrganization("sbom-cm-" + TemporaryEntity.uuid());
  }

  private SbomContinuousMonitoringEditorPageAssertions openEditor() {
    playwrightRefreshOrOpen(SbomContinuousMonitoringEditorPage.orgUrl(org.getId()));
    playwrightLogin();
    SbomContinuousMonitoringEditorPageAssertions assertions =
        new SbomContinuousMonitoringEditorPageAssertions(new SbomContinuousMonitoringEditorPage());
    assertions.shouldBeVisible();
    return assertions;
  }

  @Test
  @Tag("regression")
  public void testSbomContinuousMonitoring_editorRendersWithToggleAndSubmitControls() {
    SbomContinuousMonitoringEditorPageAssertions assertions = openEditor();

    assertions.shouldShowToggleAndUpdateControls();
    assertions.shouldShowLearnMoreButton();
  }

  /**
   * NxStatefulForm signals "no changes to save" by rendering a role=alert message rather than
   * by disabling the submit button — the indicator assertions check that alert.
   */
  @Test
  @Tag("regression")
  public void testSbomContinuousMonitoring_toggleChangesUpdateButtonDirtyState() {
    SbomContinuousMonitoringEditorPageAssertions assertions = openEditor();
    SbomContinuousMonitoringEditorPage editorPage = new SbomContinuousMonitoringEditorPage();

    assertions.shouldHaveToggleUnchecked();
    assertions.shouldShowNoChangesToSaveIndicator();

    editorPage.enableToggle().click();
    assertions.shouldHaveToggleChecked();
    assertions.shouldShowChangesPendingIndicator();

    editorPage.enableToggle().click();
    assertions.shouldHaveToggleUnchecked();
    assertions.shouldShowNoChangesToSaveIndicator();
  }

  @Test
  @Tag("regression")
  public void testSbomContinuousMonitoring_toggleDisabledWhenParentHasMonitoringConfigured() {
    // Configure CM at ROOT so the child org inherits with toggleEnabled=false. Cleanup is
    // tempEntity.after() — deletes every PolicyMonitoring row, including this ROOT-scoped one.
    tempEntity.newPolicyMonitoring(Organization.ROOT_ORGANIZATION_ID, Stage.ID_COMPLIANCE);

    SbomContinuousMonitoringEditorPageAssertions assertions = openEditor();

    assertions.shouldHaveToggleDisabled();
    assertions.shouldShowNoChangesToSaveIndicator();
  }

  @Test
  @Tag("regression")
  public void testSbomContinuousMonitoring_learnMoreOpensDocumentation() {
    SbomContinuousMonitoringEditorPageAssertions assertions = openEditor();
    SbomContinuousMonitoringEditorPage editorPage = new SbomContinuousMonitoringEditorPage();

    Page popup = page.waitForPopup(() -> editorPage.learnMoreButton().click());
    assertThat(popup).hasURL(LEARN_MORE_URL_PATTERN);
    popup.close();

    assertions.shouldBeVisible();
  }
}
