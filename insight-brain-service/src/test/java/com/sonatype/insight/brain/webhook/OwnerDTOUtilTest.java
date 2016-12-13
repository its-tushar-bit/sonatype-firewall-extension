/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.webhook.dto.PolicyManagementPayload.OwnerDTO;

import org.junit.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class OwnerDTOUtilTest
    extends AbstractComponentTest
{
  @Inject
  private OwnerDTOUtil ownerDTOUtil;

  @Test
  public void testBuildOwnerDTO_Organization() {
    Organization organization = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(organization.getId());
    Label label = tempEntity.newLabel(organization.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(organization.getId());
    Policy policy = tempEntity.newPolicy(organization.getId(), "policy");
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    MembershipMapping member = tempEntity.newMembershipMapping(organization.getId(), role.getId(), user.getUsername());

    ManagementEvent event = new ManagementEvent();
    event.ownerId = organization.getId();

    OwnerDTO payload = ownerDTOUtil.buildOwnerDTO(event);
    assertThat(payload.id, is(organization.getId()));
    assertThat(payload.publicId, is(organization.getPublicId()));
    assertThat(payload.type, is(organization.getType().name()));
    assertThat(payload.name, is(organization.getName()));
    assertThat(payload.parentOwnerId, is(organization.getParentOwnerId()));

    assertThat(payload.applicationCategories, hasSize(1));
    assertThat(payload.applicationCategories.get(0).id, is(tag.getId()));
    assertThat(payload.applicationCategories.get(0).name, is(tag.getName()));
    assertThat(payload.applicationCategories.get(0).description, is(tag.getDescription()));
    assertThat(payload.applicationCategories.get(0).color, is(tag.getColor().toValue()));

    assertThat(payload.labels, hasSize(1));
    assertThat(payload.labels.get(0).id, is(label.getId()));
    assertThat(payload.labels.get(0).name, is(label.getLabel()));
    assertThat(payload.labels.get(0).description, is(label.getDescription()));
    assertThat(payload.labels.get(0).color, is(label.getColor().toValue()));

    assertThat(payload.licenseThreatGroups, hasSize(1));
    assertThat(payload.licenseThreatGroups.get(0).id, is(licenseThreatGroup.getId()));
    assertThat(payload.licenseThreatGroups.get(0).name, is(licenseThreatGroup.getName()));
    assertThat(payload.licenseThreatGroups.get(0).threatLevel, is(licenseThreatGroup.getThreatLevel()));

    assertThat(payload.policies, hasSize(1));
    assertThat(payload.policies.get(0).id, is(policy.getId()));
    assertThat(payload.policies.get(0).name, is(policy.getName()));
    assertThat(payload.policies.get(0).threatLevel, is(policy.getThreatLevel()));

    assertThat(payload.roles, hasSize(1));
    assertThat(payload.roles.get(0).id, is(role.getId()));
    assertThat(payload.roles.get(0).name, is(role.getName()));
    assertThat(payload.roles.get(0).members, hasSize(1));
    assertThat(payload.roles.get(0).members.get(0).name, is(member.getMemberName()));
    assertThat(payload.roles.get(0).members.get(0).type, is(member.getMemberType().name()));
  }

  @Test
  public void testBuildOwnerDTO_Application() {
    Application application = tempEntity.newApplicationWithParent("publicId");
    Label label = tempEntity.newLabel(application.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(application.getId());
    Policy policy = tempEntity.newPolicy(application.getId(), "policy");
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    MembershipMapping member = tempEntity.newMembershipMapping(application.getId(), role.getId(), user.getUsername());

    ManagementEvent event = new ManagementEvent();
    event.ownerId = application.getId();

    OwnerDTO payload = ownerDTOUtil.buildOwnerDTO(event);
    assertThat(payload.id, is(application.getId()));
    assertThat(payload.publicId, is(application.getPublicId()));
    assertThat(payload.type, is(application.getType().name()));
    assertThat(payload.name, is(application.getName()));
    assertThat(payload.parentOwnerId, is(application.getParentOwnerId()));

    assertThat(payload.applicationCategories, is(nullValue()));

    assertThat(payload.labels, hasSize(1));
    assertThat(payload.labels.get(0).id, is(label.getId()));
    assertThat(payload.labels.get(0).name, is(label.getLabel()));
    assertThat(payload.labels.get(0).description, is(label.getDescription()));
    assertThat(payload.labels.get(0).color, is(label.getColor().toValue()));

    assertThat(payload.licenseThreatGroups, hasSize(1));
    assertThat(payload.licenseThreatGroups.get(0).id, is(licenseThreatGroup.getId()));
    assertThat(payload.licenseThreatGroups.get(0).name, is(licenseThreatGroup.getName()));
    assertThat(payload.licenseThreatGroups.get(0).threatLevel, is(licenseThreatGroup.getThreatLevel()));

    assertThat(payload.policies, hasSize(1));
    assertThat(payload.policies.get(0).id, is(policy.getId()));
    assertThat(payload.policies.get(0).name, is(policy.getName()));
    assertThat(payload.policies.get(0).threatLevel, is(policy.getThreatLevel()));

    assertThat(payload.roles, hasSize(1));
    assertThat(payload.roles.get(0).id, is(role.getId()));
    assertThat(payload.roles.get(0).name, is(role.getName()));
    assertThat(payload.roles.get(0).members, hasSize(1));
    assertThat(payload.roles.get(0).members.get(0).name, is(member.getMemberName()));
    assertThat(payload.roles.get(0).members.get(0).type, is(member.getMemberType().name()));
  }
}
