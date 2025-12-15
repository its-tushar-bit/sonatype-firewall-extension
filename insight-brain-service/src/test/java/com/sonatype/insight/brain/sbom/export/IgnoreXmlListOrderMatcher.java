/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.export;

import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;

/**
 * Makes the tests more resilient by ignoring the order of List elements in the XML
 */
public class IgnoreXmlListOrderMatcher
    extends DefaultNodeMatcher
{
  public IgnoreXmlListOrderMatcher() {
    super(ElementSelectors.conditionalBuilder()
        // externalRefs: match by referenceLocator (or switch to ./comment if you prefer)
        .whenElementIsNamed("externalRefs")
        .thenUse(ElementSelectors.and(
            ElementSelectors.byName,
            ElementSelectors.byXPath("./referenceLocator", ElementSelectors.byNameAndText)
        ))
        // relationships: match by the tuple (spdxElementId, relationshipType, relatedSpdxElement)
        .whenElementIsNamed("relationships")
        .thenUse(ElementSelectors.and(
            ElementSelectors.byName,
            ElementSelectors.byXPath("./spdxElementId", ElementSelectors.byNameAndText),
            ElementSelectors.byXPath("./relationshipType", ElementSelectors.byNameAndText),
            ElementSelectors.byXPath("./relatedSpdxElement", ElementSelectors.byNameAndText)
        ))
        // default: name-based matching
        .elseUse(ElementSelectors.byName)
        .build()
    );
  }
}
