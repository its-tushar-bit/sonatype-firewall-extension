/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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
 * @since 1.7
 */
public class ActiveDirectoryStaticGroupsQueryTest
    extends AbstractLdapSimulationTest
{

  @Before
  public void setup() throws IOException {
    application = tempEntity.newApplicationWithParent("test");
    ldapServer = tempEntity.newLdapServer("Active Directory");

    configureLDAP("active-directory.properties");
  }

  @Test
  public void testStaticGroupsWithLeadingAndTrailingWildcards() {
    GatlingPropertiesBuilder props =
        configureGatling(
            "LDAP Search Simulation with Active Directory Static Groups. Queries contain both leading and trailing " +
                "wildcards",
            "com.sonatype.insight.brain.LdapQuerySimulation");
    int result = Gatling.fromMap(props.build());
    assertThat("Failures were detected from Gatling", result, equalTo(0));
  }

}
