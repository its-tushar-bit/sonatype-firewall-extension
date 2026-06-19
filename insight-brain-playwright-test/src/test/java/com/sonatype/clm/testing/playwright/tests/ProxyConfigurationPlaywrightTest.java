/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.ProxyConfigurationPage;
import com.sonatype.clm.testing.playwright.pages.ProxyConfigurationPageAssertions;
import com.sonatype.insight.brain.api.v2.service.ApiProxyServerConfigurationService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.categories.SanityTest;
import org.junit.experimental.categories.Category;

/**
 * Playwright test for the Proxy Configuration page.
 */
public class ProxyConfigurationPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String HOSTNAME = "proxy.server";

  private static final String PORT = "8080";

  private static final int PORT_AS_INT = Integer.parseInt(PORT);

  private static final String PROXY_USERNAME = "u";

  private static final String PROXY_PASSWORD = "p";

  /** Five-null-byte mask the UI renders in the password field once a proxy password is saved. */
  private static final String FAKE_PASSWORD = "\u0000\u0000\u0000\u0000\u0000";

  private static final String EXCLUDE_HOSTS_INPUT = "foo.bar,bar.baz,baz.qux";

  private static final String SINGLE_EXCLUDE_HOST = "host.to.exclude";

  @Before
  public void openProxyConfigPageAsAdmin() {
    playwrightRefreshOrOpen(ProxyConfigurationPage.url());
    playwrightLogin();
  }

  @After
  public void cleanup() {
    reverseProxyServer.reset();
  }

  @Override
  protected void afterDatabaseReset() {
    testCLMServer.getCLMServer()
        .getInstance(ApiProxyServerConfigurationService.class)
        .applyProxyServerConfigurationToClients();
  }

  @Test
  @Category(SanityTest.class)
  public void testSave_MinimalData() {
    playwrightRefreshOrOpen(ProxyConfigurationPage.url());

    ProxyConfigurationPage proxyPage = new ProxyConfigurationPage();
    ProxyConfigurationPageAssertions proxyAssertions = new ProxyConfigurationPageAssertions(proxyPage);
    proxyAssertions.shouldHaveNoLoadError();
    proxyAssertions.shouldBeEmpty();

    proxyPage.fillMinimal(HOSTNAME, PORT);
    proxyPage.save();
    waitForSubmitMask();

    proxyAssertions.shouldShowHostname(HOSTNAME);
    proxyAssertions.shouldShowPort(PORT);
    proxyAssertions.shouldShowEmptyUsername();
    proxyAssertions.shouldShowPassword(FAKE_PASSWORD);
    proxyAssertions.shouldShowEmptyExcludeHosts();
  }

  @Test
  @Category(SanityTest.class)
  public void testSave_CompleteData() {
    playwrightRefreshOrOpen(ProxyConfigurationPage.url());

    ProxyConfigurationPage proxyPage = new ProxyConfigurationPage();
    ProxyConfigurationPageAssertions proxyAssertions = new ProxyConfigurationPageAssertions(proxyPage);
    proxyPage.fillAll(HOSTNAME, PORT, PROXY_USERNAME, PROXY_PASSWORD, EXCLUDE_HOSTS_INPUT);
    proxyPage.save();
    waitForSubmitMask();

    proxyAssertions.shouldShowHostname(HOSTNAME);
    proxyAssertions.shouldShowPort(PORT);
    proxyAssertions.shouldShowUsername(PROXY_USERNAME);
    proxyAssertions.shouldShowPassword(FAKE_PASSWORD);
    proxyAssertions.shouldShowExcludeHosts(EXCLUDE_HOSTS_INPUT);
  }

  @Test
  @Category(SanityTest.class)
  public void testReadAndDelete() {
    tempEntity.setProxyServerConfiguration(HOSTNAME, PORT_AS_INT, PROXY_USERNAME,
        PROXY_PASSWORD.toCharArray(), SINGLE_EXCLUDE_HOST);
    playwrightRefreshOrOpen(ProxyConfigurationPage.url());

    ProxyConfigurationPage proxyPage = new ProxyConfigurationPage();
    ProxyConfigurationPageAssertions proxyAssertions = new ProxyConfigurationPageAssertions(proxyPage);
    proxyAssertions.shouldShowHostname(HOSTNAME);
    proxyAssertions.shouldShowPort(PORT);
    proxyAssertions.shouldShowUsername(PROXY_USERNAME);
    proxyAssertions.shouldShowPassword(FAKE_PASSWORD);
    proxyAssertions.shouldShowExcludeHosts(SINGLE_EXCLUDE_HOST);

    proxyAssertions.shouldHideDeleteModal();
    proxyPage.clickDelete();
    proxyAssertions.shouldShowDeleteModal();
    proxyPage.cancelDelete();

    proxyAssertions.shouldShowHostname(HOSTNAME);
    proxyAssertions.shouldShowPort(PORT);

    proxyPage.clickDelete();
    proxyAssertions.shouldShowDeleteModal();
    proxyPage.confirmDelete();
    waitForSubmitMask();

    proxyAssertions.shouldBeEmpty();
  }

  @Test
  @Category(RegressionTest.class)
  public void testProxyConfigurationPageRenders() {
    ProxyConfigurationPage proxyPage = new ProxyConfigurationPage();
    ProxyConfigurationPageAssertions proxyAssertions = new ProxyConfigurationPageAssertions(proxyPage);

    proxyAssertions.shouldHaveNoLoadError();
    proxyAssertions.shouldRenderPageLayout();
  }

  @Test
  @Category(RegressionTest.class)
  public void testProxySaveConfiguration() {
    ProxyConfigurationPage proxyPage = new ProxyConfigurationPage();
    ProxyConfigurationPageAssertions proxyAssertions = new ProxyConfigurationPageAssertions(proxyPage);

    proxyPage.fillMinimal(HOSTNAME, PORT);
    proxyPage.save();
    waitForSubmitMaskSuccess();

    proxyAssertions.shouldShowHostname(HOSTNAME);
    proxyAssertions.shouldShowPort(PORT);
  }
}
