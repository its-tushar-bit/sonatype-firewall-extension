/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import java.util.List;

import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.SamlUser;

import org.junit.Before;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(MtiqTest.class)
public abstract class AbstractMtiqAccessEditorPlaywrightTest
    extends AbstractMtiqUiTest
{
  protected MembershipMappingDAO membershipMappingDAO;

  protected RoleDAO roleDAO;

  protected Owner currentOwner;

  @Before
  public void setUpDaosAndLogin() {
    playwrightRefreshOrOpen("/");
    playwrightLogin();
    roleDAO = lookup(RoleDAO.class);
    membershipMappingDAO = lookup(MembershipMappingDAO.class);
  }

  /** Seed a SAML realm (disables group search) + two users with role memberships on {@code owner}. */
  protected void init(Owner owner) {
    this.currentOwner = owner;

    tempEntity.newSamlConfiguration();
    SamlUser u1 = tempEntity.newSamlUser("a-john@doe.net", "John", "Doe", "a-john@doe.net");
    SamlUser u2 = tempEntity.newSamlUser("b-jane@doe.net", "Jane", "Doe", "b-jane@doe.net");

    List<Role> applicationRoles = roleDAO.getApplicationRoles();
    assertThat(applicationRoles).hasSizeGreaterThanOrEqualTo(3);
    Role role = applicationRoles.get(0);
    tempEntity.newMembershipMapping(currentOwner.getId(), role.getId(), u1.getUsername());
    tempEntity.newMembershipMapping(currentOwner.getId(), role.getId(), u2.getUsername());
    tempEntity.newMembershipMapping(currentOwner.getId(), applicationRoles.get(2).getId(), u1.getUsername());
  }

  protected abstract String newRoleEditorUrl();

  protected List<MembershipMapping> getMembershipMappings(String ownerId, String roleName) {
    return membershipMappingDAO.getByContextIdAndRoleId(ownerId, roleDAO.getByName(roleName).getId());
  }
}
