/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class OidcConfigurationPage
    extends BasePage
{
  private static final Locator.GetByRoleOptions SAVE_OPTS =
      new Locator.GetByRoleOptions().setName("Save").setExact(true);

  private static final Locator.GetByRoleOptions CANCEL_OPTS =
      new Locator.GetByRoleOptions().setName("Cancel");

  private static final Locator.GetByRoleOptions DELETE_CONFIG_OPTS =
      new Locator.GetByRoleOptions().setName("Delete Configuration");

  private static final Locator.GetByRoleOptions OK_OPTS =
      new Locator.GetByRoleOptions().setName("OK");

  public OidcConfigurationPage() {
    super();
  }

  public static String url() {
    return "/assets/index.html#/oidc";
  }

  public Locator container() {
    return locator("#oidc-configuration-page");
  }

  public Locator clientId() {
    return byLabel("Client ID");
  }

  public Locator clientSecret() {
    return byLabel("Client Secret");
  }

  public Locator idpIssuer() {
    return byLabel("IDP Issuer");
  }

  public Locator authorizationUrl() {
    return byLabel("Authorization URL");
  }

  public Locator tokenUrl() {
    return byLabel("Token URL");
  }

  public Locator jwksUrl() {
    return byLabel("JWKS URL");
  }

  public Locator jwsAlgorithm() {
    return byLabel("JWS Algorithm");
  }

  public Locator usernameClaim() {
    return byLabel("Username Claim");
  }

  public Locator emailClaim() {
    return byLabel("Email Claim");
  }

  public Locator saveButton() {
    return locator("#oidc-configuration-page").getByRole(AriaRole.BUTTON, SAVE_OPTS);
  }

  public Locator cancelButton() {
    return locator("#oidc-configuration-page").getByRole(AriaRole.BUTTON, CANCEL_OPTS);
  }

  public Locator deleteButton() {
    return locator("#oidc-configuration-page").getByRole(AriaRole.BUTTON, DELETE_CONFIG_OPTS);
  }

  public Locator deleteModal() {
    return locator("#oidc-config-delete-modal");
  }

  public Locator deleteModalSubmitButton() {
    return deleteModal().getByRole(AriaRole.BUTTON, OK_OPTS);
  }

  public Locator deleteModalCancelButton() {
    return deleteModal().getByRole(AriaRole.BUTTON, CANCEL_OPTS);
  }
}
