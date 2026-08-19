/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Collections;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiApplicationTagDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.tag.ApplicationTag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiApplicationAdapterTest
{
  @Test
  public void testConvertToDTO_nullValue() {
    final ApiApplicationDTO apiApplicationDTO = ApiApplicationAdapter.convertToDTO(null, Collections.emptyList());
    assertThat(apiApplicationDTO).isNull();
  }

  @Test
  public void testConvertToDTO() {
    final Application application = new Application();
    application.setId("appId");
    application.setPublicId("publicId");
    application.setName("appName");
    application.setOrganizationId("orgId");
    application.setContactInternalName("appContactUserName");

    ApiApplicationDTO apiApplicationDTO = ApiApplicationAdapter.convertToDTO(application, Collections.emptyList());
    assertThat(apiApplicationDTO).isNotNull();
    assertThat(apiApplicationDTO.id).isEqualTo(application.getId());
    assertThat(apiApplicationDTO.publicId).isEqualTo(application.getPublicId());
    assertThat(apiApplicationDTO.name).isEqualTo(application.getName());
    assertThat(apiApplicationDTO.organizationId).isEqualTo(application.getOrganizationId());
    assertThat(apiApplicationDTO.contactUserName).isEqualTo(application.getContactInternalName());
    assertThat(apiApplicationDTO.applicationTags).isEmpty();
  }

  @Test
  public void testConvertToDTO_WithApplicationTags() {
    final Application application = new Application();
    application.setId("appId");
    application.setPublicId("publicId");
    application.setName("appName");
    application.setOrganizationId("orgId");
    application.setContactInternalName("appContactUserName");

    ApplicationTag appTag = new ApplicationTag(application.getId(), "tagId");
    appTag.setId("appTagId");

    ApiApplicationDTO apiApplicationDTO =
        ApiApplicationAdapter.convertToDTO(application, Collections.singletonList(appTag));
    assertThat(apiApplicationDTO).isNotNull();
    assertThat(apiApplicationDTO.id).isEqualTo(application.getId());
    assertThat(apiApplicationDTO.publicId).isEqualTo(application.getPublicId());
    assertThat(apiApplicationDTO.name).isEqualTo(application.getName());
    assertThat(apiApplicationDTO.organizationId).isEqualTo(application.getOrganizationId());
    assertThat(apiApplicationDTO.contactUserName).isEqualTo(application.getContactInternalName());

    ApiApplicationTagDTO expectedTagDTO = new ApiApplicationTagDTO();
    expectedTagDTO.id = appTag.getId();
    expectedTagDTO.applicationId = appTag.getApplicationId();
    expectedTagDTO.tagId = appTag.getTagId();
    assertThat(apiApplicationDTO.applicationTags).usingRecursiveFieldByFieldElementComparator()
        .containsExactly(expectedTagDTO);
  }
}
