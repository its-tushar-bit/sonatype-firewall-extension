/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dashboard.ComponentRiskDTO;
import com.sonatype.insight.brain.dashboard.DashboardViolationRiskDTO;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;

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
   * @since 1.41
   */
  public static ComponentDisplayName fromFilename(String filename, String hash) {
    return fromFilenames(
        !StringUtils.isBlank(filename) ? Collections.singletonList(filename) : Collections.emptyList(), hash);
  }

  private static ComponentDisplayName fromFilenames(Collection<String> fileNames, String hash) {
    ComponentDisplayName name = new ComponentDisplayName();
    if (fileNames != null && !fileNames.isEmpty()) {
      boolean firstFilename = true;
      for (String filename : fileNames) {
        if (!firstFilename) {
          name.add(", ");
        }
        firstFilename = false;
        name.add("Filename", filename);
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
  public static String deriveComponentName(DashboardViolationRiskDTO dto) {
    return dto.displayName != null
        ? dto.displayName.toString()
        : !StringUtils.isBlank(dto.filename) ? dto.filename : "Unknown";
  }

  public static String deriveComponentName(ComponentRiskDTO dto) {
    return dto.displayName != null
        ? dto.displayName.toString()
        : !StringUtils.isBlank(dto.filename) ? dto.filename : "Unknown";
  }
}
