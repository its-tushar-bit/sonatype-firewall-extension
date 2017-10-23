/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.insight.brain.dashboard.ComponentRiskDTO;
import com.sonatype.insight.brain.dashboard.NewestRiskDTO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.io.FilenameUtils.getName;
import static org.apache.commons.io.FilenameUtils.normalizeNoEndSeparator;

/**
 * Utility to build CLM Server specific component display names from coordinates.
 *
 * @since 1.13.0
 */
public class ComponentDisplayNameUtil
    extends com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil
{
  private static final String FILENAMES = "filenames";
  private static final String HASH = "hash";

  public static ComponentDisplayName fromPolicyViolation(PolicyViolation policyViolation) {
    return fromIdentifier(policyViolation.getComponentIdentifier());
  }

  public static void injectDisplayName(ObjectNode objectNode) {
    ComponentDisplayName displayFieldValues = fromJsonNode(objectNode);
    JsonNode displayNameNode = JsonUtils.asTree(displayFieldValues);
    objectNode.set("displayName", displayNameNode);
  }

  public static void injectDisplayName(ComponentFact componentFact) {
    componentFact.setDisplayName(fromIdentifier(componentFact.getComponentIdentifier()));
  }

  public static ComponentDisplayName fromJsonNode(ObjectNode objectNode) {
    ComponentIdentifier componentIdentifier = ComponentIdentifierAdapter.getComponentIdentifier(objectNode);
    if (componentIdentifier != null) {
      return fromIdentifier(componentIdentifier);
    }
    else {
      List<String> fileNames = null;
      ArrayNode fileNamesNode = (ArrayNode) objectNode.get(FILENAMES);
      if (fileNamesNode != null && fileNamesNode.size() > 0) {
        fileNames = new ArrayList<>();
        for (int i = 0; i < fileNamesNode.size(); i++) {
          fileNames.add(fileNamesNode.get(i).textValue());
        }
      }
      String hash = JsonUtils.getNullableString(objectNode.get(HASH));
      return fromFilenames(fileNames, hash);
    }
  }

  /**
   * @since 1.24.0
   */
  public static ComponentDisplayName fromPathnames(Collection<String> pathNames, String hash) {
    Set<String> fileNames = new LinkedHashSet<>();
    if (pathNames != null) {
      for (String pathName : pathNames) {
        fileNames.add(new File(pathName).getName());
      }
    }
    return fromFilenames(new ArrayList<>(fileNames), hash);
  }

  private static ComponentDisplayName fromFilenames(List<String> fileNames, String hash) {
    ComponentDisplayName name = new ComponentDisplayName();
    if (fileNames != null && fileNames.size() > 0) {
      int fileNamesSize = fileNames.size();
      for (int i = 0; i < fileNamesSize; i++) {
        name.add("Filename", fileNames.get(i));
        if (i < fileNamesSize - 1) {
          name.add(", ");
        }
      }
    }
    else {
      name.add("(Anonymized Path) SHA1: ");
      name.add("Hash", hash);
    }
    return name;
  }

  /**
   * @since 1.38
   */
  public static String deriveComponentName(NewestRiskDTO dto) {
    return dto.displayName != null ? dto.displayName.toString() :
        !isEmpty(dto.pathnames) ? getName(normalizeNoEndSeparator(dto.pathnames.get(0))) : "Unknown";
  }

  public static String deriveComponentName(ComponentRiskDTO dto) {
    return dto.displayName != null ? dto.displayName.toString() :
        !isEmpty(dto.pathnames) ? getName(normalizeNoEndSeparator(dto.pathnames.iterator().next())) : "Unknown";
  }
}
