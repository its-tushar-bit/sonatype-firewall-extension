/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.nio.CharBuffer;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

import jakarta.inject.Named;

import static org.apache.commons.text.WordUtils.capitalizeFully;

/*
 * Project names and IDs can only use a small subset of characters: a-z A-Z - _
 * This causes problems during bulk imports from external applications.
 * This class contains utility functions to strip or replace invalid characters.
 */
@Named
public class ApplicationNameConverter
{
  /**
   * Splits a project name with 'words' separated by - or _ into words separated by ' ' and capitalizes words
   * for better readability when displayed in the UI.
   */
  public String toReadableName(final String name) {
    return capitalizeFully(mergeMultipleSpaceCharacters(mapCharacters(name, this::mapWhitespaceLikeCharacterToSpace)));
  }

  /**
   * Removes characters not allowed in names and removes duplicate space.
   */
  public String toName(String name) {
    return mergeMultipleSpaceCharacters(filterCharacters(name, this::isAllowedCharacterInName));
  }

  /**
   * Removes characters not allowed in public IDs.
   */
  public String toPublicId(String publicId) {
    return filterCharacters(publicId, this::isAllowedCharacterInPublicId);
  }

  private String filterCharacters(final String string, final IntPredicate codePointFilter) {
    return newString(CharBuffer.wrap(string).chars().filter(codePointFilter).toArray());
  }

  private String mapCharacters(final String string, final IntUnaryOperator mapper) {
    return newString(CharBuffer.wrap(string).chars().map(mapper).toArray());
  }

  private String newString(final int[] codePoints) {
    return new String(codePoints, 0, codePoints.length);
  }

  private String mergeMultipleSpaceCharacters(final String name) {
    return name.replaceAll(" +", " ").trim();
  }

  private boolean isAllowedCharacterInName(final int codePoint) {
    return Character.isLetterOrDigit(codePoint) || "-._ ".indexOf(codePoint) >= 0;
  }

  private boolean isAllowedCharacterInPublicId(final int codePoint) {
    return Character.isLetterOrDigit(codePoint) || "-._".indexOf(codePoint) >= 0;
  }

  private int mapWhitespaceLikeCharacterToSpace(final int codePoint) {
    return "-_".indexOf(codePoint) >= 0 ? ' ' : codePoint;
  }
}
