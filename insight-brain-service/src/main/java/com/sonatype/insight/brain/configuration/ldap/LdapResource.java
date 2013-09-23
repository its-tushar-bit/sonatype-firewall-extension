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
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapConnectionDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.ldap.LdapRealm;
import com.sonatype.insight.error.exception.BadRequestException;

import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

import org.codehaus.plexus.util.StringUtils;

/**
 * @since 1.7
 */
@Named
@Path(LdapResource.SERVICE_PATH)
public class LdapResource
{

  public static final String SERVICE_PATH = "rest/config/ldap";

  public static final String FAKE_PASSWORD = "#~FAKE~CLM~PASSWORD~#";

  // XXX do we need to obfuscate this or pretend no one will notice?
  private static final String ENC = "CMMDwoV";

  private final LdapServerDAO serverDao = new LdapServerDAO();

  private final LdapConnectionDAO connDao = new LdapConnectionDAO();

  private final LdapUserMappingDAO umapDao = new LdapUserMappingDAO();

  private PlexusCipher cipher;

  @Inject
  public LdapResource(PlexusCipher cipher) {
    this.cipher = cipher;
  }

  /**
   * @since 1.7
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public List<LdapServer> getAll() {
    List<LdapServer> result = new ArrayList<LdapServer>();
    for (LdapServer server : serverDao.getAll()) {
      result.add(server);
    }
    return result;
  }

  /**
   * @since 1.7
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public LdapServer addLdapServer(LdapServer server) {
    serverDao.insert(server);
    return server;
  }

  /**
   * @since 1.7
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public LdapServer updateLdapServer(LdapServer server) {
    serverDao.update(server);
    return server;
  }

  /**
   * @since 1.7
   */
  @DELETE
  @Path("{ldapServerId}")
  public void deleteLdapServer(@PathParam("ldapServerId") final String serverId) {
    serverDao.delete(serverDao.getByIdNotNull(serverId));
  }

  // connection

  /**
   * @since 1.7
   */
  @GET
  @Path("{ldapServerId}/connection")
  @Produces(MediaType.APPLICATION_JSON)
  public LdapConnection getConnection(@PathParam("ldapServerId") String serverId) {
    LdapConnection conn = connDao.getByServerId(serverId);
    if (conn == null) {
      conn = new LdapConnection();
      conn.setServerId(serverId);
    }
    return fakeOutPassword(conn);
  }

  /**
   * @since 1.7
   */
  @PUT
  @Path("{ldapServerId}/connection")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public LdapConnection updateLdapConnection(@PathParam("ldapServerId") String serverId, LdapConnection conn)
      throws PlexusCipherException
  {
    if (serverId == null || !serverId.equals(conn.getServerId())) {
      throw new BadRequestException("Inconsistent ldap server id");
    }

    LdapConnection encrypted = encryptPassword(conn);
    if (conn.getId() != null) {
      connDao.update(encrypted);
    }
    else {
      connDao.insert(encrypted);
    }
    return fakeOutPassword(encrypted);
  }

  // user mapping

  /**
   * @since 1.7
   */
  @GET
  @Path("{ldapServerId}/userMapping")
  @Produces(MediaType.APPLICATION_JSON)
  public LdapUserMapping getUserMapping(@PathParam("ldapServerId") String serverId) {
    LdapUserMapping umap = umapDao.getByServerId(serverId);
    if (umap == null) {
      umap = new LdapUserMapping();
      umap.setServerId(serverId);
    }
    return umap;
  }

  /**
   * @since 1.7
   */
  @PUT
  @Path("{ldapServerId}/userMapping")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public LdapUserMapping updateUserMapping(@PathParam("ldapServerId") String serverId, LdapUserMapping umap)
      throws PlexusCipherException
  {
    if (serverId == null || !serverId.equals(umap.getServerId())) {
      throw new BadRequestException("Inconsistent ldap server id");
    }

    if (umap.getId() != null) {
      umapDao.update(umap);
    }
    else {
      umapDao.insert(umap);
    }
    return umap;
  }

  /**
   * @since 1.7
   */
  @PUT
  @Path("test")
  // XXX connection-test or connection/test?
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public LdapConnectionStatus testConnection(LdapConnection conn) throws PlexusCipherException {
    try {
      String password = conn.getSystemPassword();
      if (FAKE_PASSWORD.equals(password) && conn.getId() != null) {
        password = decryptPassword(connDao.getByIdNotNull(conn.getId())).getSystemPassword();
      }

      LdapRealm.testConnection(conn.getUrl(), conn.getAuthenticationMethod().getMethod(), conn.getSystemUsername(),
          password, conn.getSaslRealm());
      return LdapConnectionStatus.OK;
    }
    catch (NamingException e) {
      return new LdapConnectionStatus(Status.FAILURE, e.toString());
    }
  }

  // password encryption

  private LdapConnection fakeOutPassword(LdapConnection config) {
    if (StringUtils.isNotBlank(config.getSystemPassword())) {
      LdapConnection copy = new LdapConnection(config);
      copy.setSystemPassword(FAKE_PASSWORD);
      return copy;
    }
    return config;
  }

  private LdapConnection decryptPassword(LdapConnection config) throws PlexusCipherException {
    if (StringUtils.isNotBlank(config.getSystemPassword())) {
      LdapConnection copy = new LdapConnection(config);
      copy.setSystemPassword(cipher.decryptDecorated(config.getSystemPassword(), ENC));
      return copy;
    }
    return config;
  }

  private LdapConnection encryptPassword(LdapConnection config) throws PlexusCipherException {
    if (StringUtils.isNotBlank(config.getSystemPassword())) {
      LdapConnection copy = new LdapConnection(config);
      if (FAKE_PASSWORD.equals(config.getSystemPassword())) {
        copy.setSystemPassword(connDao.getByIdNotNull(config.getId()).getSystemPassword());
      }
      else {
        copy.setSystemPassword(cipher.encryptAndDecorate(config.getSystemPassword(), ENC));
      }
      return copy;
    }
    return config;
  }
}
