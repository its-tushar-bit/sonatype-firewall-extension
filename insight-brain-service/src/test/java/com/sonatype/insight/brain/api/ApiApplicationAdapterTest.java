/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

import com.sonatype.insight.brain.api.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.model.Application;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class ApiApplicationAdapterTest
{
  private ApiApplicationAdapter apiApplicationAdapter = new ApiApplicationAdapter();


  @Test
  public void testConvertToApplicationDTO_nullValue() {
    final ApiApplicationDTO apiApplicationDTO = apiApplicationAdapter.convertToDTO(null);
    assertThat(apiApplicationDTO, nullValue());
  }

  @Test
  public void testConvertToApplicationDTO() {
    final Application application = new Application();
    application.setId("appId");
    application.setPublicId("publicId");
    application.setName("appName");
    application.setOrganizationId("orgId");
    application.setContactInternalName("appContactUserName");

    ApiApplicationDTO apiApplicationDTO = apiApplicationAdapter.convertToDTO(application);
    assertThat(apiApplicationDTO, notNullValue());
    assertThat(apiApplicationDTO.id, is(application.getId()));
    assertThat(apiApplicationDTO.publicId, is(application.getPublicId()));
    assertThat(apiApplicationDTO.name, is(application.getName()));
    assertThat(apiApplicationDTO.organizationId, is(application.getOrganizationId()));
    assertThat(apiApplicationDTO.contactUserName, is(application.getContactInternalName()));
  }
}
