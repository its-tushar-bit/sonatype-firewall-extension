/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Set;

/**
 * Constants for component format types and their identification characteristics.
 *
 * <p>Package formats are categorized into two types based on how components are identified:
 * <ul>
 * <li><b>Coordinate-based formats</b>: Identify components by coordinates (name+version) rather than file hash.
 *     These formats can have synthetic hash generated when hash is not provided.</li>
 * <li><b>Hash-based formats</b>: Identify components by file content hash. These formats require actual file
 *     hashes for proper identification and cannot use synthetic hashes.</li>
 * </ul>
 *
 * @see <a href="https://sonatype.atlassian.net/browse/NEXUS-49174">NEXUS-49174</a>
 */
public final class ComponentFormatConstants
{
  /**
   * Coordinate-based package formats that identify components by coordinates (name+version)
   * rather than file hash. These formats can have synthetic hash generated when hash is not provided.
   *
   * <p>For these formats:
   * <ul>
   * <li>Components are identified by their coordinates (name + version)</li>
   * <li>Hash is optional when using packageUrl in Firewall Evaluate API</li>
   * <li>If hash is not provided, a synthetic hash is generated for HDS validation</li>
   * <li>The synthetic hash satisfies validation but is not used for component lookup</li>
   * </ul>
   *
   * <p>Hash-based formats (maven, npm, pypi, nuget, docker, rubygems) are NOT in this set
   * because they require actual file hashes for proper identification.
   */
  public static final Set<String> COORDINATE_BASED_FORMATS = Set.of(
      "golang",
      "conan",
      "cargo",
      "cocoapods",
      "cran",
      "conda",
      "composer",
      "hf-model"
  );

  /**
   * Checks if the given format is a coordinate-based format that identifies components
   * by coordinates rather than file hash.
   *
   * @param format the package format (e.g., "golang", "maven", "npm")
   * @return true if coordinate-based format, false if hash-based format or format is null
   */
  public static boolean isCoordinateBasedFormat(String format) {
    return format != null && COORDINATE_BASED_FORMATS.contains(format.toLowerCase());
  }

  private ComponentFormatConstants() {
    // Utility class, prevent instantiation
  }
}
