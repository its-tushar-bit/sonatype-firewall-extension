/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.OwnerSummaryPage;
import com.sonatype.clm.testing.playwright.pages.PolicyEditorPage;
import com.sonatype.clm.testing.playwright.pages.UnsavedChangesModalComponent;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Regression tests for the global UI-Router UnsavedChanges guard.
 * Filling the Policy Editor name marks the route dirty; a hash navigation triggers the guard.
 * Cancel blocks the transition; Continue allows it.
 */
public class UnsavedChangesGuardPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String GUARD_POLICY_NAME = "UnsavedChangesGuardPolicy";

  private static final int GUARD_POLICY_THREAT_LEVEL = 5;

  private static final String DIRTY_POLICY_NAME = "DirtyPolicyName";

  private PolicyEditorPage editorPage;

  private UnsavedChangesModalComponent unsavedChangesModal;

  @BeforeEach
  public void setUpPageObjects() {
    editorPage = new PolicyEditorPage();
    unsavedChangesModal = new UnsavedChangesModalComponent();
  }

  @AfterEach
  public void dismissModalIfOpen() {
    unsavedChangesModal.continueIfOpen();
  }

  /** Cancel path: UnsavedChangesModal blocks SPA transition; policy editor stays open. */
  @Test
  @Tag("regression")
  public void testUnsavedChangesGuard_cancelBlocksNavigationAndKeepsForm() {
    Organization org = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(org.getId(), GUARD_POLICY_NAME, GUARD_POLICY_THREAT_LEVEL);

    playwrightRefreshOrOpen(PolicyEditorPage.url(org, policy));
    playwrightLogin();

    editorPage.container().waitFor();

    // Makes the route dirty via the policy-editor Redux slice.
    editorPage.policyName().fill(DIRTY_POLICY_NAME);

    // Hash navigation triggers the UI-Router onBefore guard.
    playwrightRefreshOrOpen(OwnerSummaryPage.url(org.getId()));
    assertThat(unsavedChangesModal.container()).isVisible();

    unsavedChangesModal.cancelButton().click();
    assertThat(unsavedChangesModal.container()).isHidden();
    assertThat(editorPage.container()).isVisible();
  }

  /** Continue path: UnsavedChangesModal allows SPA transition; dirty name NOT persisted. */
  @Test
  @Tag("regression")
  public void testUnsavedChangesGuard_continueDiscardsChangesAndNavigates() {
    Organization org = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(org.getId(), GUARD_POLICY_NAME, GUARD_POLICY_THREAT_LEVEL);

    playwrightRefreshOrOpen(PolicyEditorPage.url(org, policy));
    playwrightLogin();

    editorPage.container().waitFor();

    // Makes the route dirty via the policy-editor Redux slice.
    editorPage.policyName().fill(DIRTY_POLICY_NAME);

    // Hash navigation triggers the UI-Router onBefore guard.
    playwrightRefreshOrOpen(OwnerSummaryPage.url(org.getId()));
    assertThat(unsavedChangesModal.container()).isVisible();

    unsavedChangesModal.continueButton().click();
    assertThat(unsavedChangesModal.container()).isHidden();
    assertThat(editorPage.container()).isHidden();

    // Navigate back and confirm the dirty name was NOT saved — Continue discards, not saves.
    playwrightRefreshOrOpen(PolicyEditorPage.url(org, policy));
    editorPage.container().waitFor();
    assertThat(editorPage.policyName()).hasValue(GUARD_POLICY_NAME);
  }
}
