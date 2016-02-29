/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
  'use strict';

  var labelTemplate = {id: null, ownerId: null, label: null, labelLowercase: null, color: null, description: ''};

  var labelModule = angular.module('Labels', ['CLMAppLocation', 'Stores']);

  labelModule.service('LabelStore', [
    'CachedStore', 'CLMAppLocations', function(CachedStore, CLMAppLocations) {
      var labelStoreTemplate = {
        getUrl: CLMAppLocations.getLabelsUrl,
        template: labelTemplate
      };

      return CachedStore.get(labelStoreTemplate);
    }
  ]);
}());
