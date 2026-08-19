/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ArtifactoryRepoConfigPage;
import com.sonatype.clm.testing.playwright.pages.ArtifactoryRepoConfigPageAssertions;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Regression tests for the Artifactory Repository Base Configurations screen
 * ({@code ArtifactoryRepositoryBaseConfigurations.jsx},
 * URL fragment {@code /artifactoryRepositoryBaseConfigurations}).
 *
 * <p>
 * The {@code BUILT_FROM_SOURCE} feature flag is enabled in {@link #setUp()} and
 * restored in {@link #tearDown()} so sibling test classes see no state change.
 */
public class ArtifactoryRepoConfigPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String SEEDED_BASE_URL = "http://artifactory.example.com";

  private static final String ARIA_DISABLED = "aria-disabled";

  private static final String SEEDED_USERNAME = "admin";

  // Placeholder test credential — not a production secret.
  private static final char[] SEEDED_PASSWORD = "secret".toCharArray();

  private boolean originalBuiltFromSource;

  private Organization testOrg;

  @BeforeEach
  public void setUp() {
    originalBuiltFromSource = SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled();
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(true);
    testOrg = tempEntity.newOrganization();
    navigateToConfig(testOrg.getId());
    playwrightLogin();
  }

  @AfterEach
  public void tearDown() {
    SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.setEnabled(originalBuiltFromSource);
  }

  /**
   * Root org shows only "Disable" and "Enable" radios; no "Inherit".
   * Child org (testOrg) shows "Inherit" in addition to the other options.
   */
  @Test
  @Tag("regression")
  public void testRootOrgPage_showsEnableAndDisableRadiosWithNoInherit() {
    navigateToConfig(Organization.ROOT_ORGANIZATION_ID);
    ArtifactoryRepoConfigPage page = new ArtifactoryRepoConfigPage();
    ArtifactoryRepoConfigPageAssertions assertions = new ArtifactoryRepoConfigPageAssertions(page);
    assertions.shouldBeLoaded();
    assertions.shouldNotShowInheritRadio();
    assertions.shouldShowEnableRadio();
    assertions.shouldShowDisableRadio();
  }

  /** Child org shows the "Inherit" radio. */
  @Test
  @Tag("regression")
  public void testChildOrgPage_showsInheritRadio() {
    ArtifactoryRepoConfigPageAssertions assertions =
        new ArtifactoryRepoConfigPageAssertions(new ArtifactoryRepoConfigPage());
    assertions.shouldBeLoaded();
    assertions.shouldShowInheritRadio();
    assertions.shouldShowEnableAndOverrideRadio();
    assertions.shouldShowDisableRadio();
  }

  /** "Allow Override" checkbox is visible at org-level nodes. */
  @Test
  @Tag("regression")
  public void testOrgPage_showsAllowOverrideCheckbox() {
    ArtifactoryRepoConfigPageAssertions assertions =
        new ArtifactoryRepoConfigPageAssertions(new ArtifactoryRepoConfigPage());
    assertions.shouldBeLoaded();
    assertions.shouldShowAllowOverrideCheckbox();
  }

  /**
   * When the parent org has {@code allowArtifactoryConnectionOverride=false},
   * the child org's config page shows the lock alert and all radio controls are disabled.
   *
   * <p>
   * The parent's {@code allowArtifactoryConnectionOverride} flag is set via the DAO rather than
   * through the UI. The UI PUT path for this field is unreliable in the embedded test environment
   * due to SPA hash-routing: {@code page.navigate()} with a different-org hash is a
   * same-document navigation that preserves Redux state, causing stale {@code serverData} to block
   * the child's {@code load()} dispatch. The DAO update bypasses that race entirely.
   *
   * <p>
   * {@code NxRadio.input()} uses {@code getByRole(AriaRole.RADIO)} which fails to resolve the
   * CSS-hidden radio input when it is also HTML-disabled. Raw CSS selectors are used instead —
   * the NxRadio {@code id} prop lands on the wrapper {@code <label>}, and a child combinator
   * descends to the actual {@code <input type="radio">}.
   */
  @Test
  @Tag("regression")
  public void testLockedPage_showsLockAlertAndDisabledControls() {
    OrganizationDAO organizationDAO = testCLMServer.getCLMServer().getInstance(OrganizationDAO.class);
    Organization parentOrg = tempEntity.newOrganization();
    parentOrg.setAllowArtifactoryConnectionOverride(false);
    organizationDAO.update(parentOrg);
    Organization lockedChild = tempEntity.newOrganization(parentOrg);
    // Navigate twice: first call is a hash-change (component stays mounted from @Before setup),
    // second call hits the same URL and triggers page.reload() in playwrightRefreshOrOpen,
    // which forces the component to remount and dispatches load() for lockedChild's org.
    // TODO (CLM-42177): Remove double-navigate once hash-routing component lifecycle is fixed.
    navigateToConfig(lockedChild.getId());
    navigateToConfig(lockedChild.getId());
    ArtifactoryRepoConfigPage page = new ArtifactoryRepoConfigPage();
    ArtifactoryRepoConfigPageAssertions assertions = new ArtifactoryRepoConfigPageAssertions(page);
    assertions.shouldBeLoaded();
    assertions.shouldShowLockAlert();
    assertThat(page.inheritRadioRawInput()).isDisabled();
    assertThat(page.disableRadioRawInput()).isDisabled();
  }

  /** Selecting "Disable" hides the LOCAL header and the Add button. */
  @Test
  @Tag("regression")
  public void testDisableMode_hidesConnectionListAndDisablesAddButton() {
    ArtifactoryRepoConfigPage page = new ArtifactoryRepoConfigPage();
    ArtifactoryRepoConfigPageAssertions assertions = new ArtifactoryRepoConfigPageAssertions(page);
    assertions.shouldBeLoaded();

    page.disableRadio().label().click();
    assertions.shouldNotShowLocalHeader();
    // The Add button lives inside {formEnabled && (…)} — it is not rendered in Disable mode.
    assertThat(page.addButton()).not().isVisible();
  }

  /**
   * Selecting "Enable and Override" and saving shows the LOCAL header and activates the Add button.
   *
   * <p>
   * The Add button's {@code aria-disabled} state is driven by the server-saved {@code enabled}
   * value, not by the form radio selection. Clicking the radio updates the form state
   * ({@code formEnabled=true}), which renders the section and the LOCAL header immediately, but
   * {@code enabled} remains the old server value until {@link ArtifactoryRepoConfigPage#clickUpdateAndWait}
   * persists the change.
   */
  @Test
  @Tag("regression")
  public void testEnableMode_showsLocalHeaderAndActivatesAddButton() {
    ArtifactoryRepoConfigPage page = new ArtifactoryRepoConfigPage();
    ArtifactoryRepoConfigPageAssertions assertions = new ArtifactoryRepoConfigPageAssertions(page);
    assertions.shouldBeLoaded();

    page.enableAndOverrideRadio().label().click();
    assertions.shouldShowLocalHeader();
    assertThat(page.addButton()).hasAttribute(ARIA_DISABLED, "true");
    page.clickUpdateAndWait();
    assertThat(page.addButton()).not().hasAttribute(ARIA_DISABLED, "true");
  }

  /**
   * Enable mode with no seeded connections shows the empty-list placeholder.
   */
  @Test
  @Tag("regression")
  public void testEnableMode_emptyListMessageShownWhenNoConnectionsExist() {
    ArtifactoryRepoConfigPage page = new ArtifactoryRepoConfigPage();
    ArtifactoryRepoConfigPageAssertions assertions = new ArtifactoryRepoConfigPageAssertions(page);
    assertions.shouldBeLoaded();

    page.enableAndOverrideRadio().label().click();
    assertions.shouldShowEmptyListMessage();
  }

  /**
   * When {@code allowChange=false} (parent disabled override), the Add button is {@code aria-disabled}.
   *
   * <p>
   * Divergence: the row's text says "Changes are not allowed" as the tooltip; the actual
   * tooltip text is "Parent organizations must Allow Override." (from the Redux slice constant).
   *
   * <p>
   * The Add button lives inside {@code {formEnabled && (…)}}. {@code formEnabled} is
   * {@code formState.enabled} — the child org's own saved {@code enabled} value, NOT the
   * inherited value. So {@code lockedChild.enabled} must be {@code true} for the section to
   * render. The parent's {@code allowArtifactoryConnectionOverride=false} makes
   * {@code allowChange=false}, which then drives {@code aria-disabled=true} on the button via
   * {@code aria-disabled={!allowChange || !enabled || …}}.
   *
   * <p>
   * Both fields are set via direct DAO updates rather than through the UI to avoid the SPA
   * hash-routing issue described in {@link #testLockedPage_showsLockAlertAndDisabledControls}.
   */
  @Test
  @Tag("regression")
  public void testLockedMode_addButtonIsDisabled() {
    OrganizationDAO organizationDAO = testCLMServer.getCLMServer().getInstance(OrganizationDAO.class);
    Organization parentOrg = tempEntity.newOrganization();
    // Disable override on the parent so the child's allowChange=false.
    parentOrg.setAllowArtifactoryConnectionOverride(false);
    organizationDAO.update(parentOrg);
    Organization lockedChild = tempEntity.newOrganization(parentOrg);
    // Set the child's own enabled=true so formEnabled=true and the Add button section renders.
    // Without this, formState.enabled=null → {formEnabled && (…)} renders nothing.
    lockedChild.setArtifactoryConnectionEnabled(true);
    organizationDAO.update(lockedChild);
    // Double-navigate to force page.reload() on the second call (same URL), ensuring the
    // component remounts and load() fetches lockedChild's config from the API.
    navigateToConfig(lockedChild.getId());
    navigateToConfig(lockedChild.getId());
    ArtifactoryRepoConfigPage page = new ArtifactoryRepoConfigPage();
    new ArtifactoryRepoConfigPageAssertions(page).shouldBeLoaded();
    assertThat(page.addButton()).hasAttribute(ARIA_DISABLED, "true");
    // Divergence: row says "Changes are not allowed"; actual tooltip is the Redux slice constant.
    new ArtifactoryRepoConfigPageAssertions(page).shouldShowLockedAddButtonTooltip();
  }

  /**
   * Clicking "Add a Repository" opens the modal with the correct title and authentication radio group.
   * The live "Test Configuration" HTTP call to an Artifactory host cannot be automated in the
   * embedded test environment.
   */
  @Test
  @Tag("regression")
  public void testAddModal_opensWithCorrectTitleAndAuthRadiogroup() {
    ArtifactoryRepoConfigPage page = new ArtifactoryRepoConfigPage();
    new ArtifactoryRepoConfigPageAssertions(page).shouldBeLoaded();

    page.enableAndOverrideRadio().label().click();
    page.clickUpdateAndWait();
    page.addButton().click();

    new ArtifactoryRepoConfigPageAssertions(page).shouldShowAddModal();
    assertThat(page.addModalHeading()).containsText("Add Artifactory Repository Configuration");
    assertThat(page.anonymousAuthRadio().label()).isVisible();
    assertThat(page.credentialsAuthRadio().label()).isVisible();
  }

  /** Selecting "Allow Anonymous Access" hides the credential fields. */
  @Test
  @Tag("regression")
  public void testAddModal_anonymousAuth_hidesCredentialFields() {
    ArtifactoryRepoConfigPage page = new ArtifactoryRepoConfigPage();
    page.enableAndOverrideRadio().label().click();
    page.clickUpdateAndWait();
    page.addButton().click();

    page.anonymousAuthRadio().label().click();
    assertThat(page.usernameInput()).not().isVisible();
    assertThat(page.passwordInput()).not().isVisible();
  }

  /** Selecting "Enter Username and Password" reveals the credential fields. */
  @Test
  @Tag("regression")
  public void testAddModal_credentialsAuth_showsUsernameAndPasswordFields() {
    ArtifactoryRepoConfigPage page = new ArtifactoryRepoConfigPage();
    page.enableAndOverrideRadio().label().click();
    page.clickUpdateAndWait();
    page.addButton().click();

    page.credentialsAuthRadio().label().click();
    assertThat(page.usernameInput()).isVisible();
    assertThat(page.passwordInput()).isVisible();
  }

  /** "Test Configuration" is {@code aria-disabled} when Base URL is empty. */
  @Test
  @Tag("regression")
  public void testAddModal_testButtonDisabledWithoutBaseUrl() {
    ArtifactoryRepoConfigPage page = new ArtifactoryRepoConfigPage();
    page.enableAndOverrideRadio().label().click();
    page.clickUpdateAndWait();
    page.addButton().click();

    assertThat(page.testConfigButton()).hasAttribute(ARIA_DISABLED, "true");
  }

  /**
   * Clicking the edit (pen) icon for an existing connection opens the modal
   * with the heading "Edit Artifactory Repository Configuration" and the "Update" submit button.
   * The existing Base URL is pre-populated in the form.
   */
  @Test
  @Tag("regression")
  public void testEditModal_opensPrePopulatedWithExistingValues() {
    enableOrgAndSeedConnection();

    ArtifactoryRepoConfigPage page = new ArtifactoryRepoConfigPage();
    ArtifactoryRepoConfigPageAssertions assertions = new ArtifactoryRepoConfigPageAssertions(page);
    assertions.shouldBeLoaded();
    assertions.shouldShowEditButton();

    page.editButton().click();

    assertions.shouldShowAddModal();
    assertThat(page.addModalHeading()).containsText("Edit Artifactory Repository Configuration");
    assertThat(page.addModal()
        .getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName("Update"))).isVisible();
    assertThat(page.baseUrlInput()).hasValue(SEEDED_BASE_URL);
  }

  /** Clicking "Cancel" in the delete modal keeps the row. */
  @Test
  @Tag("regression")
  public void testDeleteModal_cancelPreservesRow() {
    enableOrgAndSeedConnection();

    ArtifactoryRepoConfigPage page = new ArtifactoryRepoConfigPage();
    ArtifactoryRepoConfigPageAssertions assertions = new ArtifactoryRepoConfigPageAssertions(page);
    assertions.shouldBeLoaded();
    assertions.shouldShowDeleteButton();

    page.deleteButton().click();
    assertions.shouldShowDeleteModal();
    page.deleteCancelButton().click();
    assertions.shouldHideDeleteModal();
    assertions.shouldShowDeleteButton();
  }

  /**
   * Clicking "OK" in the delete modal removes the connection row and issues the DELETE request.
   */
  @Test
  @Tag("regression")
  public void testDeleteModal_confirmRemovesRow() {
    enableOrgAndSeedConnection();

    ArtifactoryRepoConfigPage page = new ArtifactoryRepoConfigPage();
    ArtifactoryRepoConfigPageAssertions assertions = new ArtifactoryRepoConfigPageAssertions(page);
    assertions.shouldBeLoaded();

    page.deleteButton().click();
    assertions.shouldShowDeleteModal();
    page.clickDeleteConfirmAndWait();
    assertions.shouldHideDeleteModal();
    assertions.shouldNotShowEditButton();
  }

  /**
   * Selecting "Enable and Override" and clicking "Update" persists the configuration
   * via a PUT to the status endpoint.
   */
  @Test
  @Tag("regression")
  public void testUpdateForm_savesEnableConfiguration() {
    ArtifactoryRepoConfigPage page = new ArtifactoryRepoConfigPage();
    new ArtifactoryRepoConfigPageAssertions(page).shouldBeLoaded();

    page.enableAndOverrideRadio().label().click();
    page.clickUpdateAndWait();

    assertThat(page.enableAndOverrideRadio().input()).isChecked();
  }

  private void navigateToConfig(String ownerId) {
    playwrightRefreshOrOpen(ArtifactoryRepoConfigPage.url(ownerId));
  }

  /**
   * Sets {@code testOrg} to enabled mode and seeds one connection, then reloads the page.
   * Used by tests that require a connection row to be visible in the list.
   */
  private void enableOrgAndSeedConnection() {
    testOrg.setArtifactoryConnectionEnabled(true);
    testCLMServer.getCLMServer().getInstance(OrganizationDAO.class).update(testOrg);
    tempEntity.newArtifactoryConnection(testOrg.getId(), SEEDED_BASE_URL, SEEDED_USERNAME, SEEDED_PASSWORD);
    navigateToConfig(testOrg.getId());
  }
}
