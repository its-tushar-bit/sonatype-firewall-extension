/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;

/**
 * This base class is intended to be used when you need a full e2e integration test from the REST endpoint to the DB.
 * Check docs in {@link AbstractBrainServiceIntegrationTest}
 */
public abstract class AbstractResourceTest
    extends AbstractBrainServiceIntegrationTest
{
  /**
   * Helper method to create a user with specified permissions.
   * Reduces code duplication when creating test users with specific permission sets.
   *
   * @param permissions the permissions to grant to the user
   * @return a new user with the specified permissions
   */
  protected User createUserWithPermissions(Permission... permissions) {
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false /* global */, permissions);
    tempEntity.newMembershipMapping(Organization.ROOT_ORGANIZATION_ID, role.getId(), user.getUsername());
    return user;
  }
}
