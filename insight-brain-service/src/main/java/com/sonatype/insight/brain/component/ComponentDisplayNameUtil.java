/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.component;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.MavenIdentifier;
import com.sonatype.clm.dto.model.component.NugetIdentifier;
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
  private static final String MAVEN = "maven";
  private static final String NUGET = "nuget";

  private static final String GROUP_ID = "groupId";
  private static final String ARTIFACT_ID = "artifactId";
  private static final String VERSION = "version";
  private static final String PACKAGE_ID = "id";

  private static final String FILENAMES = "filenames";

  private static final String HASH = "hash";
  private static final String SHA1_20 = "sha1_20";

  public static ComponentDisplayName fromPolicyViolation(PolicyViolation policyViolation) {
    return fromGav(policyViolation.getGroupId(), policyViolation.getArtifactId(), policyViolation.getVersion());
  }

  public static void injectDisplayName(ObjectNode objectNode) {
    // First augment existing json to contain meta data which will exist after CLM-3574 CLM-3190
    final String groupId = JsonUtils.getNullableString(objectNode.get(GROUP_ID));
    final String artifactId = JsonUtils.getNullableString(objectNode.get(ARTIFACT_ID));
    final String version = JsonUtils.getNullableString(objectNode.get(VERSION));
    final String hash = JsonUtils.getNullableString(objectNode.get(HASH));
    JsonNode fileNamesNode = objectNode.get(FILENAMES);

    ObjectNode infoNode = new ObjectNode(JsonNodeFactory.instance);
    if (!Strings.isNullOrEmpty(groupId)) {
      infoNode.put("format", MAVEN);

      ObjectNode mavenNode = new ObjectNode(JsonNodeFactory.instance);
      mavenNode.put(GROUP_ID, groupId);
      mavenNode.put(ARTIFACT_ID, artifactId);
      mavenNode.put(VERSION, version);
      objectNode.put(MAVEN, mavenNode);
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
    MavenIdentifier mavenIdentifier = new MavenIdentifier();
    mavenIdentifier.groupId = componentFact.getGroupId();
    mavenIdentifier.artifactId = componentFact.getArtifactId();
    mavenIdentifier.version = componentFact.getVersion();
    componentFact.setDisplayName(fromMavenIdentifier(mavenIdentifier));
  }

  public static ComponentDisplayName fromJsonNode(ObjectNode objectNode) {
    JsonNode infoNode = objectNode.get("info");
    switch (infoNode.get("format").textValue()) {
      case MAVEN:
        JsonNode mavenNode = objectNode.get(MAVEN);
        MavenIdentifier mavenIdentifier = new MavenIdentifier();
        mavenIdentifier.groupId = mavenNode.get(GROUP_ID).textValue();
        mavenIdentifier.artifactId = mavenNode.get(ARTIFACT_ID).textValue();
        mavenIdentifier.version = mavenNode.get(VERSION).textValue();
        return fromMavenIdentifier(mavenIdentifier);
      case NUGET:
        JsonNode nugetNode = objectNode.get(NUGET);
        NugetIdentifier nugetIdentifier = new NugetIdentifier();
        nugetIdentifier.id = nugetNode.get(PACKAGE_ID).textValue();
        nugetIdentifier.version = nugetNode.get(VERSION).textValue();
        return fromNuGetIdentifier(nugetIdentifier);
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

  public static ComponentDisplayName fromMavenIdentifier(MavenIdentifier mavenIdentifier) {
    return generateDisplayFieldValues(MAVEN, mavenIdentifier, null, null, null);
  }

  public static ComponentDisplayName fromNuGetIdentifier(NugetIdentifier nugetIdentifier) {
    return generateDisplayFieldValues(NUGET, null, nugetIdentifier, null, null);
  }

  public static ComponentDisplayName fromFilenames(List<String> fileNames, String sha) {
    return generateDisplayFieldValues("unknown", null, null, fileNames, sha);
  }

  private static ComponentDisplayName generateDisplayFieldValues(String format, MavenIdentifier mavenIdentifier,
      NugetIdentifier nugetIdentifier, List<String> fileNames, String hash)
  {
    switch (format) {
      case MAVEN:
        return fromGav(mavenIdentifier.groupId, mavenIdentifier.artifactId, mavenIdentifier.version);
      case NUGET:
        return fromNuGet(nugetIdentifier.id, nugetIdentifier.version);
      default:
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
