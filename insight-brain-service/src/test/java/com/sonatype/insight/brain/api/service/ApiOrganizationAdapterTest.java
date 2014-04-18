/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.api.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.api.dto.ApiTagDTO;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;

import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ApiOrganizationAdapterTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private ApiOrganizationAdapter apiOrganizationAdapter = new ApiOrganizationAdapter();

  @Test
  public void testConvertEntityToDTO() {

    Organization org =  tempEntity.newOrganization();
    Tag tag = tempEntity.newTag(org.getId());
    List<Tag> tagList = new ArrayList<>();
    tagList.add(tag);

    List<Organization> organizations = new ArrayList<>();
    Map<String, List<Tag>> orgTagMap = new HashMap<>();

    organizations.add(org);
    orgTagMap.put(org.getId(), tagList);

    ApiOrganizationListDTO apiOrganizationListDTO = apiOrganizationAdapter.convert(organizations, orgTagMap);
    assertThat(apiOrganizationListDTO, notNullValue());

    assertThat(apiOrganizationListDTO.organizations, hasSize(1));

    ApiOrganizationDTO organizationDTO = apiOrganizationListDTO.organizations.get(0);
    assertThat(organizationDTO.id, is(org.getId()));
    assertThat(organizationDTO.name, is(org.getName()));

    assertThat(organizationDTO.tags, hasSize(1));

    ApiTagDTO apiTagDTO = organizationDTO.tags.get(0);
    assertThat(apiTagDTO.id, is(tag.getId()));
    assertThat(apiTagDTO.name, is(tag.getName()));
    assertThat(apiTagDTO.description, is(tag.getDescription()));
    assertThat(apiTagDTO.color, is(tag.getColor()));
  }
}
