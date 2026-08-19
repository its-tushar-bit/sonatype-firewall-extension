/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationTagDTO;
import com.sonatype.insight.brain.model.tag.ApplicationTag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiApplicationTagAdapterTest
{
  private static final String ID = "testId";

  private static final String TAG_ID = "testTagId";

  private static final String APPLICATION_ID = "testApplicationTagId";

  @Test
  public void testConvertToDTO() {
    List<ApplicationTag> applicationTags = new ArrayList<>();
    ApplicationTag applicationTag = new ApplicationTag(APPLICATION_ID, TAG_ID);
    applicationTag.setId(ID);
    applicationTags.add(applicationTag);

    List<ApiApplicationTagDTO> apiApplicationTagDTOs = ApiApplicationTagAdapter.convertToDTO(applicationTags);
    assertThat(apiApplicationTagDTOs).hasSize(1);
    assertThat(apiApplicationTagDTOs.get(0).id).isEqualTo(applicationTag.getId());
    assertThat(apiApplicationTagDTOs.get(0).applicationId).isEqualTo(applicationTag.getApplicationId());
    assertThat(apiApplicationTagDTOs.get(0).tagId).isEqualTo(applicationTag.getTagId());
  }

  @Test
  public void testConvertToDTO_nullList() {
    List<ApiApplicationTagDTO> apiApplicationTagDTOs = ApiApplicationTagAdapter.convertToDTO(null);
    assertThat(apiApplicationTagDTOs).isEmpty();
  }

  @Test
  public void testConvertFromDTO() {
    List<ApiApplicationTagDTO> apiApplicationTagDTOs = new ArrayList<>();
    ApiApplicationTagDTO apiApplicationTagDTO = new ApiApplicationTagDTO();
    apiApplicationTagDTO.id = ID;
    apiApplicationTagDTO.tagId = TAG_ID;
    apiApplicationTagDTOs.add(apiApplicationTagDTO);

    List<ApplicationTag> applicationTags = ApiApplicationTagAdapter.convertFromDTO(APPLICATION_ID,
        apiApplicationTagDTOs);
    assertThat(applicationTags).hasSize(1);
    assertThat(applicationTags.get(0).getId()).isEqualTo(apiApplicationTagDTO.id);
    assertThat(applicationTags.get(0).getApplicationId()).isEqualTo(APPLICATION_ID);
    assertThat(applicationTags.get(0).getTagId()).isEqualTo(apiApplicationTagDTO.tagId);
  }

  @Test
  public void testConvertFromDTO_nullList() {
    List<ApplicationTag> applicationTags = ApiApplicationTagAdapter.convertFromDTO(APPLICATION_ID, null);
    assertThat(applicationTags).isEmpty();
  }
}
