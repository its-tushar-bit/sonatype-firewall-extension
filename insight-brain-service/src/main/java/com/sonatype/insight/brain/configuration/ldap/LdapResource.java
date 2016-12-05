/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.error.exception.BadRequestException;

/**
 * @since 1.7
 */
@Named
@Path(LdapResource.RESOURCE_PATH)
public class LdapResource
{

  public static final String RESOURCE_PATH = "rest/config/ldap";

  public static final String CONNECTION_PATH = "{ldapServerId}/connection";

  public static final String USER_MAPPING_PATH = "{ldapServerId}/userMapping";

  public static final String TEST_CONNECTION_PATH = "{ldapServerId}/testConnection";

  public static final String TEST_USER_MAPPING_PATH = "{ldapServerId}/testUserMapping";

  public static final String TEST_LOGIN_PATH = "{ldapServerId}/testLogin";

  public static final String PRIORITY_PATH = "priority";

  private final LdapServerDAO serverDao = new LdapServerDAO();

  private final LdapUserMappingDAO umapDao = new LdapUserMappingDAO();

  private final LdapManager ldapManager;

  @Inject
  public LdapResource(LdapManager ldapManager) {
    this.ldapManager = ldapManager;
  }

  /**
   * @since 1.7
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public List<LdapServer> getAll() {
    List<LdapServer> result = new ArrayList<>();
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
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
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
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public LdapServer updateLdapServer(LdapServer server) {
    serverDao.update(server);
    return server;
  }

  /**
   * @since 1.7
   */
  @DELETE
  @Path("{ldapServerId}")
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void deleteLdapServer(@PathParam("ldapServerId") final String serverId) {
    serverDao.delete(serverDao.getByIdNotNull(serverId));
  }

  // connection

  /**
   * @since 1.7
   */
  @GET
  @Path(CONNECTION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public LdapConnection getConnection(@PathParam("ldapServerId") String serverId) {
    return ldapManager.loadConnection(serverId);
  }

  /**
   * @since 1.7
   */
  @PUT
  @Path(CONNECTION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public LdapConnection updateLdapConnection(@PathParam("ldapServerId") String serverId, LdapConnection conn) {
    validateServerId(serverId, conn);
    return ldapManager.saveConnection(conn);
  }

  // user mapping

  /**
   * @since 1.7
   */
  @GET
  @Path(USER_MAPPING_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
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
  @Path(USER_MAPPING_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public LdapUserMapping updateUserMapping(@PathParam("ldapServerId") String serverId, LdapUserMapping umap) {
    validateServerId(serverId, umap);

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
  @Path(TEST_CONNECTION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public LdapConnectionStatus testConnection(@PathParam("ldapServerId") String serverId, LdapConnection conn) {
    validateServerId(serverId, conn);

    try {
      ldapManager.testConnection(conn);
      return LdapConnectionStatus.SUCCESS;
    }
    catch (NamingException e) {
      return new LdapConnectionStatus(Status.FAILURE, e.toString());
    }
  }

  /**
   * Returns 20 random ldap users. Meant to visually inspect user mapping configuration in UI.
   * 
   * @since 1.7
   */
  @PUT
  @Path(TEST_USER_MAPPING_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public List<LdapUser> testUserMapping(@PathParam("ldapServerId") String serverId, LdapUserMapping umap) {
    validateServerId(serverId, umap);

    try {
      return ldapManager.testUserMapping(umap, 20);
    }
    catch (IllegalStateException e) {
      // happens when ldap server connection is not configured
      throw new BadRequestException(e);
    }
    catch (NamingException e) {
      throw new BadRequestException(e);
    }
  }

  /**
   * @since 1.7
   */
  @PUT
  @Path(TEST_LOGIN_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public LdapConnectionStatus testLogin(@PathParam("ldapServerId") String serverId, LdapTestLoginRequest request) {
    LdapUserMapping umap = request.getUserMapping();
    validateServerId(serverId, umap);

    try {
      ldapManager.testUserLogin(umap, request.getUsername(), request.getPassword().toCharArray());
      return LdapConnectionStatus.SUCCESS;
    }
    catch (IllegalStateException e) {
      // happens when ldap server connection is not configured
      return new LdapConnectionStatus(Status.FAILURE, e.toString());
    }
    catch (NamingException e) {
      return new LdapConnectionStatus(Status.FAILURE, e.toString());
    }
  }

  private void validateServerId(String serverId, HasLdapServerId entity) {
    if (serverId == null || entity == null || !serverId.equals(entity.getServerId())) {
      throw new BadRequestException("Inconsistent LDAP server ID.");
    }
  }

  /**
   * @since 1.25
   */
  @PUT
  @Path(PRIORITY_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void updatePriority(List<String> serverIds) {
    serverDao.updatePriority(serverIds);
  }
}
