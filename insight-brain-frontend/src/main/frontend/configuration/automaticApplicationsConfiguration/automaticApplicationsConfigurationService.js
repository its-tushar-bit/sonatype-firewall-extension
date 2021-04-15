/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function automaticApplicationsConfigurationService($http, $q, CLMLocations) {
  return {
    getConfiguration: getConfiguration,
    saveConfiguration: saveConfiguration,
  };

  function getConfiguration() {
    return $http.get(CLMLocations.getAutomaticApplicationsConfigurationUrl()).then(function (response) {
      return response.data;
    });
  }

  function saveConfiguration(configuration) {
    return $http.put(CLMLocations.getAutomaticApplicationsConfigurationUrl(), configuration).then(function (response) {
      return response.data;
    });
  }
}

automaticApplicationsConfigurationService.$inject = ['$http', '$q', 'CLMLocations'];

export default automaticApplicationsConfigurationService;
