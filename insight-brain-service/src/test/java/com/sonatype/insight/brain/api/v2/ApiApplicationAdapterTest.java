/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.test.InjectedTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiApplicationAdapterTest
    extends InjectedTest
{
  @Inject
  private ApiApplicationAdapter apiApplicationAdapter;

  @Test
  public void testConvertToDTO_nullValue() {
    final ApiApplicationDTO apiApplicationDTO = apiApplicationAdapter.convertToDTO(null);
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

    ApiApplicationDTO apiApplicationDTO = apiApplicationAdapter.convertToDTO(application);
    assertThat(apiApplicationDTO).isNotNull();
    assertThat(apiApplicationDTO.id).isEqualTo(application.getId());
    assertThat(apiApplicationDTO.publicId).isEqualTo(application.getPublicId());
    assertThat(apiApplicationDTO.name).isEqualTo(application.getName());
    assertThat(apiApplicationDTO.organizationId).isEqualTo(application.getOrganizationId());
    assertThat(apiApplicationDTO.contactUserName).isEqualTo(application.getContactInternalName());
  }
}
