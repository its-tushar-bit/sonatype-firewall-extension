/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.sonatype.insight.brain.model.repository.ProxyRepositoryComponent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Regression-specific page object for the Firewall Component Details page.
 * Sanity-facing locators and URL helpers live in {@link FirewallComponentDetailsPage}.
 */
public class FirewallComponentDetailsRegressionPage
    extends BasePage
{
  private static final String ROOT = "#firewall-component-details-page";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public FirewallComponentDetailsRegressionPage() {
  }

  public static String urlViolationsTab(ProxyRepositoryComponent component) {
    return buildComponentDetailsUrl(component, "violations");
  }

  private static String buildComponentDetailsUrl(ProxyRepositoryComponent component, String tabId) {
    try {
      String componentIdentifierJson =
          URLEncoder.encode(OBJECT_MAPPER.writeValueAsString(component.getComponentIdentifier()),
              StandardCharsets.UTF_8);
      String pathname = URLEncoder.encode(component.getPathname(), StandardCharsets.UTF_8);
      String repositoryId = URLEncoder.encode(component.getRepositoryId(), StandardCharsets.UTF_8);
      String hash = URLEncoder.encode(component.getHash(), StandardCharsets.UTF_8);
      String matchStateId = URLEncoder.encode(component.getMatchStateId(), StandardCharsets.UTF_8);
      return "/assets/index.html#/firewall/repository/" + repositoryId
          + "/component/" + componentIdentifierJson
          + "/" + hash
          + "/" + matchStateId
          + "/" + tabId
          + "?pathname=" + pathname;
    }
    catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to build component details URL", e);
    }
  }

  public Locator container() {
    return locator(ROOT);
  }

  public Locator overviewTab() {
    return locator(ROOT).getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName("Overview"));
  }

  /** "Policy Violations" tab button (live label; manual suite listed it as "Violations"). */
  public Locator violationsTab() {
    return locator(ROOT).getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName("Policy Violations"));
  }

  public Locator securityTab() {
    return locator(ROOT).getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName("Security"));
  }

  public Locator legalTab() {
    return locator(ROOT).getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName("Legal"));
  }

  public Locator labelsTab() {
    return locator(ROOT).getByRole(AriaRole.TAB, new Locator.GetByRoleOptions().setName("Labels"));
  }
}
