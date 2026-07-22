/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GuideComponentArtifactDeserializationTest
{
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void deserializesMinimalFields() throws Exception {
    String json = """
        {
          "extension": "jar",
          "classifier": "sources",
          "publishedDate": "2021-02-26T20:41:06Z"
        }
        """;

    GuideComponentArtifact artifact = mapper.readValue(json, GuideComponentArtifact.class);

    assertThat(artifact.extension()).isEqualTo("jar");
    assertThat(artifact.classifier()).isEqualTo("sources");
  }

  @Test
  public void deserializesWithRefids() throws Exception {
    String json = """
        {
          "extension": "jar",
          "classifier": "",
          "publishedDate": "2021-02-26T20:40:52Z",
          "refids": [
            {
              "refid": "CVE-2025-48924",
              "severity": 6.9
            }
          ]
        }
        """;

    GuideComponentArtifact artifact = mapper.readValue(json, GuideComponentArtifact.class);

    assertThat(artifact.extension()).isEqualTo("jar");
    assertThat(artifact.classifier()).isEmpty();
    assertThat(artifact.refids()).hasSize(1);
  }

  @Test
  public void deserializesUnknownFields_ignored() throws Exception {
    // @JsonIgnoreProperties(ignoreUnknown = true) guard — should not throw.
    // HDS may return additional fields like isPrimary, maxCvss, isMalware, dts which we ignore.
    String json = """
        {
          "extension": "jar",
          "isPrimary": true,
          "maxCvss": 7.5,
          "isMalware": false,
          "dts": {
            "overall": 78
          },
          "someFutureField": "value"
        }
        """;

    GuideComponentArtifact artifact = mapper.readValue(json, GuideComponentArtifact.class);

    assertThat(artifact.extension()).isEqualTo("jar");
  }
}
