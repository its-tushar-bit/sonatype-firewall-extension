/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
var clmBuildTimestamp = '${build.timestamp}';
/*global window, $ */
/*jslint plusplus:true */
(function() {
  "use strict";

  function toParams(componentType, hash, matchState, proprietary, coordinates) {
    var params = {};

    if (coordinates) {
      params.componentIdentifier = JSON.stringify({ format : componentType, coordinates : coordinates });
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
    if (window.reportId) {
      params.reportId = window.reportId;
    }
    return param(params);
  }

  function createComponentUrl(clientType) {
    return function (appPublicId, componentType, hash, matchState, proprietary, coordinates) {
      var url = basePath + 'rest/' + clientType + '/componentDetails/' + encodeURIComponent(appPublicId);

      return url + '?' + toParams(componentType, hash, matchState, proprietary, coordinates);
    };
  }

  function createComponentListUrl(clientType) {
    return function (appPublicId, componentType, hash, matchState, proprietary, coordinates) {
      var url = basePath + 'rest/' + clientType + '/componentDetails/' + encodeURIComponent(appPublicId) + '/list';

      return url + '?' + toParams(componentType, hash, matchState, proprietary, coordinates);
    };
  }

  var features = ["policy", "labels", "release-graph", "policy-violations", "notification", "reevaluate-policy",
                  "component-identifier"],// Lowercase
      param = window.$ ? $.param : function(obj) {
        var string = '',
            field;
        for (field in obj) {
          if (obj[field]) {
            string += '&' + encodeURIComponent(field) + '=' + encodeURIComponent(obj[field]);
          }
        }
        return string.substring(1);
      },
      basePath = (function() {
        var scripts = window.document.getElementsByTagName('script'),
            index;
        if (scripts.length) {
          for (var i = 0; i < scripts.length; i++) {
            if (scripts[i].src) {
              index = scripts[i].src.indexOf('policy-assets/js/brain.client.js');
              if (index == -1) {
                index = scripts[i].src.indexOf('assets/js/brain.client.js');
              }

              if (index != -1) {
                return scripts[i].src.substring(0, index);
              }
            }
          }
        }
        return '/';
      }());

  window.Brain = {

    /**
     * This is only for unit testing
     * @since version 1.12
     * @param newBasePath - the new BasePath
     */
    "setBasePath": function(newBasePath){
      basePath = newBasePath;
    },
    /**
     * Check if the Brain instance supports a feature
     * @since version 1.1
     */
    "hasFeature": function(feature) {
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
    'getApplicationListUrl' : function () {
      return basePath + 'rest/application/services/names';
    },
    /**
     * Get the Brain's version.
     * @since version 1.1
     */
    "getVersion": function() {
      return "${project.version}";
    },
    'ci': {
      /**
       * Get the URL for the agnostic coordinate ComponentDetails resource
       *
       * @since version 1.13
       */
      'getComponentUrl' : createComponentUrl('ci'),
      /**
       * Get the URL for the agnostic coordinate ComponentDetailsList resource
       *
       * @since version 1.13
       */
      'getComponentListUrl' : createComponentListUrl('ci')
    },
    'ide': {
      /**
       * Get the URL for the agnostic coordinate ComponentDetails resource
       *
       * @since version 1.13
       */
      'getComponentUrl' : createComponentUrl('ide'),
      /**
       * Get the URL for the agnostic coordinate ComponentDetailsList resource
       *
       * @since version 1.13
       */
      'getComponentListUrl' : createComponentListUrl('ide')
    },
    'rm' : {
      /**
       * Get the URL for the agnostic coordinate ComponentDetails resource
       *
       * @since version 1.13
       */
      'getComponentUrl' : createComponentUrl('rm'),
      /**
       * Get the URL for the agnostic coordinate ComponentDetailsList resource
       *
       * @since version 1.13
       */
      'getComponentListUrl' : createComponentListUrl('rm')
    },
    /**
     * Get the URL for the vulnerability detail content
     *
     * @since version 1.14
     */
    'getVulnerabilityDetailUrl' : function (source, refId) {
      return '/rest/vulnerability/details/' + encodeURIComponent(source) + '/' + encodeURIComponent(refId);
    }
  };
}());
