/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.DataRetentionEditorPage;
import com.sonatype.clm.testing.playwright.pages.DataRetentionEditorPageAssertions;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Organization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Regression tests for the org-level Data Retention editor
 * ({@code #/management/edit/organization/{orgId}/data-retention}).
 */
public class DataRetentionEditorPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String ORG_NAME_PREFIX = "DataRetentionTestOrg";

  private Organization org;

  @BeforeEach
  public void setUp() {
    org = tempEntity.newOrganization(ORG_NAME_PREFIX + "-" + TemporaryEntity.uuid());
    playwrightRefreshOrOpen(DataRetentionEditorPage.url(org.getId()));
    playwrightLogin();
  }

  @Test
  @Tag("regression")
  public void testDataRetentionEditor_savesAndPersistsRetentionWindow() {
    DataRetentionEditorPage editorPage = new DataRetentionEditorPage();
    DataRetentionEditorPageAssertions assertions = new DataRetentionEditorPageAssertions(editorPage);

    assertions.shouldBeVisible();

    editorPage.clickCustomRadioForBuildStage();
    editorPage.buildStageAgeInput().fill("30");

    editorPage.updateButton().click();
    waitForSubmitMaskSuccess();

    playwrightRefreshOrOpen(DataRetentionEditorPage.url(org.getId()));
    DataRetentionEditorPage reloadedPage = new DataRetentionEditorPage();
    new DataRetentionEditorPageAssertions(reloadedPage).shouldBeVisible();
    assertThat(reloadedPage.buildStageAgeInput()).hasValue("30");
  }

  @Test
  @Tag("regression")
  public void testDataRetentionEditor_invalidAgeShowsValidationError() {
    DataRetentionEditorPage editorPage = new DataRetentionEditorPage();
    DataRetentionEditorPageAssertions assertions = new DataRetentionEditorPageAssertions(editorPage);

    assertions.shouldBeVisible();

    editorPage.clickCustomRadioForBuildStage();
    editorPage.buildStageAgeInput().fill("0");

    // NxStatefulForm renders the submit button as enabled and blocks via onSubmit when
    // validationErrors is set, so the user-observable signal here is the inline field error.
    assertions.shouldShowBuildStageAgeValidationError();
  }
}
