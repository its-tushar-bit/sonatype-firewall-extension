/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.migration.RootOrganizationConfigMigrationUtils;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SidebarServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SidebarService sidebarService;

  private RootOrganizationConfigMigrationUtils rootOrganizationConfigMigrationUtils;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    rootOrganizationConfigMigrationUtils = mock(RootOrganizationConfigMigrationUtils.class);
    binder.bind(RootOrganizationConfigMigrationUtils.class).toInstance(rootOrganizationConfigMigrationUtils);
  }

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

    assertThat(ownerDetailsDTO.roles.membersByRole, hasSize(new RoleDAO().getApplicationRoles().size()));
  }

  @Test
  public void testGetOwnerDetails_Application() {
    Application application = tempEntity.newApplicationWithParent("OwnerManagerServiceTestApplication");

    Policy policy = tempEntity.newPolicy(application.getId(), application.getName() + " Policy");
    Label label = tempEntity.newLabel(application.getId());
    LicenseThreatGroup licenseThreatGroup = tempEntity.newLicenseThreatGroup(application.getId());

    OwnerDetailsDTO ownerDetailsDTO = sidebarService.getOwnerDetails(OwnerType.APPLICATION, application.getId());
    assertThat(ownerDetailsDTO.tags, hasSize(0));

    assertThat(ownerDetailsDTO.policies, hasSize(1));
    assertThat(ownerDetailsDTO.policies.get(0).getId(), is(policy.getId()));

    assertThat(ownerDetailsDTO.labels, hasSize(1));
    assertThat(ownerDetailsDTO.labels.get(0).getId(), is(label.getId()));

    assertThat(ownerDetailsDTO.licenseThreatGroups, hasSize(1));
    assertThat(ownerDetailsDTO.licenseThreatGroups.get(0).getId(), is(licenseThreatGroup.getId()));

    assertThat(ownerDetailsDTO.roles.membersByRole, hasSize(new RoleDAO().getApplicationRoles().size()));
  }

  @Test
  public void testGetOwnerList_WithoutRootOrganization() {
    Organization orgOne = tempEntity.newOrganization();
    Application appOne = tempEntity.newApplication(orgOne.getId());
    Application appTwo = tempEntity.newApplication(orgOne.getId());
    Organization orgTwo = tempEntity.newOrganization();
    Application appThree = tempEntity.newApplication(orgTwo.getId());

    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(false);
    OwnerListDTO ownerListDTO = sidebarService.getOwnerList();
    assertOwnerListDTO(ownerListDTO, orgOne, appOne, appTwo, orgTwo, appThree, false);
  }

  @Test
  public void testGetOwnerList_WithRootOrganization() {
    Organization orgOne = tempEntity.newOrganization();
    Application appOne = tempEntity.newApplication(orgOne.getId());
    Application appTwo = tempEntity.newApplication(orgOne.getId());
    Organization orgTwo = tempEntity.newOrganization();
    Application appThree = tempEntity.newApplication(orgTwo.getId());

    when(rootOrganizationConfigMigrationUtils.isMigrated()).thenReturn(true);
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
    assertThat(ownerListDTO.organizations, hasSize(withRootOrganization ? 3 : 2));
    for (SidebarOrganizationDTO organization : ownerListDTO.organizations) {
      if (organization.id.equals(orgOne.getId())) {
        assertThat(organization.name, is(orgOne.getName()));
        assertThat(organization.applications, hasSize(2));

        for (SidebarApplicationDTO application : organization.applications) {
          if (application.id.equals(appOne.getId())) {
            assertThat(application.publicId, is(appOne.getPublicId()));
            assertThat(application.organizationId, is(appOne.getOrganizationId()));
            assertThat(application.name, is(appOne.getName()));
          }
          else if (application.id.equals(appTwo.getId())) {
            if (application.id.equals(appTwo.getId())) {
              assertThat(application.publicId, is(appTwo.getPublicId()));
              assertThat(application.organizationId, is(appTwo.getOrganizationId()));
              assertThat(application.name, is(appTwo.getName()));
            }
            else {
              fail("Unexpected application ID " + application.id);
            }
          }
        }
      }
      else if (organization.id.equals(orgTwo.getId())) {
        assertThat(organization.name, is(orgTwo.getName()));
        assertThat(organization.applications, hasSize(1));

        SidebarApplicationDTO application = organization.applications.get(0);
        assertThat(application.id, is(appThree.getId()));
        assertThat(application.publicId, is(appThree.getPublicId()));
        assertThat(application.organizationId, is(appThree.getOrganizationId()));
        assertThat(application.name, is(appThree.getName()));
      }
      else if (withRootOrganization && organization.id.equals(Organization.ROOT_ORGANIZATION_ID)) {
        assertThat(organization.applications, hasSize(0));
      }
      else {
        fail("Unexpected organization ID " + organization.id);
      }
    }
  }
}
