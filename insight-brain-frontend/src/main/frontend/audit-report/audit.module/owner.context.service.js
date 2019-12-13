/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
function decode(encodedString) {
  return decodeURIComponent((encodedString || '').replace(/\+/g, '%20'));
}

export default function OwnerContext($window) {
  var search = $window.location.search,
      result = {};
  if (search.length === 0) {
    return;
  }

  search = search.substring(1).split('&');
  angular.forEach(search, function(item) {
    var field = item.split('=');
    result[decode(field[0])] = decode(field[1]);
  });

  return {
    ownerType: 'repository',
    ownerId: result.repositoryId
  };
}
OwnerContext.$inject = ['$window'];
