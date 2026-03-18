/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq;

import com.sonatype.clm.testing.functional.elements.DeleteModal;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NxFormSelect;
import com.sonatype.clm.testing.functional.pages.SourceControlEditorPage;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.*;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static org.assertj.core.api.Assertions.assertThat;

public class RootOrganizationSourceControlEditorTest
    extends AbstractMtiqSourceControlEditorTest
{
  @Before
  public void init() {
    Organization rootOrg = lookup(OrganizationDAO.class).getById(ROOT_ORGANIZATION_ID);
    super.init(rootOrg);
  }

  @Test
  public void testStartNoSourceControl() {
    navigateToSourceControlEditorPage(true);

    SourceControlEditorPage.providerSelect().shouldBe(visible);
    SourceControlEditorPage.token().shouldBe(visible);
    SourceControlEditorPage.baseBranchInput().shouldBe(visible);
    // SSH not enabled for MTIQ, only for IQ
    SourceControlEditorPage.sshEnabledFieldset().shouldBe(hidden);
    SourceControlEditorPage.pullRequestCommentingFieldset().shouldBe(visible);
    SourceControlEditorPage.pullRequestCommentingFieldset().toggle().shouldBe(disabled);
    SourceControlEditorPage.sourceControlEvaluationsFieldset().shouldBe(visible);
    SourceControlEditorPage.sourceControlEvaluationsFieldset().toggle().shouldBe(disabled);
    SourceControlEditorPage.automatedCommitFeedbackFieldset().shouldBe(visible);
    SourceControlEditorPage.automatedCommitFeedbackFieldset().toggle().shouldBe(disabled);

    SourceControlEditorPage.resetButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldBe(visible).shouldHave(text("Create"));

    eyesWatcher.eyesCheck("'Use SSH for Git Operations' option not visible");
  }

  @Test
  public void testSourceControlEditorCreate() {
    navigateToSourceControlEditorPage(true);

    SourceControlEditorPage.providerSelect().chooseOption(new NxFormSelect.Option(3, "Github"));
    SourceControlEditorPage.token().shouldBe(enabled).setValue("secret_key");
    SourceControlEditorPage.pullRequestCommentingFieldset().toggle().shouldNotBe(disabled).shouldBe(selected);
    SourceControlEditorPage.remediationPullRequestsFieldset().shouldBe(hidden);
    // SSH not enabled in MTIQ, only in IQ
    SourceControlEditorPage.sshEnabledFieldset().shouldBe(hidden);
    SourceControlEditorPage.sourceControlEvaluationsFieldset().toggle().shouldNotBe(disabled).shouldBe(selected);
    SourceControlEditorPage.automatedCommitFeedbackFieldset().toggle().shouldNotBe(disabled).shouldBe(selected);
    SourceControlEditorPage.saveButton().shouldHave(text("Create")).click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.resetButton().shouldBe(enabled);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));

    assertSourceControl(ROOT_ORGANIZATION_ID, null, "secret_key", GITHUB, PR_COMMENTING_ON,
        REMEDIATION_PR_OFF, SOURCE_EVALS_ON, COMMIT_STATUS_ON);
  }

  @Test
  public void testSourceControlEditorUpdate() {
    tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, GITHUB, TOKEN, null, "master",
            PR_COMMENTING_ON, REMEDIATION_PR_ON, SOURCE_EVALS_ON, COMMIT_STATUS_ON);

    navigateToSourceControlEditorPage(true);

    SourceControlEditorPage.providerSelect().chooseOption(new NxFormSelect.Option(3, "Github"));
    SourceControlEditorPage.token().shouldBe(enabled).setValue("secret_key");
    SourceControlEditorPage.pullRequestCommentingFieldset().toggleControl().shouldBe(enabled).click();
    SourceControlEditorPage.remediationPullRequestsFieldset().shouldBe(hidden);
    // SSH not enabled in MTIQ, only in IQ
    SourceControlEditorPage.sshEnabledFieldset().shouldBe(hidden);
    // manual pull request not enabled in MTIQ, only in IQ
    SourceControlEditorPage.manualPullRequestsFieldset().shouldBe(hidden);
    SourceControlEditorPage.automatedCommitFeedbackFieldset().toggleControl().shouldBe(enabled).click();
    SourceControlEditorPage.sourceControlEvaluationsFieldset().toggleControl().shouldBe(enabled).click();
    SourceControlEditorPage.saveButton().shouldHave(text("Update")).click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.saveButton().shouldHave(text("Update"));
    SourceControlEditorPage.resetButton().shouldBe(enabled);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));

    assertSourceControl(ROOT_ORGANIZATION_ID, null, "secret_key", GITHUB, PR_COMMENTING_OFF,
        REMEDIATION_PR_OFF, SOURCE_EVALS_OFF, COMMIT_STATUS_OFF);
  }

  @Test
  public void testSourceControlEditorReset() {
    tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, GITHUB, TOKEN, null, "master",
            PR_COMMENTING_ON, REMEDIATION_PR_ON, SOURCE_EVALS_ON, COMMIT_STATUS_ON);

    navigateToSourceControlEditorPage(true);

    assertThat(SourceControlEditorPage.providerSelect().selectedItem().getText()).isEqualTo("GitHub");
    SourceControlEditorPage.token().shouldBe(enabled);
    SourceControlEditorPage.pullRequestCommentingFieldset().toggleControl().shouldBe(enabled);
    SourceControlEditorPage.remediationPullRequestsFieldset().shouldBe(hidden);
    // SSH not enabled in MTIQ, only in IQ
    SourceControlEditorPage.sshEnabledFieldset().shouldBe(hidden);
    SourceControlEditorPage.automatedCommitFeedbackFieldset().toggleControl().shouldBe(enabled);
    SourceControlEditorPage.sourceControlEvaluationsFieldset().toggleControl().shouldBe(enabled);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));

    SourceControlEditorPage.resetButton().shouldBe(enabled).click();
    DeleteModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();

    assertSourceControlIsNull(ROOT_ORGANIZATION_ID);
  }
}
