/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.LdapPage;
import com.sonatype.clm.testing.playwright.pages.LdapPageAssertions;
import com.sonatype.clm.testing.playwright.pages.ProductLicensePage;
import com.sonatype.clm.testing.playwright.pages.ProductLicensePageAssertions;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

/**
 * Coverage for System Preferences: Product License, EULA, and LDAP screens.
 */
public class ProductLicenseAndLdapPlaywrightTest
    extends AbstractIqUiTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private static final String LDAP_SERVER_NAME_PREFIX = "pw-ldap-server";

  private ProductLicensePage licensePage;

  private ProductLicensePageAssertions licenseAssertions;

  private LdapPage ldapPage;

  private LdapPageAssertions ldapAssertions;

  private String ldapServerId;

  @Before
  public void setUp() {
    LdapServer server = tempEntity.newLdapServer(LDAP_SERVER_NAME_PREFIX + TemporaryEntity.uuid());
    tempEntity.newLdapConnection(server.getId());
    tempEntity.newLdapUserMapping(server.getId());
    ldapServerId = server.getId();

    playwrightRefreshOrOpen(ProductLicensePage.url());
    playwrightLogin();

    licensePage = new ProductLicensePage();
    licenseAssertions = new ProductLicensePageAssertions(licensePage);
    ldapPage = new LdapPage();
    ldapAssertions = new LdapPageAssertions(ldapPage);
  }

  @Test
  @Category(RegressionTest.class)
  public void testProductLicensePage_renders() {
    licenseAssertions.shouldShowPageHeading();
    licenseAssertions.shouldShowLicenseDetails();
    licenseAssertions.shouldShowExpirationDate();
    licenseAssertions.shouldShowLicenseTypes();
    licenseAssertions.shouldShowInstallButton();
  }

  @Test
  @Category(RegressionTest.class)
  public void testProductLicense_eulaModalOnInstall() {
    licenseAssertions.shouldShowInstallButton();
    licensePage.licenseFileInput().setInputFiles(createTempLicenseFile());

    licenseAssertions.shouldShowEulaModal();
    licenseAssertions.shouldShowEulaHeading();
    licenseAssertions.shouldShowEulaAcceptButton();
    licenseAssertions.shouldShowEulaDeclineButton();
  }

  private Path createTempLicenseFile() {
    try {
      Path tempFile = tempDir.newFile("test-license.lic").toPath();
      Files.writeString(tempFile, "dummy-license-content");
      return tempFile;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  @Category(RegressionTest.class)
  public void testLdapServersList_renders() {
    playwrightRefreshOrOpen(LdapPage.listUrl());

    ldapAssertions.shouldShowListHeading();
    ldapAssertions.shouldShowTileHeading();
    ldapAssertions.shouldShowAddServerButton();
    ldapAssertions.shouldShowServerList();
    ldapAssertions.shouldHaveServerCount(1);
  }

  @Test
  @Category(RegressionTest.class)
  public void testLdapCreateServer_formRenders() {
    playwrightRefreshOrOpen(LdapPage.createUrl());

    ldapAssertions.shouldShowCreateContainer();
    ldapAssertions.shouldShowCreateHeading();
    ldapAssertions.shouldShowCreateForm();
    ldapAssertions.shouldShowServerNameInput();
  }

  @Test
  @Category(RegressionTest.class)
  public void testLdapEditConnection_fieldsRender() {
    playwrightRefreshOrOpen(LdapPage.editConnectionUrl(ldapServerId));

    ldapAssertions.shouldShowEditorContainer();
    ldapAssertions.shouldShowEditorHeading();
    ldapAssertions.shouldShowServerNameInput();
    ldapAssertions.shouldShowHostnameInput();
    ldapAssertions.shouldShowPortInput();
    ldapAssertions.shouldShowSearchBaseInput();
    ldapAssertions.shouldShowProtocolSelector();
    ldapAssertions.shouldShowAuthMethodSelector();
    ldapAssertions.shouldShowTestConnectionButton();
    ldapAssertions.shouldShowRemoveServerButton();
  }

  @Test
  @Category(RegressionTest.class)
  public void testLdapEditUserMapping_fieldsRender() {
    playwrightRefreshOrOpen(LdapPage.editUserMappingUrl(ldapServerId));

    ldapAssertions.shouldShowEditorContainer();
    ldapAssertions.shouldShowUserBaseDnInput();
    ldapAssertions.shouldShowUserObjectClassInput();
    ldapAssertions.shouldShowUserIdAttributeInput();
    ldapAssertions.shouldShowUserRealNameAttributeInput();
    ldapAssertions.shouldShowUserEmailAttributeInput();
    ldapAssertions.shouldShowGroupMappingTypeSelector();
  }

  @Test
  @Category(RegressionTest.class)
  public void testLdapRemoveServer_modalRenders() {
    playwrightRefreshOrOpen(LdapPage.editConnectionUrl(ldapServerId));

    ldapAssertions.shouldShowRemoveServerButton();
    ldapPage.removeServerButton().click();

    ldapAssertions.shouldShowRemoveModal();
    ldapAssertions.shouldShowRemoveModalHeading();
    ldapAssertions.shouldShowRemoveModalWarning();
    ldapAssertions.shouldShowRemoveModalDeleteButton();
    ldapAssertions.shouldShowRemoveModalCancelButton();
  }
}
