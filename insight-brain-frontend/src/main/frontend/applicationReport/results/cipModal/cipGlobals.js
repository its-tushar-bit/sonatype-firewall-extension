/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {toURIParams} from '../../../util/urlUtil';

window.CLM = {
  path: '../',
  assetsPath: '../assets/'
};

// CIP config
window.clmEndpoint = {
  type: 'ci',
  migrate: false,
  selectApplication: false,
  openView: angular.noop,
  linkTarget: '_blank',
  path: window.CLM.assetsPath + '/version-graph/',
  canAddProprietary: true
};

window.Brain = {
  // used by Component Info tab
  ci: {
    getComponentListUrl(ownerType, ownerId, componentType, hash, matchState, proprietary, coordinates, pathname) {
      const url = window.CLM.path + 'rest/ci/componentDetails/' + ownerType + '/' + encodeURIComponent(ownerId) +
          '/allVersions';
      return url + '?' + toParams(componentType, hash, matchState, proprietary, coordinates, pathname);
    },
    getComponentUrl(ownerType, ownerId, componentType, hash, matchState, proprietary, coordinates, pathname) {
      const url = window.CLM.path + 'rest/ci/componentDetails/' + ownerType + '/' + encodeURIComponent(ownerId);
      return url + '?' + toParams(componentType, hash, matchState, proprietary, coordinates, pathname);
    }
  },

  // used by Vulnerabilities tab
  getVulnerabilityDetailUrl: function(source, refId, componentIdentifier, hash) {
    const url = 'rest/vulnerability/details/' + encodeURIComponent(source) + '/' + encodeURIComponent(refId);

    const params = toURIParams({
      hash,
      componentIdentifier: componentIdentifier && JSON.stringify(componentIdentifier)
    });

    if (params.length > 0) {
      return url + '?' + params;
    }

    return url;
  },

  /**
   * Gets the component remediation URL
   *
   * @since 1.66.0
   */
  getSuggestedRemediationUrlForApplication: function (internalApplicationId) {
    return window.CLM.path + 'api/v2/components/remediation/application/' + encodeURIComponent(internalApplicationId);
  },

  /**
   * Gets the URL for the internal application ID
   *
   * @since 1.66.0
   */
  getInternalApplicationIdUrlForApplicationId: function (applicationId) {
    return window.CLM.path + 'api/v2/applications?publicId=' + encodeURIComponent(applicationId);
  }
};

function toParams(componentType, hash, matchState, proprietary, coordinates, pathname) {
  const componentIdentifier = coordinates && JSON.stringify({ format: componentType, coordinates });
  return toURIParams({
    componentIdentifier,
    hash,
    matchState,
    proprietary,
    pathname
  });
}
