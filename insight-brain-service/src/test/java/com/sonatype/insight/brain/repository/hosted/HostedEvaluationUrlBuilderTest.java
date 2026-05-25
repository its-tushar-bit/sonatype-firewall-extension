/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.BaseUrl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HostedEvaluationUrlBuilderTest
{
  @Mock
  private BaseUrl baseUrl;

  private HostedEvaluationUrlBuilder builder;

  @Before
  public void setUp() {
    builder = new HostedEvaluationUrlBuilder(baseUrl);
  }

  @Test
  public void build_producesExpectedUrlFormat() {
    when(baseUrl.get()).thenReturn("https://iq.example.com/");
    Repository repo = mockRepo("repo-abc123", "maven-releases");

    String url = builder.build(repo);

    assertThat(url)
        .isEqualTo(
            "https://iq.example.com/assets/index.html#/hostedRepos/repo-abc123/components?repositoryPublicId=maven-releases");
  }

  @Test
  public void build_urlIsEnvSpecific_neverHardcodesLocalhost() {
    when(baseUrl.get()).thenReturn("https://production-iq.sonatype.com/");
    Repository repo = mockRepo("r1", "releases");

    String url = builder.build(repo);

    assertThat(url).startsWith("https://production-iq.sonatype.com/");
    assertThat(url).doesNotContain("localhost");
  }

  @Test
  public void build_nullRepository_throws() {
    // Null-check fires before any BaseUrl lookup; no stubbing needed.
    assertThatThrownBy(() -> builder.build(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void build_publicIdWithUrlUnsafeCharacters_isUrlEncoded() {
    // CLM-39870 PR-2 review fix: repository.public_id has no charset constraint, so values
    // with spaces, '+', '&', '%' must be URL-encoded into the query parameter or the audit
    // record stores a malformed URL. Repository ID is UUID-style so it is left unencoded.
    when(baseUrl.get()).thenReturn("https://iq.example.com/");
    Repository repo = mockRepo("repo-1", "my repo+special&name");

    String url = builder.build(repo);

    assertThat(url).isEqualTo(
        "https://iq.example.com/assets/index.html#/hostedRepos/repo-1/components?repositoryPublicId=my+repo%2Bspecial%26name");
  }

  @Test
  public void build_baseUrlNotConfigured_propagatesIllegalStateException() {
    when(baseUrl.get()).thenThrow(new IllegalStateException(BaseUrl.ERR_MSG_BASE_URL_NOT_CONFIGURED));
    Repository repo = mockRepo("r1", "releases");

    assertThatThrownBy(() -> builder.build(repo))
        .isInstanceOf(IllegalStateException.class);
  }

  private static Repository mockRepo(final String id, final String publicId) {
    Repository repo = new Repository();
    repo.setId(id);
    repo.setPublicId(publicId);
    return repo;
  }
}
