/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cyclonedx.model.AttachmentText;
import org.cyclonedx.model.Swid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import us.springett.parsers.cpe.Cpe;
import us.springett.parsers.cpe.CpeParser;
import us.springett.parsers.cpe.exceptions.CpeParsingException;
import us.springett.parsers.cpe.values.LogicalValue;

public class SbomIdentityUtils
{
  private static final Logger log = LoggerFactory.getLogger(SbomIdentityUtils.class);

  private static DocumentBuilderFactory documentBuilderFactory;

  /**
   * CPE 2.2 Structure:
   * <pre>
   * cpe:/{part}:{vendor}:{product}:{version}:{update}:{edition}:{language}
   * </pre>
   * Mapping:
   * <pre>
   * pkg:cpe/{vendor}/{product}@{version}?update={update}&edition={edition}&language={language}
   * </pre>
   * <p>
   * CPE 2.3 Structure :
   * <pre>
   * cpe:{cpe_version}:{part}:{vendor}:{product}:{version}:{update}:{edition}:
   *    {language}:{sw_edition}:{target_sw}:{target_hw}:{other}
   * </pre>
   * Mapping:
   * <pre>
   * pkg:cpe/{vendor}/{product}@{version}?update={update}&edition={edition}&language={language}&
   *    sw_edition={sw_edition}&target_sw={target_sw}&target_hw={target_hw}&other={other}
   * </pre>
   * <p>
   * All PURL qualifiers are specified only if they are not empty or *.
   */
  public static PackageUrlIdentifier buildPackageUrlFromCpe(final String cpe) {
    // CPE examples:
    // cpe:/a:microsoft:internet_explorer:8.0.6001:beta
    // cpe:2.3:a:microsoft:internet_explorer:8.0.6001:beta:*:*:*:*:*:*

    if (StringUtils.isBlank(cpe)) {
      return null;
    }

    try {
      PackageURLBuilder packageURLBuilder = PackageURLBuilder.aPackageURL();
      Cpe cpeParsedValue = CpeParser.parse(cpe);
      packageURLBuilder.withType("generic");
      if (isCpeQualifierValid(cpeParsedValue.getProduct())) {
        packageURLBuilder.withName(ThirdPartyScanResultUtils.getTruncatedName(urlDecode(cpeParsedValue.getProduct())));
      }
      if (isCpeQualifierValid(cpeParsedValue.getVersion())) {
        packageURLBuilder
            .withVersion(ThirdPartyScanResultUtils.getTruncatedVersion(urlDecode(cpeParsedValue.getVersion())));
      }
      if (isCpeQualifierValid(cpeParsedValue.getVendor())) {
        packageURLBuilder.withNamespace(urlDecode(cpeParsedValue.getVendor()));
      }
      if (isCpeQualifierValid(cpeParsedValue.getUpdate())) {
        packageURLBuilder.withQualifier("update", cpeParsedValue.getUpdate());
      }
      if (isCpeQualifierValid(cpeParsedValue.getEdition())) {
        packageURLBuilder.withQualifier("edition", cpeParsedValue.getEdition());
      }
      if (isCpeQualifierValid(cpeParsedValue.getLanguage())) {
        packageURLBuilder.withQualifier("language", cpeParsedValue.getLanguage());
      }
      if (isCpeQualifierValid(cpeParsedValue.getSwEdition())) {
        packageURLBuilder.withQualifier("sw_edition", cpeParsedValue.getSwEdition());
      }
      if (isCpeQualifierValid(cpeParsedValue.getTargetSw())) {
        packageURLBuilder.withQualifier("target_sw", cpeParsedValue.getTargetSw());
      }
      if (isCpeQualifierValid(cpeParsedValue.getTargetHw())) {
        packageURLBuilder.withQualifier("target_hw", cpeParsedValue.getTargetHw());
      }
      if (isCpeQualifierValid(cpeParsedValue.getOther())) {
        packageURLBuilder.withQualifier("other", cpeParsedValue.getOther());
      }
      PackageURL packageUrl = packageURLBuilder.build();
      return new PackageUrlIdentifier(packageUrl.canonicalize());
    }
    catch (MalformedPackageURLException | UnsupportedEncodingException e) {
      throw new InvalidPackageURLException(e.getMessage(), e);
    }
    catch (CpeParsingException e) {
      log.debug("Error parsing CPE {}", cpe, e);
    }
    return null;
  }

  private static boolean isCpeQualifierValid(String cpeQualifier) {
    return StringUtils.isNotEmpty(cpeQualifier)
        && !LogicalValue.ANY.getAbbreviation().equals(cpeQualifier)
        && !LogicalValue.NA.getAbbreviation().equals(cpeQualifier);
  }

  private static String urlDecode(String input) throws UnsupportedEncodingException {
    if (StringUtils.isEmpty(input)) {
      return input;
    }
    return URLDecoder.decode(input, StandardCharsets.UTF_8.name());
  }

  /**
   * Creates SWID based package URLs as following:
   * <ul>
   * <li>The {@code namespace} is the optional name and regid of the entity with a role of softwareCreator.
   * If specified, name is required and is the first segment in the namespace. If regid is known, it must be specified
   * as the second segment in the namespace. A maximum of two segments are supported.
   * <li>The {@code name} is the name as defined in the SWID SoftwareIdentity element.
   * <li>The {@code version} is the version as defined in the SWID SoftwareIdentity element.
   * <li>The qualifier {@code tag_id} must not be empty and corresponds to the tagId as defined in the SWID
   * SoftwareIdentity element.
   * <li>The qualifier {@code tag_version} is an optional integer and corresponds to the tagVersion as defined in the
   * SWID SoftwareIdentity element. If not specified, defaults to {@code 0}.
   * <li>The qualifier {@code patch} is optional and corresponds to the patch as defined in the SWID SoftwareIdentity
   * element. If not specified, defaults to {@code false}.
   * <li>The qualifier {@code tag_creator_name} is optional. If the tag creator is different from the software creator,
   * the tag_creator_name qualifier should be specified.
   * <li>The qualifier {@code tag_creator_regid} is optional. If the tag creator is different from the software creator,
   * the tag_creator_regid qualifier should be specified.
   * </ul>
   */
  public static PackageUrlIdentifier buildPackageUrlFromSwid(final Swid swid) {
    if (swid == null) {
      return null;
    }

    try {
      PackageURLBuilder packageURLBuilder = PackageURLBuilder.aPackageURL();
      packageURLBuilder
          .withType("swid")
          .withName(ThirdPartyScanResultUtils.getTruncatedName(swid.getName()))
          .withVersion(ThirdPartyScanResultUtils.getTruncatedVersion(swid.getVersion()))
          .withQualifier("tag_id", swid.getTagId());

      // 1.6 requires checking for nulls as it returns now objects and not primitives int
      if (swid.getTagVersion() != null &&  swid.getTagVersion() != 0) {
        packageURLBuilder.withQualifier("tag_version", String.valueOf(swid.getTagVersion()));
      }
      // 1.6 requires checking for nulls as it returns now objects and not primitives boolean
      if (swid.isPatch() != null && swid.isPatch()) {
        packageURLBuilder.withQualifier("patch", "true");
      }
      processAttachmentText(packageURLBuilder, swid.getAttachmentText());

      PackageURL packageUrl = packageURLBuilder.build();
      return new PackageUrlIdentifier(packageUrl.canonicalize());
    }
    catch (MalformedPackageURLException e) {
      throw new InvalidPackageURLException(e.getMessage(), e);
    }
  }

  private static void processAttachmentText(
      final PackageURLBuilder packageURLBuilder,
      final AttachmentText attachmentText)
  {
    if (attachmentText == null) {
      return;
    }
    Pair<Node, Node> pair = extractEntityNodes(attachmentText);
    if (pair == null) {
      return;
    }
    String namespace = null;
    String softwareCreatorName = null;
    String softwareCreatorRegid = null;

    Node softwareCreatorNode = pair.getLeft();
    if (softwareCreatorNode != null) {
      softwareCreatorName = extractAttribute(softwareCreatorNode, "name");
      if (StringUtils.isNotBlank(softwareCreatorName)) {
        namespace = softwareCreatorName;
      }
      softwareCreatorRegid = extractAttribute(softwareCreatorNode, "regid");
      if (StringUtils.isNotBlank(softwareCreatorRegid)) {
        namespace = namespace == null ? softwareCreatorRegid : namespace + "/" + softwareCreatorRegid;
      }
      if (namespace != null) {
        packageURLBuilder.withNamespace(namespace);
      }
    }
    Node tagCreatorNode = pair.getRight();
    if (tagCreatorNode != null) {
      String tagCreatorName = extractAttribute(tagCreatorNode, "name");
      if (StringUtils.isNotBlank(tagCreatorName)) {
        if (softwareCreatorName == null || !softwareCreatorName.equals(tagCreatorName)) {
          packageURLBuilder.withQualifier("tag_creator_name", tagCreatorName);
        }
      }
      String tagCreatorRegid = extractAttribute(tagCreatorNode, "regid");
      if (StringUtils.isNotBlank(tagCreatorRegid)) {
        if (softwareCreatorRegid == null || !softwareCreatorRegid.equals(tagCreatorRegid)) {
          packageURLBuilder.withQualifier("tag_creator_regid", tagCreatorRegid);
        }
      }
    }
  }

  private static Pair<Node, Node> extractEntityNodes(AttachmentText attachmentText) {
    if (!"base64".equals(attachmentText.getEncoding())) {
      log.info("Invalid SWID text encoding: {}", attachmentText.getEncoding());
      return null;
    }
    String text = attachmentText.getText();
    if (StringUtils.isEmpty(text)) {
      log.info("Empty SWID text content");
      return null;
    }

    try {
      byte[] decodedXml = Base64.getDecoder().decode(text.getBytes());

      DocumentBuilder builder = getDocumentBuilderFactory().newDocumentBuilder();
      try (InputStream is = new ByteArrayInputStream(decodedXml)) {
        Document xmlDocument = builder.parse(is);
        Node softwareCreatorNode = extractEntity(xmlDocument, "softwareCreator");
        Node tagCreatorNode = extractEntity(xmlDocument, "tagCreator");
        return Pair.of(softwareCreatorNode, tagCreatorNode);
      }
    }
    catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
      log.debug("Cannot parse SWID text due to: {}", e.getMessage());
    }
    return null;
  }

  private static Node extractEntity(final Document xmlDocument, final String role) throws XPathExpressionException {
    XPath xPath = XPathFactory.newInstance().newXPath();
    String expression = "/SoftwareIdentity/Entity[@role='" + role + "']";
    return (Node) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODE);
  }

  private static String extractAttribute(final Node node, final String attributeName) {
    NamedNodeMap attributes = node.getAttributes();
    if (attributes == null) {
      return null;
    }
    Node namedItem = attributes.getNamedItem(attributeName);
    return namedItem == null ? null : namedItem.getNodeValue();
  }

  /**
   * Gets or constructs a new document builder with security features enabled to prevent
   * XML External Entity (XXE) attacks.
   */
  private static DocumentBuilderFactory getDocumentBuilderFactory() throws ParserConfigurationException {
    if (documentBuilderFactory == null) {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      documentBuilderFactory = factory;
    }
    return documentBuilderFactory;
  }
}
