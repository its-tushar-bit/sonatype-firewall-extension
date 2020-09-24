/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.Map;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.Test;

import static com.sonatype.insight.brain.api.experimental.ScmOnboardingConfigResource.RESOURCE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class ScmOnboardingConfigResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testGetConfig() throws Exception {
    // when GETing config
    HttpResponse response = restRequest().path(RESOURCE_PATH).get();

    // then result is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK_200);

    // and value is present
    @SuppressWarnings("unchecked")
    Map<String, Object> responseMap = response.getBody(Map.class);
    assertThat(responseMap).containsEntry("manifestScanFeatureEnabled", false);
  }
}
