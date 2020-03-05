/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain;

import java.io.IOException;
import java.util.Properties;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.ldap.LdapAuthenticationMethod;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapProtocol;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.service.InsightBrainService;

import com.excilys.ebi.gatling.core.config.GatlingPropertiesBuilder;
import com.google.common.io.Resources;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TestRule;

/**
 * @since 1.7
 */
public class AbstractLdapSimulationTest
{
  @ClassRule
  public static TestRule startServiceRule = new DropwizardAppRule<>(InsightBrainService.class,
      Resources.getResource("config-test.yml").getPath());

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  protected LdapServer ldapServer;

  protected Application application;

  protected void configureLDAP(final String propertiesFile) throws IOException {
    Properties properties = new Properties();
    properties.load(Resources.getResource(propertiesFile).openStream());
    LdapConnection ldapConnection = new LdapConnection();
    ldapConnection.setServerId(ldapServer.getId());
    ldapConnection.setHostname(prop(properties, "hostname"));
    ldapConnection.setProtocol(LdapProtocol.valueOf(prop(properties, "protocol")));
    ldapConnection.setPort(Integer.valueOf(prop(properties, "port")));
    ldapConnection.setSearchBase(prop(properties, "searchBase"));
    ldapConnection.setAuthenticationMethod(LdapAuthenticationMethod.valueOf(prop(properties, "authenticationMethod")));
    ldapConnection.setSystemUsername(prop(properties, "systemUsername"));
    ldapConnection.setSystemPassword(prop(properties, "systemPassword").toCharArray());
    ldapConnection.setSaslRealm(prop(properties, "saslRealm"));
    ldapConnection.setConnectionTimeout(Integer.valueOf(prop(properties, "connectionTimeout")));
    ldapConnection.setRetryDelay(Integer.valueOf(prop(properties, "retryDelay")));
    new LdapConnectionDAO().insert(ldapConnection);

    LdapUserMapping ldapUserMapping = new LdapUserMapping();
    ldapUserMapping.setServerId(ldapServer.getId());
    ldapUserMapping.setUserPasswordAttribute(prop(properties, "userPasswordAttribute"));
    ldapUserMapping.setUserBaseDN(prop(properties, "userBaseDN"));
    ldapUserMapping.setUserObjectClass(prop(properties, "userObjectClass"));
    ldapUserMapping.setUserIDAttribute(prop(properties, "userIDAttribute"));
    ldapUserMapping.setUserRealNameAttribute(prop(properties, "userRealNameAttribute"));
    ldapUserMapping.setUserEmailAttribute(prop(properties, "userEmailAttribute"));
    ldapUserMapping.setGroupBaseDN(prop(properties, "groupBaseDN"));
    ldapUserMapping.setGroupObjectClass(prop(properties, "groupObjectClass"));
    ldapUserMapping.setGroupIDAttribute(prop(properties, "groupIDAttribute"));
    ldapUserMapping.setGroupMemberAttribute(prop(properties, "groupMemberAttribute"));
    ldapUserMapping.setGroupMemberFormat(prop(properties, "groupMemberFormat"));
    ldapUserMapping.setUserMemberOfGroupAttribute(prop(properties, "userMemberOfGroupAttribute"));
    ldapUserMapping.setGroupMappingType(LdapGroupMappingType.valueOf(prop(properties, "groupMappingType")));
    ldapUserMapping.setDynamicGroupSearchEnabled(true);
    new LdapUserMappingDAO().insert(ldapUserMapping);
  }

  private static String prop(final Properties properties, final String propertyName) {
    return properties.getProperty(propertyName);
  }

  protected GatlingPropertiesBuilder configureGatling(String description, String clazz) {
    GatlingPropertiesBuilder props = new GatlingPropertiesBuilder();
    props.runDescription(description);
    props.clazz(clazz);
    return props;
  }
}
