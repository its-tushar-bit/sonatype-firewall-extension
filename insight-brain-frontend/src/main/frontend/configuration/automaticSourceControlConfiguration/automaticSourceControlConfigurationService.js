/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function automaticSourceControlConfigurationService($http, CLMLocations) {
  return {
    getConfiguration: getConfiguration,
    saveConfiguration: saveConfiguration,
  };

  function getConfiguration() {
    return $http.get(CLMLocations.getAutomaticSourceControlConfigurationUrl()).then(function (response) {
      return response.data;
    });
  }

  function saveConfiguration(configuration) {
    return $http.put(CLMLocations.getAutomaticSourceControlConfigurationUrl(), configuration).then(function (response) {
      return response.data;
    });
  }
}

automaticSourceControlConfigurationService.$inject = ['$http', 'CLMLocations'];

export default automaticSourceControlConfigurationService;
