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
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.security.Authorize;

@Named
@Singleton
public class MultiTenantUserService
    implements MtiqUserService
{
  private final SamlUserDAO samlUserDAO;

  @Inject
  public MultiTenantUserService(final SamlUserDAO samlUserDAO) {
    this.samlUserDAO = samlUserDAO;
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
    samlUserDAO.insert(MtiqUserDTO.samlUserFromMtiqUser(user));
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  @Override
  public void deleteByUser(final MtiqUserDTO user) {
    SamlUser samlUser = samlUserDAO.getByUsernameNotNull(user.getEmail());
    samlUserDAO.delete(samlUser);
  }
}
