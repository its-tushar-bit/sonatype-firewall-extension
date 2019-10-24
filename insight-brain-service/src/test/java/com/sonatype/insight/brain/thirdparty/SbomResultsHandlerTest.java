/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.cyclonedx.BomParser;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.junit.Test;
import org.mockito.Spy;

import static org.assertj.core.api.Assertions.assertThat;

public class SbomResultsHandlerTest
    extends AbstractComponentTest
{
  @Spy
  private SbomResultHandler sbomResultHandlerSpy;

  @Test
  public void testHandle_filterContent_newThirdPartyFileMultipleEntries() throws Exception {
    String sbomContent = getSbomFile("sbom-multiple-components.xml");
    ThirdPartyScanContent content =
        new ThirdPartyScanContent(null, null, null, null, sbomContent);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandlerSpy.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNotNull();
    assertFilteredSbomFile(filteredContent, 2);
  }

  @Test
  public void testHandle_nullContent() throws Exception {
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, null);
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandlerSpy.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isNull();
  }

  @Test
  public void testHandle_emptyContent() throws Exception {
    ThirdPartyScanContent content = new ThirdPartyScanContent(null, null, null, null, "");
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    String filteredContent = sbomResultHandlerSpy.handleAndFilterContents(content, thirdPartyFile);
    assertThat(filteredContent).isBlank();
  }

  private String getSbomFile(final String fileName) throws Exception {
    URL resource = getClass().getResource("/ThirdPartyResultsProcessorTest/sbom/" + fileName);
    return new String(Files.readAllBytes(Paths.get(resource.toURI())));
  }

  private void assertFilteredSbomFile(String content, int expectedComponentCount) throws Exception {

    Bom bom = getBom(content);
    assertThat(bom).isNotNull();
    assertThat(bom.getComponents()).hasSize(expectedComponentCount);

    for (Component component : bom.getComponents()) {
      assertThat(component.getComponents()).isNull();
      assertThat(component.getName()).isNotNull();
      assertThat(component.getVersion()).isNotNull();
      assertThat(component.getType()).isNotNull();
    }
  }

  private Bom getBom(String content) throws ParseException, IOException {
    BomParser parser = new BomParser();
    return parser.parse(new StringReader(content));
  }
}
