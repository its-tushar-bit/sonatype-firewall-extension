/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.dependency.DependencyNode;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.List;

public class DependencyTreeParser
{
  private static final Logger log = LoggerFactory.getLogger(DependencyTreeParser.class);

  public static final int MAX_RECURSION_DEPTH = 100000;

  private final Map<String, Set<String>> dependencyTree = new HashMap<>();

  private final Map<ComponentIdentifier, String> dependencyType = new HashMap<>();

  public void parse(JsonNode dependenciesJsonData) throws IOException {
    if (dependenciesJsonData == null) {
      return;
    }
    JsonNode dependencyTreeNode = dependenciesJsonData.path("dependencyTree");
    if (!dependencyTreeNode.isMissingNode()) {
      DependencyNode tree = JsonUtils.asPojo(dependencyTreeNode, DependencyNode.class);
      if (tree != null) {
        walkDependencyTreeAndSaveComponentDependencyData(Collections.singletonList(tree), 0);
      }
    }
  }

  private void walkDependencyTreeAndSaveComponentDependencyData(
      List<DependencyNode> children,
      int recursionDepth)
  {
    for (DependencyNode child : children) {
      if (child.getComponentIdentifier() != null) {
        dependencyType.putIfAbsent(child.getComponentIdentifier(), child.isDirect() ? "D" : "T");
      }
      PackageUrlIdentifier purl = PackageUrlIdentifier.fromComponentIdentifier(child.getComponentIdentifier());
      if (purl != null) {
        this.dependencyTree.putIfAbsent(purl.getPackageUrl(), new HashSet<>(child.getChildren()
            .stream()
            .map(it -> PackageUrlIdentifier.fromComponentIdentifier(it.getComponentIdentifier()))
            .filter(Objects::nonNull)
            .map(PackageUrlIdentifier::getPackageUrl)
            .toList()));
      }
      if (++recursionDepth <= MAX_RECURSION_DEPTH) {
        walkDependencyTreeAndSaveComponentDependencyData(child.getChildren(), recursionDepth);
      }
      else {
        log.warn("Dependency tree depth exceeded {}, skipping child dependencies", recursionDepth);
      }
    }
  }

  public Optional<String> getDependencyType(ComponentIdentifier componentIdentifier) {
    String dependencyTypeSymbol = dependencyType.get(componentIdentifier);
    if (dependencyTypeSymbol != null) {
      return Optional.of(dependencyTypeSymbol);
    }
    return Optional.empty();
  }

  public Optional<String> getDependencyType(String purl) {
    try {
      ComponentIdentifier componentIdentifier = new PackageUrlIdentifier(purl).ensureCompleteIdentifier();
      return getDependencyType(componentIdentifier);
    }
    catch (InvalidPackageURLException e) {
      return Optional.empty();
    }
  }

  public Optional<Set<String>> getComponentDependencies(String purl) {
    Set<String> dependencies = dependencyTree.get(purl);
    if (dependencies != null) {
      return Optional.of(dependencies);
    }
    return Optional.empty();
  }

  @VisibleForTesting
  Map<String, Set<String>> getDependencyTreeMap() {
    return dependencyTree;
  }

  @VisibleForTesting
  Map<ComponentIdentifier, String> getDependencyTypeMap() {
    return dependencyType;
  }
}
