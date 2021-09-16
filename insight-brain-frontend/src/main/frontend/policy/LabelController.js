/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import CLMContextLocationModule from '../util/CLMContextLocation';
import storesModule from '../util/Stores';

var labelTemplate = { id: null, label: null, color: null, description: '' };

var labelModule = angular.module('Labels', [CLMContextLocationModule.name, storesModule.name]);

labelModule.service('LabelStore', [
  'CachedStore',
  'CLMContextLocations',
  function (CachedStore, CLMContextLocations) {
    var labelStoreTemplate = {
      getUrl: CLMContextLocations.getLabelsUrl,
      template: labelTemplate,
    };

    return CachedStore.get(labelStoreTemplate);
  },
]);

export default labelModule;
