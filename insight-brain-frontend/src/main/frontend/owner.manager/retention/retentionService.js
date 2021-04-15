/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop } from 'ramda';

export default function retentionService($http, CLMContextLocations) {
  return {
    getRootOrganizationRetentionPolicies,
    getRetentionPolicies,
    setRetentionPolicies,
  };

  function getRootOrganizationRetentionPolicies() {
    return $http.get(CLMContextLocations.getRetentionPoliciesUrl('ROOT_ORGANIZATION_ID')).then(prop('data'));
  }

  function getRetentionPolicies() {
    return $http.get(CLMContextLocations.getRetentionPoliciesUrl(CLMContextLocations.getEntityId())).then(prop('data'));
  }

  function setRetentionPolicies(retentionPolicies) {
    return $http
      .put(CLMContextLocations.getRetentionPoliciesUrl(CLMContextLocations.getEntityId()), retentionPolicies)
      .then(prop('data'));
  }
}

retentionService.$inject = ['$http', 'CLMContextLocations'];
