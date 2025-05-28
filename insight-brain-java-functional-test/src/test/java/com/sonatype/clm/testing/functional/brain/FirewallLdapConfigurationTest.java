/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.brain;

import com.sonatype.clm.testing.functional.elements.SidebarNavigation;
import com.sonatype.clm.testing.functional.pages.FirewallLdapConfigurationPage;
import com.sonatype.clm.testing.functional.pages.FirewallLdapServerListPage;
import com.sonatype.clm.testing.functional.pages.FirewallPage;
import com.sonatype.clm.testing.functional.pages.LdapConfigurationPage;
import com.sonatype.clm.testing.functional.pages.LdapServerListPage;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.Condition.attribute;

/**
 * This class exists in response to CLM-34525. Firewall's LDAP configuration page is now on a different route, and
 * on that route, various pieces of route-checking frontend logic no longer worked. To detect that, a separate test
 * is needed that tests the whole page again but in firewall.
 */
public class FirewallLdapConfigurationTest
    extends LdapConfigurationTest
{
  @Override
  @Before
  public void before() {
    // Don't call super.before() - it's not needed
    setLicensedProducts(ProductLicenseDetails.PRODUCT_FIREWALL_V2);
    createTestLdapServer();
    refreshOrOpen(FirewallPage.url());
  }

  @Override
  protected LdapConfigurationPage getLdapConfigurationPage() {
    return new FirewallLdapConfigurationPage();
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
