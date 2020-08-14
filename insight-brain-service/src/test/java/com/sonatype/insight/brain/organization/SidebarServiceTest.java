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
import com.sonatype.insight.brain.organization.OwnerListDTO.SidebarApplicationDTO;
import com.sonatype.insight.brain.organization.OwnerListDTO.SidebarOrganizationDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class SidebarServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SidebarService sidebarService;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
  }

  @Test
  public void testGetOwnerDetails_Organization() {
    Organization organization = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(organization.getId());
    Policy policy = tempEntity.newPolicy(organization);
    Label label = tempEntity.newLabel(organization.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(organization.getId());

    OwnerDetailsDTO ownerDetailsDTO = sidebarService.getOwnerDetails(OwnerType.ORGANIZATION, organization.getId());
    assertThat(ownerDetailsDTO.tags).hasSize(1);
    assertThat(ownerDetailsDTO.tags.get(0).getId()).isEqualTo(tag.getId());

    assertThat(ownerDetailsDTO.policies).hasSize(1);
    assertThat(ownerDetailsDTO.policies.get(0).getId()).isEqualTo(policy.getId());

    assertThat(ownerDetailsDTO.labels).hasSize(1);
    assertThat(ownerDetailsDTO.labels.get(0).getId()).isEqualTo(label.getId());

    assertThat(ownerDetailsDTO.licenseThreatGroups).hasSize(1);
    assertThat(ownerDetailsDTO.licenseThreatGroups.get(0).getId()).isEqualTo(licenseThreatGroup.getId());

    assertThat(ownerDetailsDTO.roles.membersByRole).hasSameSizeAs(new RoleDAO().getApplicationRoles());
  }

  @Test
  public void testGetOwnerDetails_Application() {
    Application application = tempEntity.newApplicationWithParent("OwnerManagerServiceTestApplication");

    Policy policy = tempEntity.newPolicy(application);
    Label label = tempEntity.newLabel(application.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(application.getId());

    OwnerDetailsDTO ownerDetailsDTO = sidebarService.getOwnerDetails(OwnerType.APPLICATION, application.getId());
    assertThat(ownerDetailsDTO.tags).isEmpty();

    assertThat(ownerDetailsDTO.policies).hasSize(1);
    assertThat(ownerDetailsDTO.policies.get(0).getId()).isEqualTo(policy.getId());

    assertThat(ownerDetailsDTO.labels).hasSize(1);
    assertThat(ownerDetailsDTO.labels.get(0).getId()).isEqualTo(label.getId());

    assertThat(ownerDetailsDTO.licenseThreatGroups).hasSize(1);
    assertThat(ownerDetailsDTO.licenseThreatGroups.get(0).getId()).isEqualTo(licenseThreatGroup.getId());

    assertThat(ownerDetailsDTO.roles.membersByRole).hasSameSizeAs(new RoleDAO().getApplicationRoles());
  }

  @Test
  public void testGetOwnerList() {
    Organization orgOne = tempEntity.newOrganization();
    Application appOne = tempEntity.newApplication(orgOne.getId());
    Application appTwo = tempEntity.newApplication(orgOne.getId());
    Organization orgTwo = tempEntity.newOrganization();
    Application appThree = tempEntity.newApplication(orgTwo.getId());

    OwnerListDTO ownerListDTO = sidebarService.getOwnerList();
    assertOwnerListDTO(ownerListDTO, orgOne, appOne, appTwo, orgTwo, appThree, true);
  }

  private void assertOwnerListDTO(OwnerListDTO ownerListDTO,
                                  Organization orgOne,
                                  Application appOne,
                                  Application appTwo,
                                  Organization orgTwo,
                                  Application appThree,
                                  boolean withRootOrganization)
  {
    assertThat(ownerListDTO.organizations).hasSize(withRootOrganization ? 3 : 2);
    for (SidebarOrganizationDTO organization : ownerListDTO.organizations) {
      if (organization.id.equals(orgOne.getId())) {
        assertThat(organization.name).isEqualTo(orgOne.getName());
        assertThat(organization.applications).hasSize(2);

        for (SidebarApplicationDTO application : organization.applications) {
          if (application.id.equals(appOne.getId())) {
            assertThat(application.publicId).isEqualTo(appOne.getPublicId());
            assertThat(application.organizationId).isEqualTo(appOne.getOrganizationId());
            assertThat(application.name).isEqualTo(appOne.getName());
          }
          else if (application.id.equals(appTwo.getId())) {
            if (application.id.equals(appTwo.getId())) {
              assertThat(application.publicId).isEqualTo(appTwo.getPublicId());
              assertThat(application.organizationId).isEqualTo(appTwo.getOrganizationId());
              assertThat(application.name).isEqualTo(appTwo.getName());
            }
            else {
              fail("Unexpected application ID " + application.id);
            }
          }
        }
      }
      else if (organization.id.equals(orgTwo.getId())) {
        assertThat(organization.name).isEqualTo(orgTwo.getName());
        assertThat(organization.applications).hasSize(1);

        SidebarApplicationDTO application = organization.applications.get(0);
        assertThat(application.id).isEqualTo(appThree.getId());
        assertThat(application.publicId).isEqualTo(appThree.getPublicId());
        assertThat(application.organizationId).isEqualTo(appThree.getOrganizationId());
        assertThat(application.name).isEqualTo(appThree.getName());
      }
      else if (withRootOrganization && organization.id.equals(Organization.ROOT_ORGANIZATION_ID)) {
        assertThat(organization.applications).isEmpty();
      }
      else {
        fail("Unexpected organization ID " + organization.id);
      }
    }
  }
}
