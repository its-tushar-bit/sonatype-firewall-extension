/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiThirdPartyScanServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ApiThirdPartyScanService thirdPartyScanService;

  private Application app;

  @Before
  public void before() throws Exception {
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testScanComponents()
      throws Exception
  {
    String sbom = getSbomFile("/ApiThirdPartyResourceTest/valid_sbom.xml");

    ApiThirdPartyScanTicketDTO scanResult =
        thirdPartyScanService.scanComponents(app.getId(), "clair", "build", sbom);
    assertThat(scanResult).isNotNull();
    assertThat(scanResult.statusUrl).isNotNull();
    assertThat(new URI(scanResult.statusUrl)).isNotNull();
  }

  private String getSbomFile(String path) throws Exception {
    byte[] bytes = Files.readAllBytes(Paths.get(getClass().getResource(path).toURI()));
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
