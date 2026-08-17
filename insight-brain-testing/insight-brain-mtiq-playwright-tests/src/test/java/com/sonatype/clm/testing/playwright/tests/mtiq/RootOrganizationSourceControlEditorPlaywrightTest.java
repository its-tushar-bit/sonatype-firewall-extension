/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.pages.SourceControlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.SourceControlRegressionPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;

/**
 * Root SCM editor uses {@code NxToggle} for boolean options (no inherit/override radios). Product
 * features are mocked deterministically (automation on, PRs off) so Remediation/SSH/Manual PR
 * toggles are omitted. Token field is reached via the GitHub App / PAT chooser after selecting GitHub.
 */
@Tag("mtiq")
public class RootOrganizationSourceControlEditorPlaywrightTest
    extends AbstractMtiqSourceControlEditorPlaywrightTest
{
  private static final String TOGGLE_PR_COMMENTING = "source-control-pull-request-commenting";

  private static final String TOGGLE_EVALUATIONS = "source-control-evaluations";

  private static final String TOGGLE_COMMIT_FEEDBACK = "automated-commit-feedback";

  private static final String TOGGLE_REMEDIATION_PR = "source-control-remediation-pull-requests";

  private static final String TOGGLE_SSH = "source-control-ssh";

  private SourceControlConfigurationPage editor;

  private SourceControlRegressionPage scm;

  @BeforeEach
  public void init() {
    Organization rootOrg = lookup(OrganizationDAO.class).getById(ROOT_ORGANIZATION_ID);
    super.init(rootOrg);
    editor = new SourceControlConfigurationPage();
    scm = new SourceControlRegressionPage();
  }

  private Locator toggleInput(String toggleId) {
    return editor.toggle(toggleId).locator(".nx-toggle__input");
  }

  @Override
  protected SourceControlRegressionPage scm() {
    return scm;
  }

  @Override
  protected void navigateToEditor() {
    navigateToOrgSourceControlEditor(ROOT_ORGANIZATION_ID);
  }

  @Test
  public void testStartNoSourceControl() {
    navigateWithMockedFeatures();

    assertThat(editor.providerSelect()).isVisible();
    assertThat(editor.defaultBranchInput()).isVisible();

    // SSH and Remediation PR are omitted for MTIQ (saas-lifecycle-scm-prs-enabled off).
    assertThat(editor.toggle(TOGGLE_SSH)).hasCount(0);
    assertThat(editor.toggle(TOGGLE_REMEDIATION_PR)).hasCount(0);

    // Remaining option toggles render but are disabled until a provider is selected.
    assertThat(editor.toggle(TOGGLE_PR_COMMENTING)).isVisible();
    assertThat(toggleInput(TOGGLE_PR_COMMENTING)).isDisabled();
    assertThat(editor.toggle(TOGGLE_EVALUATIONS)).isVisible();
    assertThat(toggleInput(TOGGLE_EVALUATIONS)).isDisabled();
    assertThat(editor.toggle(TOGGLE_COMMIT_FEEDBACK)).isVisible();
    assertThat(toggleInput(TOGGLE_COMMIT_FEEDBACK)).isDisabled();

    assertThat(scm.resetButton()).isVisible();
    assertThat(editor.submitButton()).hasText("Create");
  }

  @Test
  public void testSourceControlEditorCreate() {
    navigateWithMockedFeatures();

    editor.selectGitHubPersonalAccessTokenCredentials();
    assertThat(editor.accessTokenInput()).isEnabled();
    editor.accessTokenInput().fill(PLAIN_TOKEN);

    // Options default to enabled/checked once a provider is chosen.
    assertThat(toggleInput(TOGGLE_PR_COMMENTING)).isEnabled();
    assertThat(toggleInput(TOGGLE_PR_COMMENTING)).isChecked();
    assertThat(toggleInput(TOGGLE_EVALUATIONS)).isChecked();
    assertThat(toggleInput(TOGGLE_COMMIT_FEEDBACK)).isChecked();
    assertThat(editor.toggle(TOGGLE_REMEDIATION_PR)).hasCount(0);
    assertThat(editor.toggle(TOGGLE_SSH)).hasCount(0);

    assertThat(editor.submitButton()).hasText("Create");
    editor.submitButton().click();

    assertThat(editor.submitButton()).hasText("Update");
    assertThat(scm.resetButton()).isEnabled();
    assertThat(editor.accessTokenInput()).hasValue(FAKE_SECRET_KEY);

    assertSourceControl(ROOT_ORGANIZATION_ID, null, PLAIN_TOKEN, GITHUB, PR_COMMENTING_ON,
        REMEDIATION_PR_OFF, SOURCE_EVALS_ON, COMMIT_STATUS_ON);
  }

  @Test
  public void testSourceControlEditorUpdate() {
    // Remediation PRs are unsupported in MTIQ (saas-lifecycle-scm-prs-enabled off), so that toggle is
    // never rendered and cannot be changed through the UI; its persisted value is whatever was loaded.
    // Seed it OFF (a valid MTIQ state) so the post-update assertion honestly reflects the editor —
    // the legacy test seeded it ON and relied on now-removed frontend behavior that forced it false.
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, GITHUB, encryptToken(PLAIN_TOKEN), null, "master",
        PR_COMMENTING_ON, REMEDIATION_PR_OFF, SOURCE_EVALS_ON, COMMIT_STATUS_ON);

    navigateWithMockedFeatures();

    editor.selectGitHubPersonalAccessTokenCredentials();
    assertThat(editor.accessTokenInput()).isEnabled();
    editor.accessTokenInput().fill(PLAIN_TOKEN);

    // Flip the three MTIQ options off (all seeded on).
    editor.toggle(TOGGLE_PR_COMMENTING).click();
    editor.toggle(TOGGLE_COMMIT_FEEDBACK).click();
    editor.toggle(TOGGLE_EVALUATIONS).click();
    assertThat(editor.toggle(TOGGLE_REMEDIATION_PR)).hasCount(0);
    assertThat(editor.toggle(TOGGLE_SSH)).hasCount(0);

    assertThat(editor.submitButton()).hasText("Update");
    editor.submitButton().click();

    assertThat(editor.submitButton()).hasText("Update");
    assertThat(scm.resetButton()).isEnabled();
    assertThat(editor.accessTokenInput()).hasValue(FAKE_SECRET_KEY);

    assertSourceControl(ROOT_ORGANIZATION_ID, null, PLAIN_TOKEN, GITHUB, PR_COMMENTING_OFF,
        REMEDIATION_PR_OFF, SOURCE_EVALS_OFF, COMMIT_STATUS_OFF);
  }

  @Test
  public void testSourceControlEditorReset() {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, GITHUB, encryptToken(PLAIN_TOKEN), null, "master",
        PR_COMMENTING_ON, REMEDIATION_PR_ON, SOURCE_EVALS_ON, COMMIT_STATUS_ON);

    navigateWithMockedFeatures();

    assertThat(editor.providerSelect()).hasValue("github");
    assertThat(editor.accessTokenInput()).isEnabled();
    assertThat(toggleInput(TOGGLE_PR_COMMENTING)).isEnabled();
    assertThat(toggleInput(TOGGLE_EVALUATIONS)).isEnabled();
    assertThat(toggleInput(TOGGLE_COMMIT_FEEDBACK)).isEnabled();
    assertThat(editor.toggle(TOGGLE_REMEDIATION_PR)).hasCount(0);
    assertThat(editor.toggle(TOGGLE_SSH)).hasCount(0);
    assertThat(editor.submitButton()).hasText("Update");

    assertThat(scm.resetButton()).isEnabled();
    scm.resetButton().click();
    assertThat(scm.resetModal()).isVisible();
    scm.resetModalContinueButton().click();
    assertThat(scm.resetModal()).isHidden();

    assertSourceControlIsNull(ROOT_ORGANIZATION_ID);
  }
}
