/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.scanscrubber;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ScanScrubber
{
  private static final Logger log = LoggerFactory.getLogger(ScanScrubber.class);

  @VisibleForTesting
  static final String DELIMITER = ";";

  private static final String PROGRAM_NAME = "java -jar nexus-iq-tools.jar scanscrubber";

  @VisibleForTesting
  static final List<String> JAVASCRIPT_PATHNAME_SUFFIX_EXCLUDES = Arrays.asList( //
      "[^/]+/[0-9]+\\.[0-9]+\\.[0-9]+[^/]*/", // js package name i.e. [component name]/[version]
      "[^/]*[0-9]+\\.[0-9]+\\.[0-9]+[^/]*/", // js package name i.e. [component name?][separator?][version]
      "node_modules/", // js package container name
      "bower_components/", // js package container name
      "package/", // js package name
      "package\\.json", // js package file
      "bower\\.json", // js package file
      "(?:tests?|specs?|examples?|fixtures?)/", // directories that may contain bogus js package files
      "@([^/]+)/", // keep @ character indicating an npm scoped package
      "([^/]*)\\.\\w{1,5}/?"); // keep file suffixes

  public static class ListConverter
      implements IStringConverter<List<String>>
  {
    @Override
    public List<String> convert(String value) {
      return StringUtils.isBlank(value) ? new ArrayList<>() : Arrays.stream(value.split(DELIMITER))
          .collect(Collectors.toList());
    }
  }

  @VisibleForTesting
  static class Node
  {
    private Node parent;

    private Collection<Node> children = new LinkedHashSet<>();

    private final String name;

    private String scrubbedName;

    private Node(String pathname) {
      name = pathname.substring(pathname.lastIndexOf('/') + 1);
      scrubbedName = name;
    }

    private String getPathname() {
      return buildPathname(node -> node.name) + (children.isEmpty() ? "" : "/");
    }

    @VisibleForTesting
    String getScrubbedPathname() {
      return buildPathname(node -> node.scrubbedName);
    }

    private String buildPathname(Function<Node, String> nodeToName) {
      StringBuilder pathname = new StringBuilder(nodeToName.apply(this));
      Node current = this;
      while (current.parent != null) {
        current = current.parent;
        pathname.insert(0, "/").insert(0, nodeToName.apply(current));
      }
      return pathname.toString();
    }

    private void setScrubbedPathname(String pathname) {
      int lastSlashIndex = pathname.lastIndexOf("/");
      scrubbedName = pathname.substring(lastSlashIndex + 1);
      if (lastSlashIndex != -1) {
        parent.setScrubbedPathname(pathname.substring(0, lastSlashIndex));
      }
    }

    @Override
    public String toString() {
      return getScrubbedPathname();
    }
  }

  @Parameter(names = "-inputFile", description = "Path to the scan file (gzipped xml) to be scrubbed.", required = true)
  private String inputFile;

  @Parameter(names = "-outputFile", description = "Path to the scrubbed scan file (gzipped xml) to be output." + //
      " Default: inputFile/../[random text]-scan.xml.gz")
  private String outputFile;

  @Parameter(names = "-pathnameSuffixExcludes", description = "[" + DELIMITER + "] delimited list of regular " + //
      "expressions representing pathname suffixes (must have complete segments) to be excluded from scrubbing, any" + //
      " capturing group will be scrubbed.", //
      listConverter = ListConverter.class)
  private List<String> pathnameSuffixExcludes = JAVASCRIPT_PATHNAME_SUFFIX_EXCLUDES;

  private List<Pattern> pathnameSuffixExcludesPatterns;

  public String getInputFile() {
    return inputFile;
  }

  public void setInputFile(String inputFile) {
    this.inputFile = inputFile;
  }

  public String getOutputFile() {
    if (outputFile == null) {
      outputFile = new File(new File(inputFile).getParentFile(), randomString() + "-scan.xml.gz").getAbsolutePath();
    }
    return outputFile;
  }

  public void setOutputFile(String outputFile) {
    this.outputFile = outputFile;
  }

  @VisibleForTesting
  List<Pattern> getPathnameSuffixExcludesPatterns() {
    if (pathnameSuffixExcludesPatterns == null) {
      pathnameSuffixExcludesPatterns = pathnameSuffixExcludes.stream()
          .map(suffixPattern -> "(?:^|/)" + suffixPattern + "$").map(Pattern::compile).collect(Collectors.toList());
    }
    return pathnameSuffixExcludesPatterns;
  }

  public void scrub() throws IOException, TransformerException, ParserConfigurationException, SAXException {
    long start = System.currentTimeMillis();
    Document document = read(inputFile);
    log.info("Read scan file {} into memory in {} ms.", inputFile, System.currentTimeMillis() - start);

    start = System.currentTimeMillis();
    scrub(document);
    log.info("Scrubbed scan file {} in memory in {} ms.", inputFile, System.currentTimeMillis() - start);

    start = System.currentTimeMillis();
    write(document, getOutputFile());
    log.info("Wrote scrubbed scan file to {} in {} ms.", outputFile, System.currentTimeMillis() - start);
  }

  @VisibleForTesting
  static Document read(String inputFile) throws ParserConfigurationException, IOException, SAXException {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new InputSource(new XmlStreamReader(new GZIPInputStream(new FileInputStream(inputFile)))));
  }

  @VisibleForTesting
  Map<String, Node> scrub(Document document) {
    long start = System.currentTimeMillis();
    Map<String, Node> pathnamesToNodes = buildTree(document, "", new LinkedHashMap<>());
    log.debug("Built tree with {} nodes in {} ms.", pathnamesToNodes.size(), System.currentTimeMillis() - start);

    start = System.currentTimeMillis();
    pathnamesToNodes = scrubPathnames(pathnamesToNodes);
    log.debug("Scrubbed pathnames of {} nodes in {} ms.", pathnamesToNodes.size(), System.currentTimeMillis() - start);

    start = System.currentTimeMillis();
    setPathnames(document, "", pathnamesToNodes);
    log.debug("Set scrubbed pathnames in {} ms.", System.currentTimeMillis() - start);

    start = System.currentTimeMillis();
    removeIds(document);
    log.debug("Removed ids in {} ms.", System.currentTimeMillis() - start);
    return pathnamesToNodes;
  }

  private Map<String, Node> buildTree(org.w3c.dom.Node node, String pathname, Map<String, Node> pathnamesToNodes) {
    if (node.hasAttributes()) {
      Attr path = (Attr) node.getAttributes().getNamedItem("path");
      if (path != null) {
        pathname = pathname.isEmpty() ? path.getValue() : pathname + "/" + path.getValue();
        Node child = pathnamesToNodes.computeIfAbsent(pathname, Node::new);
        boolean ancestorsExist = false;
        String pathnamePart = pathname;
        int lastSlashIndex = pathnamePart.lastIndexOf('/');
        while (lastSlashIndex != -1 && !ancestorsExist) {
          pathnamePart = pathnamePart.substring(0, lastSlashIndex);
          lastSlashIndex = pathnamePart.lastIndexOf('/');
          Node parent = pathnamesToNodes.get(pathnamePart);
          if (parent == null) {
            parent = new Node(pathnamePart);
            pathnamesToNodes.put(pathnamePart, parent);
          }
          else {
            ancestorsExist = true;
          }
          parent.children.add(child);
          child.parent = parent;
          child = parent;
        }
      }
    }
    NodeList nodeList = node.getChildNodes();
    for (int childIndex = 0; childIndex < nodeList.getLength(); childIndex++) {
      buildTree(nodeList.item(childIndex), pathname, pathnamesToNodes);
    }
    return pathnamesToNodes;
  }

  private Map<String, Node> scrubPathnames(Map<String, Node> pathnamesToNodes) {
    pathnamesToNodes.values().stream().filter(node -> node.parent == null).forEach(this::scrubPathnames);
    return pathnamesToNodes;
  }

  private void scrubPathnames(Node node) {
    String pathname = node.getPathname();
    boolean excluded = false;
    for (Pattern pathnameSuffixExclude : getPathnameSuffixExcludesPatterns()) {
      Matcher matcher = pathnameSuffixExclude.matcher(pathname);
      if (matcher.find()) {
        pathname = replaceGroups(matcher, pathname).substring(matcher.start()).replaceFirst("^/*", "")
            .replaceFirst("/*$", "");
        node.setScrubbedPathname(pathname);
        log.debug("{} generated pathname suffix exclude replacement {}", pathnameSuffixExclude.pattern(), pathname);
        excluded = true;
        break;
      }
    }
    if (!excluded) {
      node.scrubbedName = randomString();
    }
    for (Node child : node.children) {
      scrubPathnames(child);
    }
  }

  private String replaceGroups(Matcher matcher, String replacement) {
    int offset = 0;
    for (int group = 1; group <= matcher.groupCount(); group++) {
      String subreplacement = randomString();
      replacement = replacement.substring(0, matcher.start(group) + offset) + subreplacement +
          replacement.substring(matcher.end(group) + offset);
      offset += subreplacement.length() - matcher.group(group).length();
    }
    return replacement;
  }

  private String randomString() {
    return RandomStringUtils.randomAlphabetic(16);
  }

  private void setPathnames(org.w3c.dom.Node node, String pathname, Map<String, Node> pathnamesToNodes) {
    if (node.hasAttributes()) {
      Attr path = (Attr) node.getAttributes().getNamedItem("path");
      if (path != null) {
        pathname = pathname.isEmpty() ? path.getValue() : pathname + "/" + path.getValue();
        String scrubbedPathname = pathnamesToNodes.get(pathname).getScrubbedPathname();
        path.setValue(scrubbedPathname.substring(
            StringUtils.lastOrdinalIndexOf(scrubbedPathname, "/", StringUtils.countMatches(path.getValue(), "/") + 1) +
                1));
      }
    }
    NodeList nodeList = node.getChildNodes();
    for (int childIndex = 0; childIndex < nodeList.getLength(); childIndex++) {
      setPathnames(nodeList.item(childIndex), pathname, pathnamesToNodes);
    }
  }

  private void removeIds(org.w3c.dom.Node node) {
    if (node.getNodeName().equals("ids")) {
      Element parent = (Element) node.getParentNode();
      parent.removeChild(node);
    }
    NodeList nodeList = node.getChildNodes();
    for (int childIndex = 0; childIndex < nodeList.getLength(); childIndex++) {
      removeIds(nodeList.item(childIndex));
    }
  }

  @VisibleForTesting
  static void write(Document document, String outputFile) throws IOException, TransformerException {
    try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(new FileOutputStream(outputFile))) {
      createTransformer().transform(new DOMSource(document), new StreamResult(gzipOutputStream));
    }
  }

  @VisibleForTesting
  static Transformer createTransformer() throws TransformerConfigurationException {
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    return transformer;
  }

  @VisibleForTesting
  static ScanScrubber run(String... args) {
    ScanScrubber scanScrubber = new ScanScrubber();
    JCommander jCommander = new JCommander(scanScrubber);
    jCommander.setProgramName(ScanScrubber.PROGRAM_NAME);
    try {
      jCommander.parse(args);
      scanScrubber.scrub();
    }
    catch (ParameterException e) {
      StringBuilder usage = new StringBuilder();
      jCommander.usage(usage);
      log.info(usage.toString());
      throw e;
    }
    catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return scanScrubber;
  }

  public static void main(String... args) {
    run(args);
  }
}
