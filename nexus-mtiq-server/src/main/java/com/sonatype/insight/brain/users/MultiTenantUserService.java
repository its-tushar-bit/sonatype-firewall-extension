/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.users;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.auth.MultiTenantAuth0ManagementService;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.db.dao.TenantMetadataDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.TenantMetadata;
import com.sonatype.insight.brain.security.AbstractUserService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.CurrentUser;

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

  @Inject
  public MultiTenantUserService(final DefaultWebSessionManager webSessionManager,
                                final SessionDAO sessionDAO,
                                final SamlUserDAO samlUserDAO,
                                final TenantMetadataDAO tenantMetadataDAO,
                                final MultiTenantAuth0ManagementService multiTenantAuth0ManagementService,
                                final CurrentUser currentUser)
  {
    super(sessionDAO, webSessionManager, currentUser, samlUserDAO);
    this.tenantMetadataDAO = tenantMetadataDAO;
    this.multiTenantAuth0ManagementService = multiTenantAuth0ManagementService;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Override
  public List<MtiqUserDTO> getAllUsers() {
    return samlUserDAO.getAll().stream()
        .map(MtiqUserDTO::samlUserToMtiqUser)
        .collect(Collectors.toList());
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Override
  public void inviteUser(final MtiqUserDTO user) {
    TenantMetadata tenantMetadata = getTenantMetadata();

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
    validateUserToDeleteIsNotCurrentlyLoggedIn(SamlUser.SAML_REALM_ID, username);
    TenantMetadata tenantMetadata = getTenantMetadata();

    log.debug("Deleting Auth0 user");
    multiTenantAuth0ManagementService.deleteUser(username, tenantMetadata.getConnectionId());

    SamlUser samlUser = samlUserDAO.getByUsername(username);
    if (samlUser != null) {
      deleteUser(samlUser);
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
