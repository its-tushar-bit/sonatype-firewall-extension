/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
var clmBuildTimestamp = CLM_BUILD_TIMESTAMP;
/*global window, $ */
/*jslint plusplus:true */
(function () {
  'use strict';

  function toParams(componentType, hash, matchState, proprietary, coordinates, pathname, identificationSource, scanId) {
    var params = {};

    if (coordinates) {
      params.componentIdentifier = JSON.stringify({
        format: componentType,
        coordinates: coordinates,
      });
    }

    if (hash) {
      params.hash = hash;
    }
    if (matchState) {
      params.matchState = matchState;
    }
    if (proprietary) {
      params.proprietary = proprietary;
    }
    if (pathname) {
      params.pathname = pathname;
    }
    if (window.reportId) {
      params.reportId = window.reportId;
      params.scanId = params.reportId;
    }
    if (identificationSource) {
      params.identificationSource = identificationSource;
    }
    if (scanId) {
      params.scanId = scanId;
    }
    return param(params);
  }

  function createComponentUrl(clientType) {
    return function (
      ownerType,
      ownerId,
      componentType,
      hash,
      matchState,
      proprietary,
      coordinates,
      pathname,
      identificationSource,
      scanId
    ) {
      var url = basePath + 'rest/' + clientType + '/componentDetails/' + ownerType + '/' + encodeURIComponent(ownerId);

      return (
        url +
        '?' +
        toParams(componentType, hash, matchState, proprietary, coordinates, pathname, identificationSource, scanId)
      );
    };
  }

  function createComponentListUrl(clientType) {
    return function (
      ownerType,
      ownerId,
      componentType,
      hash,
      matchState,
      proprietary,
      coordinates,
      pathname,
      identificationSource,
      scanId
    ) {
      var url =
        basePath +
        'rest/' +
        clientType +
        '/componentDetails/' +
        ownerType +
        '/' +
        encodeURIComponent(ownerId) +
        '/allVersions';

      return (
        url +
        '?' +
        toParams(componentType, hash, matchState, proprietary, coordinates, pathname, identificationSource, scanId)
      );
    };
  }

  var features = [
      'policy',
      'labels',
      'release-graph',
      'policy-violations',
      'notification',
      'reevaluate-policy',
      'component-identifier',
    ], // Lowercase
    param = window.$
      ? $.param
      : function (obj) {
          var string = '',
            field;
          for (field in obj) {
            if (obj[field]) {
              string += '&' + encodeURIComponent(field) + '=' + encodeURIComponent(obj[field]);
            }
          }
          return string.substring(1);
        },
    basePath = (function () {
      var scripts = window.document.getElementsByTagName('script'),
        index;
      if (scripts.length) {
        for (var i = 0; i < scripts.length; i++) {
          if (scripts[i].src) {
            index = scripts[i].src.indexOf('policy-assets/js/brain.client.js');
            if (index === -1) {
              index = scripts[i].src.indexOf('assets/brain.client.js');
            }
            if (index === -1) {
              index = scripts[i].src.indexOf('assets/policy/js/brain.client.js');
            }
            if (index === -1) {
              index = scripts[i].src.indexOf('assets/assets/js/brain.client.js');
            }

            if (index !== -1) {
              return scripts[i].src.substring(0, index);
            }
          }
        }
      }
      return '/';
    })();

  window.Brain = {
    /**
     * This is only for unit testing
     * @since version 1.12
     * @param newBasePath - the new BasePath
     */
    setBasePath: function (newBasePath) {
      basePath = newBasePath;
    },
    /**
     * Check if the Brain instance supports a feature
     * @since version 1.1
     */
    hasFeature: function (feature) {
      var i;
      feature = feature.toLowerCase();
      for (i = 0; i < features.length; i++) {
        if (feature === features[i]) {
          return true;
        }
      }
      return false;
    },
    /**
     * Get the list of applications
     * @since version 1.10
     */
    getApplicationListUrl: function () {
      return basePath + 'rest/application/services/names';
    },
    /**
     * Get the integrator list of applications
     * @since version 1.21
     */
    getIntegratorApplicationListUrl: function () {
      return basePath + 'rest/integration/applications?goal=VIEW_CIP';
    },
    /**
     * Get the Brain's version.
     * @since version 1.1
     */
    getVersion: function () {
      return '${project.version}';
    },

    /**
     * Gets the headers required to pass CSRF protection.
     *
     * @since version 1.16
     */
    getCsrfHeaders: function () {
      return {
        'X-CSRF-TOKEN': document.cookie.replace(/((^|.*;\s*)CLM-CSRF-TOKEN\s*=\s*([^;]*).*)|^.*$/, '$3'),
      };
    },

    /**
     * Gets the URL to re-evaluate the policy for the currently viewed report.
     *
     * @since version 1.16
     */
    getCurrentReportReevaluateUrl: function () {
      return '../reevaluatePolicy';
    },

    /**
     * @since 1.19.0
     */
    getRepositoryResultsUrl: function (repositoryId, componentKey) {
      var path = 'rest/repositories/' + encodeURIComponent(repositoryId) + '/report/details';

      if (componentKey) {
        path += '?' + param(componentKey);
      }

      return basePath + path;
    },

    /**
     * Gets the component remediation URL
     *
     * @since 1.66.0
     */
    getSuggestedRemediationUrlForApplication: function (internalApplicationId) {
      return basePath + 'api/v2/components/remediation/application/' + encodeURIComponent(internalApplicationId);
    },

    /**
     * Gets the URL for the internal application ID
     *
     * @since 1.66.0
     */
    getInternalApplicationIdUrlForApplicationId: function (applicationId) {
      return basePath + 'api/v2/applications?publicId=' + encodeURIComponent(applicationId);
    },

    ci: {
      /**
       * Get the URL for the agnostic coordinate ComponentDetails resource
       *
       * @since version 1.13
       */
      getComponentUrl: createComponentUrl('ci'),
      /**
       * Get the URL for the agnostic coordinate ComponentDetailsList resource
       *
       * @since version 1.13
       */
      getComponentListUrl: createComponentListUrl('ci'),
    },
    ide: {
      /**
       * Get the URL for the agnostic coordinate ComponentDetails resource
       *
       * @since version 1.13
       */
      getComponentUrl: createComponentUrl('ide'),
      /**
       * Get the URL for the agnostic coordinate ComponentDetailsList resource
       *
       * @since version 1.13
       */
      getComponentListUrl: createComponentListUrl('ide'),
    },
    rm: {
      /**
       * Get the URL for the agnostic coordinate ComponentDetails resource
       *
       * @since version 1.13
       */
      getComponentUrl: createComponentUrl('rm'),
      /**
       * Get the URL for the agnostic coordinate ComponentDetailsList resource
       *
       * @since version 1.13
       */
      getComponentListUrl: createComponentListUrl('rm'),
    },
    /**
     * Get the URL for the vulnerability detail content
     *
     * @since version 1.14
     */
    getVulnerabilityDetailUrl: function (source, refId, componentIdentifier, hash) {
      var url = 'rest/vulnerability/details/' + encodeURIComponent(source) + '/' + encodeURIComponent(refId),
        params = {};

      if (componentIdentifier) {
        params.componentIdentifier = JSON.stringify(componentIdentifier);
      }
      if (hash) {
        params.hash = hash;
      }

      params = param(params);
      if (params.length > 0) {
        url += '?' + params;
      }

      return url;
    },

    /**
     * @since version 1.19.0
     */
    getComponentReevaluationUrl: function (owner, hash) {
      return basePath + 'rest/repositories/' + owner.ownerId + '/evaluate/' + hash;
    },

    /**
     * @since 1.19.0
     */
    getRepositoryEvaluateUrl: function (owner) {
      return basePath + 'rest/repositories/' + owner.ownerId + '/evaluate';
    },
  };
})();
