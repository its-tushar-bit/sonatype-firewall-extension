/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq;

import com.sonatype.clm.testing.functional.elements.NavPills;
import com.sonatype.clm.testing.functional.pages.*;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.TestEncryptionKeyStore;
import com.sonatype.nexus.scm.SourceControlProvider;

import static com.codeborne.selenide.Condition.*;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractMtiqSourceControlEditorTest
    extends AbstractMtiqFunctionalTest
{
  protected static final String YE_OLE_ORGANIZATION = "Ye Ole Organization";

  protected static final String YE_OLE_APPLICATION = "Ye Ole Application";

  protected SourceControlDAO sourceControlDAO;

  protected Owner currentOwner;

  protected static final boolean PR_COMMENTING_ON = true;

  protected static final boolean REMEDIATION_PR_ON = true;

  protected static final boolean SOURCE_EVALS_ON = true;

  protected static final boolean COMMIT_STATUS_ON = true;

  protected static final boolean PR_COMMENTING_OFF = false;

  protected static final boolean REMEDIATION_PR_OFF = false;

  protected static final boolean SOURCE_EVALS_OFF = false;

  protected static final boolean COMMIT_STATUS_OFF = false;

  protected static final String TOKEN =
      new String(new PasswordHandler(new TestEncryptionKeyStore()).encryptPassword(
          "secret_key".toCharArray()));

  protected void init(Owner currentOwner) {
    refreshOrOpen(IndexPage.url());
    loginAsAdmin();
    sourceControlDAO = lookup(SourceControlDAO.class);
    this.currentOwner = currentOwner;
  }

  protected void assertSourceControl(
      final String ownerId,
      final String repositoryUrl,
      final String token,
      final SourceControlProvider provider,
      final boolean prCommentingEnabled,
      final boolean remediationPREnabled,
      final boolean sourceEvalsEnabled,
      final boolean commitStatusEnabled)
  {
    final SourceControl sourceControl = sourceControlDAO.getByOwnerId(ownerId);
    assertThat(sourceControl.getProvider()).isEqualTo(provider);
    assertThat(sourceControl.getRepositoryUrl()).isEqualTo(repositoryUrl);
    assertThat(sourceControl.getOwnerId()).isEqualTo(ownerId);
    // SSH should never be enabled in Multitenant IQ
    assertThat(sourceControl.getSshEnabled()).isNull();
    assertThat(getDecryptedToken(sourceControl.getToken())).isEqualTo(token);
    assertThat(sourceControl.getPullRequestCommentingEnabled()).isEqualTo(prCommentingEnabled);
    assertThat(sourceControl.getRemediationPullRequestsEnabled()).isEqualTo(remediationPREnabled);
    assertThat(sourceControl.getSourceControlEvaluationsEnabled()).isEqualTo(sourceEvalsEnabled);
    assertThat(sourceControl.getCommitStatusEnabled()).isEqualTo(commitStatusEnabled);
  }

  protected void assertSourceControlIsNull(final String ownerId) {
    final SourceControl sourceControl = sourceControlDAO.getByOwnerId(ownerId);
    assertThat(sourceControl).isNull();
  }

  private String getDecryptedToken(String token) {
    return new String(new PasswordHandler(new TestEncryptionKeyStore()).decryptPassword(
        token.toCharArray()));
  }

  protected void navigateToSourceControlEditorPage(boolean isRoot) {
    if (isRoot) {
      refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    }
    else {
      refreshOrOpen(OwnerSummaryPage.url(currentOwner));
      OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
    }

    NavPills navPills = OwnerSummaryPage.navigationPills();
    navPills.sourceControl().click();
    OwnerSummaryPage.sourceControlTile().sourceControlLink().click();
  }
}
