/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.auth.MultiTenantAuth0ManagementService;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.error.exception.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class MultiTenantUserService
    implements MtiqUserService
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantUserService.class.getName());

  private final SamlUserDAO samlUserDAO;

  private final TenantMetadataDAO tenantMetadataDAO;

  private final MultiTenantAuth0ManagementService multiTenantAuth0ManagementService;

  private final CurrentUser currentUser;

  @Inject
  public MultiTenantUserService(final SamlUserDAO samlUserDAO,
                                final TenantMetadataDAO tenantMetadataDAO,
                                final MultiTenantAuth0ManagementService multiTenantAuth0ManagementService,
                                final CurrentUser currentUser)
  {
    this.samlUserDAO = samlUserDAO;
    this.tenantMetadataDAO = tenantMetadataDAO;
    this.multiTenantAuth0ManagementService = multiTenantAuth0ManagementService;
    this.currentUser = currentUser;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Override
  public Set<MtiqUserDTO> getAllUsers() {
    return samlUserDAO.getAll().stream()
        .map(MtiqUserDTO::samlUserToMtiqUser)
        .collect(Collectors.toSet());
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Override
  public void inviteUser(final MtiqUserDTO user) {
    TenantMetadata tenantMetadata = getTenantMetadata();

    if (tenantMetadata == null) {
      throw new RuntimeException("Tenant metadata not found");
    }

    multiTenantAuth0ManagementService.createOrUpdateUser(user.getEmail(), user.getFirstName(),
        user.getLastName(), tenantMetadata.getConnectionName(), tenantMetadata.getApplicationId(),
        tenantMetadata.getConnectionId());
    log.debug("user created on Auth0 service successfully");
    samlUserDAO.upsertByUsername(MtiqUserDTO.samlUserFromMtiqUser(user));
    log.info("Auth0 user created successfully");
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Override
  public void deleteByUsername(final String username) {
    shouldNotDeleteLoggedInUser(username);
    TenantMetadata tenantMetadata = getTenantMetadata();

    log.debug("Deleting Auth0 user");
    multiTenantAuth0ManagementService.deleteUser(username, tenantMetadata.getConnectionId());

    SamlUser samlUser = samlUserDAO.getByUsername(username);
    if (samlUser != null) {
      samlUserDAO.delete(samlUser);
    }
  }

  private void shouldNotDeleteLoggedInUser(final String username) {
    if (username.equals(currentUser.getUsername())) {
      throw new BadRequestException("A user who is logged in cannot delete themself.");
    }
  }

  private TenantMetadata getTenantMetadata() {
    TenantMetadata tenantMetadata = tenantMetadataDAO.get();

    if (tenantMetadata == null) {
      throw new RuntimeException("Tenant metadata not found");
    }
    return tenantMetadata;
  }
}
