/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick, prop } from 'ramda';

function PolicyViolationGrandfatheringService($http, CLMContextLocations) {
  return {
    getGrandfathering: getGrandfathering,
    setGrandfathering: setGrandfathering,
    getStatusMessage: getStatusMessage,
  };

  function getGrandfathering() {
    return $http.get(CLMContextLocations.getGrandfatheringUrl()).then(function ({ data }) {
      const config = pick(['inheritedFromOrganizationName', 'allowOverride', 'allowChange'], data);

      // The returned data contains the calculated value of the "enabled" flag based on the
      // current settings for the owner and its parents. For enabled values that are being
      // inherited, we need to adjust accordingly and null out the enabled value for this
      // particular owner (since the value is not coming from this owner but a parent).
      config.enabled = data.inheritedFromOrganizationName === null ? data.enabled : null;
      config.calculatedEnabled = data.enabled;

      // For the root organization, values that have not yet been set in the backend are treated
      // as false (as there's nowhere else to inherit from), so nulls need to be set to false.
      if (CLMContextLocations.isRootOrg()) {
        config.enabled = config.enabled || false;
        config.calculatedEnabled = config.calculatedEnabled || false;
      }
      return config;
    });
  }

  function setGrandfathering(configuration) {
    return $http
      .put(CLMContextLocations.getGrandfatheringUrl(), pick(['enabled', 'allowOverride'], configuration))
      .then(prop('data'));
  }

  function getStatusMessage(configuration) {
    let msg = '';
    if (configuration.inheritedFromOrganizationName !== null) {
      msg += `Inherit from ${configuration.inheritedFromOrganizationName} (`;
    }
    msg += 'Grandfathering is ' + (configuration.calculatedEnabled ? 'enabled' : 'disabled');
    if (configuration.inheritedFromOrganizationName !== null) {
      msg += ')';
    }
    return msg;
  }
}

PolicyViolationGrandfatheringService.$inject = ['$http', 'CLMContextLocations'];

export default PolicyViolationGrandfatheringService;
