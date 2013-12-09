/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.io.IOException;

import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.model.Application;

import com.excilys.ebi.gatling.app.Gatling;
import com.excilys.ebi.gatling.core.config.GatlingPropertiesBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @since 1.7
 */
public class LdapActiveDirectoryStaticGroupsSimulationTest
    extends AbstractLdapSimulationTest
{

  @BeforeClass
  public static void setup() throws IOException {
    application = new Application("test", "test", null);
    new ApplicationDAO().insert(application);
    
    LdapServerDAO ldapServerDAO = new LdapServerDAO();
    ldapServer = new LdapServer("Active Directory");
    ldapServerDAO.insert(ldapServer);

    configureLDAP("active-directory.properties");
  }

  @Test
  public void testLdapSearchSimulation() {
    GatlingPropertiesBuilder props =
        configureGatling("LDAP Search Simulation with Active Directory Static Groups", "com.sonatype.insight.brain.LdapQuerySimulation");
    int result = Gatling.fromMap(props.build());
    assertThat("Failures were detected from Gatling", result, equalTo(0));
  }

}
