/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.ide.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;

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
  private static final String SHA1_20 = "sha1_20";

  public static ComponentDisplayName fromPolicyViolation(PolicyViolation policyViolation) {
    return fromIdentifier(policyViolation.getComponentIdentifier());
  }

  public static void injectDisplayName(ObjectNode objectNode) {
    // First augment existing json to contain meta data which will exist after CLM-3574 CLM-3190
    final String groupId = JsonUtils.getNullableString(objectNode.get(ComponentIdentifier.MAVEN_GROUP_ID));
    final String artifactId = JsonUtils.getNullableString(objectNode.get(ComponentIdentifier.MAVEN_ARTIFACT_ID));
    final String version = JsonUtils.getNullableString(objectNode.get(ComponentIdentifier.VERSION));
    final String hash = JsonUtils.getNullableString(objectNode.get(HASH));
    JsonNode fileNamesNode = objectNode.get(FILENAMES);

    ObjectNode infoNode = new ObjectNode(JsonNodeFactory.instance);
    if (!Strings.isNullOrEmpty(groupId)) {
      infoNode.put("format", ComponentIdentifier.FORMAT_MAVEN);

      ObjectNode mavenNode = new ObjectNode(JsonNodeFactory.instance);
      mavenNode.put(ComponentIdentifier.MAVEN_GROUP_ID, groupId);
      mavenNode.put(ComponentIdentifier.MAVEN_ARTIFACT_ID, artifactId);
      mavenNode.put(ComponentIdentifier.VERSION, version);
      objectNode.put(ComponentIdentifier.FORMAT_MAVEN, mavenNode);
    }
    else {
      infoNode.put("format", "unknown");
    }

    if (fileNamesNode != null && fileNamesNode.size() > 0) {
      List<String> fileNames = new ArrayList<>();
      for (JsonNode fileNameNode : fileNamesNode) {
        fileNames.add(fileNameNode.textValue());
      }
      infoNode.put(FILENAMES, fileNamesNode);
    }

    if (!Strings.isNullOrEmpty(hash)) {
      ObjectNode hashNode = new ObjectNode(JsonNodeFactory.instance);
      hashNode.put(SHA1_20, hash);
      infoNode.put(HASH, hashNode);
    }
    objectNode.put("info", infoNode);

    // Generate the display name and augment info node with CLM Server generated array of Field/Value pairs
    ComponentDisplayName displayFieldValues = fromJsonNode(objectNode);
    JsonNode displayNameNode = JsonUtils.asTree(displayFieldValues);
    infoNode.put("displayName", displayNameNode);
  }

  public static void injectDisplayName(ComponentFact componentFact) {
    ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates(componentFact.getGroupId(),
        componentFact.getArtifactId(), componentFact.getVersion(), null, null);
    componentFact.setDisplayName(fromComponentIdentifier(mavenIdentifier));
  }

  public static ComponentDisplayName fromJsonNode(ObjectNode objectNode) {
    JsonNode infoNode = objectNode.get("info");
    switch (infoNode.get("format").textValue()) {
      case ComponentIdentifier.FORMAT_MAVEN:
        JsonNode mavenNode = objectNode.get(ComponentIdentifier.FORMAT_MAVEN);
        ComponentIdentifier mavenIdentifier = ComponentIdentifier.createMavenCoordinates(
            mavenNode.get(ComponentIdentifier.MAVEN_GROUP_ID).textValue(),
            mavenNode.get(ComponentIdentifier.MAVEN_ARTIFACT_ID).textValue(),
            mavenNode.get(ComponentIdentifier.VERSION).textValue());
        return fromComponentIdentifier(mavenIdentifier);
      case ComponentIdentifier.FORMAT_NUGET:
        JsonNode nugetNode = objectNode.get(ComponentIdentifier.FORMAT_NUGET);
        ComponentIdentifier nugetIdentifier = ComponentIdentifier
            .createNugetCoordinates(nugetNode.get(ComponentIdentifier.NUGET_PACKAGE_ID).textValue(),
                nugetNode.get(ComponentIdentifier.VERSION).textValue());
        return fromComponentIdentifier(nugetIdentifier);
      default:
        List<String> fileNames = null;
        ArrayNode fileNamesNode = (ArrayNode)infoNode.get(FILENAMES);
        if (fileNamesNode != null && fileNamesNode.size() > 0) {
          fileNames = new ArrayList<>();
          for (int i = 0; i < fileNamesNode.size(); i++) {
            fileNames.add(fileNamesNode.get(i).textValue());
          }
        }
        String sha = null;
        JsonNode hashNode = infoNode.get(HASH);
        if (hashNode != null) {
          sha = hashNode.get(SHA1_20).textValue();
        }
        return fromFilenames(fileNames, sha);
    }
  }

  public static ComponentDisplayName fromComponentIdentifier(ComponentIdentifier componentIdentifier) {
    return generateDisplayFieldValues(componentIdentifier, null, null);
  }

  public static ComponentDisplayName fromFilenames(List<String> fileNames, String sha) {
    return generateDisplayFieldValues(null, fileNames, sha);
  }

  private static ComponentDisplayName generateDisplayFieldValues(ComponentIdentifier identifier,
      List<String> fileNames, String hash)
  {
    if (identifier != null) {
      return fromIdentifier(identifier);
    }
    else {
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
  }
}
