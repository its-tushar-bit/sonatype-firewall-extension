/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';

export default function InnerSourceRepositoryService($http, CLMLocations) {
  return {
    getRepositoryConnections: getRepositoryConnections,
  };

  function getRepositoryConnections(ownerType, ownerId) {
    return $http.get(CLMLocations.getRepositoryConnections(ownerType, ownerId)).then(prop('data'));
  }
}

InnerSourceRepositoryService.$inject = ['$http', 'CLMLocations'];
