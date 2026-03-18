/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.elements.NxDeleteModal;
import com.sonatype.clm.testing.functional.elements.Tooltip;
import com.sonatype.clm.testing.functional.elements.UnsavedModal;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.ProxyConfigurationPage;
import com.sonatype.clm.testing.functional.utils.FormUtils;
import com.sonatype.insight.brain.api.v2.service.ApiProxyServerConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.PasswordHandler;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static com.sonatype.clm.testing.functional.utils.FormUtils.DEFAULT_VALIDATION_ERRORS_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openqa.selenium.Keys.BACK_SPACE;

public class ProxyConfigurationPageTest
    extends AbstractFunctionalTest
{
  private final ProxyConfigurationPage proxyConfigurationPage = new ProxyConfigurationPage();

  private static final PasswordHandler pwHandler = testCLMServer.getCLMServer().getInstance(PasswordHandler.class);

  private static final String FAKE_PASSWORD = "\u0000\u0000\u0000\u0000\u0000";

  private ProxyServerConfigurationDAO dao;

  @BeforeClass
  public static void beforeClass() {
    refreshOrOpen(ProxyConfigurationPage.url());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    dao = lookup(ProxyServerConfigurationDAO.class);
  }

  @Override
  protected void afterDatabaseReset() {
    testCLMServer.getCLMServer()
        .getInstance(ApiProxyServerConfigurationService.class)
        .applyProxyServerConfigurationToClients();
  }

  @Test
  public void testSave_MinimalData() {
    refreshOrOpen(ProxyConfigurationPage.url());
    proxyConfigurationPage.loadError().shouldNotBe(visible);
    assertNoProxyServerConfigured();
    eyesWatcher.eyesCheck("Proxy Configuration Page - Empty State");
    proxyConfigurationPage.hostName().setValue("proxy.server");
    proxyConfigurationPage.port().setValue("8080");

    save();

    proxyConfigurationPage.hostName().shouldBe(value("proxy.server"));
    proxyConfigurationPage.port().shouldBe(value("8080"));
    proxyConfigurationPage.username().shouldBe(empty);
    proxyConfigurationPage.password().shouldBe(value(FAKE_PASSWORD));
    proxyConfigurationPage.excludeHosts().shouldBe(empty);

    ProxyServerConfiguration configuration = dao.get();
    assertThat(configuration.getHostname()).isEqualTo("proxy.server");
    assertThat(configuration.getPort()).isEqualTo(8080);
    assertThat(configuration.getUsername()).isNull();
    assertThat(configuration.getPassword()).isNull();
    assertThat(configuration.getExcludeHosts()).isNull();
  }

  @Test
  public void testSave_CompleteData() {
    refreshOrOpen(ProxyConfigurationPage.url());

    proxyConfigurationPage.hostName().setValue("proxy.server");
    proxyConfigurationPage.port().setValue("8080");
    proxyConfigurationPage.username().setValue("u");
    proxyConfigurationPage.password().setValue("p");
    proxyConfigurationPage.excludeHosts().setValue("foo.bar,bar.baz,baz.qux");

    save();

    proxyConfigurationPage.hostName().shouldBe(value("proxy.server"));
    proxyConfigurationPage.port().shouldBe(value("8080"));
    proxyConfigurationPage.username().shouldBe(value("u"));
    proxyConfigurationPage.password().shouldBe(value(FAKE_PASSWORD));
    proxyConfigurationPage.excludeHosts().shouldBe(value("foo.bar,bar.baz,baz.qux"));

    ProxyServerConfiguration proxyServerConfiguration = dao.get();
    assertThat(proxyServerConfiguration.getHostname()).isEqualTo("proxy.server");
    assertThat(proxyServerConfiguration.getPort()).isEqualTo(8080);
    assertThat(proxyServerConfiguration.getUsername()).isEqualTo("u");
    assertThat(decryptPassword(proxyServerConfiguration)).isEqualTo("p");
    assertThat(proxyServerConfiguration.getExcludeHosts()).isEqualTo("foo.bar, bar.baz, baz.qux");
  }

  @Test
  public void testRead() {
    tempEntity.setProxyServerConfiguration("proxy.server", 8080, "u", "p".toCharArray(), "host.to.exclude");

    refreshOrOpen(ProxyConfigurationPage.url());

    proxyConfigurationPage.hostName().shouldBe(value("proxy.server"));
    proxyConfigurationPage.port().shouldBe(value("8080"));
    proxyConfigurationPage.username().shouldBe(value("u"));
    proxyConfigurationPage.password().shouldBe(value(FAKE_PASSWORD));
    proxyConfigurationPage.excludeHosts().shouldBe(value("host.to.exclude"));
  }

  @Test
  public void testDelete() {
    tempEntity.setProxyServerConfiguration("proxy.server", 8080);
    refreshOrOpen(ProxyConfigurationPage.url());

    NxDeleteModal deleteModal = new NxDeleteModal("#proxy-config-delete-modal");

    // Make sure cancel does not delete
    deleteModal.shouldNotBe(visible);
    proxyConfigurationPage.delete().shouldBe(enabled).click();
    deleteModal.shouldBe(visible);
    deleteModal.closeButton().click();

    proxyConfigurationPage.hostName().shouldBe(value("proxy.server"));
    proxyConfigurationPage.port().shouldBe(value("8080"));
    proxyConfigurationPage.username().shouldBe(empty);
    proxyConfigurationPage.password().shouldBe(value(FAKE_PASSWORD));
    proxyConfigurationPage.excludeHosts().shouldBe(empty);
    assertThat(dao.get()).isNotNull();

    // Make sure OK on delete modal does delete
    deleteModal.shouldNotBe(visible);
    proxyConfigurationPage.delete().shouldBe(enabled).click();
    deleteModal.shouldBe(visible);
    eyesWatcher.eyesCheck("Proxy Configuration Page - Delete Modal");

    FormMask.seeAndWaitForDismissal();
    deleteModal.submitButton().click();
    assertNoProxyServerConfigured();
  }

  @Test
  public void testPortIsRequired() {
    refreshOrOpen(ProxyConfigurationPage.url());
    proxyConfigurationPage.hostName().setValue("a.hostname");
    proxyConfigurationPage.save().click();
    FormUtils.getAlertElement(proxyConfigurationPage)
        .shouldBe(visible)
        .shouldBe(text(DEFAULT_VALIDATION_ERRORS_PREFIX + " Hostname and Port are required details."));
    cancel();
  }

  @Test
  public void testHostnameIsRequired() {
    refreshOrOpen(ProxyConfigurationPage.url());
    proxyConfigurationPage.port().setValue("8080");
    proxyConfigurationPage.save().click();
    FormUtils.getAlertElement(proxyConfigurationPage)
        .shouldBe(visible)
        .shouldBe(text(DEFAULT_VALIDATION_ERRORS_PREFIX + " Hostname and Port are required details."));
    cancel();
  }

  @Test
  public void testHostnameUpdateRequiresPassword() {
    tempEntity.setProxyServerConfiguration("host", 8080);
    refreshOrOpen(ProxyConfigurationPage.url());
    proxyConfigurationPage.hostName().setValue("new-host");
    proxyConfigurationPage.save().click();
    FormUtils.getAlertElement(proxyConfigurationPage)
        .shouldBe(visible)
        .shouldBe(text(DEFAULT_VALIDATION_ERRORS_PREFIX +
            " Password must be provided when updating Hostname or Port."));
    cancel();
  }

  @Test
  public void testPortUpdateRequiresPassword() {
    tempEntity.setProxyServerConfiguration("host", 8080);
    refreshOrOpen(ProxyConfigurationPage.url());
    proxyConfigurationPage.port().setValue("9090");
    proxyConfigurationPage.save().click();
    FormUtils.getAlertElement(proxyConfigurationPage)
        .shouldBe(visible)
        .shouldBe(text(DEFAULT_VALIDATION_ERRORS_PREFIX +
            " Password must be provided when updating Hostname or Port."));
    cancel();
  }

  @Test
  public void testUnsavedChangesModal() {
    refreshOrOpen(ProxyConfigurationPage.url());
    proxyConfigurationPage.port().setValue("8080");
    proxyConfigurationPage.hostName().setValue("new-host");

    testUnsavedChangesModal_Cancel();
    testUnsavedChangesModal_Continue();
  }

  @Test
  public void testCancelRevertsAllFields() {
    tempEntity.setProxyServerConfiguration("host", 8080);
    refreshOrOpen(ProxyConfigurationPage.url());

    proxyConfigurationPage.hostName().setValue("new-hostname");
    proxyConfigurationPage.port().setValue("9090");
    proxyConfigurationPage.username().setValue("u");
    proxyConfigurationPage.password().setValue("p");
    proxyConfigurationPage.excludeHosts().setValue("a.host");

    cancel();

    proxyConfigurationPage.hostName().shouldHave(value("host"));
    proxyConfigurationPage.port().shouldHave(value("8080"));
    proxyConfigurationPage.username().shouldBe(empty);
    proxyConfigurationPage.password().shouldHave(value(FAKE_PASSWORD));
    proxyConfigurationPage.excludeHosts().shouldBe(empty);
  }

  @Test
  public void testUpdateExcludeHostsPasswordNotModified() {
    tempEntity.setProxyServerConfiguration("proxy.server", 8080, "u", encrypt("p"), "host.to.exclude");
    refreshOrOpen(ProxyConfigurationPage.url());
    proxyConfigurationPage.excludeHosts().setValue("new.host");

    save();

    ProxyServerConfiguration configuration = dao.get();
    assertThat(configuration.getHostname()).isEqualTo("proxy.server");
    assertThat(configuration.getPort()).isEqualTo(8080);
    assertThat(configuration.getUsername()).isEqualTo("u");
    assertThat(decryptPassword(configuration)).isEqualTo("p");
    assertThat(configuration.getExcludeHosts()).isEqualTo("new.host");
  }

  @Test
  public void testUpdateConfigurationAddCredentials() {
    tempEntity.setProxyServerConfiguration("proxy.server", 8080);
    refreshOrOpen(ProxyConfigurationPage.url());
    proxyConfigurationPage.username().setValue("u");
    proxyConfigurationPage.password().setValue("p");

    save();

    ProxyServerConfiguration configuration = dao.get();
    assertThat(configuration.getHostname()).isEqualTo("proxy.server");
    assertThat(configuration.getPort()).isEqualTo(8080);
    assertThat(configuration.getUsername()).isEqualTo("u");
    assertThat(decryptPassword(configuration)).isEqualTo("p");
    assertThat(configuration.getExcludeHosts()).isNull();
  }

  @Test
  public void testUpdateConfigurationRemovePassword() {
    tempEntity.setProxyServerConfiguration("proxy.server", 8080, "u", encrypt("password"));

    refreshOrOpen(ProxyConfigurationPage.url());

    // Tests the field is completely selected by one click
    proxyConfigurationPage.password().click();
    proxyConfigurationPage.password().sendKeys(BACK_SPACE);

    save();

    ProxyServerConfiguration configuration = dao.get();
    assertThat(configuration.getHostname()).isEqualTo("proxy.server");
    assertThat(configuration.getPort()).isEqualTo(8080);
    assertThat(configuration.getUsername()).isEqualTo("u");
    assertThat(configuration.getPassword()).isNull();
  }

  @Test
  public void testUpdateAddPassword() {
    tempEntity.setProxyServerConfiguration("proxy.server", 8080, "u", null);

    refreshOrOpen(ProxyConfigurationPage.url());

    // Tests the field is completely selected by one click
    proxyConfigurationPage.password().click();
    proxyConfigurationPage.password().sendKeys("my-new-password");

    save();

    ProxyServerConfiguration configuration = dao.get();
    assertThat(configuration.getHostname()).isEqualTo("proxy.server");
    assertThat(configuration.getPort()).isEqualTo(8080);
    assertThat(configuration.getUsername()).isEqualTo("u");
    assertThat(decryptPassword(configuration)).isEqualTo("my-new-password");
  }

  @Test
  public void testAccessWithoutLicense() {
    uninstallLicense();
    refreshOrOpen(DashboardPage.url());
    refresh(); // Refresh to ensure the page is loaded after the license is uninstalled
    refreshOrOpen(ProxyConfigurationPage.url());

    assertNoProxyServerConfigured();
    proxyConfigurationPage.productLicenseNavigation().shouldBe(visible);
  }

  @Test
  public void testAccessWithLicense() {
    refreshOrOpen(ProxyConfigurationPage.url());
    proxyConfigurationPage.productLicenseNavigation().shouldNotBe(visible);
  }

  @Test
  public void testPageNotAccessible() {
    try {
      User user = tempEntity.newUser("john.doe", "John", "Doe", "john@doe.com");
      refreshOrOpen(DashboardPage.url());
      logout();
      login(user.getUsername(), user.getPassword());
      refreshOrOpen(ProxyConfigurationPage.url());
      proxyConfigurationPage.hostName().shouldNotBe(visible);
      proxyConfigurationPage.loadError()
          .shouldBe(visible)
          .shouldHave(text("An error occurred loading data. It appears you do not have permission to access this " +
              "page. If you believe this to be incorrect please contact your administrator."));
    }
    finally {
      logout();
      refreshOrOpen(ProxyConfigurationPage.url());
      loginAsAdmin();
    }
  }

  @Test
  public void testMustShowHostnameAndPortRequiredTooltipForInvalidPort() {
    refreshOrOpen(ProxyConfigurationPage.url());

    proxyConfigurationPage.hostName().setValue("proxy.server");
    proxyConfigurationPage.port().setValue("nineteen-eighty-four");

    proxyConfigurationPage.save().click();
    FormUtils.getAlertElement(proxyConfigurationPage)
        .shouldBe(visible)
        .shouldBe(text(DEFAULT_VALIDATION_ERRORS_PREFIX + " Hostname and Port are required details."));
    cancel();
  }

  private void assertNoProxyServerConfigured() {
    proxyConfigurationPage.hostName().shouldBe(empty);
    proxyConfigurationPage.port().shouldBe(empty);
    proxyConfigurationPage.username().shouldBe(empty);
    proxyConfigurationPage.password().shouldBe(empty);
    proxyConfigurationPage.excludeHosts().shouldBe(empty);
    proxyConfigurationPage.delete().shouldBe(disabled);

    assertThat(dao.get()).isNull();
  }

  private void save() {
    Tooltip.get().shouldNotBe(visible);
    proxyConfigurationPage.save().shouldBe(enabled).click();
    FormMask.seeAndWaitForDismissal();
  }

  private void cancel() {
    proxyConfigurationPage.cancel().shouldBe(enabled).click();
  }

  private char[] encrypt(String password) {
    return pwHandler.encryptPassword(password.toCharArray());
  }

  private String decryptPassword(ProxyServerConfiguration configuration) {
    return String.valueOf(pwHandler.decryptPassword(configuration.getPassword()));
  }

  private void testUnsavedChangesModal_Cancel() {
    refreshOrOpen(DashboardPage.url());
    DashboardPage.dashboardContainer().shouldNotBe(visible);

    UnsavedModal unsavedChangesModal = new UnsavedModal();
    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.cancelButton().click();

    DashboardPage.dashboardContainer().shouldNotBe(visible);

    proxyConfigurationPage.title().shouldBe(visible).shouldHave(text("Configure Proxy"));
  }

  private void testUnsavedChangesModal_Continue() {
    refreshOrOpen(DashboardPage.url());
    DashboardPage.dashboardContainer().shouldNotBe(visible);

    UnsavedModal unsavedChangesModal = new UnsavedModal();
    unsavedChangesModal.shouldBe(visible);
    unsavedChangesModal.continueButton().click();

    DashboardPage.dashboardContainer().shouldBe(visible);
  }
}
