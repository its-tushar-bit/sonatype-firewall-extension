/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository.hosted;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import com.sonatype.insight.brain.scan.datastore.ScanEntity;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScanXmlParserTest
{

  private static String xml(final String body) {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<scan version=\"2.24\">\n" + body + "\n</scan>";
  }

  private static ScanEntity plainXmlEntity(final String xmlContent) throws IOException {
    ScanEntity entity = mock(ScanEntity.class);
    byte[] bytes = xmlContent.getBytes(StandardCharsets.UTF_8);
    when(entity.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream(bytes));
    return entity;
  }

  private static ScanEntity gzipXmlEntity(final String xmlContent) throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    try (GZIPOutputStream gz = new GZIPOutputStream(buf)) {
      gz.write(xmlContent.getBytes(StandardCharsets.UTF_8));
    }
    byte[] gzipped = buf.toByteArray();
    ScanEntity entity = mock(ScanEntity.class);
    when(entity.getInputStream()).thenAnswer(inv -> new ByteArrayInputStream(gzipped));
    return entity;
  }

  @Test
  public void extractComponentInfo_plainXml_returnsCorrectInfo() throws IOException {
    String content = xml(
        "<repository id=\"repo-1\" name=\"maven-releases\" format=\"maven2\"/>\n" +
            "<dir path=\"com/example/foo-1.0.jar\" sha1=\"abc123def456ghi7\" sha512=\"ignored\">\n</dir>");

    ScanComponentInfo info = ScanXmlParser.extractComponentInfo(plainXmlEntity(content));

    assertThat(info).isNotNull();
    assertThat(info.pathname()).isEqualTo("com/example/foo-1.0.jar");
    assertThat(info.hash()).isEqualTo("abc123def456ghi7");
    assertThat(info.format()).isEqualTo("maven2");
  }

  @Test
  public void extractComponentInfo_gzipXml_returnsCorrectInfo() throws IOException {
    String content = xml(
        "<repository id=\"repo-1\" name=\"r\" format=\"npm\"/>\n" +
            "<dir path=\"@scope/pkg/1.0.0/pkg.tgz\" sha1=\"1234567890abcdef1234\" sha512=\"x\">\n</dir>");

    ScanComponentInfo info = ScanXmlParser.extractComponentInfo(gzipXmlEntity(content));

    assertThat(info).isNotNull();
    assertThat(info.pathname()).isEqualTo("@scope/pkg/1.0.0/pkg.tgz");
    assertThat(info.hash()).isEqualTo("1234567890abcdef1234");
    assertThat(info.format()).isEqualTo("npm");
  }

  @Test
  public void extractComponentInfo_repositoryBeforeDir_formatPopulated() throws IOException {
    String content = xml(
        "<repository id=\"r\" name=\"n\" format=\"maven2\"/>\n" +
            "<configuration/>\n" +
            "<dir path=\"a/b.jar\" sha1=\"hash001\" sha512=\"x\">\n</dir>");

    ScanComponentInfo info = ScanXmlParser.extractComponentInfo(plainXmlEntity(content));

    assertThat(info).isNotNull();
    assertThat(info.format()).isEqualTo("maven2");
    assertThat(info.hash()).isEqualTo("hash001");
  }

  @Test
  public void extractComponentInfo_dirBeforeRepository_formatIsNull() throws IOException {
    String content = xml(
        "<dir path=\"a/b.jar\" sha1=\"hash002\" sha512=\"x\">\n</dir>\n" +
            "<repository id=\"r\" name=\"n\" format=\"maven2\"/>");

    ScanComponentInfo info = ScanXmlParser.extractComponentInfo(plainXmlEntity(content));

    assertThat(info).isNotNull();
    assertThat(info.pathname()).isEqualTo("a/b.jar");
    assertThat(info.hash()).isEqualTo("hash002");
    assertThat(info.format()).isNull();
  }

  @Test
  public void extractComponentInfo_noDirElement_returnsNull() throws IOException {
    String content = xml("<repository id=\"r\" name=\"n\" format=\"maven2\"/>");

    ScanComponentInfo info = ScanXmlParser.extractComponentInfo(plainXmlEntity(content));

    assertThat(info).isNull();
  }

  @Test
  public void extractComponentInfo_dirMissingSha1_returnsNull() throws IOException {
    String content = xml(
        "<repository id=\"r\" name=\"n\" format=\"maven2\"/>\n" +
            "<dir path=\"a/b.jar\" sha512=\"x\">\n</dir>");

    ScanComponentInfo info = ScanXmlParser.extractComponentInfo(plainXmlEntity(content));

    assertThat(info).isNull();
  }

  @Test
  public void extractComponentInfo_dirMissingPath_returnsNull() throws IOException {
    String content = xml(
        "<repository id=\"r\" name=\"n\" format=\"maven2\"/>\n" +
            "<dir sha1=\"hash003\" sha512=\"x\">\n</dir>");

    ScanComponentInfo info = ScanXmlParser.extractComponentInfo(plainXmlEntity(content));

    assertThat(info).isNull();
  }

  @Test
  public void extractComponentInfo_inputStreamThrowsIOException_returnsNull() throws IOException {
    ScanEntity entity = mock(ScanEntity.class);
    when(entity.getInputStream()).thenThrow(new IOException("disk error"));

    ScanComponentInfo info = ScanXmlParser.extractComponentInfo(entity);

    assertThat(info).isNull();
  }

  @Test
  public void extractComponentInfo_malformedXml_returnsNull() throws IOException {
    String content = "this is not xml at all <<<>>>";

    ScanComponentInfo info = ScanXmlParser.extractComponentInfo(plainXmlEntity(content));

    assertThat(info).isNull();
  }

  @Test
  public void extractComponentInfo_multipleDirElements_stopsAtFirst() throws IOException {
    String content = xml(
        "<repository id=\"r\" name=\"n\" format=\"maven2\"/>\n" +
            "<dir path=\"first/a.jar\" sha1=\"hash_first\" sha512=\"x\">\n</dir>\n" +
            "<dir path=\"second/b.jar\" sha1=\"hash_second\" sha512=\"y\">\n</dir>");

    ScanComponentInfo info = ScanXmlParser.extractComponentInfo(plainXmlEntity(content));

    assertThat(info).isNotNull();
    assertThat(info.pathname()).isEqualTo("first/a.jar");
    assertThat(info.hash()).isEqualTo("hash_first");
  }

  @Test
  public void extractComponentInfo_emptyXml_returnsNull() throws IOException {
    String content = xml("");

    ScanComponentInfo info = ScanXmlParser.extractComponentInfo(plainXmlEntity(content));

    assertThat(info).isNull();
  }
}
