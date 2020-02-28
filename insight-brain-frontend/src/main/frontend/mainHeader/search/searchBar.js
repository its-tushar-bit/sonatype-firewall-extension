/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './searchBar.html';

export default {
  template: template,
  controllerAs: 'vm',
  controller: SearchBarController
};

function SearchBarController(searchService, searchSuggesterService, $q, $state, Messages) {
  const vm = this;

  Object.assign(vm, {
    error: undefined,
    query: searchService.query,
    suggestions: [],

    search() {
      vm.error = undefined;
      const promises = [];
      searchService.query = vm.query;
      searchService.pageSize = 10;
      searchService.page = 1;
      promises.push(searchService.search());
      return $q.all(promises).then(function(results) {
        searchService.results = results[0].data.groupingByDTOS;
        searchService.totalNumberOfHits = results[0].data.totalNumberOfHits;
        searchService.isExactTotalNumberOfHits = results[0].data.isExactTotalNumberOfHits;
        $state.go('searchResults', {}, {reload: true});
      }, function(error) {
        vm.error = Messages.getHttpErrorMessage(error);
      });
    },

    searchSuggester() {
      vm.error = undefined;
      const promises = [];
      searchSuggesterService.query = vm.query.replace('*', '');
      promises.push(searchSuggesterService.search());
      return $q.all(promises).then(function(results) {
        vm.suggestions = results[0].data.searchResultItems;
        searchSuggesterService.results = results[0].data.searchResultItems;
      }, function(error) {
        vm.error = Messages.getHttpErrorMessage(error);
      });
    }
  });
}

SearchBarController.$inject = [
  'searchService', 'searchSuggesterService', '$q', '$state', 'Messages'
];
