/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard;

public class GavDTO
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