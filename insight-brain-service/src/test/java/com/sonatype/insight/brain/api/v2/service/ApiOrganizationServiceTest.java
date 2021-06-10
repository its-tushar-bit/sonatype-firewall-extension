/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiTagDTO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.Sets;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

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

    ApiOrganizationDTO apiOrganizationDTO = new ApiOrganizationDTO(null, ORGANIZATION_NAME);
    ApiOrganizationDTO newOrganizationDTO = apiOrganizationService.addOrganization(apiOrganizationDTO);

    Organization organization = organizationDAO.getByIdNotNull(newOrganizationDTO.id);
    tempEntity.register(organization);

    assertThat(organization.getName()).isEqualTo(ORGANIZATION_NAME);
    assertThat(organization.getParentOrganizationId()).isEqualTo(Organization.ROOT_ORGANIZATION_ID);

    assertThat(newOrganizationDTO.id).isNotEmpty();
    assertThat(newOrganizationDTO.name).isEqualTo(ORGANIZATION_NAME);
    assertThat(newOrganizationDTO.tags).isEmpty();
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
        new OrganizationDAO().getById(Organization.ROOT_ORGANIZATION_ID), Collections.emptyList());
    assertOrganizationData(apiOrganizationListDTO.organizations.get(1), organization, Collections.singletonList(tag));
  }

  @Test
  public void testGetOrganizations_GivenName() {
    Organization organization = tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(organization.getId());

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
