/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.configuration.ldap.LdapConnectionStatus.Status;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapServerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ldap.LdapUserMappingDAO;
import com.sonatype.insight.brain.model.configuration.ldap.HasLdapServerId;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapGroupMappingType;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.error.exception.BadRequestException;

import com.codahale.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.7
 */
@Named
@Timed
@Path(LdapResource.RESOURCE_PATH)
public class LdapResource
{
  private static final Logger log = LoggerFactory.getLogger(LdapResource.class);

  public static final String RESOURCE_PATH = "rest/config/ldap";

  public static final String CONNECTION_PATH = "{ldapServerId}/connection";

  public static final String USER_MAPPING_PATH = "{ldapServerId}/userMapping";

  public static final String TEST_CONNECTION_PATH = "{ldapServerId}/testConnection";

  public static final String TEST_USER_MAPPING_PATH = "{ldapServerId}/testUserMapping";

  public static final String TEST_LOGIN_PATH = "{ldapServerId}/testLogin";

  public static final String PRIORITY_PATH = "priority";

  private final LdapServerDAO serverDao = new LdapServerDAO();

  private final LdapUserMappingDAO umapDao = new LdapUserMappingDAO();

  private final LdapService ldapService;

  @Inject
  public LdapResource(LdapService ldapService) {
    this.ldapService = ldapService;
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
  @Audited(AuditEvent.CREATE_LDAP_SERVER)
  public LdapServer addLdapServer(LdapServer server) {
    serverDao.insert(server);
    auditLdapServer(server);
    return server;
  }

  /**
   * @since 1.7
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Audited(AuditEvent.UPDATE_LDAP_SERVER)
  public LdapServer updateLdapServer(LdapServer server) {
    serverDao.update(server);
    auditLdapServer(server);
    return server;
  }

  /**
   * @since 1.7
   */
  @DELETE
  @Path("{ldapServerId}")
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Audited(AuditEvent.DELETE_LDAP_SERVER)
  public void deleteLdapServer(@PathParam("ldapServerId") final String serverId) {
    LdapServer server = serverDao.getByIdNotNull(serverId);
    serverDao.delete(server);
    auditLdapServer(server);
  }

  private void auditLdapServer(final LdapServer server) {
    AuditData.get().setData("ldapServerId", server.getId()).setData("ldapServerName", server.getName());
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
    return ldapService.loadConnection(serverId);
  }

  /**
   * @since 1.7
   */
  @PUT
  @Path(CONNECTION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Audited(AuditEvent.CONFIGURE_LDAP_CONNECTION)
  public LdapConnection updateLdapConnection(@PathParam("ldapServerId") String serverId, LdapConnection conn) {
    validateServerId(serverId, conn);
    LdapConnection ldapConnection = ldapService.saveConnection(conn);
    auditLdapConnection(ldapConnection);
    return ldapConnection;
  }

  private void auditLdapConnection(final LdapConnection ldapConnection) {
    auditLdapServer(serverDao.getByIdNotNull(ldapConnection.getServerId()));
    AuditData.get()
        .setData("ldapProtocol", ldapConnection.getProtocol().getProtocol())
        .setData("ldapHostname", ldapConnection.getHostname())
        .setData("ldapPort", ldapConnection.getPort())
        .setData("ldapSearchBaseDn", ldapConnection.getSearchBase())
        .setData("ldapAuthenticationMethod",
            ldapConnection.getAuthenticationMethod().getMethod().toLowerCase(Locale.ROOT))
        .setData("ldapSaslRealm", ldapConnection.getSaslRealm())
        .setData("ldapUsername", ldapConnection.getSystemUsername())
        .setData("ldapConnectionTimeoutInSeconds", ldapConnection.getConnectionTimeout())
        .setData("ldapRetryDelayInSeconds", ldapConnection.getRetryDelay());
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
  @Audited(AuditEvent.CONFIGURE_LDAP_USER_MAPPING)
  public LdapUserMapping updateUserMapping(@PathParam("ldapServerId") String serverId, LdapUserMapping umap) {
    validateServerId(serverId, umap);

    if (umap.getId() != null) {
      umapDao.update(umap);
    }
    else {
      umapDao.insert(umap);
    }
    auditLdapUserMapping(umap);
    return umap;
  }

  private void auditLdapUserMapping(final LdapUserMapping umap) {
    auditLdapServer(serverDao.getByIdNotNull(umap.getServerId()));
    AuditData.get()
        .setData("ldapUserBaseDn", umap.getUserBaseDN())
        .setData("ldapUserSubtree", umap.isUserSubtree() ? "enabled" : "disabled")
        .setData("ldapUserObjectClass", umap.getUserObjectClass())
        .setData("ldapUserFilter", umap.getUserFilter())
        .setData("ldapUserIdAttribute", umap.getUserIDAttribute())
        .setData("ldapUserRealNameAttribute", umap.getUserRealNameAttribute())
        .setData("ldapUserEmailAttribute", umap.getUserEmailAttribute())
        .setData("ldapUserPasswordAttribute", umap.getUserPasswordAttribute())
        .setEnum("ldapGroupType", umap.getGroupMappingType());
    if (umap.getGroupMappingType().equals(LdapGroupMappingType.STATIC)) {
      AuditData.get()
          .setData("ldapStaticGroupBaseDn", umap.getGroupBaseDN())
          .setData("ldapStaticGroupSubtree", umap.isGroupSubtree() ? "enabled" : "disabled")
          .setData("ldapStaticGroupObjectClass", umap.getGroupObjectClass())
          .setData("ldapStaticGroupIdAttribute", umap.getGroupIDAttribute())
          .setData("ldapStaticGroupMemberAttribute", umap.getGroupMemberAttribute())
          .setData("ldapStaticGroupMemberFormat", umap.getGroupMemberFormat());
    }
    else if (umap.getGroupMappingType().equals(LdapGroupMappingType.DYNAMIC)) {
      AuditData.get()
          .setData("ldapDynamicGroupMemberOfAttribute", umap.getUserMemberOfGroupAttribute())
          .setData("ldapDynamicGroupSearch", umap.isDynamicGroupSearchEnabled() ? "enabled" : "disabled");
    }
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
      ldapService.testConnection(conn);
      return LdapConnectionStatus.SUCCESS;
    }
    catch (NamingException e) {
      // Log the exception at debug level for customer and Sonatype support investigations
      // (see https://issues.sonatype.org/browse/CLM-13799)
      log.debug("LDAP connection test failed", e);
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
      return ldapService.testUserMapping(umap, 20);
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
      ldapService.testUserLogin(umap, request.getUsername(), request.getPassword().toCharArray());
      return LdapConnectionStatus.SUCCESS;
    }
    catch (IllegalStateException e) {
      // happens when ldap server connection is not configured
      return new LdapConnectionStatus(Status.FAILURE, e.toString());
    }
    catch (NamingException e) {
      // Log the exception at debug level for customer and Sonatype support investigations
      // (see https://issues.sonatype.org/browse/CLM-13799)
      log.debug("LDAP login test failed", e);
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
  @Audited(AuditEvent.PRIORITIZE_LDAP)
  public void updatePriority(List<String> serverIds) {
    List<LdapServerDTO> serverList = serverIds.stream()
        .map(serverId -> new LdapServerDTO(serverDao.getByIdNotNull(serverId))).collect(Collectors.toList());
    serverDao.updatePriority(serverIds);
    AuditData.get().setData("ldapServerOrder", serverList);
  }
}
