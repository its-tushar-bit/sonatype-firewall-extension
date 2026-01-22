/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
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
import com.sonatype.insight.brain.webhook.ManagementEvent.OwnerEvent;
import com.sonatype.insight.brain.webhook.dto.PolicyManagementPayload.OwnerDTO;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OwnerDTOUtilTest
    extends AbstractComponentTest
{
  @Inject
  private OwnerDTOUtil ownerDTOUtil;

  @Inject
  private ApplicationDAO applicationDAO;

  @Test
  public void testBuildOwnerDTO_Organization() {
    Organization organization = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(organization.getId());
    Label label = tempEntity.newLabel(organization.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(organization.getId());
    Policy policy = tempEntity.newPolicy(organization);
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    MembershipMapping member = tempEntity.newMembershipMapping(organization.getId(), role.getId(), user.getUsername());

    ManagementEvent event = new ManagementEvent();
    event.ownerId = organization.getId();

    OwnerDTO payload = ownerDTOUtil.buildOwnerDTO(event);
    assertThat(payload.id).isEqualTo(organization.getId());
    assertThat(payload.publicId).isEqualTo(organization.getPublicId());
    assertThat(payload.type).isEqualTo(organization.getType().name());
    assertThat(payload.name).isEqualTo(organization.getName());
    assertThat(payload.parentOwnerId).isEqualTo(organization.getParentOwnerId());

    assertThat(payload.applicationCategories).hasSize(1);
    assertThat(payload.applicationCategories.get(0).id).isEqualTo(tag.getId());
    assertThat(payload.applicationCategories.get(0).name).isEqualTo(tag.getName());
    assertThat(payload.applicationCategories.get(0).description).isEqualTo(tag.getDescription());
    assertThat(payload.applicationCategories.get(0).color).isEqualTo(tag.getColor().toValue());

    assertThat(payload.labels).hasSize(1);
    assertThat(payload.labels.get(0).id).isEqualTo(label.getId());
    assertThat(payload.labels.get(0).name).isEqualTo(label.getLabel());
    assertThat(payload.labels.get(0).description).isEqualTo(label.getDescription());
    assertThat(payload.labels.get(0).color).isEqualTo(label.getColor().toValue());

    assertThat(payload.licenseThreatGroups).hasSize(1);
    assertThat(payload.licenseThreatGroups.get(0).id).isEqualTo(licenseThreatGroup.getId());
    assertThat(payload.licenseThreatGroups.get(0).name).isEqualTo(licenseThreatGroup.getName());
    assertThat(payload.licenseThreatGroups.get(0).threatLevel).isEqualTo(licenseThreatGroup.getThreatLevel());

    assertThat(payload.policies).hasSize(1);
    assertThat(payload.policies.get(0).id).isEqualTo(policy.getId());
    assertThat(payload.policies.get(0).name).isEqualTo(policy.getName());
    assertThat(payload.policies.get(0).threatLevel).isEqualTo(policy.getThreatLevel());

    assertThat(payload.roles).hasSize(1);
    assertThat(payload.roles.get(0).id).isEqualTo(role.getId());
    assertThat(payload.roles.get(0).name).isEqualTo(role.getName());
    assertThat(payload.roles.get(0).members).hasSize(1);
    assertThat(payload.roles.get(0).members.get(0).name).isEqualTo(member.getMemberName());
    assertThat(payload.roles.get(0).members.get(0).type).isEqualTo(member.getMemberType().name());
  }

  @Test
  public void testBuildOwnerDTO_Application() {
    Application application = tempEntity.newApplicationWithParent("publicId");
    Label label = tempEntity.newLabel(application.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(application.getId());
    Policy policy = tempEntity.newPolicy(application);
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(false, Permission.READ);
    MembershipMapping member = tempEntity.newMembershipMapping(application.getId(), role.getId(), user.getUsername());

    ManagementEvent event = new ManagementEvent();
    event.ownerId = application.getId();

    OwnerDTO payload = ownerDTOUtil.buildOwnerDTO(event);
    assertThat(payload.id).isEqualTo(application.getId());
    assertThat(payload.publicId).isEqualTo(application.getPublicId());
    assertThat(payload.type).isEqualTo(application.getType().name());
    assertThat(payload.name).isEqualTo(application.getName());
    assertThat(payload.parentOwnerId).isEqualTo(application.getParentOwnerId());

    assertThat(payload.applicationCategories).isNull();

    assertThat(payload.labels).hasSize(1);
    assertThat(payload.labels.get(0).id).isEqualTo(label.getId());
    assertThat(payload.labels.get(0).name).isEqualTo(label.getLabel());
    assertThat(payload.labels.get(0).description).isEqualTo(label.getDescription());
    assertThat(payload.labels.get(0).color).isEqualTo(label.getColor().toValue());

    assertThat(payload.licenseThreatGroups).hasSize(1);
    assertThat(payload.licenseThreatGroups.get(0).id).isEqualTo(licenseThreatGroup.getId());
    assertThat(payload.licenseThreatGroups.get(0).name).isEqualTo(licenseThreatGroup.getName());
    assertThat(payload.licenseThreatGroups.get(0).threatLevel).isEqualTo(licenseThreatGroup.getThreatLevel());

    assertThat(payload.policies).hasSize(1);
    assertThat(payload.policies.get(0).id).isEqualTo(policy.getId());
    assertThat(payload.policies.get(0).name).isEqualTo(policy.getName());
    assertThat(payload.policies.get(0).threatLevel).isEqualTo(policy.getThreatLevel());

    assertThat(payload.roles).hasSize(1);
    assertThat(payload.roles.get(0).id).isEqualTo(role.getId());
    assertThat(payload.roles.get(0).name).isEqualTo(role.getName());
    assertThat(payload.roles.get(0).members).hasSize(1);
    assertThat(payload.roles.get(0).members.get(0).name).isEqualTo(member.getMemberName());
    assertThat(payload.roles.get(0).members.get(0).type).isEqualTo(member.getMemberType().name());
  }

  @Test
  public void testBuildOwnerDTO_DeletedOwner() {
    Application application = tempEntity.newApplicationWithParent("publicId");
    applicationDAO.delete(application);

    OwnerEvent event = new OwnerEvent();
    event.owner = application;
    event.ownerId = application.getId();

    OwnerDTO payload = ownerDTOUtil.buildOwnerDTO(event);
    assertThat(payload.id).isEqualTo(application.getId());
    assertThat(payload.publicId).isEqualTo(application.getPublicId());
    assertThat(payload.type).isEqualTo(application.getType().name());
    assertThat(payload.name).isEqualTo(application.getName());
    assertThat(payload.parentOwnerId).isEqualTo(application.getParentOwnerId());
    assertThat(payload.applicationCategories).isNull();
    assertThat(payload.labels).isEmpty();
    assertThat(payload.licenseThreatGroups).isEmpty();
    assertThat(payload.policies).isEmpty();
    assertThat(payload.roles).isEmpty();
  }
}
