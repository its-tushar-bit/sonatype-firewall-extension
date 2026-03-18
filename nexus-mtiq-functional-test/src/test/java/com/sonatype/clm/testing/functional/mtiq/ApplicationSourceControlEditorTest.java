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

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.selected;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.FAKE_SECRET_KEY;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationSourceControlEditorTest
    extends AbstractMtiqSourceControlEditorTest
{
  private static final String REPOSITORY_URL = "https://a.com/b/c";

  @Before
  public void init() {
    super.init(tempEntity.newApplicationWithParent(getClass().getSimpleName() + "ȧpp", YE_OLE_APPLICATION,
        YE_OLE_ORGANIZATION));
  }

  @Test
  public void testStartNoSourceControl() {
    navigateToSourceControlEditorPage(false);

    validateInitialFormState();

    eyesWatcher.eyesCheck("'Use SSH for Git Operations' option not visible");

    SourceControlEditorPage.resetButton().shouldBe(visible);
    SourceControlEditorPage.saveButton().shouldBe(visible).shouldHave(text("Update"));
  }

  @Test
  public void testSourceControlEditorUpdate() {
    navigateToSourceControlEditorPage(false);

    SourceControlEditorPage.repositoryUrl().setValue(REPOSITORY_URL);
    SourceControlEditorPage.providerFieldset().labels().get(1).shouldHave(text("Override")).click();
    SourceControlEditorPage.providerSelect().chooseOption(new NxFormSelect.Option(3, "Github"));
    SourceControlEditorPage.token().shouldBe(enabled).setValue("secret_key");
    SourceControlEditorPage.baseBranchFieldset().labels().get(1).shouldHave(text("Override")).click();
    SourceControlEditorPage.baseBranchInput().setValue("main");
    SourceControlEditorPage.pullRequestCommentingFieldset().labels().get(2).shouldHave(text("Disabled")).click();
    SourceControlEditorPage.automatedCommitFeedbackFieldset().labels().get(2).shouldHave(text("Disabled")).click();
    SourceControlEditorPage.sourceControlEvaluationsFieldset().labels().get(2).shouldHave(text("Disabled")).click();
    // SSH not enabled in MTIQ, only in IQ
    SourceControlEditorPage.sshEnabledFieldset().shouldBe(hidden);

    SourceControlEditorPage.saveButton().shouldHave(text("Update")).click();
    FormMask.seeAndWaitForDismissal();

    SourceControlEditorPage.resetButton().shouldBe(enabled);
    SourceControlEditorPage.token().shouldHave(value(FAKE_SECRET_KEY));
    // SSH not enabled in MTIQ, only in IQ
    SourceControlEditorPage.sshEnabledFieldset().shouldBe(hidden);

    assertSourceControl(currentOwner.getId(), REPOSITORY_URL, "secret_key", GITHUB, PR_COMMENTING_OFF,
        REMEDIATION_PR_OFF, SOURCE_EVALS_OFF, COMMIT_STATUS_OFF);
  }

  @Test
  public void testSourceControlEditorReset() {
    tempEntity
        .newSourceControl(currentOwner.getId(), GITHUB, TOKEN, REPOSITORY_URL, "main",
            PR_COMMENTING_ON, REMEDIATION_PR_ON, SOURCE_EVALS_ON, COMMIT_STATUS_ON);

    navigateToSourceControlEditorPage(false);

    assertThat(SourceControlEditorPage.repositoryUrl().getValue()).isEqualTo(REPOSITORY_URL);
    SourceControlEditorPage.providerFieldset().radioInputs().get(1).shouldBe(enabled, selected);
    SourceControlEditorPage.token().shouldBe(enabled);
    SourceControlEditorPage.baseBranchFieldset().radioInputs().get(1).shouldBe(enabled, selected);
    SourceControlEditorPage.pullRequestCommentingFieldset().radioInputs().get(1).shouldBe(enabled, selected);
    SourceControlEditorPage.automatedCommitFeedbackFieldset().radioInputs().get(1).shouldBe(enabled, selected);
    SourceControlEditorPage.sourceControlEvaluationsFieldset().radioInputs().get(1).shouldBe(enabled, selected);
    // SSH not enabled in MTIQ, only in IQ
    SourceControlEditorPage.sshEnabledFieldset().shouldBe(hidden);
    SourceControlEditorPage.saveButton().shouldHave(text("Update"));

    SourceControlEditorPage.resetButton().shouldBe(enabled).click();
    DeleteModal.continueButton().click();
    FormMask.seeAndWaitForDismissal();

    validateInitialFormState();
    assertSourceControlIsNull(ROOT_ORGANIZATION_ID);
  }

  private void validateInitialFormState() {
    SourceControlEditorPage.repositoryUrl().shouldBe(empty);

    SourceControlEditorPage.providerFieldset().labels().get(0).shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.providerFieldset().radioInputs().get(0).shouldBe(enabled, selected);
    SourceControlEditorPage.providerFieldset().labels().get(1).shouldHave(text("Override"));

    SourceControlEditorPage.credentialsFieldset().labels().get(0).shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.credentialsFieldset().labels().get(1).shouldHave(text("Override"));
    SourceControlEditorPage.credentialsFieldset().radioInputs().get(1).shouldBe(disabled, selected);

    SourceControlEditorPage.baseBranchFieldset().labels().get(0).shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.baseBranchFieldset().radioInputs().get(0).shouldBe(disabled, selected);
    SourceControlEditorPage.baseBranchFieldset().labels().get(1).shouldHave(text("Override"));

    SourceControlEditorPage.pullRequestCommentingFieldset()
        .labels()
        .get(0)
        .shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.pullRequestCommentingFieldset().radioInputs().get(0).shouldBe(disabled);
    SourceControlEditorPage.pullRequestCommentingFieldset().labels().get(1).shouldHave(text("Enabled"));
    SourceControlEditorPage.pullRequestCommentingFieldset().radioInputs().get(1).shouldBe(disabled, selected);
    SourceControlEditorPage.pullRequestCommentingFieldset().labels().get(2).shouldHave(text("Disabled"));
    SourceControlEditorPage.pullRequestCommentingFieldset().radioInputs().get(2).shouldBe(disabled);

    SourceControlEditorPage.sourceControlEvaluationsFieldset()
        .labels()
        .get(0)
        .shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.sourceControlEvaluationsFieldset().radioInputs().get(0).shouldBe(disabled);
    SourceControlEditorPage.sourceControlEvaluationsFieldset().labels().get(1).shouldHave(text("Enabled"));
    SourceControlEditorPage.sourceControlEvaluationsFieldset().radioInputs().get(1).shouldBe(disabled, selected);
    SourceControlEditorPage.sourceControlEvaluationsFieldset().labels().get(2).shouldHave(text("Disabled"));
    SourceControlEditorPage.sourceControlEvaluationsFieldset().radioInputs().get(2).shouldBe(disabled);

    SourceControlEditorPage.automatedCommitFeedbackFieldset()
        .labels()
        .get(0)
        .shouldHave(text("Inherit (Not Configured)"));
    SourceControlEditorPage.automatedCommitFeedbackFieldset().radioInputs().get(0).shouldBe(disabled);
    SourceControlEditorPage.automatedCommitFeedbackFieldset().labels().get(1).shouldHave(text("Enabled"));
    SourceControlEditorPage.automatedCommitFeedbackFieldset().radioInputs().get(1).shouldBe(disabled, selected);
    SourceControlEditorPage.automatedCommitFeedbackFieldset().labels().get(2).shouldHave(text("Disabled"));
    SourceControlEditorPage.automatedCommitFeedbackFieldset().radioInputs().get(2).shouldBe(disabled);

    // SSH not enabled for MTIQ, only for IQ
    SourceControlEditorPage.sshEnabledFieldset().shouldBe(hidden);
  }
}
