/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function ProprietaryMatchersService($http, $q, Messages) {
  return {
    addComponentMatchers: addComponentMatchers,
    getApplicationInfo: getApplicationInfo,
  };

  /**
   * @param appId String
   * @returns Promise resolving to Application Info,
   *          or rejecting with error message.
   */
  function getApplicationInfo(appId) {
    var url = CLM.path + 'rest/application/' + appId;

    return $http.get(url).then(
      function (response) {
        return response.data;
      },
      function (error) {
        return $q.reject(Messages.getHttpErrorMessage(error));
      }
    );
  }

  /**
   * Add proprietary matchers to application configuration based on provided path names and regex.
   * If a matcher already exist, it will be ignored (on the server side).
   *
   * @param ownerAppID String
   * @param pathNames Array of File Path Strings
   * @param regex String
   * @returns Promise resolving to Proprietary Config including the newly added file paths and regex,
   *          or rejecting with error message.
   */
  function addComponentMatchers(ownerAppID, pathNames, regex) {
    var url = CLM.path + 'rest/proprietary/application/' + ownerAppID + '/add';

    var data = {
      paths: pathNames,
      regex: regex,
    };

    return $http.post(url, data).then(
      function (response) {
        return response.data;
      },
      function (error) {
        return $q.reject(Messages.getHttpErrorMessage(error));
      }
    );
  }
}

ProprietaryMatchersService.$inject = ['$http', '$q', 'Messages'];
