/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.owner;

import java.util.Collections;
import java.util.Map;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dto.OwnerHierarchyDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ComponentH2Test
public class OwnerServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private OwnerService ownerService;

  @Inject
  private OwnerDAO ownerDAO;

  @Test
  public void testGetHierarchyNoAuth_OwnerDoesNotExist() {
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> ownerService.getHierarchyNoAuth(
        OwnerType.APPLICATION, "doesNotExist"));
    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> ownerService.getHierarchyNoAuth(
        OwnerType.ORGANIZATION, "doesNotExist"));
  }

  @Test
  public void testGetHierarchyNoAuth_RootOrganization() {
    Owner rootOrg = ownerDAO.getById(ROOT_ORGANIZATION_ID);
    OwnerHierarchyDTO expectedHierarchy = new OwnerHierarchyDTO(rootOrg.getId(), rootOrg.getPublicId(),
        rootOrg.getName(), rootOrg.getType(), null);

    OwnerHierarchyDTO hierarchy = ownerService.getHierarchyNoAuth(rootOrg.getType(), rootOrg.getPublicId());

    assertThat(hierarchy).usingRecursiveComparison().isEqualTo(expectedHierarchy);
  }

  @Test
  public void testGetHierarchyNoAuth_Organization() {
    Organization organization = tempEntity.newOrganization();
    OwnerHierarchyDTO organizationHierarchy = new OwnerHierarchyDTO(organization.getId(), organization.getPublicId(),
        organization.getName(), organization.getType(), null);
    Owner rootOrganization = ownerDAO.getById(ROOT_ORGANIZATION_ID);
    OwnerHierarchyDTO expectedHierarchy = new OwnerHierarchyDTO(rootOrganization.getId(),
        rootOrganization.getPublicId(), rootOrganization.getName(), rootOrganization.getType(),
        Collections.singletonList(organizationHierarchy));

    OwnerHierarchyDTO hierarchy = ownerService.getHierarchyNoAuth(organization.getType(), organization.getPublicId());

    assertThat(hierarchy).usingRecursiveComparison().isEqualTo(expectedHierarchy);
  }

  @Test
  public void testGetHierarchyNoAuth_Application() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    OwnerHierarchyDTO applicationHierarchy = new OwnerHierarchyDTO(application.getId(), application.getPublicId(),
        application.getName(), application.getType(), null);
    OwnerHierarchyDTO organizationHierarchy = new OwnerHierarchyDTO(organization.getId(), organization.getPublicId(),
        organization.getName(), organization.getType(), Collections.singletonList(applicationHierarchy));
    Owner rootOrganization = ownerDAO.getById(ROOT_ORGANIZATION_ID);
    OwnerHierarchyDTO expectedHierarchy = new OwnerHierarchyDTO(rootOrganization.getId(),
        rootOrganization.getPublicId(), rootOrganization.getName(), rootOrganization.getType(),
        Collections.singletonList(organizationHierarchy));

    OwnerHierarchyDTO hierarchy = ownerService.getHierarchyNoAuth(application.getType(), application.getPublicId());

    assertThat(hierarchy).usingRecursiveComparison().isEqualTo(expectedHierarchy);
  }

  @Test
  public void testGetOwnersWithReadPermissionsById_ROOT_ORGANIZATION_ID() {
    Map<String, Owner> allOwnersById = ownerService.getOwnersWithReadPermissionsById();
    assertThat(allOwnersById).isNotEmpty();
    assertThat(allOwnersById).hasSize(2);
    assertThat(allOwnersById.containsKey(ROOT_ORGANIZATION_ID)).isTrue();
    assertThat(allOwnersById.containsKey(REPOSITORY_CONTAINER_ID)).isTrue();
  }

  @Test
  public void testGetOwnersWithReadPermissionsById_SeveralOrganizations() {
    Organization organization = tempEntity.newOrganization();
    Organization organization1 = tempEntity.newOrganization();

    Map<String, Owner> allOwnersById = ownerService.getOwnersWithReadPermissionsById();

    assertThat(allOwnersById).isNotEmpty();
    assertThat(allOwnersById).hasSize(4);
    assertThat(allOwnersById.containsKey(organization.getId())).isTrue();
    assertThat(allOwnersById.containsKey(organization1.getId())).isTrue();
    assertThat(allOwnersById.containsKey(ROOT_ORGANIZATION_ID)).isTrue();
    assertThat(allOwnersById.containsKey(REPOSITORY_CONTAINER_ID)).isTrue();
  }

  @Test
  public void testGetOwnersWithReadPermissionsById_SeveralApplications() {
    Application application = tempEntity.newApplication(ROOT_ORGANIZATION_ID);
    Application application1 = tempEntity.newApplication(ROOT_ORGANIZATION_ID);

    Map<String, Owner> allOwnersById = ownerService.getOwnersWithReadPermissionsById();

    assertThat(allOwnersById).isNotEmpty();
    assertThat(allOwnersById).hasSize(4);
    assertThat(allOwnersById.containsKey(application.getId())).isTrue();
    assertThat(allOwnersById.containsKey(application1.getId())).isTrue();
    assertThat(allOwnersById.containsKey(ROOT_ORGANIZATION_ID)).isTrue();
    assertThat(allOwnersById.containsKey(REPOSITORY_CONTAINER_ID)).isTrue();
  }

  @Test
  public void testGetOwnersWithReadPermissionsById_SeveralRepositories() {
    Repository repository = tempEntity.newRepository();
    Repository repository1 = tempEntity.newRepository();

    Map<String, Owner> allOwnersById = ownerService.getOwnersWithReadPermissionsById();

    assertThat(allOwnersById).isNotEmpty();
    assertThat(allOwnersById).hasSize(4);
    assertThat(allOwnersById.containsKey(repository.getId())).isTrue();
    assertThat(allOwnersById.containsKey(repository1.getId())).isTrue();
    assertThat(allOwnersById.containsKey(ROOT_ORGANIZATION_ID)).isTrue();
    assertThat(allOwnersById.containsKey(REPOSITORY_CONTAINER_ID)).isTrue();
  }

  @Test
  public void testGetOwnersWithReadPermissionsById_RepositoryManager() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    Map<String, Owner> allOwnersById = ownerService.getOwnersWithReadPermissionsById();

    assertThat(allOwnersById).isNotEmpty();
    assertThat(allOwnersById).hasSize(2);
    assertThat(allOwnersById.containsKey(repositoryManager.getId())).isFalse();
    assertThat(allOwnersById.containsKey(ROOT_ORGANIZATION_ID)).isTrue();
    assertThat(allOwnersById.containsKey(REPOSITORY_CONTAINER_ID)).isTrue();
  }

  @Test
  public void testGetOwnerByTypeAndInternalId_WithResult() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newApplicationWithParent();

    OwnerDTO result = ownerService.getOwnerByTypeAndInternalId(application.getType(), application.getId());

    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(application.getId());
    assertThat(result.getName()).isEqualTo(application.getName());
    assertThat(result.getParentOwnerId()).isEqualTo(application.getParentOwnerId());
    assertThat(result.getPublicId()).isEqualTo(application.getPublicId());
    assertThat(result.getType()).isEqualTo(application.getType());
  }

  @Test
  public void testGetOwnerByTypeAndInternalId_NoResult() {
    tempEntity.newOrganization();
    tempEntity.newApplicationWithParent();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> ownerService.getOwnerByTypeAndInternalId(OwnerType.ORGANIZATION, "doesNotExist"));

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> ownerService.getOwnerByTypeAndInternalId(OwnerType.APPLICATION, "doesNotExist"));
  }

  @Test
  public void testGetOwnerByTypeAndInternalId_WrongOwnerType() {
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplicationWithParent(organization);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> ownerService.getOwnerByTypeAndInternalId(OwnerType.ORGANIZATION, application.getId()));

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> ownerService.getOwnerByTypeAndInternalId(OwnerType.APPLICATION, organization.getId()));
  }
}
