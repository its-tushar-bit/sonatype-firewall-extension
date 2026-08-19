/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This interface represents a component's reachable vulnerabilities (if any).
 * <ul>
 * <li>Missing reachable vulnerabilities means we have no analysis and reachability is unknown.</li>
 * <li>Present reachable vulnerabilities means we have an analysis and reachability is detected or not.</li>
 * </ul>
 * If reachable vulnerabilities are present, that means we've determined the component is vulnerable to the
 * given vulnerabilities via certain paths.
 */
public sealed interface ReachableComponentVulnerabilities
{
  /**
   * Combines this ReachableComponentVulnerabilities with another one.
   *
   * @param other the other ReachableComponentVulnerabilities to combine with.
   * @return a new ReachableComponentVulnerabilities that is the combination of this and other.
   */
  ReachableComponentVulnerabilities combine(ReachableComponentVulnerabilities other);

  /**
   * Represents the case where a component has missing reachable vulnerability references (i.e., reachability for any
   * vulnerability is unknown).
   */
  enum MissingReachableComponentVulnerabilities
      implements
      ReachableComponentVulnerabilities
  {
    INSTANCE;

    @Override
    public ReachableComponentVulnerabilities combine(final ReachableComponentVulnerabilities other) {
      if (other == null || other instanceof MissingReachableComponentVulnerabilities) {
        return this;
      }
      return new PresentReachableComponentVulnerabilities(
          ((PresentReachableComponentVulnerabilities) other).references);
    }
  }

  /**
   * Represents the case where a component has present vulnerability references (i.e., reachability for a particular
   * vulnerability is detected or not detected).
   */
  record PresentReachableComponentVulnerabilities(Set<String> references)
      implements ReachableComponentVulnerabilities
  {
    public PresentReachableComponentVulnerabilities(final Set<String> references) {
      this.references = new HashSet<>(Objects.requireNonNull(references));
    }

    @Override
    public Set<String> references() {
      return Collections.unmodifiableSet(references);
    }

    @Override
    public ReachableComponentVulnerabilities combine(final ReachableComponentVulnerabilities other) {
      if (other == null || other instanceof MissingReachableComponentVulnerabilities) {
        return new PresentReachableComponentVulnerabilities(references);
      }
      var combined = new PresentReachableComponentVulnerabilities(references);
      combined.references.addAll(((PresentReachableComponentVulnerabilities) other).references);
      return combined;
    }
  }
}
