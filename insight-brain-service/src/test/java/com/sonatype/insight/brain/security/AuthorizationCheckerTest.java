/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.Collection;

import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.User;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class AuthorizationCheckerTest
{

  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private AuthorizationChecker checker = new AuthorizationChecker();

  private MembershipMappingDAO membershipDAO = new MembershipMappingDAO();

  private RoleDAO roleDAO = new RoleDAO();

  private MembershipMapping newMembershipMapping(User user, String contextId, String roleId) {
    MembershipMapping membership = new MembershipMapping(contextId, roleId, user.getUsername(), MemberType.USER);
    membershipDAO.insert(membership);
    return membership;
  }

  @Test
  public void testIsPermitted_AdminHasFullAccess() {
    User user = tempEntity.newUser();
    newMembershipMapping(user, MembershipMapping.GLOBAL_CONTEXT_ID, roleDAO.getByName("Administrator").getId());
    Collection<String> contextIds = Arrays.asList("app", "org", MembershipMapping.GLOBAL_CONTEXT_ID);

    assertThat(checker.isPermitted(user.getUsername(), Permission.READ, contextIds), is(true));
    assertThat(checker.isPermitted(user.getUsername(), Permission.WRITE, contextIds), is(true));
    assertThat(checker.isPermitted(user.getUsername(), Permission.ADMIN, contextIds), is(true));
  }

  @Test
  public void testIsPermitted_OwnerHasReadWriteAccess() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    newMembershipMapping(user, app.getId(), roleDAO.getByName("Owner").getId());
    Collection<String> contextIds = Arrays.asList(app.getId());

    assertThat(checker.isPermitted(user.getUsername(), Permission.READ, contextIds), is(true));
    assertThat(checker.isPermitted(user.getUsername(), Permission.WRITE, contextIds), is(true));
    assertThat(checker.isPermitted(user.getUsername(), Permission.ADMIN, contextIds), is(false));
  }

  @Test
  public void testIsPermitted_DeveloperHasReadAccess() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    newMembershipMapping(user, app.getId(), roleDAO.getByName("Developer").getId());
    Collection<String> contextIds = Arrays.asList(app.getId());

    assertThat(checker.isPermitted(user.getUsername(), Permission.READ, contextIds), is(true));
    assertThat(checker.isPermitted(user.getUsername(), Permission.WRITE, contextIds), is(false));
    assertThat(checker.isPermitted(user.getUsername(), Permission.ADMIN, contextIds), is(false));
  }

  @Test
  public void testIsPermitted_NonMemberHasNoAccess() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    Collection<String> contextIds = Arrays.asList(app.getId(), org.getId(), MembershipMapping.GLOBAL_CONTEXT_ID);

    assertThat(checker.isPermitted(user.getUsername(), Permission.READ, contextIds), is(false));
    assertThat(checker.isPermitted(user.getUsername(), Permission.WRITE, contextIds), is(false));
    assertThat(checker.isPermitted(user.getUsername(), Permission.ADMIN, contextIds), is(false));
  }

  @Test
  public void testIsPermitted_AccessInherited() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    newMembershipMapping(user, org.getId(), roleDAO.getByName("Owner").getId());
    Collection<String> contextIds = Arrays.asList(app.getId(), org.getId());

    assertThat(checker.isPermitted(user.getUsername(), Permission.READ, contextIds), is(true));
    assertThat(checker.isPermitted(user.getUsername(), Permission.WRITE, contextIds), is(true));
    assertThat(checker.isPermitted(user.getUsername(), Permission.ADMIN, contextIds), is(false));
  }
}
