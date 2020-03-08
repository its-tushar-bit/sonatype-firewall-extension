/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function searchService($http, CLMLocations) {
  return {
    query: undefined,
    results: undefined,
    pageSize: undefined,
    page: undefined,
    totalNumberOfHits: undefined,
    isExactTotalNumberOfHits: undefined,
    search
  };

  function search() {
    return $http.get(CLMLocations.getAdvancedSearchUrl(),
        {params: {search: this.query, pageSize: this.pageSize, page: this.page}});
  }
}

searchService.$inject = [
  '$http', 'CLMLocations'
];
