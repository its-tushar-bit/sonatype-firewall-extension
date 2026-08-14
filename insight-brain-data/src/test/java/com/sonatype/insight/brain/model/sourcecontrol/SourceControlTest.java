/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.model.sourcecontrol.SourceControl.convertUrlIfNeeded;
import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlTest
{
  @Test
  public void testConvertUrlIfNeeded_HttpUrls() {
    String repositoryUrl = "http://server/owner/repo";
    String convertedUrl = convertUrlIfNeeded(repositoryUrl);
    assertThat(convertedUrl).isEqualTo(repositoryUrl);

    repositoryUrl = "https://server/owner/repo";
    convertedUrl = convertUrlIfNeeded(repositoryUrl);
    assertThat(convertedUrl).isEqualTo(repositoryUrl);

    // strip out the "user@" for https
    repositoryUrl = "https://user@server/owner/repo";
    assertThat(convertUrlIfNeeded(repositoryUrl)).isEqualTo("https://server/owner/repo");

    // strip out the user for http
    repositoryUrl = "http://default:default@server:9090/owner/repo.git";
    assertThat(convertUrlIfNeeded(repositoryUrl)).isEqualTo("http://server:9090/owner/repo");
  }

  @Test
  public void testConvertUrlIfNeeded_SshNoConversion() {
    String givenUrl = "git@server:owner/repo.git";
    assertThat(convertUrlIfNeeded(givenUrl)).isEqualTo(givenUrl);

    givenUrl = "ssh://server/owner/repo.git";
    assertThat(convertUrlIfNeeded(givenUrl)).isEqualTo(givenUrl);
  }
}
