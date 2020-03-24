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

function SearchBarController(searchService, searchSuggesterService, $state, Messages) {
  const vm = this;

  Object.assign(vm, {
    error: undefined,
    query: searchService.query || '',
    suggestions: [],

    search() {
      vm.error = undefined;
      searchService.query = vm.query;
      searchService.pageSize = 10;
      searchService.page = null; // no page index to signal new search
      return searchService.search().then(function(response) {
        searchService.results = response.data.groupingByDTOS;
        searchService.totalNumberOfHits = response.data.totalNumberOfHits;
        searchService.isExactTotalNumberOfHits = response.data.isExactTotalNumberOfHits;
        $state.go('searchResults', {}, {reload: true});
      }, function(error) {
        vm.error = Messages.getHttpErrorMessage(error);
      });
    },

    searchSuggester() {
      return searchSuggesterService.suggest(vm.query).then(function(response) {
        // ignore delayed suggestions for previous query values
        if (vm.query === response.data.searchQuery) {
          vm.suggestions = response.data.searchResultItems;
        }
      });
    }
  });
}

SearchBarController.$inject = [
  'searchService', 'searchSuggesterService', '$state', 'Messages'
];
