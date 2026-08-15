/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.search.AdvancedSearchStatusDTO;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.search.AdvancedSearchResource.RESOURCE_PATH;
import static com.sonatype.insight.brain.search.AdvancedSearchResource.STATUS_PATH;
import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2AdvancedSearchResourceTest
{
  private IqTestContext ctx;

  private SystemConfigurationPropertyDAO dao;

  @BeforeEach
  void setUp() {
    dao = ctx.lookup(SystemConfigurationPropertyDAO.class);
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().path(RESOURCE_PATH);
  }

  @Test
  void testGetStatus() throws Exception {
    FileUtils.deleteDirectory(ctx.lookup(InsightWork.class).getSearchIndexDir());

    HttpResponse response = restRequest().path(STATUS_PATH).get();

    ctx.assertResponseStatus(200, response);
    AdvancedSearchStatusDTO statusDTO = response.getBody(AdvancedSearchStatusDTO.class);
    assertThat(statusDTO.isEnabled).isFalse();
    assertThat(statusDTO.lastIndexTime).isNull();
  }

  @Test
  void testSetStatus() throws Exception {
    AdvancedSearchStatusDTO statusDTO = new AdvancedSearchStatusDTO();
    statusDTO.isEnabled = true;

    HttpResponse response = restRequest().path(STATUS_PATH).body(statusDTO).put();

    ctx.assertResponseStatus(204, response);
    assertThat(dao.getByName(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED).getValue()).isEqualTo("true");
  }
}
