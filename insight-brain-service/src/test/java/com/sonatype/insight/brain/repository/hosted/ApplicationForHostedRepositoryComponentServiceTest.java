/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationForHostedRepositoryComponentServiceTest
{
  @Test
  public void generatePublicId_standardPath() {
    String result = ApplicationForHostedRepositoryComponentService.generatePublicId(
        "maven-releases", "com/example/artifact/1.0/artifact-1.0.jar");
    assertThat(result).isEqualTo("maven-releases_com_example_artifact_1.0_artifact-1.0.jar");
  }

  @Test
  public void generatePublicId_nullPathname_usesUnknown() {
    String result = ApplicationForHostedRepositoryComponentService.generatePublicId("maven-releases", null);
    assertThat(result).isEqualTo("maven-releases_unknown");
    assertThat(result).doesNotContain("/").doesNotContain("null");
  }

  @Test
  public void generatePublicId_nullRepositoryPublicId_usesRepoPrefix() {
    String result = ApplicationForHostedRepositoryComponentService.generatePublicId(null, "org/lib/1.0/lib-1.0.jar");
    assertThat(result).startsWith("repo_");
    assertThat(result).contains("org_lib_1.0_lib-1.0.jar");
  }

  @Test
  public void generatePublicId_bothNull_returnsDefault() {
    String result = ApplicationForHostedRepositoryComponentService.generatePublicId(null, null);
    assertThat(result).isEqualTo("repo_unknown");
  }

  @Test
  public void generatePublicId_specialCharsAreSanitized() {
    String result = ApplicationForHostedRepositoryComponentService.generatePublicId(
        "my repo@v2", "com/example:lib/1.0 beta/lib-1.0.jar");
    assertThat(result).doesNotContain(" ").doesNotContain("@").doesNotContain(":");
    assertThat(result).matches("[a-zA-Z0-9\\-._]+");
  }

  @Test
  public void generatePublicId_longString_truncatedTo200() {
    String longRepo = "r".repeat(200);
    String longPath = "p".repeat(200);
    String result = ApplicationForHostedRepositoryComponentService.generatePublicId(longRepo, longPath);
    assertThat(result).hasSizeLessThanOrEqualTo(200);
  }

  @Test
  public void generatePublicId_exactly200_notTruncated() {
    String repo = "a".repeat(5);
    String path = "b".repeat(194);
    String result = ApplicationForHostedRepositoryComponentService.generatePublicId(repo, path);
    assertThat(result.length()).isLessThanOrEqualTo(200);
  }

  @Test
  public void generatePublicId_slashesInPathnameBecomeUnderscores() {
    String result = ApplicationForHostedRepositoryComponentService.generatePublicId(
        "releases", "a/b/c/d.jar");
    assertThat(result).isEqualTo("releases_a_b_c_d.jar");
  }
}
