/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.LinkedHashMap;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;

/**
 * @since 1.13
 */
public class ComponentFactory
{
  /**
   * Helper method to construct Maven Components from GAV properties.
   */
  public static Component forGav(String groupId, String artifactId, String version, MatchState matchState) {
    Component component = new Component(ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version));
    component.setMatchState(matchState);
    return component;
  }

  public static Component forCoordinates(String format, String... coord) {
    ComponentIdentifier componentIdentifier;
    switch (format) {
      case ComponentIdentifier.FORMAT_MAVEN:
        if (coord.length == 5) {
          // this method takes maven coordinates in the order GAVCE, but we have them as GAVEC, so swap the last two
          componentIdentifier = ComponentIdentifier
              .createMavenCoordinates(coord[0], coord[1], coord[2], coord[4], coord[3]);
        }
        else {
          componentIdentifier = ComponentIdentifier.createMavenCoordinates(coord[0], coord[1], coord[2]);
        }
        break;
      case ComponentIdentifier.FORMAT_ANAME:
        componentIdentifier = ComponentIdentifier.createAnameCoordinates(coord[0], coord[1], coord[2]);
        break;
      case ComponentIdentifier.FORMAT_PYPI:
        componentIdentifier = ComponentIdentifier.createPypiCoordinates(coord[0], coord[1], coord[2], coord[3]);
        break;
      default:
        throw new IllegalArgumentException("Unsupported component identifier format:" + format);
    }
    Component component = new Component(componentIdentifier);
    component.setMatchState(MatchState.EXACT);
    return component;
  }
  
  public static Component forCoordinatesPackageUrl(String format, String... coord) {
    ComponentIdentifier componentIdentifier;
    switch (format) {
      case ComponentIdentifier.FORMAT_MAVEN:
        if (coord.length == 5) {
          // this method takes maven coordinates in the order GAVCE, but we have them as GAVEC, so swap the last two
          componentIdentifier =
              ComponentIdentifier.createMavenCoordinates(coord[0], coord[1], coord[2], coord[4], coord[3]);
        }
        else {
          componentIdentifier = ComponentIdentifier.createMavenCoordinates(coord[0], coord[1], coord[2]);
        }
        break;
      case ComponentIdentifier.FORMAT_ANAME:
        componentIdentifier = ComponentIdentifier.createAnameCoordinates(coord[1], coord[4], coord[2]);
        break;
      case ComponentIdentifier.FORMAT_PYPI:
        componentIdentifier = ComponentIdentifier.createPypiCoordinates(coord[1], coord[2], coord[4], coord[3]);
        break;
      case ComponentIdentifier.FORMAT_GOLANG:
        componentIdentifier = ComponentIdentifier.createGolangCoordinates(coord[1], coord[2]);
        break;
      case ComponentIdentifier.FORMAT_NPM:
        componentIdentifier = ComponentIdentifier.createNpmCoordinates(coord[1], coord[2]);
        break;
      case ComponentIdentifier.FORMAT_NUGET:
        componentIdentifier = ComponentIdentifier.createNugetCoordinates(coord[1], coord[2]);
        break;
      case ComponentIdentifier.FORMAT_RPM:
        componentIdentifier = ComponentIdentifier.createRpmCoordinates(coord[1], coord[2], coord[4]);
        break;
      case ComponentIdentifier.FORMAT_RUBYGEMS:
        componentIdentifier = ComponentIdentifier.createRubyGemsCoordinates(coord[1], coord[2], coord[4]);
        break;
      default:
        componentIdentifier = ComponentFactory.createGenericPackageUrl(format, coord);
    }
    Component component = new Component(componentIdentifier);
    component.setMatchState(MatchState.EXACT);
    return component;
  }
  
  private static ComponentIdentifier createGenericPackageUrl(String format, String... coord) {
    Map<String, String> coordinates = new LinkedHashMap<>();
    coordinates.put("namespace", coord[0]);
    coordinates.put("name", coord[1]);
    coordinates.put("version", coord[2]);
    coordinates.put("type", coord[4]);
    coordinates.put("qualifier", coord[4]);
    return new ComponentIdentifier(format, coordinates);
  }
}
