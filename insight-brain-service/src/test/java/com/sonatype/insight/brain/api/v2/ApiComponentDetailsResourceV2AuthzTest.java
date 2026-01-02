/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.service.AbstractResourceAuthzTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import com.sonatype.insight.brain.common.test.SlowTest;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ApiComponentDetailsResourceV2AuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testEvaluateComponents_Unauthenticated() throws Exception {
    HttpResponse response = restRequest().path(PublicApiPaths.COMPONENT_DETAILS_PATH_V2).anon().post();
    assertThat(response.getStatusCode()).isEqualTo(401);
  }
}
