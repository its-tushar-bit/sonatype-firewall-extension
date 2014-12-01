/**
 * @license Copyright (c) 2012-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
  'use strict';

  angular.module('ComponentUtils', []).service('ComponentUtil', [
    function() {
      /**
       * @since 1.13.0
       * Exists to enhance legacy report data structure with componentIdentifier where needed
       * @param component representation of a coordinate agnostic component or maven GAV
       */
      var enhanceWithComponentIdentifier = function(component) {
        var componentIdentifier = component.componentIdentifier;
        if (!componentIdentifier) {
          // This component represents an unknown or maven GAV.
          // Extension and classifier properties are only populated for claimed components
          var coordinates = null;
          if (component.groupId) {
            coordinates = {
              groupId: component.groupId,
              artifactId: component.artifactId,
              version: component.version
            };
            if (component.extension) {
              coordinates.extension = component.extension;
            }
            if (component.classifier) {
              coordinates.classifier = component.classifier;
            }
            angular.extend(component, {
              componentIdentifier: {
                format: 'maven',
                coordinates: coordinates
              }
            });
          }
        }
        return componentIdentifier;
      };
      return {
        enhanceWithComponentIdentifier: enhanceWithComponentIdentifier
      };
    }
  ]);

}());
