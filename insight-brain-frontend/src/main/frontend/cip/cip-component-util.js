/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
export default angular.module('ComponentUtils', []).service('ComponentUtil', [
  function () {
    /**
     * Subset of the logic implemented on HDS for displaying unknown components.
     * Required when revoking the claim on an existing component.
     * @param dataItem
     */
    var setDisplayNameAndCoordinates = function (dataItem) {
      dataItem.displayName = {};
      if (dataItem.filenames && dataItem.filenames.length > 0) {
        dataItem.displayName.parts = [];
        for (var i = 0; i < dataItem.filenames.length; i++) {
          dataItem.displayName.parts.push({
            field: 'Filename',
            value: dataItem.filenames[i],
          });
          if (i < dataItem.filenames.length - 1) {
            dataItem.displayName.parts.push({ value: ', ' });
          }
        }
      } else {
        dataItem.displayName.parts = [
          { value: '(Anonymized Path) SHA1: ' },
          { field: 'Hash', value: dataItem.hash },
        ];
      }
      // Set coordinates value for filtering and sorting
      dataItem.coordinates = $.map(dataItem.displayName.parts, function (p) {
        return p.value;
      }).join('');
    };

    /**
     * @since 1.13.0
     * Exists to enhance legacy report data structure with componentIdentifier where needed
     * @param component representation of a coordinate agnostic component or maven GAV
     */
    var enhanceWithComponentIdentifier = function (component) {
      var componentIdentifier = component.componentIdentifier;
      if (!componentIdentifier) {
        // This component represents an unknown or maven GAV.
        var coordinates = null;
        if (component.groupId) {
          coordinates = {
            groupId: component.groupId,
            artifactId: component.artifactId,
            version: component.version,
          };
          // Extension and classifier properties should only populated for claimed components in data prior to v1.13.0
          if (component.extension) {
            coordinates.extension = component.extension;
          }
          if (component.classifier) {
            coordinates.classifier = component.classifier;
          }
          angular.extend(component, {
            componentIdentifier: {
              format: 'maven',
              coordinates: coordinates,
            },
          });
        }
      }
    };

    return {
      setDisplayNameAndCoordinates: setDisplayNameAndCoordinates,
      enhanceWithComponentIdentifier: enhanceWithComponentIdentifier,
    };
  },
]);
