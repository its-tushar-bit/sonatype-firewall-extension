/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.service;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.api.dto.ApiApplicationTagDTO;
import com.sonatype.insight.brain.model.tag.ApplicationTag;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

public class ApiApplicationTagAdapterTest
{

  private final ApiApplicationTagAdapter apiApplicationTagAdapter = new ApiApplicationTagAdapter();

  private static final String ID = "testId";

  private static final String TAG_ID = "testTagId";

  private static final String APPLICATION_ID = "testApplicationTagId";

  @Test
  public void testConvertToDTO() {
    List<ApplicationTag> applicationTags = new ArrayList<>();
    ApplicationTag applicationTag = new ApplicationTag(APPLICATION_ID, TAG_ID);
    applicationTag.setId(ID);
    applicationTags.add(applicationTag);

    List<ApiApplicationTagDTO> apiApplicationTagDTOs = apiApplicationTagAdapter.convertToDTO(applicationTags);
    assertThat(apiApplicationTagDTOs, hasSize(1));
    assertThat(apiApplicationTagDTOs.get(0).id, is(applicationTag.getId()));
    assertThat(apiApplicationTagDTOs.get(0).applicationId, is(applicationTag.getApplicationId()));
    assertThat(apiApplicationTagDTOs.get(0).tagId, is(applicationTag.getTagId()));
  }

  @Test
  public void testConvertToDTO_nullList() {
    List<ApiApplicationTagDTO> apiApplicationTagDTOs = apiApplicationTagAdapter.convertToDTO(null);
    assertThat(apiApplicationTagDTOs, hasSize(0));
  }

  @Test
  public void testConvertFromDTO() {
    List<ApiApplicationTagDTO> apiApplicationTagDTOs = new ArrayList<>();
    ApiApplicationTagDTO apiApplicationTagDTO = new ApiApplicationTagDTO();
    apiApplicationTagDTO.id = ID;
    apiApplicationTagDTO.tagId = TAG_ID;
    apiApplicationTagDTOs.add(apiApplicationTagDTO);

    List<ApplicationTag> applicationTags = apiApplicationTagAdapter
        .convertFromDTO(APPLICATION_ID, apiApplicationTagDTOs);
    assertThat(applicationTags, hasSize(1));
    assertThat(applicationTags.get(0).getId(), is(apiApplicationTagDTO.id));
    assertThat(applicationTags.get(0).getApplicationId(), is(APPLICATION_ID));
    assertThat(applicationTags.get(0).getTagId(), is(apiApplicationTagDTO.tagId));
  }

  @Test
  public void testConvertFromDTO_nullList() {
    List<ApplicationTag> applicationTags = apiApplicationTagAdapter.convertFromDTO(APPLICATION_ID, null);
    assertThat(applicationTags, hasSize(0));
  }
}
