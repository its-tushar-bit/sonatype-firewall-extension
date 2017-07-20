/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default
function systemConfigurationPropertyService($http, $rootScope, $q, CLMLocations) {
  return {
    isSuccessMetricsEnabled: isSuccessMetricsEnabled,
    saveSuccessMetricsEnabled: saveSuccessMetricsEnabled,
    checkSuccessMetricsEnabled: checkSuccessMetricsEnabled
  };

  function isSuccessMetricsEnabled() {
    return getSystemConfigurationProperty('SUCCESS_METRICS_ENABLED').then(function(value) {
      return value === 'true';
    });
  }

  function checkSuccessMetricsEnabled() {
    return isSuccessMetricsEnabled().then(function(enabled) {
      if (enabled) {
        return $q.resolve(true);
      }
      else {
        return $q.reject('Success metrics have been disabled by your system administrator.');
      }
    });
  }

  function saveSuccessMetricsEnabled(successMetricsEnabled) {
    var successMetricsProperty = {name: 'SUCCESS_METRICS_ENABLED', value: successMetricsEnabled.toString()};
    return saveSystemConfigurationProperty(successMetricsProperty).then(function(response) {
      $rootScope.$broadcast('successMetricsConfigurationUpdated', response.value === 'true');
      return response;
    });
  }

  function getSystemConfigurationProperty(name) {
    return $http.get(CLMLocations.getSystemConfigurationPropertyUrl(name)).then(function(response) {
      return response.data.value;
    });
  }

  function saveSystemConfigurationProperty(systemConfigurationProperty) {
    return $http.put(CLMLocations.getSystemConfigurationPropertiesUrl(), systemConfigurationProperty).then(function(response) {
      return response.data;
    });
  }
}

systemConfigurationPropertyService.$inject = ['$http', '$rootScope', '$q', 'CLMLocations'];
