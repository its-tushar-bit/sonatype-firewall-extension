/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import java.util.List;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.MtiqUserManagementPage;
import com.sonatype.clm.testing.playwright.pages.MtiqUserManagementPageAssertions;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.model.security.User;

import com.microsoft.playwright.Locator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Category(MtiqTest.class)
public class MtiqUserManagementPlaywrightTest
    extends AbstractMtiqUiTest
{
  private static final String INVALID_SPACE_ERROR = "No leading, trailing or double spaces or tabs";

  private static final String INVALID_CHARS_ERROR = "Use valid characters";

  private static final String INVALID_EMAIL_ERROR = "Use valid format";

  private static final String EMPTY_VALUE_ERROR = "Must be non-empty";

  private static final String NO_PERMISSION_ERROR = "It appears you do not have permission to access this page.";

  /**
   * Placeholder tenant-metadata field values used to satisfy the {@code SSO_IDP_MANAGED_BY_SONATYPE}
   * branch. The User Management SPA only checks for row presence, not content.
   */
  private static final String TEST_APP_ID = "appId";

  private static final String TEST_APP_NAME = "appName";

  private static final String TEST_CONNECTION_ID = "connectionId";

  private static final String TEST_CONNECTION_NAME = "connectionName";

  private static final String TEST_ENCRYPTION_KEY_NAME = "encKeyName";

  private MtiqUserManagementPage page;

  private MtiqUserManagementPageAssertions assertions;

  /**
   * Without both the {@code SSO_IDP_MANAGED_BY_SONATYPE} flag and a {@link TenantMetadata} row, the
   * page hits the {@code Invalid request for managed idp} load-error branch on entry.
   */
  @Before
  public void seedManagedIdpAndCreatePageObjects() {
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SSO_IDP_MANAGED_BY_SONATYPE,
        String.valueOf(true));
    seedTenantMetadata();
    page = new MtiqUserManagementPage();
    assertions = new MtiqUserManagementPageAssertions(page);
  }

  private void seedTenantMetadata() {
    lookup(TenantMetadataDAO.class).insert(new TenantMetadata(TEST_APP_ID, TEST_APP_NAME, TEST_CONNECTION_ID,
        TEST_CONNECTION_NAME, TEST_ENCRYPTION_KEY_NAME, "", ""));
  }

  @After
  public void cleanupTenantMetadata() {
    TenantMetadataDAO dao = lookup(TenantMetadataDAO.class);
    dao.getAll().forEach(dao::delete);
  }

  @Test
  public void testMtiqUserManagement_pageLoadsForAdmin() {
    playwrightLoginAdminAt(MtiqUserManagementPage.url());

    assertions.shouldShowPage();
  }

  @Test
  public void testMtiqUserManagement_pageNotAccessibleToNonAdmin() {
    User user = newUser();
    playwrightLoginAt(MtiqUserManagementPage.url(), user.getUsername(), user.getPassword());

    // Frontend authorizationUtil.authErrorMessage — surfaced when checkPermissions(['CONFIGURE_SYSTEM']) fails.
    assertions.shouldShowLoadErrorContaining(NO_PERMISSION_ERROR);
  }

  @Test
  public void testMtiqUserManagement_inviteButtonNavigatesToForm() {
    playwrightLoginAdminAt(MtiqUserManagementPage.url());

    page.inviteUserButton().click();
    assertions.shouldShowInviteForm();
  }

  @Test
  public void testMtiqUserManagement_inviteFormEmptyValuesRejected() {
    openInviteForm();

    List<Locator> inputs =
        List.of(page.inviteFirstNameInput(), page.inviteLastNameInput(), page.inviteEmailInput());
    inputs.forEach(i -> {
      i.fill("a");
      i.clear();
      i.blur();
    });
    inputs.forEach(i -> assertions.shouldShowValidationErrorContaining(i, EMPTY_VALUE_ERROR));
  }

  @Test
  public void testMtiqUserManagement_inviteFormWhitespaceRejected() {
    openInviteForm();

    page.inviteFirstNameInput().fill("a  a");
    page.inviteFirstNameInput().blur();
    page.inviteLastNameInput().fill("a  a");
    page.inviteLastNameInput().blur();

    assertions.shouldShowValidationErrorContaining(page.inviteFirstNameInput(), INVALID_SPACE_ERROR);
    assertions.shouldShowValidationErrorContaining(page.inviteLastNameInput(), INVALID_SPACE_ERROR);
  }

  @Test
  public void testMtiqUserManagement_inviteFormInvalidCharactersRejected() {
    openInviteForm();

    page.inviteFirstNameInput().fill("#");
    page.inviteFirstNameInput().blur();
    page.inviteLastNameInput().fill("#");
    page.inviteLastNameInput().blur();

    assertions.shouldShowValidationErrorContaining(page.inviteFirstNameInput(), INVALID_CHARS_ERROR);
    assertions.shouldShowValidationErrorContaining(page.inviteLastNameInput(), INVALID_CHARS_ERROR);
  }

  @Test
  public void testMtiqUserManagement_inviteFormInvalidEmailRejected() {
    openInviteForm();

    page.inviteEmailInput().fill("not-an-email");
    page.inviteEmailInput().blur();

    assertions.shouldShowValidationErrorContaining(page.inviteEmailInput(), INVALID_EMAIL_ERROR);
  }

  private void openInviteForm() {
    playwrightLoginAdminAt(MtiqUserManagementPage.url());
    page.inviteUserButton().click();
    assertThat(page.inviteForm()).isVisible();
  }
}
