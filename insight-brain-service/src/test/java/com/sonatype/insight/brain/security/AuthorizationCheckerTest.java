/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.sonatype.insight.brain.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.security.AuthzFilter.Context;

import com.google.common.collect.Sets;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.empty;
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

  private MembershipMapping newGroupMapping(String groupname, String contextId, String roleId) {
    MembershipMapping membership = new MembershipMapping(contextId, roleId, groupname, MemberType.GROUP);
    membershipDAO.insert(membership);
    return membership;
  }

  @Test
  public void testIsPermitted_AdminHasFullAccess() {
    User user = tempEntity.newUser();
    newMembershipMapping(user, MembershipMapping.GLOBAL_CONTEXT_ID, roleDAO.getByName("Administrator").getId());
    Collection<String> contextIds = Arrays.asList("app", "org", MembershipMapping.GLOBAL_CONTEXT_ID);

    UserPrincipal admin = new UserPrincipal(user.getUsername(), user.calculateDisplayName(), true);
    assertThat(checker.isPermitted(admin, Permission.READ, contextIds), is(true));
    assertThat(checker.isPermitted(admin, Permission.WRITE, contextIds), is(true));
    assertThat(checker.isPermitted(admin, Permission.ADMIN, contextIds), is(true));
  }

  @Test
  public void testIsPermitted_OwnerHasReadWriteAccess() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    newMembershipMapping(user, app.getId(), roleDAO.getByName("Owner").getId());
    Collection<String> contextIds = Arrays.asList(app.getId());

    UserPrincipal owner = new UserPrincipal(user.getUsername(), user.calculateDisplayName(), true);
    assertThat(checker.isPermitted(owner, Permission.READ, contextIds), is(true));
    assertThat(checker.isPermitted(owner, Permission.WRITE, contextIds), is(true));
    assertThat(checker.isPermitted(owner, Permission.ADMIN, contextIds), is(false));
  }

  @Test
  public void testIsPermitted_DeveloperHasReadAccess() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    newMembershipMapping(user, app.getId(), roleDAO.getByName("Developer").getId());
    Collection<String> contextIds = Arrays.asList(app.getId());

    UserPrincipal developer = new UserPrincipal(user.getUsername(), user.calculateDisplayName(), true);
    assertThat(checker.isPermitted(developer, Permission.READ, contextIds), is(true));
    assertThat(checker.isPermitted(developer, Permission.WRITE, contextIds), is(false));
    assertThat(checker.isPermitted(developer, Permission.ADMIN, contextIds), is(false));
  }

  @Test
  public void testIsPermitted_NonMemberHasNoAccess() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    Collection<String> contextIds = Arrays.asList(app.getId(), org.getId(), MembershipMapping.GLOBAL_CONTEXT_ID);

    UserPrincipal userPrincipal = new UserPrincipal(user.getUsername(), user.calculateDisplayName(), true);
    for (Permission perm : Permission.values()) {
      assertThat(perm.toString(), checker.isPermitted(userPrincipal, perm, contextIds), is(false));
    }
  }

  @Test
  public void testIsPermitted_AnonymousHasNoAccess() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    Collection<String> contextIds = Arrays.asList(app.getId(), org.getId(), MembershipMapping.GLOBAL_CONTEXT_ID);

    for (Permission perm : Permission.values()) {
      assertThat(perm.toString(), checker.isPermitted(null, perm, contextIds), is(false));
    }
  }

  @Test
  public void testIsPermitted_MemberHasAccessThroughGroup() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    newGroupMapping("group", app.getId(), roleDAO.getByName("Owner").getId());
    Collection<String> contextIds = Arrays.asList(app.getId());

    UserPrincipal owner = new UserPrincipal(user.getUsername(), user.calculateDisplayName(), true, Sets.newHashSet("group"));
    assertThat(checker.isPermitted(owner, Permission.READ, contextIds), is(true));
    assertThat(checker.isPermitted(owner, Permission.WRITE, contextIds), is(true));
    assertThat(checker.isPermitted(owner, Permission.ADMIN, contextIds), is(false));
  }

  @Test
  public void testIsPermitted_AccessInherited() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    User user = tempEntity.newUser();
    newMembershipMapping(user, org.getId(), roleDAO.getByName("Owner").getId());
    Collection<String> contextIds = Arrays.asList(app.getId(), org.getId());

    UserPrincipal owner = new UserPrincipal(user.getUsername(), user.calculateDisplayName(), true);
    assertThat(checker.isPermitted(owner, Permission.READ, contextIds), is(true));
    assertThat(checker.isPermitted(owner, Permission.WRITE, contextIds), is(true));
    assertThat(checker.isPermitted(owner, Permission.ADMIN, contextIds), is(false));
  }

  @Test
  public void testFilter_Organizations() {
    List<Organization> entities = Arrays.asList(tempEntity.newOrganization(), tempEntity.newOrganization());
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, entities.get(0).getId(), role.getId());

    UserPrincipal userPrincipal = new UserPrincipal(user.getUsername(), user.calculateDisplayName(), true);
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities, Context.ORGANIZATION),
        is((Object) Arrays.asList(entities.get(0))));
  }

  @Test
  public void testFilter_Applications() {
    Organization org = tempEntity.newOrganization();
    List<Application> entities = Arrays.asList(tempEntity.newApplication(org.getId()),
        tempEntity.newApplication(org.getId()));
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newMembershipMapping(user, entities.get(1).getId(), role.getId());

    UserPrincipal userPrincipal = new UserPrincipal(user.getUsername(), user.calculateDisplayName(), true);
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities, Context.APPLICATION),
        is((Object) Arrays.asList(entities.get(1))));
  }

  @Test
  public void testFilter_WithGroup() {
    List<Organization> entities = Arrays.asList(tempEntity.newOrganization(), tempEntity.newOrganization());
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    newGroupMapping("group", entities.get(0).getId(), role.getId());

    UserPrincipal userPrincipal = new UserPrincipal(user.getUsername(), user.calculateDisplayName(), true, Sets.newHashSet("group"));
    assertThat(checker.filterByPermission(userPrincipal, Permission.READ, entities, Context.ORGANIZATION),
        is((Object) Arrays.asList(entities.get(0))));
  }

  @Test
  public void testFilter_NonMemberHasNoAccess() {
    Collection<Organization> entities = Arrays.asList(tempEntity.newOrganization());
    User user = tempEntity.newUser();
    UserPrincipal userPrincipal = new UserPrincipal(user.getUsername(), user.calculateDisplayName(), true);

    for (Permission perm : Permission.values()) {
      assertThat(perm.toString(),
          checker.filterByPermission(userPrincipal, Permission.READ, entities, Context.ORGANIZATION), is(empty()));
    }
  }

  @Test
  public void testFilter_AnonymousHasNoAccess() {
    Collection<Organization> entities = Arrays.asList(tempEntity.newOrganization());
    for (Permission perm : Permission.values()) {
      assertThat(perm.toString(), checker.filterByPermission(null, Permission.READ, entities, Context.ORGANIZATION),
          is(empty()));
    }
  }
}
