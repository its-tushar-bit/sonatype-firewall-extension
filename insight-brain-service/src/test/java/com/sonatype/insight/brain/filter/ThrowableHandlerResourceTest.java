/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.filter;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.organization.OrganizationResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.service.BaseUrl;

import com.google.inject.Binder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ThrowableHandlerResourceTest
    extends AbstractResourceTest
{
  @Override
  public void configure(final Binder binder) {
    super.configure(binder);
    binder.bind(BaseUrl.class).toInstance(mock(BaseUrl.class));
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
