/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.Comparator;

import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.InvalidVersionSpecificationException;
import org.sonatype.aether.version.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compares policy violations by threat level (descending), policy name, application name, coordinates, and then
 * pathnames.
 * 
 * @since 1.11.0
 */
public class PolicyViolationDTOComparator
    implements Comparator<PolicyViolationDTO>
{

  private static final Logger log = LoggerFactory.getLogger(PolicyViolationDTOComparator.class);

  private static final GenericVersionScheme VERSION_SCHEME = new GenericVersionScheme();

  @Override
  public int compare(PolicyViolationDTO v1, PolicyViolationDTO v2) {
    int result = v2.threatLevel - v1.threatLevel;
    if (result != 0) {
      return result;
    }

    result = v1.policyName.compareToIgnoreCase(v2.policyName);
    if (result != 0) {
      return result;
    }

    result = v1.applicationName.compareToIgnoreCase(v2.applicationName);
    if (result != 0) {
      return result;
    }

    result = compareCoordinates(v1, v2);
    if (result != 0) {
      return result;
    }

    result = nullCheck(v1.hash, v2.hash);
    if (result != 0) {
      return result;
    }
    return v1.hash.compareToIgnoreCase(v2.hash);
  }

  private int compareCoordinates(PolicyViolationDTO v1, PolicyViolationDTO v2) {
    int result = 0;

    result = nullCheck(v1.groupId, v2.groupId);
    if (result != 0) {
      return result;
    }
    else if (v1.groupId != null && v2.groupId != null) {
      result = v1.groupId.compareToIgnoreCase(v2.groupId);
      if (result != 0) {
        return result;
      }
    }

    result = nullCheck(v1.artifactId, v2.artifactId);
    if (result != 0) {
      return result;
    }
    else if (v1.artifactId != null && v2.artifactId != null) {
      result = v1.artifactId.compareToIgnoreCase(v2.artifactId);
      if (result != 0) {
        return result;
      }
    }

    result = nullCheck(v1.version, v2.version);
    if (result != 0) {
      return result;
    }
    else if (v1.version != null && v2.version != null) {
      try {
        Version parsedVersion1 = VERSION_SCHEME.parseVersion(v1.version);
        Version parsedVersion2 = VERSION_SCHEME.parseVersion(v2.version);
        return parsedVersion1.compareTo(parsedVersion2);
      }
      catch (InvalidVersionSpecificationException e) {
        log.error(
            "Unable to parse policy violation versions for policy violations with IDs {} {} and versions {} {}, defaulting to string comparison.",
            v1.id, v2.id, v1.version, v2.version, e);
      }
      return v1.version.compareToIgnoreCase(v2.version);
    }

    return result;
  }

  /**
   * <p>
   * Null objects should be treated as infinitely large.
   * </p>
   * 
   * @return 1 if o1 is not null while o2 is, or -1 if o2 is not null and o1 is. 0 if both objects are either null or
   *         not null.
   */
  private int nullCheck(Object o1, Object o2) {
    if (o1 == null && o2 != null) {
      return 1;
    }
    else if (o1 != null && o2 == null) {
      return -1;
    }

    return 0;
  }
}
