/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiDependencyTreeNodeDTO;

public class ApiDependencyTreeSearcher
{
  private boolean isDirectNode;

  /**
   * Finds all direct parent nodes of a given target identifier.
   *
   * @param root the root of the dependency tree.
   * @param targetIdentifier the component identifier to search for.
   * @return a set of direct parent nodes.
   */
  public Set<ApiDependencyTreeNodeDTO> findAllDirectParents(
      ApiDependencyTreeNodeDTO root,
      ApiComponentIdentifierDTOV2 targetIdentifier)
  {
    Set<ApiDependencyTreeNodeDTO> directParents = new HashSet<>();
    if (root == null || targetIdentifier == null) {
      return directParents;
    }

    // Start searching from the second level (children of the root)
    if (root.getChildren() != null) {
      for (ApiDependencyTreeNodeDTO child : root.getChildren()) {
        findDirectParentsRecursive(child, targetIdentifier, new ArrayDeque<>(), directParents);
        if (isDirectNode) {
          break;
        }
      }
    }

    return directParents;
  }

  /**
   * Recursively searches for direct parent nodes of a given target identifier.
   *
   * @param currentNode the current node in the traversal.
   * @param targetIdentifier the component identifier to search for.
   * @param path the path from the root to the current node.
   * @param directParents the set to collect direct parent nodes.
   */
  private void findDirectParentsRecursive(
      ApiDependencyTreeNodeDTO currentNode,
      ApiComponentIdentifierDTOV2 targetIdentifier,
      Deque<ApiDependencyTreeNodeDTO> path,
      Set<ApiDependencyTreeNodeDTO> directParents)
  {
    path.addLast(currentNode);

    // Check if the current node matches the target identifier
    if (currentNode.getComponentIdentifier().equals(targetIdentifier)) {
      if (currentNode.isDirect()) {
        isDirectNode = true;
        return; // the given node itself is direct, stop here
      }

      // Traverse the path in reverse to find the first direct parent
      Iterator<ApiDependencyTreeNodeDTO> iterator = path.descendingIterator();
      // Skip the current node (target identifier node)
      iterator.next();
      while (iterator.hasNext()) {
        ApiDependencyTreeNodeDTO parent = iterator.next();
        if (parent.isDirect()) {
          directParents.add(parent);
          break;
        }
      }
      return;
    }

    // Recursively search in the children
    if (currentNode.getChildren() != null) {
      for (ApiDependencyTreeNodeDTO child : currentNode.getChildren()) {
        findDirectParentsRecursive(child, targetIdentifier, path, directParents);
        if (isDirectNode) {
          break; // Stop further searches if the target node itself is direct
        }
      }
    }

    path.removeLast();
  }

  public boolean isDirectNode() {
    return isDirectNode;
  }
}
