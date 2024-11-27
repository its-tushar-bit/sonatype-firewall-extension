/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

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
      case ComponentIdentifier.FORMAT_NPM:
        componentIdentifier = ComponentIdentifier.createNpmCoordinates(coord[0], coord[1]);
        break;
      case ComponentIdentifier.FORMAT_COCOAPODS:
        componentIdentifier = ComponentIdentifier.createCocoapodsCoordinates(coord[0], coord[1]);
        break;
      case ComponentIdentifier.FORMAT_CONAN:
        componentIdentifier = ComponentIdentifier.createConanCoordinates(coord[0], coord[1], coord[2], coord[3]);
        break;
      case ComponentIdentifier.FORMAT_COMPOSER:
        componentIdentifier = ComponentIdentifier.createComposerCoordinates(coord[0], coord[1], coord[2]);
        break;
      case ComponentIdentifier.FORMAT_CARGO:
        componentIdentifier = ComponentIdentifier.createCargoCoordinates(coord[0], coord[1], coord[2]);
        break;
      case ComponentIdentifier.FORMAT_HUGGINGFACE_MODEL:
        // this method takes hf-model coordinates
        // repoId (namespace), model (name), version, modelFormat (classifier), modelExtension (extension)
        // so similar to maven we need to swap the last two
        componentIdentifier =
            ComponentIdentifier.createHuggingfaceModelCoordinates(coord[0], coord[1], coord[2], coord[4], coord[3]);
        break;
      default:
        throw new IllegalArgumentException("Unsupported component identifier format:" + format);
    }
    Component component = new Component(componentIdentifier);
    component.setMatchState(MatchState.EXACT);
    return component;
  }
}
