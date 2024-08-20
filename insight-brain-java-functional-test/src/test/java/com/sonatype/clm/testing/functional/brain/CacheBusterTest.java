/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sonatype.clm.testing.functional.AbstractFunctionalTest;
import com.sonatype.clm.testing.functional.elements.MainHeader;
import com.sonatype.clm.testing.functional.elements.SystemConfigMenu;
import com.sonatype.clm.testing.functional.pages.DashboardPage;
import com.sonatype.clm.testing.functional.pages.WebhookConfigurationPage;
import com.sonatype.insight.test.reverseproxy.IRequestHandler;
import com.sonatype.insight.test.reverseproxy.ReverseProxyHandler;

import org.junit.Ignore;
import org.junit.Test;

import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class CacheBusterTest 
    extends AbstractFunctionalTest
{
  @Test
  public void testAxiosCacheBuster() {

    refreshOrOpen(DashboardPage.url());

    loginAsAdmin();
    
    RequestCopyHandler handler = new RequestCopyHandler(testCLMServer.getCLMServer().getPort(), "/rest/config/webhook");
    reverseProxyServer.addHandler(handler);

    SystemConfigMenu systemConfigMenu = MainHeader.systemConfigMenu();
    systemConfigMenu.dropdownToggle().shouldBe(visible).click();
    systemConfigMenu.webhooks().click();
    WebhookConfigurationPage webhookPage = new WebhookConfigurationPage();
    webhookPage.emptyListMessage().shouldBe(visible);

    refreshOrOpen(DashboardPage.url());
    systemConfigMenu.dropdownToggle().shouldBe(visible).click();
    systemConfigMenu.webhooks().click();
    webhookPage = new WebhookConfigurationPage();
    webhookPage.emptyListMessage().shouldBe(visible);

    ArrayList<String> timestamps = handler.getAllTimestamps();

    assertThat(timestamps.get(0)).isNotEqualToIgnoringCase(timestamps.get(1));

  }

  private static class RequestCopyHandler
      implements IRequestHandler
  {
    private final String url;

    private final ReverseProxyHandler reverseProxy;

    private final ArrayList<String> timestamps;

    public RequestCopyHandler(int brainPort, String url) {
      this.url = url;
      this.reverseProxy = new ReverseProxyHandler(brainPort, System.getProperty("proxy.basePath", ""));
      timestamps = new ArrayList<>();
    }

    @Override
    public boolean matches(HttpServletRequest request) {
      return request.getRequestURI().endsWith(url);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
      timestamps.add(request.getParameter("timestamp"));
      reverseProxy.handle(request, response);
    }

    public ArrayList<String> getAllTimestamps() {
      return timestamps;
    }
  }
}
