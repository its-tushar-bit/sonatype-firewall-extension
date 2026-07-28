/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;

import com.sonatype.clm.testing.playwright.pages.SourceControlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.SourceControlInheritedOptionComponent;
import com.sonatype.clm.testing.playwright.pages.SourceControlRegressionPage;
import com.sonatype.insight.brain.model.Application;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.sonatype.clm.testing.playwright.pages.SourceControlInheritedOptionComponent.AUTOMATED_COMMIT_FEEDBACK;
import static com.sonatype.clm.testing.playwright.pages.SourceControlInheritedOptionComponent.DISABLED_INDEX;
import static com.sonatype.clm.testing.playwright.pages.SourceControlInheritedOptionComponent.INHERIT_INDEX;
import static com.sonatype.clm.testing.playwright.pages.SourceControlInheritedOptionComponent.PULL_REQUEST_COMMENTING;
import static com.sonatype.clm.testing.playwright.pages.SourceControlInheritedOptionComponent.SOURCE_CONTROL_EVALUATIONS;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;

@Category(MtiqTest.class)
public class ApplicationSourceControlEditorPlaywrightTest
    extends AbstractMtiqSourceControlEditorPlaywrightTest
{
  private static final String REPOSITORY_URL = "https://a.com/b/c";

  private String appPublicId;

  private SourceControlConfigurationPage editor;

  private SourceControlRegressionPage scm;

  private SourceControlInheritedOptionComponent options;

  @Before
  public void init() {
    // The "ȧpp" suffix intentionally forces a non-ASCII character into the public id, exercising
    // URL encoding along the source-control-editor route.
    Application application = tempEntity.newApplicationWithParent(getClass().getSimpleName() + "ȧpp",
        YE_OLE_APPLICATION, YE_OLE_ORGANIZATION);
    appPublicId = application.getPublicId();
    super.init(application);
    editor = new SourceControlConfigurationPage();
    scm = new SourceControlRegressionPage();
    options = new SourceControlInheritedOptionComponent();
  }

  @Override
  protected SourceControlRegressionPage scm() {
    return scm;
  }

  @Override
  protected void navigateToEditor() {
    navigateToAppSourceControlEditor(appPublicId);
  }

  private void validateInitialFormState() {
    assertThat(scm.repoUrlInput()).isVisible();
    assertThat(scm.repoUrlInput()).hasValue("");

    assertThat(options.radioLabels(PROVIDER_FIELDSET).nth(INHERIT_INDEX)).hasText("Inherit (Not Configured)");
    assertThat(options.radioInputs(PROVIDER_FIELDSET).nth(INHERIT_INDEX)).isChecked();
    assertThat(options.radioLabels(PROVIDER_FIELDSET).nth(1)).hasText("Override");
    assertThat(editor.providerSelect()).isDisabled();

    // Credentials / GitHub App auth is not rendered until a provider is selected.
    assertThat(options.fieldset(CREDENTIALS_FIELDSET)).hasCount(0);
    assertThat(scm.githubAuthFieldset()).hasCount(0);

    assertThat(options.radioLabels(BASE_BRANCH_FIELDSET).nth(INHERIT_INDEX)).hasText("Inherit (Not Configured)");
    assertThat(options.radioInputs(BASE_BRANCH_FIELDSET).nth(INHERIT_INDEX)).isChecked();
    assertThat(editor.defaultBranchInput()).isDisabled();

    for (String optionId : new String[]{PULL_REQUEST_COMMENTING, SOURCE_CONTROL_EVALUATIONS,
      AUTOMATED_COMMIT_FEEDBACK})
    {
      assertThat(options.radioLabels(optionId).nth(INHERIT_INDEX)).hasText("Inherit (Not Configured)");
      assertThat(options.radioLabels(optionId).nth(1)).hasText("Enabled");
      assertThat(options.radioLabels(optionId).nth(DISABLED_INDEX)).hasText("Disabled");
      assertThat(options.radioInputs(optionId).nth(INHERIT_INDEX)).isChecked();
      assertThat(options.radioInputs(optionId).nth(INHERIT_INDEX)).isDisabled();
    }

    assertThat(options.fieldset(SSH_FIELDSET)).hasCount(0);
  }

  @Test
  public void testStartNoSourceControl() {
    navigateWithMockedFeatures();

    validateInitialFormState();

    assertThat(scm.resetButton()).isVisible();
    assertThat(editor.submitButton()).hasText("Update");
  }

  @Test
  public void testSourceControlEditorUpdate() {
    navigateWithMockedFeatures();

    scm.repoUrlInput().fill(REPOSITORY_URL);
    scm.providerOverrideRadio().click();
    editor.selectGitHubPersonalAccessTokenCredentials();
    assertThat(editor.accessTokenInput()).isEnabled();
    editor.accessTokenInput().fill(PLAIN_TOKEN);

    scm.branchOverrideRadio().click();
    editor.defaultBranchInput().fill("main");

    options.clickOption(PULL_REQUEST_COMMENTING, "Disabled");
    options.clickOption(AUTOMATED_COMMIT_FEEDBACK, "Disabled");
    options.clickOption(SOURCE_CONTROL_EVALUATIONS, "Disabled");

    assertThat(options.fieldset(SSH_FIELDSET)).hasCount(0);

    assertThat(editor.submitButton()).hasText("Update");
    editor.submitButton().click();

    assertThat(scm.resetButton()).isEnabled();
    assertThat(editor.accessTokenInput()).hasValue(FAKE_SECRET_KEY);

    // Remediation is not user-controllable in MTIQ; a freshly overridden node leaves it inherited (null).
    assertSourceControl(currentOwner.getId(), REPOSITORY_URL, PLAIN_TOKEN, GITHUB, PR_COMMENTING_OFF,
        null, SOURCE_EVALS_OFF, COMMIT_STATUS_OFF);
  }

  @Test
  public void testSourceControlEditorReset() {
    tempEntity.newSourceControl(currentOwner.getId(), GITHUB, encryptToken(PLAIN_TOKEN), REPOSITORY_URL, "main",
        PR_COMMENTING_ON, REMEDIATION_PR_OFF, SOURCE_EVALS_ON, COMMIT_STATUS_ON);

    navigateWithMockedFeatures();

    assertThat(scm.repoUrlInput()).hasValue(REPOSITORY_URL);
    // Own config: provider overridden (index 1). Seeded config carries no authenticationType, so the
    // GitHub auth method shows as inherited (token hidden) — the legacy "token enabled" check is omitted.
    assertThat(options.radioInputs(PROVIDER_FIELDSET).nth(1)).isChecked();
    assertThat(editor.submitButton()).hasText("Update");

    assertThat(scm.resetButton()).isEnabled();
    scm.resetButton().click();
    assertThat(scm.resetModal()).isVisible();
    scm.resetModalContinueButton().click();
    assertThat(scm.resetModal()).isHidden();

    assertSourceControlIsNull(currentOwner.getId());
  }
}
