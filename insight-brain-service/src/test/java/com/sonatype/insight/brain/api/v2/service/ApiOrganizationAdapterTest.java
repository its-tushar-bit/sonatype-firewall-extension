/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.AbstractDataTest;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiTagDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiOrganizationAdapterTest
    extends AbstractDataTest
{
  @Test
  public void testConvert_EntityListToDTO() {
    Organization org = tempEntity.newOrganization();
    org.setParentOrganizationId("prent-test-id");
    Tag tag = tempEntity.newTag(org.getId());
    List<Tag> tagList = new ArrayList<>();
    tagList.add(tag);

    List<Organization> organizations = new ArrayList<>();
    Map<String, List<Tag>> orgTagMap = new HashMap<>();

    organizations.add(org);
    orgTagMap.put(org.getId(), tagList);

    ApiOrganizationListDTO apiOrganizationListDTO = ApiOrganizationAdapter.convert(organizations, orgTagMap);
    assertThat(apiOrganizationListDTO).isNotNull();

    assertThat(apiOrganizationListDTO.organizations).hasSize(1);

    ApiOrganizationDTO organizationDTO = apiOrganizationListDTO.organizations.get(0);
    assertThat(organizationDTO.id).isEqualTo(org.getId());
    assertThat(organizationDTO.name).isEqualTo(org.getName());
    assertThat(organizationDTO.parentOrganizationId).isEqualTo("prent-test-id");
    assertThat(organizationDTO.tags).hasSize(1);

    ApiTagDTO apiTagDTO = organizationDTO.tags.get(0);
    assertThat(apiTagDTO.id).isEqualTo(tag.getId());
    assertThat(apiTagDTO.name).isEqualTo(tag.getName());
    assertThat(apiTagDTO.description).isEqualTo(tag.getDescription());
    assertThat(apiTagDTO.color).isEqualTo(tag.getColor());
  }

  @Test
  public void testConvert_EntityToDTO() {
    Organization org = tempEntity.newOrganization();
    org.setParentOrganizationId("prent-test-id");
    Tag tag = tempEntity.newTag(org.getId());
    List<Tag> tagList = new ArrayList<>();
    tagList.add(tag);

    ApiOrganizationDTO organizationDTO = ApiOrganizationAdapter.convert(org, tagList);
    assertThat(organizationDTO.id).isEqualTo(org.getId());
    assertThat(organizationDTO.name).isEqualTo(org.getName());
    assertThat(organizationDTO.parentOrganizationId).isEqualTo("prent-test-id");
    assertThat(organizationDTO.tags).hasSize(1);

    ApiTagDTO apiTagDTO = organizationDTO.tags.get(0);
    assertThat(apiTagDTO.id).isEqualTo(tag.getId());
    assertThat(apiTagDTO.name).isEqualTo(tag.getName());
    assertThat(apiTagDTO.description).isEqualTo(tag.getDescription());
    assertThat(apiTagDTO.color).isEqualTo(tag.getColor());
  }
}
