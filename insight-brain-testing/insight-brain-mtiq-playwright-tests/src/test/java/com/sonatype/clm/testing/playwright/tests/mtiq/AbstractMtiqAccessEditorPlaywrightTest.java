/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import java.util.List;

import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.AccessEditorPage;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.SamlUser;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Shared MTIQ Access editor Playwright coverage. Ported from the Selenide
 * {@code AbstractMtiqAccessEditorTest}. Concrete subclasses seed the target owner via
 * {@link #init(Owner)} and supply the deep-link URL to the "New Role" editor by implementing
 * {@link #newRoleEditorUrl()}.
 */
@Tag("mtiq")
public abstract class AbstractMtiqAccessEditorPlaywrightTest
    extends AbstractMtiqUiTest
{
  protected MembershipMappingDAO membershipMappingDAO;

  protected RoleDAO roleDAO;

  protected Owner currentOwner;

  @BeforeEach
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
    Assertions.assertThat(applicationRoles).hasSizeGreaterThanOrEqualTo(3);
    Role role = applicationRoles.get(0);
    tempEntity.newMembershipMapping(currentOwner.getId(), role.getId(), u1.getUsername());
    tempEntity.newMembershipMapping(currentOwner.getId(), role.getId(), u2.getUsername());
    tempEntity.newMembershipMapping(currentOwner.getId(), applicationRoles.get(2).getId(), u1.getUsername());
  }

  protected abstract String newRoleEditorUrl();

  protected List<MembershipMapping> getMembershipMappings(String ownerId, String roleName) {
    return membershipMappingDAO.getByContextIdAndRoleId(ownerId, roleDAO.getByName(roleName).getId());
  }

  /**
   * Ports Selenide {@code testDisabledGroupSearchWarning}: with SAML configured the search box is
   * disabled and the "Add an External Group" form is offered instead. Adds one group via the form,
   * asserts it appears in the associated members list, saves, and verifies the resulting
   * {@link MembershipMapping} row is persisted with {@link MemberType#GROUP}. Also asserts the
   * legacy on-prem-only LDAP alert is absent in MTIQ.
   * <p>
   * Skipped from the Selenide original: the {@code addMembersForm.searchBox().shouldBe(visible)},
   * {@code disabledGroupSearchWarning}, and Applitools {@code eyesCheck} calls have no direct
   * Playwright equivalent — group-search disablement is asserted implicitly through the presence
   * of the add-group form controls, and visual regression is not part of the Playwright suite.
   */
  @Test
  @Tag("mtiq")
  public void testDisabledGroupSearchWarning_addExternalGroupPersistsMembership() {
    playwrightRefreshOrOpen(newRoleEditorUrl());

    AccessEditorPage editor = new AccessEditorPage();
    assertThat(editor.root()).isVisible();

    editor.selectRole("Component Evaluator");

    // The MTIQ AccessPage renders the "Add an External Group" form when group-search is disabled.
    assertThat(editor.addGroupInput()).isVisible();
    assertThat(editor.addGroupButton()).isVisible();
    assertThat(editor.addGroupSublabel())
        .containsText("Requires an exact match of the SAML group name");

    editor.addExternalGroup("test group");

    assertThat(editor.associatedMembers()).hasCount(1);
    assertThat(editor.associatedMembers().first()).containsText("test group");

    // Submit and wait for the submit mask to dismiss. Don't assert the editor unmounts —
    // MTIQ variants keep the access editor page mounted after a successful save (verified against
    // both the Application and Repositories variants), so the DB check below is the real signal.
    editor.submit();
    waitForSubmitMask();

    List<MembershipMapping> mappings = getMembershipMappings(currentOwner.getId(), "Component Evaluator");
    Assertions.assertThat(mappings).hasSize(1);

    MembershipMapping mapping = mappings.get(0);
    Assertions.assertThat(mapping.getMemberType()).isEqualTo(MemberType.GROUP);
    Assertions.assertThat(mapping.getMemberName()).isEqualTo("test group");

    // On-prem-only alert must not be rendered in MTIQ.
    playwrightRefreshOrOpen(newRoleEditorUrl());
    assertThat(new AccessEditorPage().ldapServersAlert()).hasCount(0);
  }
}
