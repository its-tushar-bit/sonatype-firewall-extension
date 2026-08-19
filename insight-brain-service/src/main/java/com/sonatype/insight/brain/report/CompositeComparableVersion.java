/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.semver4j.Semver;

public class CompositeComparableVersion
    implements Comparable<CompositeComparableVersion>
{
  /**
   * This regex matches any string that ends with 'snapshot' preceded by dots or dashes.
   */
  private static final String SNAPSHOT_SUFFIX_REGEX = ".*[.-]*SNAPSHOT$";

  private final String version;

  private final Semver semanticVersion;

  private final ComparableVersion genericVersion;

  public static CompositeComparableVersion fromGenericVersion(@Nullable final String version) {
    return new CompositeComparableVersion(version, false);
  }

  public static CompositeComparableVersion fromSemanticVersion(@Nullable final String version) {
    return new CompositeComparableVersion(version, true);
  }

  private CompositeComparableVersion(@Nullable final String version, final boolean isSemanticVersion) {
    if (version != null) {
      this.version = version.trim().replaceFirst("^v", "");
    }
    else {
      this.version = null;
    }
    if (isSemanticVersion) {
      this.semanticVersion = parseSemver(version == null ? "" : version);
    }
    else {
      this.semanticVersion = null;
    }
    this.genericVersion = new ComparableVersion(version == null ? "" : version);
  }

  private Semver parseSemver(String version) {
    try {
      return new Semver(version);
    }
    catch (Exception e) {
      return null;
    }
  }

  /**
   * Compares two CompositeComparableVersion objects. If both versions are semantic versions, it uses the
   * {@link org.semver4j.Semver} library to compare them. If either version is not a semantic version, it falls back to
   * using {@link org.apache.maven.artifact.versioning.ComparableVersion} for comparison.
   *
   * @param other the version to be compared.
   * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the
   *         specified version.
   */
  @Override
  public int compareTo(@NotNull final CompositeComparableVersion other) {
    if (this.semanticVersion != null && other.semanticVersion != null) {
      return this.semanticVersion.compareTo(other.semanticVersion);
    }
    return this.genericVersion.compareTo(other.genericVersion);
  }

  public boolean isEqualTo(@NotNull final CompositeComparableVersion other) {
    return this.compareTo(other) == 0;
  }

  public boolean isGreaterThan(@NotNull final CompositeComparableVersion other) {
    return this.compareTo(other) > 0;
  }

  public boolean isLowerThan(@NotNull final CompositeComparableVersion other) {
    return this.compareTo(other) < 0;
  }

  public boolean isSemanticVersion() {
    return semanticVersion != null;
  }

  /**
   * Determines whether the given next version represents a major update relative to the current version.
   * <p>
   * For semantic versions, it compares the major version components directly. If either version is non-semantic, it
   * falls back to a best-effort comparison by extracting the first numeric component before any non-numeric character.
   * </p>
   *
   * @param nextVersion the version to compare against the current version.
   * @return {@code true} if the next version's major version appears greater than that of the current version.
   *         {@code null} if the major version cannot be reliably determined from either version string.
   */
  public Boolean isMajorJump(@NotNull final CompositeComparableVersion nextVersion) {
    if (this.isSemanticVersion() && nextVersion.isSemanticVersion()) {
      return nextVersion.semanticVersion.getMajor() > this.semanticVersion.getMajor();
    }

    Integer thisMajor = extractLeadingNumber(this.version);
    Integer nextMajor = extractLeadingNumber(nextVersion.version);

    // If either major number is missing, default to null
    if (thisMajor == null || nextMajor == null) {
      return null;
    }

    return nextMajor > thisMajor;
  }

  private Integer extractLeadingNumber(String version) {
    if (StringUtils.isBlank(version)) {
      return null;
    }
    try {
      String[] parts = version.split("[^0-9]+");
      for (String part : parts) {
        if (!part.isEmpty()) {
          return Integer.parseInt(part);
        }
      }
    }
    catch (NumberFormatException ignored) {
      return null;
    }
    return null;
  }

  /**
   * Determines whether the version string represents a pre-release version.
   * <p>
   * A version is considered a pre-release if it follows semantic versioning and includes a pre-release segment (e.g.,
   * {@code 1.0.0-alpha}, {@code 2.1.3-beta.2}).
   * For non-semantic versions, it checks if the version is a SNAPSHOT version, which is treated as a pre-release.
   * </p>
   *
   * @return {@code true} if it is a pre-release version.
   *         {@code null} if it cannot be reliably determined from the version string.
   */
  public Boolean isPreRelease() {
    if (this.isSemanticVersion()) {
      return !CollectionUtils.isEmpty(semanticVersion.getPreRelease());
    }
    return isSnapshot() == null ? null : isSnapshot();
  }

  /**
   * Determines whether the version string represents a snapshot version.
   * <p>
   * A snapshot version is identified by the suffix {@code "SNAPSHOT"}, case-sensitive, and preceded by a dot (.) or
   * dash (-).
   * </p>
   *
   * @return {@code true} if the version is a snapshot.
   *         {@code null} if the version is not defined or cannot be determined.
   */
  private Boolean isSnapshot() {
    if (StringUtils.isBlank(this.version)) {
      return null;
    }
    return Pattern.matches(SNAPSHOT_SUFFIX_REGEX, this.version);
  }
}
