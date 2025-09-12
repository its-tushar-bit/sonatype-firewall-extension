/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.pages.GettingStartedPage;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage;
import com.sonatype.clm.testing.functional.utils.ScrollUtil;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.TestEncryptionKeyStore;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.BeforeClass;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.text;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractSourceControlEditorTest
    extends AbstractFunctionalTest
{
  protected static String TOKEN;

  protected static final String YE_OLE_APPLICATION = "Ye Ole Application";

  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  protected SourceControlDAO sourceControlDAO;

  protected OrganizationDAO organizationDAO;

  protected Organization rootOrganization;

  protected Organization organization;

  @Before
  public void init() {
    sourceControlDAO = lookup(SourceControlDAO.class);
    organizationDAO = lookup(OrganizationDAO.class);
    TOKEN = new String(new PasswordHandler(new TestEncryptionKeyStore()).encryptPassword(
        "secret_key".toCharArray()));
    rootOrganization = organizationDAO.getById(ROOT_ORGANIZATION_ID);
  }

  @BeforeClass
  public static void beforeClass() {
    // not a page that's relevant to this test, just somewhere to log in. Each test must navigate to its relevant page
    refreshOrOpen(GettingStartedPage.url());
    loginAsAdmin();
  }

  protected void deleteSourceControl(final String ownerId) {
    final SourceControl sourceControl = sourceControlDAO.getByOwnerId(ownerId);
    sourceControlDAO.delete(sourceControl);
  }

  protected void assertSourceControl(
      final String ownerId,
      final String repositoryUrl,
      final String token,
      final SourceControlProvider provider)
  {
    final SourceControl sourceControl = sourceControlDAO.getByOwnerId(ownerId);
    assertThat(sourceControl.getProvider()).isEqualTo(provider);
    assertThat(sourceControl.getRepositoryUrl()).isEqualTo(repositoryUrl);
    assertThat(sourceControl.getOwnerId()).isEqualTo(ownerId);
    assertThat(sourceControl.getToken()).satisfiesAnyOf(daoToken -> assertThat(daoToken).isEqualTo(token),
        daoToken -> assertThat(getDecryptedToken(daoToken)).isEqualTo(token));
  }

  private String getDecryptedToken(String token) {
    return new String(new PasswordHandler(new TestEncryptionKeyStore()).decryptPassword(
        token.toCharArray()));
  }

  protected void assertSourceControlDoesNotExist(final String ownerId) {
    final SourceControl sourceControl = sourceControlDAO.getByOwnerId(ownerId);
    assertThat(sourceControl).isNull();
  }

  protected void assertSourceControlManualPullRequest(final String ownerId, final Boolean manualPullRequestsEnabled) {
    final SourceControl sourceControl = sourceControlDAO.getByOwnerId(ownerId);
    assertThat(sourceControl.getManualPullRequestsEnabled()).isEqualTo(manualPullRequestsEnabled);
  }

  protected void assertSourceControlInnerSourceAutomatedUpdates(
      final String ownerId,
      final Boolean innerSourceAutomatedUpdatesEnabled)
  {
    final SourceControl sourceControl = sourceControlDAO.getByOwnerId(ownerId);
    assertThat(sourceControl.getInnerSourceAutomatedUpdatesEnabled()).isEqualTo(innerSourceAutomatedUpdatesEnabled);
  }

  protected abstract void verifyStartNoSourceControl();

  protected abstract void verifyStartWithSourceControl();

  protected void verifyNotificationFeaturesOnly() {
    final String unsupportedMessage = "This feature is not supported by your license";

    // Unsupported fields should be disabled and show tooltip
    ScrollUtil.scrollIntoViewInstantly(SourceControlEditorPage.baseBranchInput());
    SourceControlEditorPage.baseBranchInput().shouldBe(disabled);
    SourceControlEditorPage.baseBranchInput().hover();
    Tooltip.get().shouldHave(text(unsupportedMessage));

    ScrollUtil.scrollIntoViewInstantly(SourceControlEditorPage.remediationPullRequestsFieldset().mainLabel());
    SourceControlEditorPage.remediationPullRequestsFieldset().radioInputs().forEach(radio -> radio.shouldBe(disabled));
    SourceControlEditorPage.remediationPullRequestsFieldset().hover();
    Tooltip.get().shouldHave(text(unsupportedMessage));

    ScrollUtil.scrollIntoViewInstantly(SourceControlEditorPage.pullRequestCommentingFieldset().mainLabel());
    SourceControlEditorPage.pullRequestCommentingFieldset().radioInputs().forEach(radio -> radio.shouldBe(disabled));
    SourceControlEditorPage.pullRequestCommentingFieldset().hover();
    Tooltip.get().shouldHave(text(unsupportedMessage));

    ScrollUtil.scrollIntoViewInstantly(SourceControlEditorPage.sourceControlEvaluationsFieldset().mainLabel());
    SourceControlEditorPage.sourceControlEvaluationsFieldset().radioInputs().forEach(radio -> radio.shouldBe(disabled));
    SourceControlEditorPage.sourceControlEvaluationsFieldset().hover();
    Tooltip.get().shouldHave(text(unsupportedMessage));

    ScrollUtil.scrollIntoViewInstantly(SourceControlEditorPage.automatedCommitFeedbackFieldset().mainLabel());
    SourceControlEditorPage.automatedCommitFeedbackFieldset().radioInputs().forEach(radio -> radio.shouldBe(disabled));
    SourceControlEditorPage.automatedCommitFeedbackFieldset().hover();
    Tooltip.get().shouldHave(text(unsupportedMessage));
  }

  protected void rootOrgVerifyNotificationFeaturesOnly() {
    final String unsupportedMessage = "This feature is not supported by your license";

    // Test tooltips
    ScrollUtil.scrollIntoViewInstantly(SourceControlEditorPage.baseBranchInput());
    SourceControlEditorPage.baseBranchInput().shouldBe(disabled);
    SourceControlEditorPage.baseBranchInput().hover();
    Tooltip.get().shouldHave(text(unsupportedMessage));

    ScrollUtil.scrollIntoViewInstantly(SourceControlEditorPage.remediationPullRequestsFieldset().toggle());
    SourceControlEditorPage.remediationPullRequestsFieldset().toggle().shouldBe(disabled);
    SourceControlEditorPage.remediationPullRequestsFieldset().hover();
    Tooltip.get().shouldHave(text(unsupportedMessage));

    ScrollUtil.scrollIntoViewInstantly(SourceControlEditorPage.pullRequestCommentingFieldset().toggle());
    SourceControlEditorPage.pullRequestCommentingFieldset().toggle().shouldBe(disabled);
    SourceControlEditorPage.pullRequestCommentingFieldset().hover();
    Tooltip.get().shouldHave(text(unsupportedMessage));

    ScrollUtil.scrollIntoViewInstantly(SourceControlEditorPage.sourceControlEvaluationsFieldset().toggle());
    SourceControlEditorPage.sourceControlEvaluationsFieldset().toggle().shouldBe(disabled);
    SourceControlEditorPage.sourceControlEvaluationsFieldset().hover();
    Tooltip.get().shouldHave(text(unsupportedMessage));

    ScrollUtil.scrollIntoViewInstantly(SourceControlEditorPage.automatedCommitFeedbackFieldset().toggle());
    SourceControlEditorPage.automatedCommitFeedbackFieldset().toggle().shouldBe(disabled);
    SourceControlEditorPage.automatedCommitFeedbackFieldset().hover();
    Tooltip.get().shouldHave(text(unsupportedMessage));
  }
}
