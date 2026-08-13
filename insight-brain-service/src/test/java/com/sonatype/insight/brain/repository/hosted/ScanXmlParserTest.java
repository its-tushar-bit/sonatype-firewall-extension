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
import java.util.List;
import java.util.zip.GZIPOutputStream;

import com.sonatype.insight.brain.scan.datastore.ScanEntity;

import org.junit.jupiter.api.Test;

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
  public void extractComponentInfo_multipleDirElements_returnsFirstViaDeprecatedSingularApi() throws IOException {
    // The deprecated singular API returns the outer (first) <dir> for backward compat with the
    // sync-enforcement path that still treats archive-of-archives uploads as one binary.
    // The plural API extractComponentInfos walks all <dir> elements — see the dedicated tests below.
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

  // ---- extractComponentInfos (plural) — archive-of-archives fan-out ----

  @Test
  public void extractComponentInfos_singleDir_returnsListOfOne() throws IOException {
    // Single-component scan (the common case for a direct .jar/.war upload). The plural API must
    // return a one-element list with the same identity the singular API returns, so existing
    // single-component scans behave identically end-to-end.
    String content = xml(
        "<repository id=\"r\" name=\"n\" format=\"maven2\"/>\n" +
            "<dir path=\"com/example/foo-1.0.jar\" sha1=\"single_hash_001\" sha512=\"x\">\n</dir>");

    List<ScanComponentInfo> infos = ScanXmlParser.extractComponentInfos(plainXmlEntity(content));

    assertThat(infos).hasSize(1);
    assertThat(infos.get(0).pathname()).isEqualTo("com/example/foo-1.0.jar");
    assertThat(infos.get(0).hash()).isEqualTo("single_hash_001");
    assertThat(infos.get(0).format()).isEqualTo("maven2");
  }

  @Test
  public void extractComponentInfos_multipleDirElements_returnsAllWithSyntheticNestedPathnames() throws IOException {
    // Archive-of-archives: a .zip uploaded under maven2 layout that the scanner unpacked into two
    // inner .jar entries. The first <dir> is the outer artifact (its path is preserved verbatim);
    // each subsequent <dir> is an inner archive whose pathname is synthesised as
    // outerPath + "!/" + innerPathTail, so the IQ-side (repository_id, pathname) UNIQUE constraint
    // admits each inner artifact as a distinct proxy_repository_component row.
    String content = xml(
        "<repository id=\"r\" name=\"n\" format=\"maven2\"/>\n" +
            "<dir path=\"com/example/bundle-1.0.zip\" sha1=\"outer_zip_hash_01\" sha512=\"x\">\n</dir>\n" +
            "<dir path=\"log4j-core-2.14.1.jar\" sha1=\"inner_log4j_hash_1\" sha512=\"y\">\n</dir>\n" +
            "<dir path=\"commons-cli-1.9.0.jar\" sha1=\"inner_cli_hash_001\" sha512=\"z\">\n</dir>");

    List<ScanComponentInfo> infos = ScanXmlParser.extractComponentInfos(plainXmlEntity(content));

    assertThat(infos).hasSize(3);
    // Outer artifact: pathname unchanged from the <dir>'s `path` attribute
    assertThat(infos.get(0).pathname()).isEqualTo("com/example/bundle-1.0.zip");
    assertThat(infos.get(0).hash()).isEqualTo("outer_zip_hash_01");
    assertThat(infos.get(0).format()).isEqualTo("maven2");
    // Inner artifacts: outer!/inner pathname so the row keys are distinct
    assertThat(infos.get(1).pathname()).isEqualTo("com/example/bundle-1.0.zip!/log4j-core-2.14.1.jar");
    assertThat(infos.get(1).hash()).isEqualTo("inner_log4j_hash_1");
    assertThat(infos.get(1).format()).isEqualTo("maven2");
    assertThat(infos.get(2).pathname()).isEqualTo("com/example/bundle-1.0.zip!/commons-cli-1.9.0.jar");
    assertThat(infos.get(2).hash()).isEqualTo("inner_cli_hash_001");
    assertThat(infos.get(2).format()).isEqualTo("maven2");
  }

  @Test
  public void extractComponentInfos_innerDirPathPrefixedByOuterPath_strippedToRelative() throws IOException {
    // The scanner sometimes emits inner <dir> entries whose `path` attribute already includes the
    // outer pathname as a prefix (e.g. `outer.zip/inner.jar`). The synthesiser strips the duplicated
    // prefix so the final pathname is `outer.zip!/inner.jar`, not `outer.zip!/outer.zip/inner.jar`.
    String content = xml(
        "<repository id=\"r\" name=\"n\" format=\"maven2\"/>\n" +
            "<dir path=\"com/example/bundle-1.0.zip\" sha1=\"outer_zip_hash_02\" sha512=\"x\">\n</dir>\n" +
            "<dir path=\"com/example/bundle-1.0.zip/lib/inner.jar\" sha1=\"inner_jar_hash_01\" sha512=\"y\">\n</dir>");

    List<ScanComponentInfo> infos = ScanXmlParser.extractComponentInfos(plainXmlEntity(content));

    assertThat(infos).hasSize(2);
    assertThat(infos.get(1).pathname()).isEqualTo("com/example/bundle-1.0.zip!/lib/inner.jar");
  }

  @Test
  public void extractComponentInfos_innerDirPathPrefixedByOuterPathWithBangSeparator_strippedToRelative() throws IOException {
    // Defensive: if the scanner ever switches to the URL-style "!/" separator (the same form the
    // synthesiser already emits downstream), stripLeading must still trim the duplicated prefix.
    // Without this, the synthesised pathname would double-nest as
    // "com/example/bundle-1.0.zip!/com/example/bundle-1.0.zip!/lib/inner.jar" — a confusing
    // artifact even if it's a valid unique key.
    String content = xml(
        "<repository id=\"r\" name=\"n\" format=\"maven2\"/>\n" +
            "<dir path=\"com/example/bundle-1.0.zip\" sha1=\"outer_zip_hash_03\" sha512=\"x\">\n</dir>\n" +
            "<dir path=\"com/example/bundle-1.0.zip!/lib/inner.jar\" sha1=\"inner_bang_hash_1\" sha512=\"y\">\n</dir>");

    List<ScanComponentInfo> infos = ScanXmlParser.extractComponentInfos(plainXmlEntity(content));

    assertThat(infos).hasSize(2);
    assertThat(infos.get(1).pathname()).isEqualTo("com/example/bundle-1.0.zip!/lib/inner.jar");
  }

  @Test
  public void extractComponentInfos_dirWithMissingPathOrHash_skippedWithoutAbortingSiblings() throws IOException {
    // A malformed <dir> (missing path or sha1) must not abort the scan — siblings before and after
    // should still be returned. This protects archive-of-archives uploads from losing all inner
    // artifacts because one inner archive happened to be unreadable.
    String content = xml(
        "<repository id=\"r\" name=\"n\" format=\"maven2\"/>\n" +
            "<dir path=\"a/good-1.jar\" sha1=\"hash_good_one_01\" sha512=\"x\">\n</dir>\n" +
            "<dir sha1=\"orphan_hash_01\" sha512=\"y\">\n</dir>\n" +
            "<dir path=\"b/good-2.jar\" sha1=\"hash_good_two_02\" sha512=\"z\">\n</dir>");

    List<ScanComponentInfo> infos = ScanXmlParser.extractComponentInfos(plainXmlEntity(content));

    assertThat(infos).hasSize(2);
    assertThat(infos.get(0).pathname()).isEqualTo("a/good-1.jar");
    // Note: the malformed <dir> in the middle does not become the "outer" — the first valid <dir>
    // remains the outer, and subsequent valid <dir>s are inner artifacts under it.
    assertThat(infos.get(1).pathname()).isEqualTo("a/good-1.jar!/b/good-2.jar");
    assertThat(infos.get(1).hash()).isEqualTo("hash_good_two_02");
  }

  @Test
  public void extractComponentInfos_noDirElement_returnsEmptyList() throws IOException {
    String content = xml("<repository id=\"r\" name=\"n\" format=\"maven2\"/>");

    List<ScanComponentInfo> infos = ScanXmlParser.extractComponentInfos(plainXmlEntity(content));

    assertThat(infos).isEmpty();
  }

  @Test
  public void extractComponentInfos_inputStreamThrowsIOException_returnsEmptyList() throws IOException {
    ScanEntity entity = mock(ScanEntity.class);
    when(entity.getInputStream()).thenThrow(new IOException("disk error"));

    List<ScanComponentInfo> infos = ScanXmlParser.extractComponentInfos(entity);

    assertThat(infos).isEmpty();
  }

  @Test
  public void extractComponentInfos_gzipEncodedMultipleDirs_returnsAll() throws IOException {
    // gzip path mirrors the plain-xml path; the streaming parser treats both identically.
    String content = xml(
        "<repository id=\"r\" name=\"n\" format=\"maven2\"/>\n" +
            "<dir path=\"outer.zip\" sha1=\"gzip_outer_hash_01\" sha512=\"x\">\n</dir>\n" +
            "<dir path=\"inner-a.jar\" sha1=\"gzip_inner_a_hash01\" sha512=\"y\">\n</dir>\n" +
            "<dir path=\"inner-b.jar\" sha1=\"gzip_inner_b_hash01\" sha512=\"z\">\n</dir>");

    List<ScanComponentInfo> infos = ScanXmlParser.extractComponentInfos(gzipXmlEntity(content));

    assertThat(infos).hasSize(3);
    assertThat(infos.get(0).pathname()).isEqualTo("outer.zip");
    assertThat(infos.get(1).pathname()).isEqualTo("outer.zip!/inner-a.jar");
    assertThat(infos.get(2).pathname()).isEqualTo("outer.zip!/inner-b.jar");
  }
}
