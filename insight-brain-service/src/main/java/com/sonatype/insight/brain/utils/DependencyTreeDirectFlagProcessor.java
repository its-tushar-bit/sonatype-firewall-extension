/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeNodeDTO;

/**
 * Utility class for processing dependency tree structures to populate direct/transitive dependency flags.
 * <p>
 * This class provides functionality similar to the frontend's DependencyInfoGenerator.js, determining whether
 * dependencies are direct (immediate children of root) or transitive (descendants of direct dependencies) based on the
 * tree structure.
 * <p>
 * This is used when scanners don't provide correct direct dependency flags, allowing the system to infer this
 * information from the hierarchical structure.
 */
public final class DependencyTreeDirectFlagProcessor
{
  private DependencyTreeDirectFlagProcessor() {
    // Utility class - prevent instantiation
  }

  /**
   * Populate 'direct' flags in dependency tree based on tree structure.
   * <p>
   * Direct dependencies are immediate children of the root (depth 1). Transitive dependencies are at depth 2 or
   * greater.
   * <p>
   * This matches the frontend logic in DependencyInfoGenerator.js
   *
   * @param root The root node of the dependency tree
   */
  public static void populateDirectFlags(ApiDependencyTreeNodeDTO root) {
    if (root == null || root.getChildren() == null) {
      return;
    }

    // First level children are direct dependencies
    for (ApiDependencyTreeNodeDTO directDep : root.getChildren()) {
      directDep.setDirect(true);
      // All descendants of direct dependencies are transitive
      markChildrenAsTransitive(directDep);
    }
  }

  private static void markChildrenAsTransitive(ApiDependencyTreeNodeDTO node) {
    if (node == null || node.getChildren() == null) {
      return;
    }

    for (ApiDependencyTreeNodeDTO child : node.getChildren()) {
      child.setDirect(false);
      markChildrenAsTransitive(child);
    }
  }
}
