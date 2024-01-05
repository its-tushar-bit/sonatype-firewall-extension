/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.mtiq;

import java.util.List;

import com.sonatype.clm.testing.functional.elements.CLM;
import com.sonatype.clm.testing.functional.elements.FormMask;
import com.sonatype.clm.testing.functional.pages.AccessEditorPage;
import com.sonatype.clm.testing.functional.pages.OwnerSummaryPage;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.SamlUser;

import org.junit.Before;
import org.junit.Test;

import static com.codeborne.selenide.CollectionCondition.texts;
import static com.codeborne.selenide.Condition.cssClass;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractMtiqAccessEditorTest
    extends AbstractMtiqFunctionalTest
{
  private List<Role> applicationRoles;

  protected MembershipMappingDAO membershipMappingDAO;

  protected RoleDAO roleDAO;

  protected Owner currentOwner;

  @Before
  public void beforeClass() {
    refreshOrOpen(OwnerSummaryPage.urlToRootOrg());
    loginAsAdmin();
  }

  @Before
  public void setUp() {
    roleDAO = lookup(RoleDAO.class);
    membershipMappingDAO = lookup(MembershipMappingDAO.class);
    applicationRoles = roleDAO.getApplicationRoles();
  }

  protected void init(Owner owner) {
    this.currentOwner = owner;

    tempEntity.newSamlConfiguration();
    SamlUser u1 = tempEntity.newSamlUser("a-john@doe.net", "John", "Doe", "a-john@doe.net");
    SamlUser u2 = tempEntity.newSamlUser("b-jane@doe.net", "Jane", "Doe", "b-jane@doe.net");
    Role role = applicationRoles.get(0);
    tempEntity.newMembershipMapping(currentOwner.getId(), role.getId(), u1.getUsername());
    tempEntity.newMembershipMapping(currentOwner.getId(), role.getId(), u2.getUsername());

    role = applicationRoles.get(2);
    tempEntity.newMembershipMapping(currentOwner.getId(), role.getId(), u1.getUsername());

    refreshOrOpen(OwnerSummaryPage.url(owner));
    shouldBeOnInitialPage();
  }

  protected void shouldBeOnInitialPage() {
    OwnerSummaryPage.summaryTile().name().shouldHave(text(currentOwner.getName()));
  }

  protected List<MembershipMapping> getMembershipMappings(final String ownerId, final String roleName) {
    return membershipMappingDAO.getByContextIdAndRoleId(ownerId, roleDAO.getByName(roleName).getId());
  }

  @Test
  public void testDisabledGroupSearchWarning() {
    goFromSummaryToAddRole();

    AccessEditorPage accessEditorPage = new AccessEditorPage();
    AccessEditorPage.AddMembersForm addMembersForm = accessEditorPage.addMembersForm();
    addMembersForm.searchBox().shouldBe(visible);

    addMembersForm.roleSelect().chooseOption("Component Evaluator");
    addMembersForm.addGroupButton().shouldHave(cssClass("disabled"));
    addMembersForm.addGroupSublabel().shouldHave(text("Requires an exact match of the SAML group name"));
    addMembersForm.addGroupInput().setValue("test group");

    eyesWatcher.eyesCheck("with external group text box");

    addMembersForm.addGroupInput().click();
    addMembersForm.addGroupButton().shouldNotHave(cssClass("disabled")).click();
    addMembersForm.addedItems().shouldHaveSize(1);
    addMembersForm.addedItems().shouldHave(texts("test group (Group)")); // TODO CLM-26430
    addMembersForm.addGroupInput().setValue("test group");
    addMembersForm.addGroupButton().shouldHave(cssClass("disabled"));
    addMembersForm.saveButton().scrollIntoView(true).shouldNotBe(CLM.DISABLED).click();
    FormMask.seeAndWaitForDismissal();

    List<MembershipMapping> mappings = getMembershipMappings(currentOwner.getId(), "Component Evaluator");
    assertThat(mappings).hasSize(1);

    MembershipMapping mapping = mappings.get(0);
    assertThat(mapping.getMemberType()).isEqualTo(MemberType.GROUP);
    assertThat(mapping.getMemberName()).isEqualTo("test group");

    addMembersForm.disabledGroupSearchWarning().shouldNotBe(visible);

    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.SSO_IDP_MANAGED_BY_SONATYPE,
        String.valueOf(true));
    refresh();

    addMembersForm.disabledGroupSearchWarning().shouldNot(exist);
    addMembersForm.addGroupSublabel();

    eyesWatcher.eyesCheck("without external group text box");
  }

  abstract void goFromSummaryToAddRole();
}
