/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the Proxy Configuration page.
 */
public class ProxyConfigurationPage
    extends BasePage
{
  public ProxyConfigurationPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/proxyConfig";
  }

  public Locator hostName() {
    return byLabel("Hostname");
  }

  public Locator port() {
    // byLabel("Port") also matches the "Support Options" button because Playwright's getByLabel
    // does case-insensitive substring matching: "sup*port*" contains "port". Use the stable id.
    return locator("#proxy-config-port");
  }

  public Locator username() {
    return byLabel("Username");
  }

  public Locator password() {
    return byLabel("Password");
  }

  public Locator excludeHosts() {
    return byLabel("Exclude Hosts");
  }

  public Locator saveButton() {
    return byRole(AriaRole.BUTTON, "Save");
  }

  public Locator deleteButton() {
    return byRole(AriaRole.BUTTON, "Delete Configuration");
  }

  public Locator loadError() {
    return byRole(AriaRole.ALERT);
  }

  public Locator deleteModal() {
    // NxModal does not set aria-labelledby in this RSC version — use the stable id.
    return locator("#proxy-config-delete-modal");
  }

  public Locator deleteModalConfirmButton() {
    // submitBtnText="OK" in ProxyConfig.jsx — not "Delete Proxy Configuration?"
    return deleteModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("OK"));
  }

  public Locator deleteModalCancelButton() {
    return deleteModal().getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Cancel"));
  }

  // --------------- Actions ---------------

  public void fillMinimal(String hostname, String port) {
    hostName().fill(hostname);
    port().fill(port);
  }

  public void fillAll(String hostname, String port, String username, String password, String excludeHosts) {
    hostName().fill(hostname);
    port().fill(port);
    username().fill(username);
    password().fill(password);
    excludeHosts().fill(excludeHosts);
  }

  public void save() {
    saveButton().click();
  }

  public void clickDelete() {
    deleteButton().click();
  }

  public void confirmDelete() {
    deleteModalConfirmButton().click();
  }

  public void cancelDelete() {
    deleteModalCancelButton().click();
  }

}
