/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global ActiveXObject */

window.messageTemplate = {
  type: 'error',
  msg: 'Something bad happened!',
};
window.AngularUtils = {
  alphaSort: function (array, descending, sortProperty) {
    if (array) {
      array.sort(function (a, b) {
        var aProp = sortProperty ? a[sortProperty] : a,
          bProp = sortProperty ? b[sortProperty] : b;
        if (aProp < bProp) {
          return descending ? 1 : -1;
        } else if (aProp > bProp) {
          return descending ? -1 : 1;
        } else {
          return 0;
        }
      });
    }
  },
  hasFlash: function () {
    try {
      if (new ActiveXObject('ShockwaveFlash.ShockwaveFlash')) {
        return true;
      }
    } catch (e) {
      if (navigator.mimeTypes['application/x-shockwave-flash'] !== undefined) {
        return true;
      }
    }
    return false;
  },
  endsWith: function (str, check) {
    return str.indexOf(check, str.length - check.length) > -1;
  },
  formatPercentage: function (count, total, decimalCount) {
    if (!total) {
      return '0';
    }

    return ((count / total) * 100).toFixed(decimalCount ? decimalCount : 0);
  },
  /**
   * Format a message suitable for the clmAlerts directive
   * @param msg
   * @param type one of: error, warning, success, info
   * @returns {*}
   * @since 1.12
   */
  toAlert: function (msg, type) {
    return angular.extend({}, window.messageTemplate, type ? { type: type, msg: msg } : { msg: msg });
  },
};

window.AngularStateUtils = {
  toParentStateIfNewItem: function (scope) {
    if (scope.$state.current.name.indexOf('.new') > -1) {
      scope.$state.go(scope.$state.current.parent);
    }
  },
  fnOnNewItemState: function (scope, fn) {
    scope.$watch('$state.current.name', function (value) {
      if (value.indexOf('.new') > -1) {
        fn();
      }
    });
  },
  toNewItemState: function (scope) {
    //if user clicks new while the new state is already active
    //(or multiple events are fired causing this method to be called multiple times)
    //it will now only act once, rather than generating multiple .new suffixes
    if (scope.$state.current.name && !window.AngularUtils.endsWith(scope.$state.current.name, '.new')) {
      scope.$state.go(scope.$state.current.name + '.new');
    }
  },
};
