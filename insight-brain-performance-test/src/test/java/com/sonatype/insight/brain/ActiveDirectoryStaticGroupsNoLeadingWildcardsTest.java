/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.io.IOException;

import com.excilys.ebi.gatling.app.Gatling;
import com.excilys.ebi.gatling.core.config.GatlingPropertiesBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Comparison against queries that execute using pre and post-fixed wildcard characters in
 * LdapActiveDirectoryStaticGroupsSimulationTest.
 * The number of returned results should be smaller in almost all cases, and performance better.
 *
 * @since 1.11
 */
public class ActiveDirectoryStaticGroupsNoLeadingWildcardsTest
    extends AbstractLdapSimulationTest
{

  @Before
  public void setup() throws IOException {
    application = tempEntity.newApplicationWithParent("test");
    ldapServer = tempEntity.newLdapServer("Active Directory");

    configureLDAP("active-directory.properties");
  }

  @Test
  public void testStaticGroupsWithTrailingWildcardQueriesOnly() {
    GatlingPropertiesBuilder props =
        configureGatling(
            "LDAP Search Simulation with Active Directory Static Groups and only trailing wildcards included in the " +
                "queries",
            "com.sonatype.insight.brain.LdapQuerySimulation");

    System.setProperty("testCases", "testCasesNoLeadingWildcardsGroupSearch.csv");
    int result = Gatling.fromMap(props.build());

    assertThat("Failures were detected from Gatling", result, equalTo(0));
  }

}
