/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './searchTile.html';

export default {
  template: template,
  controllerAs: 'vm',
  controller: SearchTileController
};

function SearchTileController(searchService, $q, $state, Messages, systemConfigurationPropertyService) {
  const vm = this;

  Object.assign(vm, {
    error: undefined,
    results: searchService.results,
    page: searchService.page ? searchService.page : 1,
    pageSize: searchService.pageSize,
    totalNumberOfHits: searchService.totalNumberOfHits,
    isEnabled: undefined,

    $onInit() {
      vm.doLoad();
    },

    doLoad() {
      vm.error = undefined;
      systemConfigurationPropertyService.isFullTextSearchEnabled().then(function(data) {
        vm.isEnabled = data;
      }, function(error) {
        vm.error = Messages.getHttpErrorMessage(error);
      });
    },

    next() {
      if (!searchService.totalNumberOfHits || searchService.isExactTotalNumberOfHits &&
          searchService.totalNumberOfHits <= vm.page * searchService.pageSize) {
        return;
      }
      searchService.page++;
      vm.page = searchService.page;
      vm.search();
    },

    previous() {
      if (!searchService.page || searchService.page === 1) {
        return;
      }
      searchService.page--;
      vm.page = searchService.page;
      vm.search();
    },

    getMaxPage() {
      return searchService.totalNumberOfHits ? Math.ceil(searchService.totalNumberOfHits / searchService.pageSize) : 1;
    },

    search() {
      vm.error = undefined;
      const promises = [];
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

    threatIndicator(threatLevel) {
      switch (true) {
        case (threatLevel === 0): {
          return 'none';
        }
        case (threatLevel === 1): {
          return 'low';
        }
        case (threatLevel >= 2 && threatLevel <= 3): {
          return 'moderate';
        }
        case (threatLevel >= 4 && threatLevel <= 7): {
          return 'severe';
        }
        case (threatLevel >= 8 && threatLevel <= 10): {
          return 'critical';
        }
        default: {
          return '';
        }
      }
    }
  });
}

SearchTileController.$inject = ['searchService', '$q', '$state', 'Messages', 'systemConfigurationPropertyService'];
