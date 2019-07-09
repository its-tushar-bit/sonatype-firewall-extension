/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, window, clmBuildTimestamp, jQuery, $ */
import CommonServicesModule from './CommonServices';

export let maximizeHeightServiceInstance;

var angularCommon = angular.module('AngularCommon',
    [CommonServicesModule.name, 'ui.bootstrap', 'ngSanitize', 'vs-repeat']);

angularCommon.controller('DeleteResourceController', ['$scope', '$http', 'CLMContextLocations', 'selected',
  function ($scope, $http, CLMContextLocations, selected) {
    $scope.deletedEnabled = true;
    $scope.selected = selected;

    $scope.doDelete = function() {
      $scope.deletedEnabled = false;
      selected.$delete().then(function () {
        $scope.$close();
      }, function () {
        $scope.$dismiss(arguments);
      });
    };
  }]);

angularCommon.factory('regexFactory', function() {
  return {
    allLetters: function() {
      // eslint-disable-next-line max-len
      return (/\u0041-\u005A\u0061-\u007A\u00AA\u00B5\u00BA\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02C1\u02C6-\u02D1\u02E0-\u02E4\u02EC\u02EE\u0370-\u0374\u0376\u0377\u037A-\u037D\u0386\u0388-\u038A\u038C\u038E-\u03A1\u03A3-\u03F5\u03F7-\u0481\u048A-\u0527\u0531-\u0556\u0559\u0561-\u0587\u05D0-\u05EA\u05F0-\u05F2\u0620-\u064A\u066E\u066F\u0671-\u06D3\u06D5\u06E5\u06E6\u06EE\u06EF\u06FA-\u06FC\u06FF\u0710\u0712-\u072F\u074D-\u07A5\u07B1\u07CA-\u07EA\u07F4\u07F5\u07FA\u0800-\u0815\u081A\u0824\u0828\u0840-\u0858\u08A0\u08A2-\u08AC\u0904-\u0939\u093D\u0950\u0958-\u0961\u0971-\u0977\u0979-\u097F\u0985-\u098C\u098F\u0990\u0993-\u09A8\u09AA-\u09B0\u09B2\u09B6-\u09B9\u09BD\u09CE\u09DC\u09DD\u09DF-\u09E1\u09F0\u09F1\u0A05-\u0A0A\u0A0F\u0A10\u0A13-\u0A28\u0A2A-\u0A30\u0A32\u0A33\u0A35\u0A36\u0A38\u0A39\u0A59-\u0A5C\u0A5E\u0A72-\u0A74\u0A85-\u0A8D\u0A8F-\u0A91\u0A93-\u0AA8\u0AAA-\u0AB0\u0AB2\u0AB3\u0AB5-\u0AB9\u0ABD\u0AD0\u0AE0\u0AE1\u0B05-\u0B0C\u0B0F\u0B10\u0B13-\u0B28\u0B2A-\u0B30\u0B32\u0B33\u0B35-\u0B39\u0B3D\u0B5C\u0B5D\u0B5F-\u0B61\u0B71\u0B83\u0B85-\u0B8A\u0B8E-\u0B90\u0B92-\u0B95\u0B99\u0B9A\u0B9C\u0B9E\u0B9F\u0BA3\u0BA4\u0BA8-\u0BAA\u0BAE-\u0BB9\u0BD0\u0C05-\u0C0C\u0C0E-\u0C10\u0C12-\u0C28\u0C2A-\u0C33\u0C35-\u0C39\u0C3D\u0C58\u0C59\u0C60\u0C61\u0C85-\u0C8C\u0C8E-\u0C90\u0C92-\u0CA8\u0CAA-\u0CB3\u0CB5-\u0CB9\u0CBD\u0CDE\u0CE0\u0CE1\u0CF1\u0CF2\u0D05-\u0D0C\u0D0E-\u0D10\u0D12-\u0D3A\u0D3D\u0D4E\u0D60\u0D61\u0D7A-\u0D7F\u0D85-\u0D96\u0D9A-\u0DB1\u0DB3-\u0DBB\u0DBD\u0DC0-\u0DC6\u0E01-\u0E30\u0E32\u0E33\u0E40-\u0E46\u0E81\u0E82\u0E84\u0E87\u0E88\u0E8A\u0E8D\u0E94-\u0E97\u0E99-\u0E9F\u0EA1-\u0EA3\u0EA5\u0EA7\u0EAA\u0EAB\u0EAD-\u0EB0\u0EB2\u0EB3\u0EBD\u0EC0-\u0EC4\u0EC6\u0EDC-\u0EDF\u0F00\u0F40-\u0F47\u0F49-\u0F6C\u0F88-\u0F8C\u1000-\u102A\u103F\u1050-\u1055\u105A-\u105D\u1061\u1065\u1066\u106E-\u1070\u1075-\u1081\u108E\u10A0-\u10C5\u10C7\u10CD\u10D0-\u10FA\u10FC-\u1248\u124A-\u124D\u1250-\u1256\u1258\u125A-\u125D\u1260-\u1288\u128A-\u128D\u1290-\u12B0\u12B2-\u12B5\u12B8-\u12BE\u12C0\u12C2-\u12C5\u12C8-\u12D6\u12D8-\u1310\u1312-\u1315\u1318-\u135A\u1380-\u138F\u13A0-\u13F4\u1401-\u166C\u166F-\u167F\u1681-\u169A\u16A0-\u16EA\u1700-\u170C\u170E-\u1711\u1720-\u1731\u1740-\u1751\u1760-\u176C\u176E-\u1770\u1780-\u17B3\u17D7\u17DC\u1820-\u1877\u1880-\u18A8\u18AA\u18B0-\u18F5\u1900-\u191C\u1950-\u196D\u1970-\u1974\u1980-\u19AB\u19C1-\u19C7\u1A00-\u1A16\u1A20-\u1A54\u1AA7\u1B05-\u1B33\u1B45-\u1B4B\u1B83-\u1BA0\u1BAE\u1BAF\u1BBA-\u1BE5\u1C00-\u1C23\u1C4D-\u1C4F\u1C5A-\u1C7D\u1CE9-\u1CEC\u1CEE-\u1CF1\u1CF5\u1CF6\u1D00-\u1DBF\u1E00-\u1F15\u1F18-\u1F1D\u1F20-\u1F45\u1F48-\u1F4D\u1F50-\u1F57\u1F59\u1F5B\u1F5D\u1F5F-\u1F7D\u1F80-\u1FB4\u1FB6-\u1FBC\u1FBE\u1FC2-\u1FC4\u1FC6-\u1FCC\u1FD0-\u1FD3\u1FD6-\u1FDB\u1FE0-\u1FEC\u1FF2-\u1FF4\u1FF6-\u1FFC\u2071\u207F\u2090-\u209C\u2102\u2107\u210A-\u2113\u2115\u2119-\u211D\u2124\u2126\u2128\u212A-\u212D\u212F-\u2139\u213C-\u213F\u2145-\u2149\u214E\u2183\u2184\u2C00-\u2C2E\u2C30-\u2C5E\u2C60-\u2CE4\u2CEB-\u2CEE\u2CF2\u2CF3\u2D00-\u2D25\u2D27\u2D2D\u2D30-\u2D67\u2D6F\u2D80-\u2D96\u2DA0-\u2DA6\u2DA8-\u2DAE\u2DB0-\u2DB6\u2DB8-\u2DBE\u2DC0-\u2DC6\u2DC8-\u2DCE\u2DD0-\u2DD6\u2DD8-\u2DDE\u2E2F\u3005\u3006\u3031-\u3035\u303B\u303C\u3041-\u3096\u309D-\u309F\u30A1-\u30FA\u30FC-\u30FF\u3105-\u312D\u3131-\u318E\u31A0-\u31BA\u31F0-\u31FF\u3400-\u4DB5\u4E00-\u9FCC\uA000-\uA48C\uA4D0-\uA4FD\uA500-\uA60C\uA610-\uA61F\uA62A\uA62B\uA640-\uA66E\uA67F-\uA697\uA6A0-\uA6E5\uA717-\uA71F\uA722-\uA788\uA78B-\uA78E\uA790-\uA793\uA7A0-\uA7AA\uA7F8-\uA801\uA803-\uA805\uA807-\uA80A\uA80C-\uA822\uA840-\uA873\uA882-\uA8B3\uA8F2-\uA8F7\uA8FB\uA90A-\uA925\uA930-\uA946\uA960-\uA97C\uA984-\uA9B2\uA9CF\uAA00-\uAA28\uAA40-\uAA42\uAA44-\uAA4B\uAA60-\uAA76\uAA7A\uAA80-\uAAAF\uAAB1\uAAB5\uAAB6\uAAB9-\uAABD\uAAC0\uAAC2\uAADB-\uAADD\uAAE0-\uAAEA\uAAF2-\uAAF4\uAB01-\uAB06\uAB09-\uAB0E\uAB11-\uAB16\uAB20-\uAB26\uAB28-\uAB2E\uABC0-\uABE2\uAC00-\uD7A3\uD7B0-\uD7C6\uD7CB-\uD7FB\uF900-\uFA6D\uFA70-\uFAD9\uFB00-\uFB06\uFB13-\uFB17\uFB1D\uFB1F-\uFB28\uFB2A-\uFB36\uFB38-\uFB3C\uFB3E\uFB40\uFB41\uFB43\uFB44\uFB46-\uFBB1\uFBD3-\uFD3D\uFD50-\uFD8F\uFD92-\uFDC7\uFDF0-\uFDFB\uFE70-\uFE74\uFE76-\uFEFC\uFF21-\uFF3A\uFF41-\uFF5A\uFF66-\uFFBE\uFFC2-\uFFC7\uFFCA-\uFFCF\uFFD2-\uFFD7\uFFDA-\uFFDC/);
    }
  };
});

angularCommon.factory('commonCodeFactory', function() {
  return {
    // URI Encoded Query Parameter
    getEncodedQueryString: function(key) {
      var results = new RegExp('[\\?&]' + key + '=([^&#]*)').exec(window.location.search);
      if (results) {
        return results[1].replace(/\+/g, '%20');
      }
    }
  };
});

// Note that this implementation ignores subsequent changes to attribute values
angularCommon.directive('isDuplicate', [
  '$parse', function($parse) {
    return {
      require: 'ngModel',
      restrict: 'A',
      link: function(scope, elem, attr, ctrl) {
        var arrayNameParser = $parse(attr.isDuplicateArray),
            // Pretty rigid implementation. Assumes that the model is an field on a selected item which is
            // an item in an array of items (isDuplicateArray)
            modelObject = attr.ngModel.substr(0, attr.ngModel.lastIndexOf('.')),
            modelFieldParser = $parse(attr.ngModel.substr(attr.ngModel.lastIndexOf('.') + 1)),
            idFieldParser = $parse(attr.isDuplicateIdField),
            caseSensitive = attr.isDuplicateCaseSensitive;

        ctrl.$validators.duplicate = function(value) {
          if (!value) {
            return true;
          }
          var modelIdValue = idFieldParser($parse(modelObject)(scope)),
              array = arrayNameParser(scope);

          var passed = jQuery.grep(array, function(item) {
            if (!caseSensitive || caseSensitive === 'false') {
              return idFieldParser(item) !== modelIdValue && modelFieldParser(item) &&
                  modelFieldParser(item).toLowerCase() === value.toLowerCase();
            }
            else {
              return idFieldParser(item) !== modelIdValue && modelFieldParser(item) === value;
            }
          }).length <= 0;

          return passed;
        };

        // Allows validation to be invoked by code or user input
        scope.$watch(attr.ngModel, function(newValue) {
          if (typeof newValue !== 'undefined' && newValue !== null) {
            ctrl.$$parseAndValidate();
          }
        });
      }
    };
  }
]);

angularCommon.directive('clmButtons', [function () {
  return {
    scope: {
      acceptFn: '&',
      acceptDisabled: '&',
      cancelFn: '&',
      cancelDisabled: '&'
    },
    template: '<button class="btn btn-link btn-cancel" type="button" ng-click="decline()" ' +
                      'ng-disabled="disabled || cancelDisabled()">{{cancelText}}</button>' +
              '<button class="btn btn-primary pull-right" type="button" ng-click="accept()" ' +
                      'ng-disabled="disabled || acceptDisabled()">{{acceptText}}</button>',
    link: function(scope, element, attrs) {
      function buttonClick(clickFn) {
        return function () {
          function fn() {
            scope.disabled = false;
          }

          var promise = clickFn.apply(scope, []);
          if (promise) {
            scope.disabled = true;
            promise.then(fn, fn);
          }
        };
      }

      scope.disabled = false;
      scope.cancelText = 'Cancel';
      scope.acceptText = 'Save';

      attrs.$observe('cancelText', function (newVal) {
        scope.cancelText = newVal || 'Cancel';
      });

      attrs.$observe('acceptText', function (newVal) {
        scope.acceptText = newVal || 'Save';
      });

      scope.accept = buttonClick(scope.acceptFn);
      scope.decline = buttonClick(scope.cancelFn);
    }
  };
}]);

/**
 * Provides a half width error box for an HTTP error with a reload button.  This is intended for errors loading data.
 */
angularCommon.directive('loadError', [
  'Messages', function(messages) {
    return {
      restrict: 'A',
      priority: 99,
      template: '<div ng-show="error != null" class="iq-alert iq-alert--error">' +
                  '<i class="fa fa-warning"></i>' +
                  '<span>{{message || "An error occurred loading data."}} </span>' +
                  '<span ng-if="error">{{getDetails()}}</span>' +
                  '<div ng-if="canRetry" class="iq-btn-bar">' +
                    '<a href class="btn btn-error" ng-click="reload()"><i class="fa fa-refresh"></i>Retry</a>' +
                  '</div>' +
                '</div>',
      scope: {
        error: '=loadError',
        reload: '&reload',
        message: '=',
        canRetry: '=?'
      },
      link: function($scope) {
        if (angular.isUndefined($scope.canRetry)) {
          $scope.canRetry = true;
        }

        $scope.getDetails = function() {
          return messages.getHttpErrorMessage($scope.error);
        };
      }
    };
  }
]);

/**
 * Full width, closeable, alerts built from an array
 * @scope alerts {Object} An array of alerts, each an object with properties msg and type (error, success, info)
 * @scope noClose {Boolean} optionally hide the 'close' button
 */
angularCommon.directive('clmAlerts', function() {
  return {
    restrict: 'A',
    template: '<div class="alert {{ \'alert-\' + (alert.type || \'warning\') }}" ng-repeat="alert in alerts">' +
                '<button ng-if="!noClose" type="button" class="close" data-dismiss="alert">&times;</button>' +
                '<div class="alert-message">{{alert.msg}}</div>' +
              '</div>',
    scope: {
      alerts: '=clmAlerts',
      noClose: '=?'
    }
  };
});

/**
 * Ensure that a given value contains only valid name characters.
 */
angularCommon.directive('validNameCharacters', [
  'regexFactory', function(regexFactory) {
    var invalidNameCharactersRegex = new RegExp('[^-\\._' + regexFactory.allLetters().source + '0-9 ]', 'i');
    return {
      require: 'ngModel',
      restrict: 'A',
      link: function(scope, elem, attr, ctrl) {
        ctrl.$validators.validNameCharacters = function(value) {
          return !value || !value.match(invalidNameCharactersRegex);
        };
        // Allows validation to be invoked by code or user input
        scope.$watch(attr.ngModel, function(newValue) {
          if (typeof newValue !== 'undefined' && newValue !== null) {
            ctrl.$$parseAndValidate();
          }
        });
      }
    };
  }
]);

angularCommon.filter('filterReportColumns', function() {
  return function(items) {
    var arrayToReturn = [];
    if (items) {
      angular.forEach(items, function(item) {
        switch (item.stageName) {
          case 'Build':
          case 'Stage Release':
          case 'Release':
            arrayToReturn.push(item);
        }
      });
    }
    return arrayToReturn;
  };
});

angularCommon.directive('focusInput', ['$parse', function($parse) {
  return {
    require: '?^form',
    link: function(scope, element, attrs, form) {
      var model = $parse(attrs.focusInput);
      if (form) {
        scope.$watch(function() {
          return form.$pristine;
        }, function(newVal) {
          if (model(scope) && newVal) {
            element[0].focus();
          }
        });
      }
      scope.$watch(model, function(value) {
        if (value) {
          element[0].focus();
        }
      });
    }
  };
}]);

angularCommon.directive('clmInclude', [
  '$templateCache', '$http', '$compile', function($templateCache, $http, $compile) {
    return {
      link: function(scope, element, attrs) {
        var counter = 0,
            childScope;
        attrs.$observe('clmInclude', function(val) {
          if (val) {
            var changeCounter = ++counter;
            val = scope.$eval(val) + '?' + clmBuildTimestamp;
            $http.get(val, { cache: $templateCache }).then(function(response) {
              if (changeCounter === counter) {
                if (childScope) {
                  childScope.$destroy();
                }
                childScope = scope.$new();
                element.html(response.data);
                $compile(element.contents())(childScope);
                childScope.$emit('$includeContentLoaded');
              }
            }, function() {
              if (changeCounter === counter) {
                if (childScope) {
                  childScope.$destroy();
                }
                childScope = null;
                element.html('');
              }
            });
          }
        });
      }
    };
  }
]);

/**
 * Render four tiles representing threat levels. By default will only render tiles for set values,
 * unless 'always-show="true"' is specified.
 * Will add a small margin on the tiles by default, which can be overridden by specifying a 'margin' attribute.
 */
angularCommon.directive('chiclets', function() {
  return {
    scope: {
      critical: '=',
      severe: '=',
      moderate: '=',
      none: '='
    },
    template: '<span ng-show="critical || alwaysShow" class="{{baseClass}}" ng-class="{\'critical\': critical }" ' +
      'ng-style="style">{{ critical || "" }}</span>' +
      '<span ng-show="severe || alwaysShow" class="{{baseClass}}" ng-class="{\'severe\': severe }" ' +
      'ng-style="style">{{ severe || "" }}</span>' +
      '<span ng-show="moderate || alwaysShow" class="{{baseClass}}" ng-class="{\'moderate\': moderate }" ' +
      'ng-style="style">{{ moderate || "" }}</span>' +
      '<span ng-show="none || alwaysShow" class="{{baseClass}}" ng-class="{\'none\': none }" ' +
      'ng-style="style">{{ none || "" }}</span>',
    link: function(scope, element, attrs) {
      scope.style = {margin: attrs.margin || '2px'};
      scope.alwaysShow = attrs.alwaysShow || false;
      scope.baseClass = attrs.baseClass || 'iq-threat-indicator';
    }
  };
});

angularCommon.directive('hexagon', [
  function() {
    return {
      restrict: 'E',
      replace: true,
      template: '<svg class="hexagon" viewBox="0 0 185.5 208" preserveAspectRatio="xMidYMid meet">' +
      // eslint-disable-next-line max-len
      '<path d="M91.952,6.246L7.653,55l0.073,97.383l84.373,48.628l84.299-48.754l-0.073-97.382L91.952,6.246z M92.084,183.74"></path>' +
      '</svg>'
    };
  }
]);

/**
 * Used to ensure that model is updated when forms are filled using auto complete. This will occur when a password
 * is saved by a password manager but filled when selecting a username via a dropdown list.
 */
angularCommon.directive('autofill', ['$timeout', function($timeout) {
  return {
    require: 'ngModel',
    link: function(scope, element, attrs, ngModel) {
      function checkForChange() {
        if (!scope.$$destroyed) {
          var elementValue = element.val();
          if (elementValue !== ngModel.$modelValue) {
            ngModel.$setViewValue(elementValue);
          }
          $timeout(checkForChange, 100);
        }
      }
      $timeout(checkForChange, 100);
    }
  };
}]);

angularCommon.directive('breadcrumb', ['$state', 'state.history.service', function($state, StateHistoryService) {
  var defaultState = 'dashboard.overview.violations';
  var parentStates = [defaultState, 'dashboard.overview.components'];

  function getParentStateName(state) {
    const { name } = state,
        dotIndex = name.lastIndexOf('.'),
        nameBeforeDot = dotIndex === -1 ? undefined : name.slice(0, dotIndex);

    return nameBeforeDot;
  }

  return {
    scope: {
      stateOverride: '=?'
    },
    template: '<p class="nav-crumb"><span ng-repeat="state in states">' +
                '<span ng-if="!$first" ng-class="{ \'last-crumb\': $last }">/</span>' +
                  '<a ng-if="!$last" ui-sref="{{state.state}}">' +
                    '<i ng-if="state.icon" class="{{state.icon}}"></i>{{state.name}}' +
                  '</a>' +
                '<span ng-if="$last" class="last-crumb">' +
                  '<i ng-if="state.icon" class="{{state.icon}}"></i>' +
                  '&nbsp;{{stateOverride ? stateOverride : state.name}}' +
                '</span>' +
              '</p>',
    link: function(scope) {
      function loadCurrentState() {
        var state = $state.current;
        var states = [],
            previousState = StateHistoryService.getPreviousState();

        while (state && state.name) {
          // dashboard is an abstract state that can represented by parent states navigated from or the default state
          if (state.name === 'dashboard') {
            states.unshift({
              name: state.data.crumb,
              icon: 'sonatype-icons dashboard',
              state: previousState && $.inArray(previousState.name, parentStates) !== -1 ?
                previousState.name : defaultState
            });
          }
          // dashboard.overview is an abstract state that can be ignored in favor of its parent
          else if (state.name !== 'dashboard.overview') {
            states.unshift({
              name: state.data.crumb,
              state: state.name
            });
          }

          state = $state.get(getParentStateName(state));
        }
        scope.states = states;
      }

      scope.$on('$stateChangeSuccess', loadCurrentState);
      loadCurrentState();
    }
  };
}]);

angularCommon.service('Modal', ['$modal', function($modal) {
  return {
    open(config) {
      return $modal.open({
        windowClass: 'iq-modal',
        backdropClass: 'iq-modal-backdrop',
        animation: false,
        ...config
      });
    }
  };
}]);

angularCommon.service('Dialog', ['Modal', function (Modal) {
  function wrapClick(fn, scope, dismiss) {
    return function () {
      if (dismiss) {
        scope.$dismiss();
      }
      else {
        scope.$close();
      }
      if (fn) {
        fn();
      }
    };
  }
  var counter = 0;
  return {
    open: function (config) {
      config = angular.extend({
        backdrop: 'static',
        keyboard: false,
        template: '<div id="{{id}}"><div class="iq-modal-header"><h2>{{title}}</h2></div>' +
            '<div class="iq-modal-content" ng-bind-html="body"></div>' +
            '<div class="iq-modal-footer">' +
              '<button ng-repeat="button in buttons" ng-click="button.click()" ' +
              'ng-class="{' +
                '\'btn-error\' : button.type == \'danger\',' +
                '\'btn-primary\' : button.type == \'primary\', ' +
                '\'btn-link btn-cancel\' : button.type == \'cancel\'}" ' +
              'class="btn">{{button.name}}</button>' +
            '</div></div>',
        controller: ['$scope', function(scope) {
          // Since the buttons need to be set in the controller, the cancel button should appear last. This fixes a
          // browser bug where elements that are dynamically added float: right after others break to a new line
          for (var i = config.buttons.length - 1; i >= 0; i--) {
            var button = config.buttons[i];
            if (button.type === 'cancel') {
              config.buttons.splice(i, 1);
              config.buttons.push(button);
            }
          }
          scope.buttons = config.buttons;
          scope.title = config.title;
          scope.body = config.body;
          scope.id = config.id || ('dialog-' + counter++);

          angular.forEach(scope.buttons, function (button) {
            button.click = wrapClick(button.click, scope, button.dismiss);
          });
        }]
      }, config);

      return Modal.open(config);
    }
  };
}]);

angularCommon.service('ErrorDialog', ['Dialog', 'Messages', function (Dialog, Messages) {
  return {
    open: function (body, title) {
      if (typeof body !== 'string') {
        body = Messages.getHttpErrorMessage(body);
      }
      return Dialog.open({
        keyboard: true,
        title: title || 'Error',
        body: body,
        buttons: [{
          name: 'Close'
        }]
      });
    }
  };
}]);

/**
* Angular directive for bootstrap-multiselect. Does not do a collection watch for selected items so if this
* collection is modified outside of the multi select control, update to use $watchCollection
*/
angularCommon.directive('multiSelect', ['$compile', '$timeout', function($compile, $timeout) {
  return {
    template: '<div class="btn-group" ng-class="{ open : open }">' +
                 '<button class="btn dropdown-toggle" ng-click="toggleDropdown()" ' +
                         'ng-class="{ \'btn-small\': small }" type="button">' +
                   '<span><div>{{getText()}}</div></span> <span class="caret"></span></button>' +
                 '<ul class="dropdown-menu multiselect-container">' +
                   '<li ng-if="items.length > 9"><input type="text" ng-model="filter.name" ' +
                       'style="margin:0 auto 5px auto;width:160px;display:block" placeholder="Search" ' +
                       'ng-click="$event.stopPropagation()"></li>' +
                 '</ul>' +
               '</div>',
    scope: {
      items: '=',
      selectedIds: '=',
      effectiveIdField: '@',
      noneSelectedText: '@',
      summarizeWith: '@',
      small: '@?',
      useVsRepeat: '@?'
    },
    link: function(scope, element, attrs) {
      var effectiveIdField = scope.effectiveIdField ? scope.effectiveIdField : 'id';
      var dropdownScrollHtml =
          '<div class="dropdown-scroll">' +
            '<li ng-repeat="item in items | filter: { name : filter.name }" ' +
                // stopPropagation to avoid Bootstrap closing popout
                'ng-class="{ selected : selected[item.id]  }" ng-click="$event.stopPropagation()">' +
              '<label class="checkbox" ng-class="{ \'has-owner\': item.owner }">' +
                '<input type="checkbox" ng-model="selected[item.id]" ng-change="updateSelectedIds(item.id)">' +
                '<span ng-if="item.color" class="multi-dropdown-item-color {{item.color}}Label"></span>' +
                '<div class="multi-dropdown-item name" ' +
                     'ng-class="{ \'no-color\' : !item.color, color : item.color }">{{item.name}}</div>' +
                '<div ng-if="item.owner" class="multi-dropdown-item owner" ' +
                     'ng-class="{ \'no-color\' : !item.color, color : item.color }">in {{item.owner}}</div>' +
              '</label>' +
            '</li>' +
          '</div>';

      scope.toggleDropdown = function() {
        scope.open = !scope.open;
        if (scope.open && attrs.hasOwnProperty('useVsRepeat')) {
          $timeout(function() {
            scope.$emit('vsRepeatTrigger');
          });
        }
      };

      var dropdownScroll = angular.element(dropdownScrollHtml);
      if (attrs.hasOwnProperty('useVsRepeat')) {
        dropdownScroll.attr('vs-repeat', '');
      }
      $compile(dropdownScroll)(scope);
      element.find('ul').append(dropdownScroll);

      function updateSelection() {
        scope.selected = {};
        if (scope.selectedIds) {
          angular.forEach(scope.selectedIds, function (effectiveId) {
            var id = getIdFromEffectiveId(effectiveId);
            scope.selected[id] = true;
          });
        }
      }
      function hide(event) {
        if (scope.open) {
          var parents = $(event.target).parentsUntil(element);
          if (parents.length > 0 && parents[parents.length - 1].tagName === 'HTML') {
            scope.$applyAsync(function () {
              scope.open = false;
            });
          }
        }
      }

      function getEffectiveIdFromId(id) {
        for (var i = 0; i < scope.items.length; i++) {
          var item = scope.items[i];
          if (item.id === id) {
            return item[effectiveIdField];
          }
        }
        return id;
      }

      function getIdFromEffectiveId(effectiveId) {
        for (var i = 0; i < scope.items.length; i++) {
          var item = scope.items[i];
          if (item[effectiveIdField] === effectiveId) {
            return item.id;
          }
        }
        return effectiveId;
      }

      scope.getText = function () {
        // upon page load, getText() gets called before our watcher calls updateSelection() to sync scope.selected
        if (!scope.selectedIds || scope.selectedIds.length === 0 || !scope.selected) {
          return scope.noneSelectedText ? scope.noneSelectedText : 'None selected';
        }

        if (scope.summarizeWith && scope.selectedIds.length >= 3) {
          return scope.selectedIds.length + ' ' + scope.summarizeWith;
        }

        var names = '';
        angular.forEach(scope.items, function (item) {
          if (scope.selected[item.id]) {
            names += item.name + ', ';
          }
        });

        return names.substring(0, names.length - 2);
      };

      scope.updateSelectedIds = function (id) {
        var effectiveId = getEffectiveIdFromId(id);
        if (scope.selected[id]) {
          /* add */
          scope.selectedIds.push(effectiveId);
        }
        else if (scope.selectedIds) {
          /* remove */
          var index = scope.selectedIds.indexOf(effectiveId);
          if (index !== -1) {
            scope.selectedIds.splice(index, 1);
          }
        }
      };
      scope.filter = { name: '' };

      $(document).click(hide);
      scope.$watch('selectedIds', updateSelection);
      scope.$watch('items', updateSelection);
      scope.$on('$destroy', function () {
        $(document).unbind('click', hide);
      });
    }
  };
}]);

angularCommon.directive('threatClass', function() {
  return {
    scope: {
      threatClass: '<'
    },
    link: function(scope, element) {
      function updateClass(threatClass) {
        element.toggleClass('critical', threatClass >= 8);
        element.toggleClass('severe', threatClass >= 4 && threatClass < 8);
        element.toggleClass('moderate', threatClass >= 2 && threatClass < 4);
        element.toggleClass('none', threatClass === 1);
        element.toggleClass('ignore', threatClass < 1);
      }

      scope.$watch('threatClass', updateClass);
    }
  };
});

angularCommon.service('maximizeHeightService', ['$timeout', '$window', '$rootScope', 'stable.body.service',
  function ($timeout, $window, $rootScope, StableBodyService) {
    const maximizeHeightService = {
      maximizeHeight(element) {
        var timerId;

        // This directive can optionally be provided an argument that is taken as an initial height value to be
        // applied immediately (as opposed to after the page settles down).  This can help avoid certain race
        // conditions
        const initialHeight = element.attr('maximize-container-height');

        function debounce() {
          if (timerId) {
            $timeout.cancel(timerId);
          }
          timerId = $timeout(function() {
            var updateDimensionsTimerId = maximizeHeightService.updateDimensions(element, {
              bottomPadding: 0,
              checkBodyScroll: true,
              minHeight: 0
            });
            timerId = updateDimensionsTimerId || timerId;
          }, 20);
        }

        $rootScope.$on('recalculateContainerHeights', debounce);

        if (initialHeight) {
          element.css('height', initialHeight);
        }

        $($window).resize(debounce);
        StableBodyService.whenStable(debounce);
        debounce();

        return function() {
          if (timerId) {
            $timeout.cancel(timerId);
          }
          $($window).unbind('resize', debounce);
        };
      },

      getOffset: function (element) {
        return element.offset().top;
      },
      setDimensions: function (element, options) {
        const { overflowY } = element[0].style;

        options = angular.extend({
          bottomPadding: 35,
          checkBodyScroll: false,
          minHeight: 400
        }, options);

        // first decrease to min height in order to pre-remove any possible container scrollbars which might cause
        // narrowing and thus extra word-wrapping that can change the calculations
        element[0].style.height = options.minHeight + 'px';
        element[0].style.overflowY = 'hidden';

        var windowHeight = ($window.innerHeight) ? $window.innerHeight : $(document.body).height(),
            containerTop = this.getOffset(element) + parseInt(element.css('padding-top')),
            bottomPadding = options.bottomPadding,
            scrollWidth = options.checkBodyScroll ? (windowHeight - $('body')[0].clientHeight) : 0,
            height = Math.max(options.minHeight, windowHeight - containerTop - bottomPadding - scrollWidth);

        // use native CSS API to set height, not jquery, because jquery will only set the content-box height and we want
        // to set the border-box height (NOTE: this assumes box-sizing: border-box is applied to element)
        element[0].style.height = height + 'px';
        element[0].style.overflowY = overflowY;

      },
      updateDimensions: function (element, options) {
        var me = this;
        if (element.is(':visible')) {
          me.setDimensions(element, options);
        }
        // guard against infinitely checking an element that was removed from DOM
        else if (document.body.contains(element[0])) {
          return $timeout(function () {
            me.updateDimensions(element, options);
          }, 100);
        }
      }
    };

    return maximizeHeightService;
  }
]);

angularCommon.directive('maximizeHeight', ['$timeout', '$window', 'maximizeHeightService',
  function ($timeout, $window, maximizeHeightService) {
    return function(scope, element) {
      var timerId;

      function debounce() {
        if (timerId) {
          $timeout.cancel(timerId);
        }
        timerId = $timeout(function() {
          timerId = maximizeHeightService.updateDimensions(element) || timerId;
        }, 20);
      }

      scope.$on('$destroy', function() {
        if (timerId) {
          $timeout.cancel(timerId);
        }
        $($window).unbind('resize', debounce);
      });

      $timeout(debounce, 100);
      $($window).resize(debounce);

    };
  }]);

angularCommon.directive('maximizeContainerHeight', ['$timeout', '$window', 'maximizeHeightService',
  function ($timeout, $window, maximizeHeightService) {
    return function(scope, element) {
      const teardown = maximizeHeightService.maximizeHeight(element);

      scope.$on('$destroy', teardown);
    };
  }
]);

angularCommon.directive('entersubmit', function() {
  return {
    restrict: 'A',
    require: 'ngModel',
    scope: false,
    link: function(scope, element, attrs, ctrl) {
      element.bind('keydown', function(e) {
        if (e.keyCode === 13) { // Enter
          e.preventDefault();
          if (ctrl.$valid) {
            element.trigger('submit');
          }
        }
      });
    }
  };
});

angularCommon.run(['$injector', function($injector) {
  maximizeHeightServiceInstance = $injector.get('maximizeHeightService');
}]);

export default angularCommon;
