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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ApiComponentLabelResourceV2AuthzTest
    extends AbstractResourceAuthzTest
{
  @Test
  public void testSetApplicationComponentLabel_Unauthenticated() throws Exception {
    HttpResponse response = restRequest().path(PublicApiPaths.APP_COMPONENT_LABELS_PATH_V2)
        .parameter("bababababa", "label", app.getId()).anon().post();
    assertThat(response.getStatusCode(), is(401));
  }

  @Test
  public void testDeleteApplicationComponentLabel_Unauthenticated() throws Exception {
    HttpResponse response = restRequest().path(PublicApiPaths.APP_COMPONENT_LABELS_PATH_V2)
        .parameter("bababababa", "label", app.getId()).anon().delete();
    assertThat(response.getStatusCode(), is(401));
  }
}
