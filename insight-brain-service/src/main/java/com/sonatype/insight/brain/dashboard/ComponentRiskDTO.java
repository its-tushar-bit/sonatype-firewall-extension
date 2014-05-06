/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Carries the data backing the "Highest Risk Component View", i.e. roll-up of violations by component.
 */
public class ComponentRiskDTO
{

  public String hash;

  public int score;

  public Set<GavDTO> gavs = new HashSet<>();

  public Set<String> affectedApplicationNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

  public Set<String> violatedPolicyNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

  public static class GavDTO
  {
    public String groupId;

    public String artifactId;

    public String version;

    public GavDTO() {
    }

    public GavDTO(String groupId, String artifactId, String version) {
      this.groupId = groupId;
      this.artifactId = artifactId;
      this.version = version;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || !getClass().equals(obj.getClass())) {
        return false;
      }
      GavDTO that = (GavDTO) obj;
      return eq(artifactId, that.artifactId) && eq(version, that.version) && eq(groupId, that.groupId);
    }

    private static <T> boolean eq(T obj1, T obj2) {
      return (obj1 == null) ? obj2 == null : obj1.equals(obj2);
    }

    @Override
    public int hashCode() {
      int result = 17;
      result = 31 * result + hash(artifactId);
      result = 31 * result + hash(groupId);
      result = 31 * result + hash(version);
      return result;
    }

    private static int hash(Object obj) {
      return (obj == null) ? 0 : obj.hashCode();
    }

    @Override
    public String toString() {
      return groupId + ':' + artifactId + ':' + version;
    }
  }
}
