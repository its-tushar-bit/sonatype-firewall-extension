/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.search.AdvancedSearchResource.RESOURCE_PATH;
import static com.sonatype.insight.brain.search.AdvancedSearchResource.STATUS_PATH;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

@Category(SlowTest.class)
public class AdvancedSearchResourceTest
    extends AbstractResourceTest
{
  private SystemConfigurationPropertyDAO dao;

  @Before
  public void setUp() {
    dao = lookup(SystemConfigurationPropertyDAO.class);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(RESOURCE_PATH);
  }

  @Test
  public void testGetStatus() throws Exception {
    FileUtils.deleteDirectory(getCLMServer().getInstance(InsightWork.class).getSearchIndexDir());

    HttpResponse response = restRequest().path(STATUS_PATH).get();

    assertResponseStatus(200, response);
    AdvancedSearchStatusDTO statusDTO = response.getBody(AdvancedSearchStatusDTO.class);
    assertThat(statusDTO.isEnabled).isFalse();
    assertThat(statusDTO.lastIndexTime).isNull();
  }

  @Test
  public void testSetStatus() throws Exception {
    AdvancedSearchStatusDTO statusDTO = new AdvancedSearchStatusDTO();
    statusDTO.isEnabled = true;

    HttpResponse response = restRequest().path(STATUS_PATH).body(statusDTO).put();

    assertResponseStatus(204, response);
    assertThat(dao.getByName(SystemConfigurationProperty.ADVANCED_SEARCH_ENABLED).getValue()).isEqualTo("true");
  }
}
