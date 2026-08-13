/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Playwright page object for the LDAP configuration screens:
 * list ({@code #/ldap-servers}), create ({@code #/ldap/create}),
 * edit connection ({@code #/ldap/edit/{id}}), and edit user mapping
 * ({@code #/ldap/edit/{id}/userMapping}).
 */
public class LdapPage
    extends BasePage
{
  private static final String EDITOR_ROOT = "#ldap-configuration-editor";

  private static final String CREATE_ROOT = "#ldap-create-server";

  public LdapPage() {
    super();
  }

  public static String listUrl() {
    return "/assets/index.html#/ldap-servers";
  }

  public static String createUrl() {
    return "/assets/index.html#/ldap/create";
  }

  public static String editConnectionUrl(String ldapId) {
    return "/assets/index.html#/ldap/edit/" + ldapId;
  }

  public static String editUserMappingUrl(String ldapId) {
    return "/assets/index.html#/ldap/edit/" + ldapId + "/userMapping";
  }

  public Locator listPageHeading() {
    return page.getByRole(AriaRole.HEADING,
        new Page.GetByRoleOptions().setName("LDAP").setExact(true));
  }

  public Locator tileHeading() {
    return page.getByRole(AriaRole.HEADING,
        new Page.GetByRoleOptions().setName("Configure LDAP"));
  }

  public Locator addServerButton() {
    return page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Add a Server"));
  }

  public Locator serverList() {
    return locator("#ldap-server-list");
  }

  public Locator serverListItems() {
    return serverList().getByRole(AriaRole.LISTITEM)
        .filter(new Locator.FilterOptions().setHasNotText("No LDAP servers are defined"));
  }

  public Locator emptyListMessage() {
    return serverList().getByRole(AriaRole.LISTITEM)
        .filter(new Locator.FilterOptions().setHasText("No LDAP servers are defined"));
  }

  public Locator createContainer() {
    return locator(CREATE_ROOT);
  }

  public Locator createPageHeading() {
    return locator(CREATE_ROOT).getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Add a Server").setLevel(1));
  }

  public Locator createForm() {
    return locator("#ldap-create");
  }

  public Locator serverNameInput() {
    return page.getByLabel("Server Name");
  }

  public Locator editorContainer() {
    return locator(EDITOR_ROOT);
  }

  public Locator editorPageHeading() {
    return locator(EDITOR_ROOT).getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Edit Server").setLevel(1));
  }

  public Locator hostnameInput() {
    return editorContainer().getByLabel("Hostname");
  }

  public Locator portInput() {
    return editorContainer().getByLabel("Port");
  }

  public Locator searchBaseInput() {
    return editorContainer().getByLabel("Search Base");
  }

  public Locator protocolSelector() {
    return editorContainer().getByLabel("Protocol");
  }

  public Locator authMethodSelector() {
    return editorContainer().getByLabel("Method");
  }

  public Locator usernameInput() {
    return editorContainer().getByLabel("Username", new Locator.GetByLabelOptions().setExact(true));
  }

  public Locator passwordInput() {
    return editorContainer().getByLabel("Password", new Locator.GetByLabelOptions().setExact(true));
  }

  public Locator testConnectionButton() {
    return page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Test Connection"));
  }

  public Locator removeServerButton() {
    return page.getByRole(AriaRole.BUTTON,
        new Page.GetByRoleOptions().setName("Remove Server"));
  }

  public Locator userMappingTab() {
    return page.getByRole(AriaRole.TAB,
        new Page.GetByRoleOptions().setName("User & Group Settings"));
  }

  public Locator userBaseDnInput() {
    return locator("#ldap-user-base-dn");
  }

  public Locator userObjectClassInput() {
    return locator("#ldap-user-object-class");
  }

  public Locator userIdAttributeInput() {
    return editorContainer().getByLabel("Username Attribute");
  }

  public Locator userRealNameAttributeInput() {
    return editorContainer().getByLabel("Real Name Attribute");
  }

  public Locator userEmailAttributeInput() {
    return editorContainer().getByLabel("E-mail Attribute");
  }

  public Locator groupMappingTypeSelector() {
    return editorContainer().getByLabel("Group Type");
  }

  public Locator removeModal() {
    return byRole(AriaRole.DIALOG);
  }

  public Locator removeModalHeading() {
    return removeModal().getByRole(AriaRole.HEADING,
        new Locator.GetByRoleOptions().setName("Remove Server"));
  }

  public Locator removeModalWarning() {
    return removeModal().locator(".nx-alert--warning");
  }

  public Locator removeModalDeleteButton() {
    return removeModal().getByRole(AriaRole.BUTTON, CommonButtonOptions.DELETE_BUTTON_OPTS);
  }

  public Locator removeModalCancelButton() {
    return removeModal().getByRole(AriaRole.BUTTON, CommonButtonOptions.CANCEL_BUTTON_OPTS);
  }
}
