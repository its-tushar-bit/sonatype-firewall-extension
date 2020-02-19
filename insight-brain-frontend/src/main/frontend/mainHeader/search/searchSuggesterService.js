/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function searchSuggesterService($http, CLMContextLocations) {
  return {
    query: undefined,
    results: undefined,
    search
  };

  function search() {
    return $http.get(CLMContextLocations.getSearchSuggesterUrl(),
        {params: {search: this.query}});
  }
}

searchSuggesterService.$inject = [
  '$http', 'CLMContextLocations'
];
