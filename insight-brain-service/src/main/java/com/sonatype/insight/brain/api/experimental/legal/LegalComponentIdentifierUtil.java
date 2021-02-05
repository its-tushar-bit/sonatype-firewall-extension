/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental.legal;

import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

public class LegalComponentIdentifierUtil
{
  private LegalComponentIdentifierUtil() {
  }

  public static ComponentIdentifier removeClassifierAndExtension(ComponentIdentifier componentIdentifier) {
    TreeMap<String, String> coordinates = new TreeMap<>(componentIdentifier.getCoordinates());
    coordinates.remove(ComponentIdentifier.MAVEN_CLASSIFIER);
    coordinates.remove(ComponentIdentifier.MAVEN_EXTENSION);
    return new ComponentIdentifier(componentIdentifier.getFormat(), coordinates);
  }
}
