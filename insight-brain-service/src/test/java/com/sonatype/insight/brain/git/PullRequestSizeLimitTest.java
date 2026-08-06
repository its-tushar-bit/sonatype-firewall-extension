/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PullRequestSizeLimitTest
{
  private static final String COMMENT_GITHUB_KEY = "insight.scm.pullRequest.maxCommentChars.github";

  private static final String DESCRIPTION_AZURE_KEY = "insight.scm.pullRequest.maxDescriptionChars.azure";

  @Test
  public void testMaxCommentChars_defaults() {
    assertThat(PullRequestSizeLimit.maxCommentChars(SourceControlProvider.GITHUB)).isEqualTo(65536);
    assertThat(PullRequestSizeLimit.maxCommentChars(SourceControlProvider.BITBUCKET)).isEqualTo(32768);
    assertThat(PullRequestSizeLimit.maxCommentChars(SourceControlProvider.AZURE)).isEqualTo(150000);
    assertThat(PullRequestSizeLimit.maxCommentChars(SourceControlProvider.GITLAB)).isEqualTo(1000000);
  }

  @Test
  public void testMaxDescriptionChars_defaults() {
    assertThat(PullRequestSizeLimit.maxDescriptionChars(SourceControlProvider.GITHUB)).isEqualTo(65536);
    assertThat(PullRequestSizeLimit.maxDescriptionChars(SourceControlProvider.BITBUCKET)).isEqualTo(32768);
    assertThat(PullRequestSizeLimit.maxDescriptionChars(SourceControlProvider.AZURE)).isEqualTo(4000);
    assertThat(PullRequestSizeLimit.maxDescriptionChars(SourceControlProvider.GITLAB)).isEqualTo(1000000);
  }

  @Test
  public void testMaxCommentChars_systemPropertyOverride() {
    System.setProperty(COMMENT_GITHUB_KEY, "1234");

    try {
      assertThat(PullRequestSizeLimit.maxCommentChars(SourceControlProvider.GITHUB)).isEqualTo(1234);
    }
    finally {
      System.clearProperty(COMMENT_GITHUB_KEY);
    }
  }

  @Test
  public void testMaxDescriptionChars_systemPropertyOverride() {
    System.setProperty(DESCRIPTION_AZURE_KEY, "9000");

    try {
      assertThat(PullRequestSizeLimit.maxDescriptionChars(SourceControlProvider.AZURE)).isEqualTo(9000);
    }
    finally {
      System.clearProperty(DESCRIPTION_AZURE_KEY);
    }
  }

  @Test
  public void testMaxCommentChars_nonPositiveOverride_fallsBackToDefault() {
    System.setProperty(COMMENT_GITHUB_KEY, "0");

    try {
      assertThat(PullRequestSizeLimit.maxCommentChars(SourceControlProvider.GITHUB)).isEqualTo(65536);
    }
    finally {
      System.clearProperty(COMMENT_GITHUB_KEY);
    }
  }

  @Test
  public void testTruncate_underLimit_returnsUnchanged() {
    String body = "line one\nline two";

    String result = PullRequestSizeLimit.truncate(body, 65536, "\nFOOTER");

    assertThat(result).isEqualTo(body);
  }

  @Test
  public void testTruncate_overLimit_cutsAtNewlineBoundary() {
    String body = "aaaaaaaa\nbbbbbbbb\ncccc";
    String footer = "\nX";

    String result = PullRequestSizeLimit.truncate(body, 15, footer);

    assertThat(result).isEqualTo("aaaaaaaa" + footer);
  }

  @Test
  public void testTruncate_overLimit_noNewlineInRange_cutsAtRoom() {
    String body = "aaaaaaaaaaaaaaaaaaaa";
    String footer = "\nXY";

    String result = PullRequestSizeLimit.truncate(body, 12, footer);

    assertThat(result.length()).isLessThanOrEqualTo(12);
    assertThat(result).endsWith(footer);
    assertThat(result).startsWith("aaaaaaaaa");
  }

  @Test
  public void testTruncate_nonPositiveMax_returnsEmpty() {
    assertThat(PullRequestSizeLimit.truncate("hello world", 0, "\nF")).isEmpty();
    assertThat(PullRequestSizeLimit.truncate("hello world", -5, "\nF")).isEmpty();
  }

  @Test
  public void testTruncate_doesNotSplitSurrogatePair() {
    String body = "abc😀def";
    String footer = "\nZ";

    String result = PullRequestSizeLimit.truncate(body, 6, footer);

    assertThat(result).isEqualTo("abc" + footer);
  }
}
