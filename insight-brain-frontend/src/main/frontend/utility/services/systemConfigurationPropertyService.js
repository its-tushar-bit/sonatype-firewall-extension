/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {getScmOnboardingConfigUrl} from '../../util/CLMLocation';

const SUCCESS_METRICS_DISABLED_MESSAGE = 'Success metrics have been disabled by your system administrator.';

export default function systemConfigurationPropertyService($http, $rootScope, $q, CLMLocations) {
  return {
    isSuccessMetricsEnabled: isSuccessMetricsEnabled,
    saveSuccessMetricsEnabled: saveSuccessMetricsEnabled,
    checkSuccessMetricsEnabled: checkSuccessMetricsEnabled,
    SUCCESS_METRICS_DISABLED_MESSAGE: SUCCESS_METRICS_DISABLED_MESSAGE,
    isAdvancedSearchEnabled: isAdvancedSearchEnabled,
    isManifestScanFeatureEnabled: isManifestScanFeatureEnabled
  };

  function isSuccessMetricsEnabled() {
    return getSuccessMetricsConfiguration().then(function(configuration) {
      return configuration.enabled;
    });
  }

  function checkSuccessMetricsEnabled() {
    return isSuccessMetricsEnabled().then(function(enabled) {
      if (enabled) {
        return $q.resolve(true);
      }
      else {
        return $q.reject(SUCCESS_METRICS_DISABLED_MESSAGE);
      }
    });
  }

  function saveSuccessMetricsEnabled(successMetricsEnabled) {
    var successMetricsConfig = {enabled: successMetricsEnabled};
    return saveSuccessMetricsConfiguration(successMetricsConfig).then(function(configuration) {
      $rootScope.$broadcast('successMetricsConfigurationUpdated', configuration.enabled);
      return configuration;
    });
  }

  function getSuccessMetricsConfiguration() {
    return $http.get(CLMLocations.getSuccessMetricsConfigUrl()).then(function(response) {
      return response.data;
    });
  }

  function saveSuccessMetricsConfiguration(successMetricsConfiguration) {
    return $http.put(CLMLocations.getSuccessMetricsConfigUrl(), successMetricsConfiguration).then(function(response) {
      return response.data;
    });
  }

  function isAdvancedSearchEnabled() {
    return $http.get(CLMLocations.getAdvancedSearchConfigUrl()).then(function(response) {
      return response.data.isEnabled;
    });
  }

  function isManifestScanFeatureEnabled() {
    return $http.get(getScmOnboardingConfigUrl()).then(function(response) {
      return response.data.manifestScanFeatureEnabled;
    });
  }
}

systemConfigurationPropertyService.$inject = ['$http', '$rootScope', '$q', 'CLMLocations'];
