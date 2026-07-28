/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.cyclonedx.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.generators.xml.BomXmlGenerator;
import org.cyclonedx.model.Bom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Works around a bug in cyclonedx-core-java (as of 13.0.0): its {@code LicenseChoiceSerializer}
 * writes each {@code <license>}'s child elements in the hardcoded order
 * {@code (id|name), licensing, text, url, properties}. The CycloneDX XML schema {@code licenseType}
 * sequence, however, requires {@code (id|name), text, url, licensing, properties}. So any license
 * carrying both a {@code licensing} block and a {@code url}/{@code text} serializes to XML that
 * fails validation against the library's own XSD and cannot be re-imported. Because the ordering is
 * baked into a custom serializer, no mapper/annotation configuration can correct it.
 * <p>
 * This generator post-processes the emitted XML, reordering the child elements of every
 * {@code <license>} into the schema sequence. Only element order changes; content is preserved.
 * JSON export is unaffected (JSON ignores element order) and uses the stock generator.
 */
public class CycloneDxSchemaOrderedXmlGenerator
    extends BomXmlGenerator
{
  // licenseType element sequence per the CycloneDX XSD. Unlisted elements are kept, after these.
  private static final List<String> LICENSE_ELEMENT_ORDER = List.of("id", "name", "text", "url", "licensing");

  public CycloneDxSchemaOrderedXmlGenerator(final Bom bom, final Version version) {
    super(bom, version);
  }

  @Override
  public String toXmlString() throws GeneratorException {
    String xml = super.toXmlString();
    // The defect only manifests when a license carries a <licensing> block; skip the rewrite otherwise.
    if (xml == null || !xml.contains("<licensing>")) {
      return xml;
    }
    try {
      Document document = parse(xml);
      reorderLicenseChildren(document);
      return serialize(document);
    }
    catch (Exception e) {
      throw new GeneratorException("Failed to normalize license element ordering for XML export", e);
    }
  }

  private void reorderLicenseChildren(final Document document) {
    NodeList licenses = document.getElementsByTagNameNS("*", "license");
    for (int i = 0; i < licenses.getLength(); i++) {
      reorderChildren((Element) licenses.item(i));
    }
  }

  private void reorderChildren(final Element license) {
    // Collect element children in document order; ignore whitespace text nodes (re-indented on output).
    List<Element> children = new ArrayList<>();
    NodeList nodes = license.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        children.add((Element) node);
      }
    }
    // Stable sort by schema position; unlisted elements sort after known ones, keeping their order.
    children.sort((a, b) -> Integer.compare(rank(a), rank(b)));
    for (Element child : children) {
      license.removeChild(child);
    }
    for (Element child : children) {
      license.appendChild(child);
    }
  }

  private static int rank(final Element element) {
    int idx = LICENSE_ELEMENT_ORDER.indexOf(element.getLocalName());
    return idx < 0 ? LICENSE_ELEMENT_ORDER.size() : idx;
  }

  private static Document parse(final String xml) throws Exception {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setExpandEntityReferences(false);
    DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse(new InputSource(new StringReader(xml)));
  }

  private static String serialize(final Document document) throws Exception {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    // Strip the whitespace-only text nodes left by the original pretty-printer so re-indentation is clean.
    stripIgnorableWhitespace(document.getDocumentElement());
    StringWriter writer = new StringWriter();
    transformer.transform(new DOMSource(document), new StreamResult(writer));
    return writer.toString();
  }

  private static void stripIgnorableWhitespace(final Node node) {
    NodeList children = node.getChildNodes();
    for (int i = children.getLength() - 1; i >= 0; i--) {
      Node child = children.item(i);
      if (child.getNodeType() == Node.TEXT_NODE && child.getTextContent().isBlank()) {
        node.removeChild(child);
      }
      else if (child.getNodeType() == Node.ELEMENT_NODE) {
        stripIgnorableWhitespace(child);
      }
    }
  }
}
