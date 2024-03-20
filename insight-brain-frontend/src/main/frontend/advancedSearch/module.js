/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import AdvancedSearchContainer from 'MainRoot/advancedSearch/AdvancedSearchContainer';

export default angular
  .module('advancedSearchModule', ['ngRedux'])
  .component('advancedSearch', iqReact2Angular(AdvancedSearchContainer, [], ['$ngRedux', '$state']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider.state('advancedSearch', {
    component: 'advancedSearch',
    url: '/advancedSearch',
    data: {
      title: 'Advanced Search',
    },
  });
}

routes.$inject = ['$stateProvider'];
