/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class SidebarServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SidebarService sidebarService;

  @Test
  public void testGetOwnerDetails_Organization() {
    Organization organization = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(organization.getId());
    Policy policy = tempEntity.newPolicy(organization.getId(), organization.getName() + " Policy");
    Label label = tempEntity.newLabel(organization.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(organization.getId());

    OwnerDetailsDTO ownerDetailsDTO = sidebarService.getOwnerDetails(OwnerType.ORGANIZATION, organization.getId());
    assertThat(ownerDetailsDTO.tags, hasSize(1));
    assertThat(ownerDetailsDTO.tags.get(0).getId(), is(tag.getId()));

    assertThat(ownerDetailsDTO.policies, hasSize(1));
    assertThat(ownerDetailsDTO.policies.get(0).getId(), is(policy.getId()));

    assertThat(ownerDetailsDTO.labels, hasSize(1));
    assertThat(ownerDetailsDTO.labels.get(0).getId(), is(label.getId()));

    assertThat(ownerDetailsDTO.licenseThreatGroups, hasSize(1));
    assertThat(ownerDetailsDTO.licenseThreatGroups.get(0).getId(), is(licenseThreatGroup.getId()));

    assertThat(ownerDetailsDTO.roles, hasSize(new RoleDAO().getApplicationRoles().size()));
  }

  @Test
  public void testGetOwnerDetails_Application() {
    Application application = tempEntity.newApplicationWithParent("OwnerManagerServiceTestApplication");

    Policy policy = tempEntity.newPolicy(application.getId(), application.getName() + " Policy");
    Label label = tempEntity.newLabel(application.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(application.getId());

    OwnerDetailsDTO ownerDetailsDTO = sidebarService
        .getOwnerDetails(OwnerType.APPLICATION, application.getId());
    assertThat(ownerDetailsDTO.tags, hasSize(0));

    assertThat(ownerDetailsDTO.policies, hasSize(1));
    assertThat(ownerDetailsDTO.policies.get(0).getId(), is(policy.getId()));

    assertThat(ownerDetailsDTO.labels, hasSize(1));
    assertThat(ownerDetailsDTO.labels.get(0).getId(), is(label.getId()));

    assertThat(ownerDetailsDTO.licenseThreatGroups, hasSize(1));
    assertThat(ownerDetailsDTO.licenseThreatGroups.get(0).getId(), is(licenseThreatGroup.getId()));

    assertThat(ownerDetailsDTO.roles, hasSize(new RoleDAO().getApplicationRoles().size()));
  }
}
