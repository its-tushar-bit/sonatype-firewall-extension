/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.scanscrubber;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.assertj.core.api.Assertions.assertThat;

public class ScanScrubberTest
{
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private ScanScrubber scanScrubber;

  @Before
  public void before() throws Exception {
    scanScrubber = new ScanScrubber();
    String gzip = new File(temporaryFolder.getRoot(), "unscrubbed-scan.xml.gz").getAbsolutePath();
    compress("src/test/resources/ScanScrubberTest.xml", gzip);
    scanScrubber.setInputFile(gzip);
    scanScrubber.setOutputFile(new File(temporaryFolder.getRoot(), "scrubbed-scan.xml.gz").getAbsolutePath());
  }

  @Test
  public void testScrub_Defaults() throws Exception {
    Document document = ScanScrubber.read(scanScrubber.getInputFile());
    Document scrubbedDocument = ScanScrubber.read(scanScrubber.getInputFile());
    Map<String, String> map = getPathnamesToScrubbedPathnames(scanScrubber.scrub(scrubbedDocument));

    // files
    assertMatchesAndChanged(map, "file.js", "\\w+\\.js");
    assertMatchesAndChanged(map, "dir1/file1.js", "\\w+/\\w+\\.js");
    assertMatchesAndChanged(map, "dir1/file2.js", "\\w+/\\w+\\.js");
    assertMatchesAndChanged(map, "dir1/dir1/file1.js", "\\w+/\\w+/\\w+\\.js");
    assertMatchesAndChanged(map, "dir1/dir1/file2.js", "\\w+/\\w+/\\w+\\.js");

    // directories
    assertMatchesAndChanged(map, "dir2/file.js", "\\w+/\\w+\\.js");
    assertMatchesAndChanged(map, "dir2/dir2a/file.js", "\\w+/\\w+/\\w+\\.js");
    assertMatchesAndChanged(map, "dir2/dir2b/file.js", "\\w+/\\w+/\\w+\\.js");

    // test or example directories
    assertMatchesAndChanged(map, "test/dir11/test", "test/\\w+/test");
    assertMatchesAndChanged(map, "tests/dir11/tests", "tests/\\w+/tests");
    assertMatchesAndChanged(map, "example/dir11/example", "example/\\w+/example");
    assertMatchesAndChanged(map, "examples/dir11/examples", "examples/\\w+/examples");
    assertMatchesAndChanged(map, "fixture/dir11/fixture", "fixture/\\w+/fixture");
    assertMatchesAndChanged(map, "fixtures/dir11/fixtures", "fixtures/\\w+/fixtures");
    assertMatchesAndChanged(map, "spec/dir11/spec", "spec/\\w+/spec");
    assertMatchesAndChanged(map, "specs/dir11/specs", "specs/\\w+/specs");

    // version
    assertThat(map).containsEntry("1.0.0", "1.0.0");

    // component separator? version
    assertThat(map).containsEntry("component 1.0.0", "component 1.0.0");
    assertMatchesAndChanged(map, "dir10/component 1.0.0", "\\w+/component 1.0.0");
    assertMatchesAndChanged(map, "dir10/component 1.0.0/dir10/component-2.0.0",
        "\\w+/component 1.0.0/\\w+/component-2.0.0");

    // component/version
    assertThat(map).containsEntry("dir3/1.0.0", "dir3/1.0.0").containsEntry("dir3/dir3/1.0.0", "dir3/dir3/1.0.0");

    // node_modules
    assertThat(map).containsEntry("node_modules", "node_modules");
    assertMatchesAndChanged(map, "dir4/node_modules", "\\w+/node_modules");
    assertMatchesAndChanged(map, "node_modules/dir4/node_modules", "node_modules/\\w+/node_modules");
    assertMatchesAndChanged(map, "dir4/node_modules/dir4/node_modules", "\\w+/node_modules/\\w+/node_modules");

    // bower_components
    assertThat(map).containsEntry("bower_components", "bower_components");
    assertMatchesAndChanged(map, "dir5/bower_components", "\\w+/bower_components");

    // package
    assertThat(map).containsEntry("package", "package");
    assertMatchesAndChanged(map, "package/dir6/package", "package/\\w+/package");
    assertMatchesAndChanged(map, "dir6/package", "\\w+/package");
    assertMatchesAndChanged(map, "dir6/package/dir6/package", "\\w+/package/\\w+/package");

    // package.json
    assertThat(map).containsEntry("package.json", "package.json");
    assertMatchesAndChanged(map, "dir7/package.json", "\\w+/package.json");

    // bower.json
    assertThat(map).containsEntry("bower.json", "bower.json");
    assertMatchesAndChanged(map, "dir8/bower.json", "\\w+/bower.json");

    // @scoped
    assertMatchesAndChanged(map, "@scoped", "@\\w+");
    assertMatchesAndChanged(map, "@scoped/dir9/dir9/@scoped", "@\\w+/\\w+/\\w+/@\\w+");
    assertMatchesAndChanged(map, "dir9/@scoped", "\\w+/@\\w+");
    assertMatchesAndChanged(map, "dir9/@scoped/dir9/dir9/@scoped", "\\w+/@\\w+/\\w+/\\w+/@\\w+");

    // suffix
    assertMatchesAndChanged(map, "suffix", "\\w+");
    assertMatchesAndChanged(map, "suffix.", "\\w+");
    assertMatchesAndChanged(map, "suffix.12345", "\\w+\\.12345");
    assertMatchesAndChanged(map, "suffix.123456", "\\w+");

    // ids
    assertNoIds(scrubbedDocument);

    assertConsistentPathnames(document, map);

    // ignored
    assertEqualElements(document, scrubbedDocument, "configuration");
    assertEqualElements(document, scrubbedDocument, "summary");
  }

  @Test
  public void testReadWrite() throws Exception {
    Document document = ScanScrubber.read(scanScrubber.getInputFile());
    ScanScrubber.write(document, scanScrubber.getOutputFile());
    Document sameDocument = ScanScrubber.read(scanScrubber.getOutputFile());

    assertThat(document.isEqualNode(sameDocument)).isTrue();
  }

  @Test
  public void testCreateTransformer() throws Exception {
    Transformer transformer = ScanScrubber.createTransformer();

    assertThat(transformer.getOutputProperty(OutputKeys.OMIT_XML_DECLARATION)).isEqualTo("yes");
    assertThat(transformer.getOutputProperty(OutputKeys.INDENT)).isEqualTo("yes");
  }

  @Test
  public void testParse_PathnameSuffixExcludes() {
    assertPathnameSuffixExcludes("", new ArrayList<>());
    assertPathnameSuffixExcludes(String.join(ScanScrubber.DELIMITER, ScanScrubber.JAVASCRIPT_PATHNAME_SUFFIX_EXCLUDES),
        ScanScrubber.JAVASCRIPT_PATHNAME_SUFFIX_EXCLUDES.stream().map(suffixPattern -> "(?:^|/)" + suffixPattern + "$")
            .collect(Collectors.toList()));
  }

  private void assertPathnameSuffixExcludes(String excludes, List<String> expected) {
    ScanScrubber scanScrubber = ScanScrubber
        .run("-inputFile", this.scanScrubber.getInputFile(), "-pathnameSuffixExcludes", excludes);

    assertThat(scanScrubber.getPathnameSuffixExcludesPatterns()).extracting(Pattern::pattern).isEqualTo(expected);
  }

  private void compress(String source, String gzip) throws Exception {
    try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(new FileOutputStream(gzip))) {
      Files.copy(Paths.get(source), gzipOutputStream);
    }
  }

  private Map<String, String> getPathnamesToScrubbedPathnames(
      Map<String, com.sonatype.insight.brain.tools.scanscrubber.ScanScrubber.Node> pathnamesToNodes)
  {
    return pathnamesToNodes.entrySet().stream().collect(Collectors
        .toMap(Entry::getKey, entry -> entry.getValue().getScrubbedPathname(), (entryOne, entryTwo) -> entryOne,
            LinkedHashMap::new));
  }

  private void assertMatchesAndChanged(Map<String, String> map, String pathname, String pattern) {
    assertThat(map.get(pathname)).isNotEqualTo(pathname).matches(pattern);
  }

  private void assertNoIds(Node node) {
    assertThat(node.getNodeName()).doesNotStartWith("id");
    NodeList nodeList = node.getChildNodes();
    for (int childIndex = 0; childIndex < nodeList.getLength(); childIndex++) {
      assertNoIds(nodeList.item(childIndex));
    }
  }

  private void assertConsistentPathnames(Document document, Map<String, String> pathnamesToScrubbedPathnames) {
    for (String pathname : getPathnames(document.getDocumentElement(), "", new HashSet<>())) {
      String scrubbedPathname = pathnamesToScrubbedPathnames.get(pathname);
      assertThat(StringUtils.countMatches(pathname, "/")).isEqualTo(StringUtils.countMatches(scrubbedPathname, "/"));
      int pathnameForwardSlashIndex = pathname.lastIndexOf("/");
      int scrubbedPathnameForwardSlashIndex = scrubbedPathname.lastIndexOf("/");
      while (pathnameForwardSlashIndex != -1) {
        String pathnamePart = pathname.substring(0, pathnameForwardSlashIndex);
        String scrubbedPathnamePart = scrubbedPathname
            .substring(scrubbedPathnameForwardSlashIndex, scrubbedPathname.length());
        assertThat(pathnamesToScrubbedPathnames.get(pathnamePart) + scrubbedPathnamePart).isEqualTo(scrubbedPathname);
        pathnameForwardSlashIndex = pathname.lastIndexOf("/", pathnameForwardSlashIndex - 1);
        scrubbedPathnameForwardSlashIndex = scrubbedPathname.lastIndexOf("/", scrubbedPathnameForwardSlashIndex - 1);
      }
    }
  }

  private Collection<String> getPathnames(Node node, String pathname, Collection<String> pathnames) {
    NodeList nodeList = node.getChildNodes();
    for (int childIndex = 0; childIndex < nodeList.getLength(); childIndex++) {
      Node child = nodeList.item(childIndex);
      if (child.hasAttributes()) {
        Attr path = (Attr) child.getAttributes().getNamedItem("path");
        if (path != null) {
          getPathnames(child, pathname + "/" + path.getValue(), pathnames);
        }
      }
    }
    if (!pathname.isEmpty()) {
      pathnames.add(pathname.substring(1));
    }
    return pathnames;
  }

  private void assertEqualElements(Document documentOne, Document documentTwo, String elementName) {
    assertThat(documentOne.getElementsByTagName(elementName).item(0)
        .isEqualNode(documentTwo.getElementsByTagName(elementName).item(0))).isTrue();
  }
}
