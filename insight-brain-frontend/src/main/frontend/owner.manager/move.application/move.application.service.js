/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function MoveApplicationService(
  $http,
  Messages,
  ApplicationStore,
  $q,
  CLMLocations,
  moveApplicationMessages
) {
  return {
    getDestinationOrganizations: getDestinationOrganizations,
    moveApplication: moveApplication,
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

  /**
   * @param applicationId
   * @param organizationId
   * @returns promise resolving to Array of info messages (if any)
   *          or rejecting with the error details object:
   *          {
   *            message: <String>,
   *            incompatibilities: <Array> (optional)
   *          }
   */
  function moveApplication(applicationId, organizationId) {
    return $http
      .post(CLMLocations.getMoveApplicationUrl(applicationId, organizationId))
      .then(handleResponse, handleError);
  }

  function handleResponse(response) {
    // wait till application cache is refreshed
    return ApplicationStore.refresh().then(function () {
      const hasWarnings = response.data && response.data.warnings && response.data.warnings.length;
      return hasWarnings ? response.data.warnings : null;
    });
  }

  function handleError(error) {
    if (error.status === 409 && error.data && error.data.errors && error.data.errors.length) {
      // this is response with array of incompatibilities
      return $q.reject({
        message: moveApplicationMessages.ERROR_INCOMPATIBLE_DESTINATION,
        incompatibilities: error.data.errors,
      });
    }

    return $q.reject({
      message: Messages.getHttpErrorMessage(error),
    });
  }
}

MoveApplicationService.$inject = [
  '$http',
  'Messages',
  'ApplicationStore',
  '$q',
  'CLMLocations',
  'move.application.messages.constant',
];
