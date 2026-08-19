/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.Locale;
import java.util.Map.Entry;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import org.apache.commons.lang3.StringUtils;

/**
 * Copied from com.sonatype.nexus.procurement.ArtifactCoordinate.
 * <p>
 * The coordinates/conditions used to address one artifact. If we want to expand the rules, this is the place to do so.
 * The coordinates may be "fixed" (isFixed() returns TRUE), or "wildcarded". Examples (presented as G:A:V triplets):
 *
 * <pre>
 * org.sonatype.nexus : nexus-indexer : 1.0 - is a fixed coordinate that points exactly to what is says
 * org.sonatype* : nexus-indexer : 1.* - is a wildcard coordinate, that points to (inclusive) group 'org.sonatype' and below (ie. 'org.sonatypefoo' or  'org.sonatype.blah'), and artifact named named 'nexus-indexer' in these groups, and any version that starts with '1.'
 * org.sonatype.* : nexus-indexer : 1.0 - is a wildcard coordinate, that is like the above one, except it matches this group and its subgroups ONLY ('org.sonatypefoo' is NOT matched), and matches for version '1.0' only.
 * </pre>
 *
 * The other fields (A, V) also are able to make use of '*' (wildcard), but it will be interpreted obly as "starts with"
 * (ie. in field A, 'nexus*' will be inerpreted as artifactId.startsWith("nexus"). Same stands for V field). In case of
 * <b>groups</b> (G field), the things are a little different: blah* means group starts with 'blah' (hence, and and
 * below), blah.* means only group 'blah' and groups below 'blah' like 'blah.foo', but not 'blahfoo.foo'.
 * <p>
 * An ArtifactCoordinate is matchable only against "fixed" coordinate, hence, two wildarcded coordinate cannot be
 * matched.
 *
 * @author cstamas
 */
public class ArtifactCoordinate
{
  public static final String PLACEHOLDER = "*";

  private final ComponentIdentifier componentIdentifier;

  /**
   * Constructs an ArtifactCoordinate.
   */
  public ArtifactCoordinate(ComponentIdentifier componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
  }

  public boolean matches(final ComponentIdentifier otherComponentIdentifier) {
    if (otherComponentIdentifier == null) {
      return false;
    }

    if (!componentIdentifier.getFormat().equals(otherComponentIdentifier.getFormat())) {
      return false;
    }

    final boolean ignoreCase = !otherComponentIdentifier.isCaseSensitive();

    if (otherComponentIdentifier.isMaven()) {
      return matchesGroup(componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID),
          otherComponentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID))
          && matches(componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID),
              otherComponentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID), ignoreCase)
          && matches(componentIdentifier.get(ComponentIdentifier.VERSION),
              otherComponentIdentifier.get(ComponentIdentifier.VERSION), ignoreCase)
          && matches(componentIdentifier.get(ComponentIdentifier.MAVEN_EXTENSION),
              otherComponentIdentifier.get(ComponentIdentifier.MAVEN_EXTENSION), ignoreCase)
          && matches(
              componentIdentifier.get(ComponentIdentifier.MAVEN_CLASSIFIER),
              otherComponentIdentifier.get(ComponentIdentifier.MAVEN_CLASSIFIER), ignoreCase);
    }

    for (Entry<String, String> coord : componentIdentifier.getCoordinates().entrySet()) {
      String name = coord.getKey();
      String value = coord.getValue();
      String value2 = otherComponentIdentifier.getCoordinates().get(name);
      if (!matches(value, value2, ignoreCase)) {
        return false;
      }
    }

    return true;
  }

  public ComponentIdentifier getComponentIdentifier() {
    return componentIdentifier;
  }

  /**
   * A utility method that handles group coordinates as matchable target. The meaning of them are:
   *
   * <pre>
   * * - matches all
   * some.value - matches exactly 'some.value'
   * some.value* - matches by prefix, so 'some.value', 'some.value.more' are all ok
   * some.value.* - matches only subgroups, so 'some.value.more1', 'some.value.more2' is ok only
   * </pre>
   *
   * @param coordinate
   * @param value
   * @return
   */
  private boolean matchesGroup(String coordinate, String value) {
    if (StringUtils.isBlank(coordinate)) {
      // coordinate empty, only empty value matches
      return StringUtils.isBlank(value);
    }
    if (PLACEHOLDER.equals(coordinate)) {
      // coordinate wildcard, it matches all
      return true;
    }
    else if (StringUtils.isBlank(value)) {
      // coordinate not empty and not wildcard, value empty, no match
      return false;
    }
    else if (coordinate.endsWith("." + PLACEHOLDER)) {
      if (value.length() <= coordinate.length() - 2) {
        // coordinate ends with a joker, matches if it is prefix of value
        return value.startsWith(coordinate.substring(0, coordinate.length() - 2));
      }
      else {
        // coordinate ends with a joker, matches if it is prefix of value
        return value.startsWith(coordWithoutPlaceholder(coordinate));
      }
    }
    else if (coordinate.endsWith(PLACEHOLDER)) {
      // coordinate ends with a joker, matches if it is prefix of value
      return value.startsWith(coordWithoutPlaceholder(coordinate));
    }
    else {
      // coordinate has no joker, matches if equals
      return coordinate.equals(value);
    }
  }

  /**
   * A utility method that handles A and V coordinates as matchable target. These are handled a bit differently that G
   * coordinates! The meaning of them are:
   *
   * <pre>
   * null/empty - matches null/empty
   * * - matches all
   * some.value - matches exactly 'some.value'
   * somevalue* - matches by prefix just before the '*'
   * </pre>
   *
   * @param coordinate
   * @param value
   * @param ignoreCase
   * @return
   */
  private boolean matches(String coordinate, String value, final boolean ignoreCase) {
    if (StringUtils.isBlank(coordinate)) {
      // coordinate empty, only empty value matches
      return StringUtils.isBlank(value);
    }
    else if (PLACEHOLDER.equals(coordinate)) {
      // coordinate wildcard, it matches all
      return true;
    }
    else if (StringUtils.isBlank(value)) {
      // coordinate not empty and not wildcard, value empty, no match
      return false;
    }
    else if (coordinate.endsWith(PLACEHOLDER)) {
      // coordinate ends with a joker, matches if it is prefix of value
      if (ignoreCase) {
        return value.toLowerCase(Locale.ROOT).startsWith(coordWithoutPlaceholder(coordinate).toLowerCase(Locale.ROOT));
      }
      return value.startsWith(coordWithoutPlaceholder(coordinate));
    }
    else {
      // coordinate has no joker, matches if equals
      if (ignoreCase) {
        return coordinate.equalsIgnoreCase(value);
      }
      return coordinate.equals(value);
    }
  }

  private String coordWithoutPlaceholder(final String coordinate) {
    return coordinate.substring(0, coordinate.length() - 1);
  }

  // Object

  @Override
  public String toString() {
    return String.valueOf(componentIdentifier);
  }
}
