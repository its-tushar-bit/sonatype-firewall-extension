/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import com.sonatype.nexus.scm.SourceControlProvider;
import com.sonatype.nexus.scm.api.common.JsonUtils;

import org.sonatype.plexus.components.cipher.PlexusCipherException;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlDispatcherGithubTest
    extends BaseSourceControlDispatcherTest
{
  @Rule
  public final GitApiRule github = new GitApiRule(SourceControlProvider.GITHUB);

  private static final ImmutableMap<Object, Object> SUCCESS = ImmutableMap.builder()
      .put("url", "http://example/com")
      .put("creator",
          ImmutableMap.builder().put("login", "foo").build()
      ).build();

  private static final String API_URL = "/api/v3/repos/owner/repo/statuses/commitHash";

  @Override
  @Before
  public void setup() throws PlexusCipherException {
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    super.setup();
  }

  @Test
  public void testOnEvent() throws Exception {
    getGitApiClient().setResponseForUri(API_URL, JsonUtils.toJson(SUCCESS), 201);

    dispatcher.on(event);

    assertThat(getGitApiClient().verify(API_URL, 201)).isTrue();
  }

  /**
   * remote api will respond with 404 for misconfigured urls or if authentication is not adequate
   * https://developer.github.com/v3/troubleshooting/#why-am-i-getting-a-404-error-on-a-repository-that-exists
   */
  @Test
  public void testOnEvent_404() throws Exception {
    dispatcher.on(event);

    assertThat(getGitApiClient().verify(API_URL, 404)).isTrue();
  }

  @Override
  protected GitApiRule getGitApiClient() {
    return github;
  }
}
