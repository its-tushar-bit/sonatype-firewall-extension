/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.configuration.ldap;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.api.IqOnlyEndpoint;
import com.sonatype.insight.brain.audit.Audited;
import com.sonatype.insight.brain.model.configuration.ldap.LdapConnection;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.configuration.ldap.LdapUserMapping;

import com.codahale.metrics.annotation.Timed;

/**
 * @since 1.7
 */
@Named
@IqOnlyEndpoint
@Timed
@Path(LdapResource.RESOURCE_PATH)
public class LdapResource
{
  static final String RESOURCE_PATH = "rest/config/ldap";

  static final String CONNECTION_PATH = "{ldapServerId}/connection";

  static final String USER_MAPPING_PATH = "{ldapServerId}/userMapping";

  static final String TEST_CONNECTION_PATH = "{ldapServerId}/testConnection";

  static final String TEST_USER_MAPPING_PATH = "{ldapServerId}/testUserMapping";

  static final String TEST_LOGIN_PATH = "{ldapServerId}/testLogin";

  static final String PRIORITY_PATH = "priority";

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
  public List<LdapServer> getAllLdapServers() {
    return ldapService.getAllLdapServers();
  }

  /**
   * @since 1.7
   */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CREATE_LDAP_SERVER)
  public LdapServer addLdapServer(LdapServer ldapServer) {
    return ldapService.addLdapServer(ldapServer);
  }

  /**
   * @since 1.7
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.UPDATE_LDAP_SERVER)
  public LdapServer updateLdapServer(LdapServer ldapServer) {
    return ldapService.updateLdapServer(ldapServer);
  }

  /**
   * @since 1.7
   */
  @DELETE
  @Path("{ldapServerId}")
  @Audited(AuditEvent.DELETE_LDAP_SERVER)
  public void deleteLdapServer(@PathParam("ldapServerId") String ldapServerId) {
    ldapService.deleteLdapServer(ldapServerId);
  }

  // connection

  /**
   * @since 1.7
   */
  @GET
  @Path(CONNECTION_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public LdapConnection getLdapConnection(@PathParam("ldapServerId") String ldapServerId) {
    return ldapService.getLdapConnection(ldapServerId);
  }

  /**
   * @since 1.7
   */
  @PUT
  @Path(CONNECTION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_LDAP_CONNECTION)
  public LdapConnection upsertLdapConnection(
      @PathParam("ldapServerId") String ldapServerId,
      LdapConnection ldapConnection)
  {
    return ldapService.upsertLdapConnection(ldapServerId, ldapConnection);
  }

  // user mapping

  /**
   * @since 1.7
   */
  @GET
  @Path(USER_MAPPING_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public LdapUserMapping getLdapUserMapping(@PathParam("ldapServerId") String ldapServerId) {
    return ldapService.getLdapUserMapping(ldapServerId);
  }

  /**
   * @since 1.7
   */
  @PUT
  @Path(USER_MAPPING_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.CONFIGURE_LDAP_USER_MAPPING)
  public LdapUserMapping upsertLdapUserMapping(
      @PathParam("ldapServerId") String ldapServerId,
      LdapUserMapping ldapUserMapping)
  {
    return ldapService.upsertLdapUserMapping(ldapServerId, ldapUserMapping);
  }

  /**
   * @since 1.7
   */
  @PUT
  @Path(TEST_CONNECTION_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public LdapConnectionStatus testLdapConnection(
      @PathParam("ldapServerId") String ldapServerId,
      LdapConnection ldapConnection)
  {
    return ldapService.testLdapConnection(ldapServerId, ldapConnection);
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
  public List<LdapUser> testLdapUserMapping(
      @PathParam("ldapServerId") String ldapServerId,
      LdapUserMapping ldapUserMapping)
  {
    return ldapService.testLdapUserMapping(ldapServerId, ldapUserMapping, 20 /* maxResults */);
  }

  /**
   * @since 1.7
   */
  @PUT
  @Path(TEST_LOGIN_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public LdapConnectionStatus testUserLogin(
      @PathParam("ldapServerId") String ldapServerId,
      LdapTestLoginRequest request)
  {
    return ldapService.testUserLogin(ldapServerId, request.getUserMapping(), request.getUsername(),
        request.getPassword().toCharArray());
  }

  /**
   * @since 1.25
   */
  @PUT
  @Path(PRIORITY_PATH)
  @Consumes(MediaType.APPLICATION_JSON)
  @Audited(AuditEvent.PRIORITIZE_LDAP)
  public void updatePriority(List<String> ldapServerIds) {
    ldapService.updatePriority(ldapServerIds);
  }
}
