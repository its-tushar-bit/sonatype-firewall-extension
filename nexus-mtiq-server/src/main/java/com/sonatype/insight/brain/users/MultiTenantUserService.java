/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.auth.MultiTenantAuth0ManagementService;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.security.AbstractUserService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.MultiTenantSsoUserService;
import com.sonatype.insight.brain.security.SsoUser;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.apache.shiro.web.session.mgt.DefaultWebSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class MultiTenantUserService
    extends AbstractUserService
    implements MtiqUserService
{
  private static final Logger log = LoggerFactory.getLogger(MultiTenantUserService.class.getName());

  private final TenantMetadataDAO tenantMetadataDAO;

  private final MultiTenantAuth0ManagementService multiTenantAuth0ManagementService;

  private final MultiTenantSsoUserService multiTenantSsoUserService;

  @Inject
  public MultiTenantUserService(
      final DefaultWebSessionManager webSessionManager,
      final SessionDAO sessionDAO,
      final MultiTenantSsoUserService ssoUserService,
      final TenantMetadataDAO tenantMetadataDAO,
      final MultiTenantAuth0ManagementService multiTenantAuth0ManagementService,
      final CurrentUser currentUser)
  {
    super(sessionDAO, webSessionManager, currentUser, ssoUserService);
    this.tenantMetadataDAO = tenantMetadataDAO;
    this.multiTenantAuth0ManagementService = multiTenantAuth0ManagementService;
    this.multiTenantSsoUserService = ssoUserService;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Override
  public List<MtiqUserDTO> getAllUsers() {
    return ssoUserService.getAll()
        .stream()
        .map(MtiqUserDTO::ssoUserToMtiqUser)
        .collect(Collectors.toList());
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Override
  public void inviteUser(final MtiqUserDTO user) {
    TenantMetadata tenantMetadata = getTenantMetadata();

    try {
      multiTenantAuth0ManagementService.createOrUpdateUser(user.getEmail(), user.getFirstName(),
          user.getLastName(), tenantMetadata.getConnectionName(), tenantMetadata.getApplicationId(),
          tenantMetadata.getConnectionId(), tenantMetadata.getOrganizationId());

      log.debug("user created on Auth0 successfully");

      multiTenantSsoUserService.upsertByUsername(user);
      log.info("user created successfully");
    }
    catch (Exception e) {
      log.error(
          "User invitation failed for username: {}, auth0 applicationId:{}, connectionId: {}, " +
              "organizationId: {}",
          user.getUsername(),
          tenantMetadata.getApplicationId(),
          tenantMetadata.getConnectionId(),
          tenantMetadata.getOrganizationId());
      throw e;
    }
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Override
  public void deleteByUsername(String username) {
    username = username.toLowerCase();
    TenantMetadata tenantMetadata = getTenantMetadata();

    try {
      SsoUser ssoUser = multiTenantSsoUserService.getByUsername(username);
      if (ssoUser != null) {
        validateUserToDeleteIsNotCurrentlyLoggedIn(ssoUser.getRealmId(), username);
        deleteUser(ssoUser);
      }

      String organizationId = tenantMetadata.getOrganizationId();
      if (StringUtils.isNotBlank(organizationId)) {
        multiTenantAuth0ManagementService.removeMemberFromOrganization(organizationId, username);
        log.debug("user removed from organization successfully");
      }
      else {
        multiTenantAuth0ManagementService.deleteUser(username, tenantMetadata.getConnectionId());
        log.debug("user deleted on Auth0 successfully");
      }

      log.info("user deleted successfully");
    }
    catch (Exception e) {
      log.error("User deletion failed for username: {}, auth0 applicationId:{},  connectionId: {}, organizationId: {}",
          username,
          tenantMetadata.getApplicationId(),
          tenantMetadata.getConnectionId(),
          tenantMetadata.getOrganizationId());
      throw e;
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
