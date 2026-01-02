/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class ActionTypeResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetActionTypes() throws Exception {
    final HttpResponse response = restRequest().path(ActionTypeResource.RESOURCE_PATH).get();
    assertResponseStatus(200, response);
    final Object[] actionTypes = response.getBody(Object[].class);
    assertThat(actionTypes).isNotEmpty();
  }
}
