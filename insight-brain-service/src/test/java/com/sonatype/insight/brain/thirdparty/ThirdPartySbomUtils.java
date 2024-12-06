/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.JsonParser;
import org.cyclonedx.parsers.Parser;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xmlunit.util.Predicate;

import java.io.StringReader;

public class ThirdPartySbomUtils
{
  public static Predicate<Node> getSonatypeIdentifierNodeFilter() {
    return node -> {
      NamedNodeMap attributes = node.getAttributes();
      if (attributes == null) {
        return true;
      }
      Node attr = attributes.getNamedItem("name");
      if (attr == null) {
        return true;
      }
      if ("sonatypeIdentifier".equals(attr.getNodeValue())) {
        return false;
      }
      return true;
    };
  }

  public static Bom getFilteredBom(String content) throws ParseException {
    Parser parser = new JsonParser();
    return parser.parse(new StringReader(content));
  }
}
