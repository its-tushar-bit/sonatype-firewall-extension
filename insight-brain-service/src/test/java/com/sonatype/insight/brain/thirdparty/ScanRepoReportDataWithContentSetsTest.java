/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScanRepoReportDataWithContentSetsTest
{
  private String loadResource(String name) throws Exception {
    return new String(
        Files.readAllBytes(Paths.get(getClass().getResource("/ContainerResultsHandlerTest/" + name).toURI())),
        StandardCharsets.UTF_8);
  }

  @Test
  public void deserializeRealNeuVectorPayload_bindsModulesAndVulnerabilities() throws Exception {
    String json = loadResource("alpine-3.6.json");

    ScanRepoReportDataWithContentSets data = new Gson().fromJson(json, ScanRepoReportDataWithContentSets.class);

    assertThat(data.getReport().getModules()[0].getName()).isEqualTo("alpine-baselayout");
    assertThat(data.getReport().getVulnerabilities()[0].getName()).isEqualTo("CVE-2021-30139");
  }
}
