/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.nexus.scm.api.model.SCMRepository;

import org.apache.http.HttpStatus;
import org.junit.Test;

import static com.sonatype.insight.brain.api.experimental.ApiScmOnboardingResource.DEFAULT_HOST_URL;
import static com.sonatype.insight.brain.api.experimental.ApiScmOnboardingResource.LOAD_REPO_PATH;
import static com.sonatype.insight.brain.api.experimental.ApiScmOnboardingResource.RESOURCE_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiScmOnboardingResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testLoadRepositories() throws Exception {
    // when repositories are loaded
    HttpResponse response = restRequest().path(RESOURCE_PATH + "/" + LOAD_REPO_PATH).get();

    // then the response is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
    @SuppressWarnings("unchecked")
    List<SCMRepository> responseList = response.getBody(List.class);
    assertThat(responseList.size()).isEqualTo(13);
  }

  @Test
  public void testDefaultHostUrl() throws Exception {
    // when
    HttpResponse response = restRequest().path(RESOURCE_PATH + "/" + DEFAULT_HOST_URL)
        .query("provider", "github")
        .query("orgId", "no-org-here")
        .get();

    // then the response is OK
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SC_OK);
    Map<String, String> responseList = response.getBody(Map.class);
    assertThat(responseList.size()).isEqualTo(1);
    assertThat(responseList.get("defaultHostUrl")).isEqualTo("https://github.com/");
  }
}
