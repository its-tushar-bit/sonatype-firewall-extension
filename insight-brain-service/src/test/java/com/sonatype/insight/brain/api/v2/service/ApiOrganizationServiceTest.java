/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiTagDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiOrganizationServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiOrganizationService apiOrganizationService;

  @Inject
  private OrganizationDAO organizationDAO;

  @Test
  public void testAddOrganization() {
    final String ORGANIZATION_NAME = "testName";

    Organization parentOrg = tempEntity.newOrganization();

    ApiOrganizationDTO apiOrganizationDTO = new ApiOrganizationDTO(null, ORGANIZATION_NAME);
    apiOrganizationDTO.parentOrganizationId = parentOrg.getId();
    ApiOrganizationDTO newOrganizationDTO = apiOrganizationService.addOrganization(apiOrganizationDTO);

    Organization organization = organizationDAO.getByIdNotNull(newOrganizationDTO.id);

    assertThat(organization.getName()).isEqualTo(ORGANIZATION_NAME);
    assertThat(organization.getParentOrganizationId()).isEqualTo(parentOrg.getId());

    assertThat(newOrganizationDTO.id).isNotEmpty();
    assertThat(newOrganizationDTO.name).isEqualTo(ORGANIZATION_NAME);
    assertThat(newOrganizationDTO.tags).isEmpty();
  }

  @Test
  public void testAddOrganization_InvalidParentOrganizationId() {
    final String ORGANIZATION_NAME = "testName";

    ApiOrganizationDTO apiOrganizationDTO = new ApiOrganizationDTO(null, ORGANIZATION_NAME);
    apiOrganizationDTO.parentOrganizationId = "invalid-org-id";

    assertThatThrownBy(() -> {
      apiOrganizationService.addOrganization(apiOrganizationDTO);
    }).isInstanceOf(BadRequestException.class).hasMessage("Invalid parent organization");
  }

  @Test
  public void testAddOrganization_TagsNotSupported() {
    final String ORGANIZATION_NAME = "testName";

    ApiOrganizationDTO apiOrganizationDTO = new ApiOrganizationDTO(null, ORGANIZATION_NAME);
    apiOrganizationDTO.tags = Collections.emptyList();

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiOrganizationService.addOrganization(apiOrganizationDTO))
        .withMessage("Organization must not have tags set on creation.");

    assertThat(organizationDAO.getByName(ORGANIZATION_NAME)).isNull();
  }

  @Test
  public void testAddOrganization_IdNotSupported() {
    final String ORGANIZATION_NAME = "testName";

    ApiOrganizationDTO apiOrganizationDTO = new ApiOrganizationDTO("testId", ORGANIZATION_NAME);

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiOrganizationService.addOrganization(apiOrganizationDTO))
        .withMessage("Organization must not have an ID set on creation.");

    assertThat(organizationDAO.getByName(ORGANIZATION_NAME)).isNull();
  }

  @Test
  public void testGetOrganizationById() {
    Organization organization = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(organization.getId());

    ApiOrganizationDTO apiOrganizationDTO = apiOrganizationService.getOrganizationById(organization.getId());
    assertOrganizationData(apiOrganizationDTO, organization, Collections.singletonList(tag));
  }

  @Test
  public void testGetOrganizations_GivenEmpty() {
    Organization organization = tempEntity.newOrganization("testOrganization");
    Tag tag = tempEntity.newTag(organization.getId());

    ApiOrganizationListDTO apiOrganizationListDTO = apiOrganizationService.getOrganizations(Collections.emptySet());

    assertThat(apiOrganizationListDTO).isNotNull();
    assertThat(apiOrganizationListDTO.organizations).hasSize(2);
    assertOrganizationData(apiOrganizationListDTO.organizations.get(0),
        organizationDAO.getById(Organization.ROOT_ORGANIZATION_ID), Collections.emptyList());
    assertOrganizationData(apiOrganizationListDTO.organizations.get(1), organization, Collections.singletonList(tag));
  }

  @Test
  public void testGetOrganizations_GivenName() {
    Organization organization = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(organization.getId());

    tempEntity.newOrganizationWithRepositoryManager("org-with-repo-man");

    ApiOrganizationListDTO apiOrganizationListDTO =
        apiOrganizationService.getOrganizations(Collections.singleton(organization.getName()));

    assertThat(apiOrganizationListDTO).isNotNull();
    assertThat(apiOrganizationListDTO.organizations).hasSize(1);
    assertOrganizationData(apiOrganizationListDTO.organizations.get(0), organization, Collections.singletonList(tag));
  }

  @Test
  public void testGetOrganizations_GivenMultipleNames() {
    Organization org1 = tempEntity.newOrganization("org1");
    Tag tag1 = tempEntity.newTag(org1.getId());
    tempEntity.newOrganization("org2");
    Organization org3 = tempEntity.newOrganization("org3");
    Tag tag3 = tempEntity.newTag(org3.getId());

    tempEntity.newOrganizationWithRepositoryManager("org-with-repo-man");

    ApiOrganizationListDTO apiOrganizationListDTO =
        apiOrganizationService.getOrganizations(Sets.newHashSet(org3.getName(), org1.getName()));

    assertThat(apiOrganizationListDTO).isNotNull();
    assertThat(apiOrganizationListDTO.organizations).hasSize(2);
    assertOrganizationData(apiOrganizationListDTO.organizations.get(0), org1, Collections.singletonList(tag1));
    assertOrganizationData(apiOrganizationListDTO.organizations.get(1), org3, Collections.singletonList(tag3));
  }

  @Test
  public void testGetOrganizations_NotFound() {
    ApiOrganizationListDTO apiOrganizationListDTO =
        apiOrganizationService.getOrganizations(Collections.singleton("doesNotExist"));

    assertThat(apiOrganizationListDTO).isNotNull();
    assertThat(apiOrganizationListDTO.organizations).isEmpty();
  }

  @Test
  public void testGetOrganizations_SomeNotFound() {
    Organization organization = tempEntity.newOrganization("org");
    Tag tag = tempEntity.newTag(organization.getId());
    ApiOrganizationListDTO apiOrganizationListDTO = apiOrganizationService
        .getOrganizations(Sets.newHashSet("doesNotExist1", organization.getName(), "doesNotExist2"));

    assertThat(apiOrganizationListDTO).isNotNull();
    assertThat(apiOrganizationListDTO.organizations).hasSize(1);
    assertOrganizationData(apiOrganizationListDTO.organizations.get(0), organization, Collections.singletonList(tag));
  }

  @Test
  public void testGetOrganizations_ExcludeRepositoryManagerAndRelatedRepository() {
    tempEntity.newOrganizationWithRepositoryManager("org-exclude");
    Organization organization = tempEntity.newOrganization("test1");
    ApiOrganizationListDTO apiOrganizationListDTO = apiOrganizationService
        .getOrganizations(Collections.emptySet());

    assertThat(apiOrganizationListDTO).isNotNull();
    assertThat(apiOrganizationListDTO.organizations).hasSize(2);
    assertThat(apiOrganizationListDTO.organizations.get(0).id).isEqualTo("ROOT_ORGANIZATION_ID");
    assertThat(apiOrganizationListDTO.organizations.get(1).id).isEqualTo(organization.getId());
    assertThat(apiOrganizationListDTO.organizations)
        .noneMatch(org -> "org-exclude".equals(org.name));

    apiOrganizationListDTO = apiOrganizationService
        .getOrganizations(Sets.newHashSet("org-exclude", organization.getName()));
    assertThat(apiOrganizationListDTO.organizations).hasSize(1);
    assertThat(apiOrganizationListDTO.organizations.get(0).id).isEqualTo(organization.getId());
  }

  @Test
  public void testDeleteOrganization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Organization otherOrganization = tempEntity.newOrganization();

    apiOrganizationService.deleteOrganization(organization.getId());

    assertThat(organizationDAO.getById(organization.getId())).isNull();
    assertThat(organizationDAO.getById(otherOrganization.getId())).isNotNull();
  }

  @Test
  public void testDeleteOrganization_RootOrganization() {
    assertThatExceptionOfType(BadRequestException.class).isThrownBy(
        () -> apiOrganizationService.deleteOrganization(Organization.ROOT_ORGANIZATION_ID))
        .withMessage("The root organization cannot be deleted.");
  }

  @Test
  public void testGetOrganizationsByIds_SingleId() {
    Organization organization = tempEntity.newOrganization();

    ApiOrganizationListDTO apiOrganizationListDTO =
        apiOrganizationService.getOrganizationsByIds(Set.of(organization.getId()));

    assertThat(apiOrganizationListDTO).isNotNull();
    assertThat(apiOrganizationListDTO.organizations).hasSize(1);
    assertOrganizationData(apiOrganizationListDTO.organizations.get(0), organization, Collections.emptyList());
  }

  @Test
  public void testGetOrganizationsByIds_MultipleIds() {
    Organization org1 = tempEntity.newOrganization("org1");
    Organization org2 = tempEntity.newOrganization("org2");

    ApiOrganizationListDTO result =
        apiOrganizationService.getOrganizationsByIds(Set.of(org1.getId(), org2.getId()));

    assertThat(result).isNotNull();
    assertThat(result.organizations).hasSize(2);

    // Extract IDs from result
    List<String> resultIds = result.organizations.stream()
        .map(org -> org.id)
        .toList();

    // Assert both are present (order doesn't matter)
    assertThat(resultIds).containsExactlyInAnyOrder(org1.getId(), org2.getId());

    // Verify each organization individually by finding it
    ApiOrganizationDTO org1Result = result.organizations.stream()
        .filter(org -> org.id.equals(org1.getId()))
        .findFirst()
        .orElseThrow();
    assertOrganizationData(org1Result, org1, Collections.emptyList());

    ApiOrganizationDTO org2Result = result.organizations.stream()
        .filter(org -> org.id.equals(org2.getId()))
        .findFirst()
        .orElseThrow();
    assertOrganizationData(org2Result, org2, Collections.emptyList());
  }

  @Test
  public void testGetOrganizationsByIds_NotFound() {
    ApiOrganizationListDTO apiOrganizationListDTO =
        apiOrganizationService.getOrganizationsByIds(Set.of("non-existent-id-1", "non-existent-id-2"));

    assertThat(apiOrganizationListDTO).isNotNull();
    assertThat(apiOrganizationListDTO.organizations).isEmpty();
  }

  /**
   * Unit test to verify getOrganizations uses batch query for tags (not N+1).
   */
  @Test
  public void testGetOrganizations_UsesBatchQueryForTags() {
    OrganizationDAO mockOrgDAO = mock(OrganizationDAO.class);
    TagDAO mockTagDAO = mock(TagDAO.class);
    OrganizationService mockOrgService = mock(OrganizationService.class);

    Organization org1 = new Organization("org1");
    org1.setId("org-id-1");
    Organization org2 = new Organization("org2");
    org2.setId("org-id-2");

    when(mockOrgService.getAllWithoutRelatedRepositories()).thenReturn(Arrays.asList(org1, org2));
    when(mockTagDAO.getByOrganizationIds(any())).thenReturn(Collections.emptyList());

    ApiOrganizationService service = new ApiOrganizationService(mockOrgDAO, mockTagDAO, mockOrgService);
    service.getOrganizations(Collections.emptySet());

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Set<String>> orgIdsCaptor = ArgumentCaptor.forClass(Set.class);
    verify(mockTagDAO).getByOrganizationIds(orgIdsCaptor.capture());
    assertThat(orgIdsCaptor.getValue()).containsExactlyInAnyOrder("org-id-1", "org-id-2");
    verify(mockTagDAO, never()).getByOrganizationId(any());
  }

  private void assertOrganizationData(
      ApiOrganizationDTO apiOrganizationDTO,
      Organization organization,
      List<Tag> tags)
  {
    assertThat(apiOrganizationDTO.id).isEqualTo(organization.getId());
    assertThat(apiOrganizationDTO.name).isEqualTo(organization.getName());
    assertThat(apiOrganizationDTO.tags).hasSize(tags.size());
    apiOrganizationDTO.tags.forEach(apiTagDTO -> assertTagData(apiTagDTO, tags));
  }

  private void assertTagData(ApiTagDTO apiTagDTO, List<Tag> tags) {
    Tag tag = tags.stream().filter(t -> t.getId().equals(apiTagDTO.id)).findFirst().orElse(null);
    assertThat(tag).isNotNull();
    assertThat(apiTagDTO.id).isEqualTo(tag.getId());
    assertThat(apiTagDTO.name).isEqualTo(tag.getName());
    assertThat(apiTagDTO.description).isEqualTo(tag.getDescription());
    assertThat(apiTagDTO.color).isEqualTo(tag.getColor());
  }
}
