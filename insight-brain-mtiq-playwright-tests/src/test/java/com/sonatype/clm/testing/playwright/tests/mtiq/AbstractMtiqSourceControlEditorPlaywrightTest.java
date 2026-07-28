/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.SourceControlConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.SourceControlRegressionPage;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.EncryptionKeyStore;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.nexus.scm.SourceControlProvider;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(MtiqTest.class)
public abstract class AbstractMtiqSourceControlEditorPlaywrightTest
    extends AbstractMtiqUiTest
{
  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  protected static final String YE_OLE_APPLICATION = "Ye Ole Application";

  protected static final boolean PR_COMMENTING_ON = true;

  protected static final boolean REMEDIATION_PR_ON = true;

  protected static final boolean SOURCE_EVALS_ON = true;

  protected static final boolean COMMIT_STATUS_ON = true;

  protected static final boolean PR_COMMENTING_OFF = false;

  protected static final boolean REMEDIATION_PR_OFF = false;

  protected static final boolean SOURCE_EVALS_OFF = false;

  protected static final boolean COMMIT_STATUS_OFF = false;

  protected static final String PLAIN_TOKEN = "secret_key";

  // Fieldset ids used by the inherit/override radio-based editor (App and Org subclasses).
  // The Root editor uses NxToggle instead and defines its own TOGGLE_* ids.
  protected static final String PROVIDER_FIELDSET = "editor-source-control-provider";

  protected static final String CREDENTIALS_FIELDSET = "editor-source-control-token";

  protected static final String BASE_BRANCH_FIELDSET = "source-control-default-branch";

  protected static final String SSH_FIELDSET = "source-control-ssh";

  // Deterministic MTIQ feature set: automation supported, PRs disabled (hides remediation/ssh/manual).
  protected static final String[] MTIQ_SCM_FEATURES =
      {"multi-tenant", "automation", "saas-lifecycle-scm-enabled", "notifications"};

  protected SourceControlDAO sourceControlDAO;

  protected Owner currentOwner;

  protected void init(Owner currentOwner) {
    playwrightRefreshOrOpen("/");
    playwrightLogin();
    sourceControlDAO = lookup(SourceControlDAO.class);
    this.currentOwner = currentOwner;
  }

  protected String encryptToken(String plain) {
    return new String(passwordHandler().encryptPassword(plain.toCharArray()));
  }

  private String decryptToken(String encrypted) {
    return new String(passwordHandler().decryptPassword(encrypted.toCharArray()));
  }

  private PasswordHandler passwordHandler() {
    return new PasswordHandler(lookup(EncryptionKeyStore.class));
  }

  protected void assertSourceControl(
      final String ownerId,
      final String repositoryUrl,
      final String token,
      final SourceControlProvider provider,
      final Boolean prCommentingEnabled,
      final Boolean remediationPREnabled,
      final Boolean sourceEvalsEnabled,
      final Boolean commitStatusEnabled)
  {
    final SourceControl sourceControl = sourceControlDAO.getByOwnerId(ownerId);
    assertThat(sourceControl).as("Expected source control to be saved for owner " + ownerId).isNotNull();
    assertThat(sourceControl.getProvider()).isEqualTo(provider);
    assertThat(sourceControl.getRepositoryUrl()).isEqualTo(repositoryUrl);
    assertThat(sourceControl.getOwnerId()).isEqualTo(ownerId);
    // SSH should never be enabled in Multitenant IQ.
    assertThat(sourceControl.getSshEnabled()).isNull();
    assertThat(decryptToken(sourceControl.getToken())).isEqualTo(token);
    assertThat(sourceControl.getPullRequestCommentingEnabled()).isEqualTo(prCommentingEnabled);
    assertThat(sourceControl.getRemediationPullRequestsEnabled()).isEqualTo(remediationPREnabled);
    assertThat(sourceControl.getSourceControlEvaluationsEnabled()).isEqualTo(sourceEvalsEnabled);
    assertThat(sourceControl.getCommitStatusEnabled()).isEqualTo(commitStatusEnabled);
  }

  protected void assertSourceControlIsNull(final String ownerId) {
    assertThat(sourceControlDAO.getByOwnerId(ownerId)).isNull();
  }

  protected void navigateToOrgSourceControlEditor(String organizationId) {
    navigateAndWaitForUrl(
        OwnerSummaryPage.editOrganizationUrl(organizationId, SourceControlConfigurationPage.URL_FRAGMENT),
        SourceControlConfigurationPage.URL_FRAGMENT);
  }

  protected void navigateToAppSourceControlEditor(String appPublicId) {
    navigateAndWaitForUrl(
        OwnerSummaryPage.editApplicationUrl(appPublicId, SourceControlConfigurationPage.URL_FRAGMENT),
        SourceControlConfigurationPage.URL_FRAGMENT);
  }

  /**
   * Mocks the MTIQ SCM feature set, navigates to the owner-type-specific editor via
   * {@link #navigateToEditor()}, and reloads so the mocked product-features response is applied.
   * Subclasses provide the owner-type-specific navigation and the {@link SourceControlRegressionPage}
   * used for mocking.
   */
  protected void navigateWithMockedFeatures() {
    scm().mockProductFeatures(MTIQ_SCM_FEATURES);
    navigateToEditor();
    // Reload so the mocked /rest/product/features response is applied to the editor.
    page.reload();
    playwrightWaitUntilUrlContains(SourceControlConfigurationPage.URL_FRAGMENT);
  }

  /** Owner-type-specific navigation (App uses public id, Org/RootOrg use owner id). */
  protected abstract void navigateToEditor();

  /** The subclass-owned {@link SourceControlRegressionPage} used to mock product features. */
  protected abstract SourceControlRegressionPage scm();
}
