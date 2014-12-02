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
       * Subset of the logic implemented on HDS for displaying unknown components.
       * Required when revoking the claim on an existing component.
       * @param dataItem
       */
      var setDisplayNameAndCoordinates = function(dataItem) {
        dataItem.displayName = {};
        if (dataItem.filenames && dataItem.filenames.length > 0) {
          dataItem.displayName.parts = [];
          for (var i = 0; i < dataItem.filenames.length; i++) {
            dataItem.displayName.parts.push({ field: 'Filename', value: dataItem.filenames[i] });
            if (i < dataItem.filenames.length - 1) {
              dataItem.displayName.parts.push({ value: ', ' });
            }
          }
        }
        else {
          dataItem.displayName.parts = [
            { value: '(Anonymized Path) SHA1: ' },
            { field: 'Hash', value: dataItem.hash }
          ];
        }
        // Set coordinates value for filtering and sorting
        dataItem.coordinates = $.map(dataItem.displayName.parts, function(p) {
          return p.value;
        }).join('');
      };

      return {
        setDisplayNameAndCoordinates: setDisplayNameAndCoordinates
      };
    }
  ]);

}());
