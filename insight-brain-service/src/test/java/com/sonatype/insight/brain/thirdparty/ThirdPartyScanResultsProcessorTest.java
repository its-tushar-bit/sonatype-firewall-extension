/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.scan.file.clair.ClairScannerResult;
import com.sonatype.insight.scan.file.clair.ClairScannerVulnerability;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.test.LogOutput;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartyScanResultsProcessorTest
    extends AbstractComponentTest
{
  private final String loggerName = ThirdPartyScanResultsProcessor.class.getName();

  @Inject
  private ThirdPartyScanResultsProcessor thirdPartyScanResultsProcessor;

  @Rule
  public LogOutput logOutput = new LogOutput(loggerName);

  private Gson gson;

  @Before
  public void before() {
    gson = new Gson();
  }

  @Test
  public void testGetThirdPartyScanContents_ClairScanner() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-data.xml.gz");
    List<ThirdPartyScanContent> thirdPartyScanContents =
        thirdPartyScanResultsProcessor.getThirdPartyScanContents(scanFile);

    assertThat(thirdPartyScanContents).hasSize(1);

    ThirdPartyScanContent scanContent = thirdPartyScanContents.get(0);
    assertThat(scanContent.getItemContentType()).isEqualByComparingTo(ItemContentType.CLAIR_SCANNER);
    assertThat(scanContent.getPath()).isEqualTo("clair-scanner-out/clair-scanner-output.json");
    assertThat(scanContent.getHash()).isEqualTo("30a7c753d9515c185d85");
    assertThat(scanContent.getLastModified()).isEqualTo("1565180731371");

    ClairScannerResult result = gson.fromJson(scanContent.getContent(), ClairScannerResult.class);
    Set<ClairScannerVulnerability> vulnerabilities = result.getVulnerabilities();
    assertThat(vulnerabilities).hasSize(3);
    assertThat(result.getImage()).isEqualTo("smart-brain-boost-api-dockerized_postgres");

    Set<String> componentNameAndVersions =
        collectElements(vulnerabilities, sv -> sv.getFeatureName() + "-" + sv.getFeatureVersion());
    assertThat(componentNameAndVersions)
        .containsExactlyInAnyOrder("glibc-2.24-11+deb9u3", "libxslt-1.1.29-2.1", "apt-1.4.8");
    Set<String> cves = collectElements(vulnerabilities, ClairScannerVulnerability::getVulnerability);
    assertThat(cves).containsExactlyInAnyOrder("CVE-2019-3462", "CVE-2017-16997", "CVE-2019-13118");
  }

  @Test
  public void testGetThirdPartyScanContents_NoThirdPartyContent() throws Exception {
    File scanFile = getScanFile("scan-without-thirdparty-content.xml.gz");
    List<ThirdPartyScanContent> thirdPartyScanContents =
        thirdPartyScanResultsProcessor.getThirdPartyScanContents(scanFile);

    assertLogOutput(thirdPartyScanContents,
        "scan file scan-without-thirdparty-content.xml.gz contained a third party scan" +
            " item CLAIR_SCANNER without any content");
  }

  @Test
  public void testGetThirdPartyScanContents_InvalidFile() throws Exception {
    File scanFile = getScanFile("empty.xml.gz");
    List<ThirdPartyScanContent> thirdPartyScanContents =
        thirdPartyScanResultsProcessor.getThirdPartyScanContents(scanFile);

    assertLogOutput(thirdPartyScanContents, "error reading third party scan content from scan file empty.xml.gz");
  }

  private Set<String> collectElements(
      final Set<ClairScannerVulnerability> vulnerabilities,
      final Function<ClairScannerVulnerability, String> collector)
  {
    return vulnerabilities.stream().map(collector).collect(Collectors.toSet());
  }

  private void assertLogOutput(final List<ThirdPartyScanContent> thirdPartyScanContents, final String s) {
    assertThat(thirdPartyScanContents).hasSize(0);
    assertThat(logOutput.getErrorMessages(loggerName)).containsOnly(s);
  }

  private File getScanFile(final String fileName) throws URISyntaxException {
    URL resource = getClass().getResource("/ThirdPartyResultsProcessorTest/" + fileName);
    return new File(resource.toURI());
  }
}
