/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.io.IOException;

import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;

import com.excilys.ebi.gatling.app.Gatling;
import com.excilys.ebi.gatling.core.config.GatlingPropertiesBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.7
 */
public class ActiveDirectoryNoDynamicGroupsQueryTest
    extends AbstractLdapSimulationTest
{
  @Before
  public void setup() throws IOException {
    application = tempEntity.newApplicationWithParent("test");
    ldapServer = tempEntity.newLdapServer("Active Directory");

    configureLDAP("active-directory-dynamic.properties");
    LdapUserMappingDAO ldapUserMappingDAO = new LdapUserMappingDAO();
    LdapUserMapping ldapUserMapping = ldapUserMappingDAO.getAll().iterator().next();

    // with this set to true, timeouts occur
    ldapUserMapping.setDynamicGroupSearchEnabled(false);
    ldapUserMappingDAO.update(ldapUserMapping);
  }

  @Test
  public void testDynamicGroupsWithTrailingWildCardsOnlyAndWithoutGroupSearchEnabled() {
    GatlingPropertiesBuilder props = configureGatling(
        "LDAP Search Simulation with Active Directory Dynamic Groups. Dynamic group search is disabled and "
            + "queries contain only trailing wildcards", "com.sonatype.insight.brain.LdapQuerySimulation");

    // none of these test cases return groups from search
    System.setProperty("testCases", "testCasesNoLeadingWildcardsDynamicNoGroupSearch.csv");
    int result = Gatling.fromMap(props.build());
    assertThat(result).as("Failures were detected from Gatling").isZero();
  }
}
