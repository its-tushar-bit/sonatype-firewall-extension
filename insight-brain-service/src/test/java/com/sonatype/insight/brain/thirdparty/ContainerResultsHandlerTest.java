/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.scan.model.ItemContentType;

import com.google.gson.GsonBuilder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ContainerResultsHandlerTest
    extends AbstractComponentTest
{
  @Inject
  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  @Inject
  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  @Test
  public void testHandleAndFilterContents() throws Exception {
    URL resource = getClass().getResource("/ContainerResultsHandlerTest/alpine-3.6.json");
    String json = new String(Files.readAllBytes(Paths.get(resource.toURI())));

    ThirdPartyScanContent content =
        new ThirdPartyScanContent("container://alpine:3.6", ItemContentType.CONTAINER_URI, null, null, json);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    ContainerResultHandler containerResultHandler = new ContainerResultHandler();
    String filteredContent = containerResultHandler.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();

    List<ThirdPartyFileCoordinate> coordinates =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(coordinates).hasSize(9);

    for (ThirdPartyFileCoordinate coord : coordinates) {
      assertThat(coord.getSource()).isEqualTo("Sonatype-Container");
      assertThat(coord.getFormat()).isEqualTo("container");
    }

    List<ThirdPartyCoordinateSecurity> coordinateSecurityList = thirdPartyCoordinateSecurityDAO.getAll();
    assertThat(coordinateSecurityList).hasSize(3);

    System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(coordinateSecurityList));

    ThirdPartyCoordinateSecurity coordinateSecurity0 = coordinateSecurityList.get(0);
    assertThat(coordinateSecurity0.getRefId()).isEqualTo("CVE-2020-28928");
    assertThat(coordinateSecurity0.getDescription()).startsWith("In musl libc through 1.2.1");
    assertThat(coordinateSecurity0.getLink())
        .isEqualTo("https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2020-28928");
    assertThat(coordinateSecurity0.getSeverity()).isEqualTo(5.5f);
    assertThat(coordinateSecurity0.getVulnerabilitySource()).isEqualTo("Sonatype");
    assertThat(coordinateSecurity0.getSeverityDescription()).isEqualTo("Medium");
    assertThat(coordinateSecurity0.getAttackVector()).isEqualTo("CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:N/I:N/A:H");
    assertThat(coordinateSecurity0.getRecommendations()).isEqualTo("1.2.2_pre2-r0");

    ThirdPartyCoordinateSecurity coordinateSecurity1 = coordinateSecurityList.get(1);
    assertThat(coordinateSecurity1.getRefId()).isEqualTo("CVE-2018-1000500");
    assertThat(coordinateSecurity1.getDescription()).startsWith("Busybox contains a Missing SSL certificate");
    assertThat(coordinateSecurity1.getLink())
        .isEqualTo("https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-1000500");
    assertThat(coordinateSecurity1.getSeverity()).isEqualTo(8.1f);
    assertThat(coordinateSecurity1.getVulnerabilitySource()).isEqualTo("Sonatype");
    assertThat(coordinateSecurity1.getSeverityDescription()).isEqualTo("High");
    assertThat(coordinateSecurity1.getAttackVector()).isEqualTo("CVSS:3.1/AV:N/AC:H/PR:N/UI:N/S:U/C:H/I:H/A:H");
    assertThat(coordinateSecurity1.getRecommendations()).isEqualTo("1.28.3-r2");

    ThirdPartyCoordinateSecurity coordinateSecurity2 = coordinateSecurityList.get(2);
    assertThat(coordinateSecurity2.getRefId()).isEqualTo("CVE-2021-30139");
    assertThat(coordinateSecurity2.getDescription()).startsWith("In Alpine Linux apk-tools before 2.12.5");
    assertThat(coordinateSecurity2.getLink())
        .isEqualTo("https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-30139");
    assertThat(coordinateSecurity2.getSeverity()).isEqualTo(7.5f);
    assertThat(coordinateSecurity2.getVulnerabilitySource()).isEqualTo("Sonatype");
    assertThat(coordinateSecurity2.getSeverityDescription()).isEqualTo("High");
    assertThat(coordinateSecurity2.getAttackVector()).isEqualTo("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H");
    assertThat(coordinateSecurity2.getRecommendations()).isEqualTo("2.12.5-r0");
  }
}
