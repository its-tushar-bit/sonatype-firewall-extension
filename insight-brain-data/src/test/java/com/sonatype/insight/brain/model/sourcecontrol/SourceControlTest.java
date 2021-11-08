/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

import org.junit.Test;

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
  }

  @Test
  public void testConvertUrlIfNeeded_SshUrlsFormatOne() {
    String givenUrl = "ssh://git@server/owner/repo.git"; // user provided
    String expectedUrl = "https://server/owner/repo";
    String convertedUrl = convertUrlIfNeeded(givenUrl);
    assertThat(convertedUrl).isEqualTo(expectedUrl);

    givenUrl = "ssh://server/owner/repo.git"; // no user provided
    convertedUrl = convertUrlIfNeeded(givenUrl);
    assertThat(convertedUrl).isEqualTo(expectedUrl);
  }

  @Test
  public void testConvertUrlIfNeeded_SshUrlsFormatTwo() {
    String givenUrl = "git@server:owner/repo.git"; // user provided
    String expectedUrl = "https://server/owner/repo";
    String convertedUrl = convertUrlIfNeeded(givenUrl);
    assertThat(convertedUrl).isEqualTo(expectedUrl);
  }

  @Test
  public void testConvertUrlIfNeeded_embeddedCredentials() {
    String givenUrl = "git@server:owner/repo.git"; // user provided
    String expectedUrl = "https://server/owner/repo";
    String convertedUrl = convertUrlIfNeeded(givenUrl);
    assertThat(convertedUrl).isEqualTo(expectedUrl);
  }
}
