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
import com.sonatype.clm.testing.functional.pages.ProxyConfigurationPage;
import com.sonatype.insight.brain.api.v2.service.ApiProxyServerConfigurationService;
import com.sonatype.insight.brain.dataaccess.configuration.ProxyServerConfigurationDAO;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.security.PasswordHandler;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.value;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

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

  private String decryptPassword(ProxyServerConfiguration configuration) {
    return String.valueOf(pwHandler.decryptPassword(configuration.getPassword()));
  }
}
