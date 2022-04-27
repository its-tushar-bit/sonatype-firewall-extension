/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default function MoveApplicationService($http, Messages, $q, CLMLocations, moveApplicationMessages) {
  return {
    getDestinationOrganizations: getDestinationOrganizations,
  };

  /**
   * @param applicationId
   * @returns promise resolving to Array of available destination or rejecting with error message
   */
  function getDestinationOrganizations(applicationId) {
    return $http.get(CLMLocations.getDestinationOrganizationsUrl(applicationId)).then(
      function (response) {
        if (response.data && response.data.length) {
          return response.data;
        }
        return $q.reject(moveApplicationMessages.ERROR_NO_DESTINATIONS);
      },
      function (error) {
        return $q.reject(Messages.getHttpErrorMessage(error));
      }
    );
  }
}

MoveApplicationService.$inject = ['$http', 'Messages', '$q', 'CLMLocations', 'move.application.messages.constant'];
