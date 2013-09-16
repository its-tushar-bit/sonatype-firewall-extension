/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.naming.NamingException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.configuration.ldap.LdapConnectionStatus.Status;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConfigurationDAO;
import com.sonatype.insight.brain.ldap.LdapRealm;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import org.codehaus.plexus.util.StringUtils;

/**
 * @since 1.7
 */
@Named
@Path(LdapConfigurationResource.SERVICE_PATH)
public class LdapConfigurationResource
{

  public static final String SERVICE_PATH = "rest/config/ldap";

  public static final String FAKE_PASSWORD = "#~FAKE~CLM~PASSWORD~#";

  // XXX do we need to obfuscate this or pretend no one will notice?
  private static final String ENC = "CMMDwoV";

  private final LdapConfigurationDAO dao = new LdapConfigurationDAO();

  private PlexusCipher cipher;

  @Inject
  public LdapConfigurationResource(PlexusCipher cipher) {
    this.cipher = cipher;
  }

  /**
   * @since 1.7
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<LdapConfiguration> getAll() {
    List<LdapConfiguration> result = new ArrayList<LdapConfiguration>();
    for (LdapConfiguration encrypted : dao.getAll()) {
      result.add(fakeOutPassword(encrypted));
    }
    return result;
  }

  /**
   * @since 1.7
   */
  @GET
  @Path("{ldapConfigurationName}")
  @Produces(MediaType.APPLICATION_JSON)
  public LdapConfiguration get(@PathParam("ldapConfigurationName") String configName) {
    return fakeOutPassword(dao.getByName(configName));
  }

  /**
   * @since 1.7
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public LdapConfiguration addLdapConfiguration(LdapConfiguration config) throws PlexusCipherException {
    LdapConfiguration encrypted = encryptPassword(config);
    dao.insert(encrypted);
    return fakeOutPassword(encrypted);
  }

  /**
   * @since 1.7
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public LdapConfiguration updateLdapConfiguration(LdapConfiguration config) throws PlexusCipherException {
    LdapConfiguration encrypted = encryptPassword(config);
    dao.update(encrypted);
    return fakeOutPassword(encrypted);
  }

  @PUT
  @Path("test")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public LdapConnectionStatus testConnection(LdapConfiguration config) throws PlexusCipherException {
    try {
      String password = config.getSystemPassword();
      if (FAKE_PASSWORD.equals(password) && config.getId() != null) {
        password = decryptPassword(dao.getByIdNotNull(config.getId())).getSystemPassword();
      }

      LdapRealm.testConnection(config.getUrl(), config.getAuthenticationMethod().getMethod(),
          config.getSystemUsername(), password, config.getSaslRealm());
      return LdapConnectionStatus.OK;
    }
    catch (NamingException e) {
      return new LdapConnectionStatus(Status.FAILURE, e.toString());
    }
  }

  /**
   * @since 1.7
   */
  @DELETE
  @Path("{ldapConfigurationId}")
  public void deleteLdapConfiguration(@PathParam("ldapConfigurationId") final String configId) {
    dao.delete(dao.getByIdNotNull(configId));
  }

  // password encryption

  private LdapConfiguration fakeOutPassword(LdapConfiguration config) {
    if (StringUtils.isNotBlank(config.getSystemPassword())) {
      LdapConfiguration copy = new LdapConfiguration(config);
      copy.setSystemPassword(FAKE_PASSWORD);
      return copy;
    }
    return config;
  }

  private LdapConfiguration decryptPassword(LdapConfiguration config) throws PlexusCipherException {
    if (StringUtils.isNotBlank(config.getSystemPassword())) {
      LdapConfiguration copy = new LdapConfiguration(config);
      copy.setSystemPassword(cipher.decryptDecorated(config.getSystemPassword(), ENC));
      return copy;
    }
    return config;
  }

  private LdapConfiguration encryptPassword(LdapConfiguration config) throws PlexusCipherException {
    if (StringUtils.isNotBlank(config.getSystemPassword())) {
      LdapConfiguration copy = new LdapConfiguration(config);
      if (FAKE_PASSWORD.equals(config.getSystemPassword())) {
        copy.setSystemPassword(dao.getByIdNotNull(config.getId()).getSystemPassword());
      }
      else {
        copy.setSystemPassword(cipher.encryptAndDecorate(config.getSystemPassword(), ENC));
      }
      return copy;
    }
    return config;
  }
}
