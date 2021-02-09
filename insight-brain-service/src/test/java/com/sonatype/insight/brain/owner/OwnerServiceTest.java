/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.owner;

import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dto.OwnerHierarchyDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class OwnerServiceTest
    extends AbstractComponentTest
{
  @Inject
  private OwnerService ownerService;

  @Inject
  private OwnerDAO ownerDAO;

  @Test
  public void testGetHierarchy_OwnerDoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> ownerService.getHierarchy(
        OwnerType.APPLICATION, "doesNotExist"));
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> ownerService.getHierarchy(
        OwnerType.ORGANIZATION, "doesNotExist"));
  }

  @Test
  public void testGetHierarchy_RootOrganization() {
    Owner rootOrg = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    OwnerHierarchyDTO expectedHierarchy = new OwnerHierarchyDTO(rootOrg.getId(), rootOrg.getPublicId(),
        rootOrg.getName(), rootOrg.getType(), null);

    OwnerHierarchyDTO hierarchy = ownerService.getHierarchy(rootOrg.getType(), rootOrg.getPublicId());

    assertThat(hierarchy).usingRecursiveComparison().isEqualTo(expectedHierarchy);
  }

  @Test
  public void testGetHierarchy_Organization() {
    Organization organization = tempEntity.newOrganization();
    OwnerHierarchyDTO organizationHierarchy = new OwnerHierarchyDTO(organization.getId(), organization.getPublicId(),
        organization.getName(), organization.getType(), null);
    Owner rootOrganization = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    OwnerHierarchyDTO expectedHierarchy = new OwnerHierarchyDTO(rootOrganization.getId(),
        rootOrganization.getPublicId(), rootOrganization.getName(), rootOrganization.getType(),
        Collections.singletonList(organizationHierarchy));

    OwnerHierarchyDTO hierarchy = ownerService.getHierarchy(organization.getType(), organization.getPublicId());

    assertThat(hierarchy).usingRecursiveComparison().isEqualTo(expectedHierarchy);
  }

  @Test
  public void testGetHierarchy_Application() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    OwnerHierarchyDTO applicationHierarchy = new OwnerHierarchyDTO(application.getId(), application.getPublicId(),
        application.getName(), application.getType(), null);
    OwnerHierarchyDTO organizationHierarchy = new OwnerHierarchyDTO(organization.getId(), organization.getPublicId(),
        organization.getName(), organization.getType(), Collections.singletonList(applicationHierarchy));
    Owner rootOrganization = ownerDAO.getById(Organization.ROOT_ORGANIZATION_ID);
    OwnerHierarchyDTO expectedHierarchy = new OwnerHierarchyDTO(rootOrganization.getId(),
        rootOrganization.getPublicId(), rootOrganization.getName(), rootOrganization.getType(),
        Collections.singletonList(organizationHierarchy));

    OwnerHierarchyDTO hierarchy = ownerService.getHierarchy(application.getType(), application.getPublicId());

    assertThat(hierarchy).usingRecursiveComparison().isEqualTo(expectedHierarchy);
  }
}
