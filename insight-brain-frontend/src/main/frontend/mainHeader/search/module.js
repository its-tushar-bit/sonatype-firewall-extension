/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import clmContextLocationModule from '../../util/CLMContextLocation';
import searchService from './searchService';
import searchSuggesterService from './searchSuggesterService';
import searchBar from './searchBar';
import searchTile from './searchTile';

export default angular.module('searchModule', [clmContextLocationModule.name])
    .service('searchService', searchService)
    .service('searchSuggesterService', searchSuggesterService)
    .component('searchBar', searchBar)
    .component('searchTile', searchTile)
    .config(configureRoutes);

function configureRoutes($stateProvider) {
  $stateProvider
      .state('searchResults', {
        url: '/searchResults',
        component: 'searchTile',
        data: {
          title: 'Search Results'
        },
        params: {
          results: undefined
        }
      });
}

configureRoutes.$inject = ['$stateProvider'];
