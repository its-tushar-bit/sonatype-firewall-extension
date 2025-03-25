/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import org.junit.Before;
import org.junit.Test;

import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.FirewallLdapServerListPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.LdapServerListPage;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import static com.codeborne.selenide.Condition.attribute;

public class FirewallLdapServerListTest
    extends LdapServerListTest
{
  @Before
  public void setFirewallLicense() {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FIREWALL_V2);
    refresh();
    waitUntilUrl(FirewallPage.url());
  }

  @Override
  protected LdapServerListPage getLdapServerListPage() {
    return new FirewallLdapServerListPage();
  }

  // Sanity check that setting the license as above actually results in us being on the firewall page
  @Test
  public void testFirewallPageLogo() {
    navigateToLdapServerList();
    SidebarNavigation.productLogo().shouldHave(attribute("alt", "sonatype firewall"));
  }

  // run all superclass tests, on the firewall LDAP page
}
