/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.pages;

import com.microsoft.playwright.Locator;

public class MtiqAdministratorsEditPage
    extends AdministratorsEditPage
{
  public MtiqAdministratorsEditPage() {
    super();
  }

  /** Hidden in MTIQ via {@code !isMultiTenant} guard in {@code AdministratorsEdit.jsx}. */
  public Locator ldapGroupSearchAlert() {
    return locator("#ldap-servers-alert");
  }
}
