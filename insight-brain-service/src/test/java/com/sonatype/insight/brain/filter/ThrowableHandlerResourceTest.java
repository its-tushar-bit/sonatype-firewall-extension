/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.organization.OrganizationResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.Configuration;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@Category(SlowTest.class)
public class ThrowableHandlerResourceTest
    extends AbstractResourceTest
{
  @Override
  protected List<Class<?>> getTestConfigurationClasses() {
    List<Class<?>> configs = new ArrayList<>(super.getTestConfigurationClasses());
    configs.add(ThrowableHandlerResourceTestConfiguration.class);
    return configs;
  }

  @TestConfiguration
  static class ThrowableHandlerResourceTestConfiguration
  {
    @Bean
    @Primary
    public BaseUrl baseUrl(final Configuration configuration) {
      return spy(new BaseUrl(configuration));
    }
  }

  @Test
  public void testErrorFilter_HandlesException() throws Exception {
    doThrow(new RuntimeException("some exception")).when(getCLMServer().getInstance(BaseUrl.class)).capture(any());

    HttpResponse httpResponse = restRequest().path(OrganizationResource.RESOURCE_PATH).get();

    assertResponseStatus(500, httpResponse);
    assertThat(httpResponse.getBodyText()).matches("Internal Server Error \\(ID [a-zA-Z0-9]+\\)");
  }

  @Test
  public void testErrorFilter_NoException() throws Exception {
    doNothing().when(getCLMServer().getInstance(BaseUrl.class)).capture(any());

    HttpResponse httpResponse = restRequest().path(OrganizationResource.RESOURCE_PATH).get();

    assertResponseStatus(200, httpResponse);
    Organization[] organizations = httpResponse.getBody(Organization[].class);
    assertThat(organizations).isNotEmpty();
  }
}
