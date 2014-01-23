/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.junit.After;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class MembershipMappingDAOTest
    extends AbstractDbDAOTest
{
  private String contextId = "some-app";

  private MembershipMappingDAO membershipDAO = new MembershipMappingDAO();

  private RoleDAO roleDAO = new RoleDAO();

  private Matcher<MembershipMapping> eq(final MembershipMapping membership) {
    return new BaseMatcher<MembershipMapping>()
    {
      @Override
      public boolean matches(Object item) {
        if (!(item instanceof MembershipMapping)) {
          return false;
        }
        MembershipMapping mm = (MembershipMapping) item;
        return membership.getContextId().equals(mm.getContextId()) && membership.getRoleId().equals(mm.getRoleId())
            && membership.getMemberName().equals(mm.getMemberName())
            && membership.getMemberType().equals(mm.getMemberType());
      }

      @Override
      public void describeTo(Description description) {
        description.appendValue(membership);
      }
    };
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private Matcher<Collection<? extends MembershipMapping>> matches(List<MembershipMapping>... arrayOfMemberships) {
    List<Matcher<MembershipMapping>> matchers = new ArrayList<Matcher<MembershipMapping>>();
    for (List<MembershipMapping> memberships : arrayOfMemberships) {
      for (MembershipMapping membership : memberships) {
        matchers.add(eq(membership));
      }
    }
    return new IsIterableContainingInAnyOrder(matchers);
  }

  @After
  public void cleanup() {
    for (MembershipMapping membership : membershipDAO.getByContextId(contextId)) {
      membershipDAO.delete(membership);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testSetMembershipMappingsForContextAndRole() throws Exception {
    String roleId1 = roleDAO.getByName("Owner").getId();
    String roleId2 = roleDAO.getByName("Developer").getId();

    // check initial state
    List<MembershipMapping> memberships = membershipDAO.getByContextId(contextId);
    assertThat(memberships, is(notNullValue()));
    assertThat(memberships, is(empty()));

    // add mapping for first role
    List<MembershipMapping> memberships1 = Arrays.asList(new MembershipMapping("john", MemberType.USER),
        new MembershipMapping("admins", MemberType.GROUP));
    membershipDAO.setMembershipMappingsForContextAndRole(contextId, roleId1, memberships1);
    memberships = membershipDAO.getByContextId(contextId);
    assertThat(memberships, is(notNullValue()));
    assertThat(memberships, matches(memberships1));

    // add mapping for another role
    List<MembershipMapping> memberships2 = Arrays.asList(new MembershipMapping("jane", MemberType.USER),
        new MembershipMapping("ops", MemberType.GROUP));
    membershipDAO.setMembershipMappingsForContextAndRole(contextId, roleId2, memberships2);
    memberships = membershipDAO.getByContextId(contextId);
    assertThat(memberships, is(notNullValue()));
    assertThat(memberships, matches(memberships1, memberships2));

    // exercise update involving keeping, removing and adding new member for a role
    memberships1 = Arrays.asList(new MembershipMapping("john", MemberType.USER), new MembershipMapping("jane",
        MemberType.USER));
    memberships2 = Arrays.asList();
    membershipDAO.setMembershipMappingsForContextAndRole(contextId, roleId1, memberships1);
    membershipDAO.setMembershipMappingsForContextAndRole(contextId, roleId2, memberships2);
    memberships = membershipDAO.getByContextId(contextId);
    assertThat(memberships, is(notNullValue()));
    assertThat(memberships, matches(memberships1));

    // exercise update involving change of group flag
    memberships1 = Arrays.asList(new MembershipMapping("john", MemberType.USER), new MembershipMapping("jane",
        MemberType.GROUP));
    membershipDAO.setMembershipMappingsForContextAndRole(contextId, roleId1, memberships1);
    memberships = membershipDAO.getByContextId(contextId);
    assertThat(memberships, is(notNullValue()));
    assertThat(memberships, matches(memberships1));
  }

  @Test
  public void testSetMembershipMappingsForContextAndRole_SetSemantic() throws Exception {
    String roleId1 = roleDAO.getByName("Owner").getId();

    List<MembershipMapping> memberships = Arrays.asList(new MembershipMapping("john", MemberType.USER),
        new MembershipMapping("john", MemberType.USER));
    membershipDAO.setMembershipMappingsForContextAndRole(contextId, roleId1, memberships);
    memberships = membershipDAO.getByContextId(contextId);
    assertThat(memberships, is(notNullValue()));
    assertThat(memberships, hasSize(1));
  }

  @Test
  public void testAdminUserMappedToAdminRole() throws Exception {
    List<MembershipMapping> memberships = membershipDAO.getByContextIdAndUser(MembershipMapping.GLOBAL_CONTEXT_ID,
        "admin");
    assertThat(memberships, is(notNullValue()));
    assertThat(memberships, hasSize(1));
    MembershipMapping membership = memberships.get(0);
    assertThat(membership.getMemberType(), is(MemberType.USER));
    Role role = roleDAO.getById(membership.getRoleId());
    assertThat(role, is(notNullValue()));
    assertThat(role.getName(), is("Administrator"));
  }

  @Test
  public void testUpdateNotSupported() throws Exception {
    String roleId1 = roleDAO.getApplicationRoles().get(0).getId();

    List<MembershipMapping> memberships = Arrays.asList(new MembershipMapping("john", MemberType.USER));
    membershipDAO.setMembershipMappingsForContextAndRole(contextId, roleId1, memberships);
    memberships = membershipDAO.getByContextId(contextId);
    assertThat(memberships, hasSize(1));
    MembershipMapping membership = memberships.get(0);
    membership.setMemberName("jane");
    try {
      membershipDAO.update(membership);
      assertThat("Expected UnsupportedOperationException", false);
    }
    catch (UnsupportedOperationException e) {
      // expected
    }
  }
}
