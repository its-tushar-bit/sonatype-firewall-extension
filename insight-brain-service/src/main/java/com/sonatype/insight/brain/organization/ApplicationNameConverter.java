/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import javax.inject.Named;

import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;

/*
 * Project names and IDs can only use a small subset of characters: a-z A-Z - _
 * This causes problems during bulk imports from external applications.
 * This class contains utility functions to strip or replace invalid characters.
 */
@Named
public class ApplicationNameConverter
{
  private static final String[] UMLAUTE = new String[] {"Ä", "Ö", "Ü", "ä", "ö", "ü", "ß"};

  private static final String[] UMLAUTE_REPLACEMENT = new String[] {"Ae", "Oe", "Ue", "ae", "oe", "ue", "ss"};

  public String toReadableName(String name) {
    return WordUtils.capitalizeFully(name.replaceAll("[^\\w]+", " ").trim());
  }

  private String stripAccents(String text) {
    return StringUtils.stripAccents(StringUtils.replaceEach(text, UMLAUTE, UMLAUTE_REPLACEMENT));
  }

  private String removeCharactersNotAllowedInName(String name) {
    return name.replaceAll("[^\\w- ]+", "").replaceAll(" +", " ").trim();
  }

  private String removeCharactersNotAllowedInPublicId(String publicId) {
    return publicId.replaceAll("[^\\w-]+", "");
  }

  public String toName(String name) {
    return removeCharactersNotAllowedInName(stripAccents(name));
  }

  public String toPublicId(String publicId) {
    return removeCharactersNotAllowedInPublicId(stripAccents(publicId));
  }
}
