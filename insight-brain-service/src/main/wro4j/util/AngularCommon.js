/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, window, ActiveXObject, clmBuildTimestamp */
/* exported AngularUtils */

//global function
/* jshint strict:false */
var AngularUtils = {
  safeApply: function(scope, fn) {
    if (scope.$$phase || scope.$root.$$phase) {
      //already apply in progress, just call the function
      fn();
    }
    else {
      //otherwise wrap the function in apply
      scope.$apply(fn);
    }
  },
  alphaSort: function(array, descending, sortProperty) {
    if (array) {
      array.sort(function(a, b) {
        var aProp = sortProperty ? a[sortProperty] : a, bProp = sortProperty ? b[sortProperty] : b;
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
  hasFlash: function() {
    try {
      if (new ActiveXObject('ShockwaveFlash.ShockwaveFlash')) { return true; }
    } catch (e) {
      if (navigator.mimeTypes['application/x-shockwave-flash'] !== undefined) { return true; }
    }
    return false;
  }
};
/* jshint strict:true */
(function() {
  'use strict';

  var angularCommon;
  angularCommon = angular.module('AngularCommon', ['CommonServices']);

  angularCommon.directive('errorModal', function() {
    return {
      replace: true,
      scope: true,
      templateUrl: '../assets/components/errorModal.html?' + clmBuildTimestamp,
      link: function($scope, element) {
        function showError(errorResponse) {
          $scope.errorResponse = errorResponse;
          element.modal('show');
        }

        function showServerError(data, status, headersFn, config) {

          if (typeof data === 'object') {
            status = data.status;
            headersFn = data.headers;
            config = data.config;
            data = data.data;
          }
          var header = headersFn ? headersFn() : [];
          if (header['content-type'] && header['content-type'].indexOf('text/html') === 0) {
            $scope.errorResponse = 'Server Error';
          }
          else if (status === 0) {
            $scope.errorResponse = 'Unable to connect to CLM server';
          }
          else {
            $scope.errorResponse = data;
          }
          element.modal('show');
        }

        $scope.$on('showServerError', function(event, arg) {
          showServerError.apply(null, arg);
        });
        $scope.$on('showError', function(event, arg) {
          showError(arg);
        });
        $scope.hideError = function() {
          element.modal('hide');
        };
      }
    };
  });

  angularCommon.controller('DeleteResourceController', ['$scope', '$http', 'CLMAppLocations', 'selected', function ($scope, $http, CLMAppLocations, selected) {
    $scope.deletedEnabled = true;
    $scope.selected = selected;

    $scope.doDelete  = function() {
      $scope.deletedEnabled = false;
      selected.$delete().then(function () {
        $scope.$close();
      }, function () {
        $scope.$dismiss(arguments);
      });
    };
  }]);

  angularCommon.directive('typeAhead', [
    '$parse', function($parse) {
      return {
        restrict: 'A',
        require: '?ngModel',
        link: function postLink($scope, element, attrs, controller) {
          var source = $parse(attrs.typeAhead)($scope);
          $scope.$watch(attrs.typeAhead, function(newSource, oldSource) {
            if (oldSource !== newSource) {
              source = newSource;
            }
          });

          element.attr('data-provide', 'typeahead');
          element.typeahead({
            source: function() {
              return angular.isFunction(source) ? source.apply(this, arguments) : source;
            },
            updater: function(item) {
              if (controller) {
                $scope.$apply(function() {
                  controller.$setViewValue(item);
                });
              }
              return item;
            }
          });
        }
      };
    }
  ]);

  angularCommon.factory('regexFactory', function() {
    return {
      allLetters: function() {
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
          // Pretty rigid implementation. Assumes that the model is an field on a selected item which is an item in an array of items (isDuplicateArray)
              modelObject = attr.ngModel.substr(0, attr.ngModel.lastIndexOf('.')),
              modelFieldParser = $parse(attr.ngModel.substr(attr.ngModel.lastIndexOf('.') + 1)),
              idFieldParser = $parse(attr.isDuplicateIdField),
              caseSensitive = attr.isDuplicateCaseSensitive;

          var validator = function(value) {
            if (!value) {
              ctrl.$setValidity('duplicate', true);
              return undefined;
            }
            var modelIdValue = idFieldParser($parse(modelObject)(scope)),
                array = arrayNameParser(scope);

            var passed = jQuery.grep(array,function(item) {
              if (!caseSensitive || caseSensitive === 'false') {
                return idFieldParser(item) !== modelIdValue && modelFieldParser(item) &&
                  modelFieldParser(item).toLowerCase() === value.toLowerCase();
              } else {
                return idFieldParser(item) !== modelIdValue && modelFieldParser(item) === value;
              }
            }).length <= 0;
            ctrl.$setValidity('duplicate', passed);

            return passed ? value : undefined;
          };

          ctrl.$parsers.push(validator);

          // Allows validation to be invoked by code or user input
          scope.$watch(attr.ngModel, function(newValue) {
            if (typeof newValue !== 'undefined') {
              validator(newValue);
            }
          });
        }
      };
    }
  ]);

  /**
   * Creates a URL which is relative to the current page.  (..) ar accepted
   */
  angularCommon.directive('relativeHref', [
    '$location', function($location) {
      return {
        restrict: 'A',
        priority: 99,
        link: function($scope, element, attr) {
          function updateValue() {
            var basePath = $location.path(),
                path = attr.relativeHref,
                resolved = [];

            if (basePath) {
              if (path.charAt(0) === '/') {
                path = path.substring(1);
              }
              resolved.push.apply(resolved, basePath.split('/'));
              if (resolved[resolved.length] === '') {
                resolved.pop();
              }

              angular.forEach(path.split('/'), function(segment) {
                if ('..' === segment) {
                  resolved.splice(-1, 1);
                }
                else {
                  resolved.push(segment);
                }
              });
              attr.$set('href', '#' + resolved.join('/'));
            }
          }

          attr.$observe('relativeHref', updateValue);
          $scope.$watch('location.url()', updateValue);
        }
      };
    }
  ]);

  angularCommon.directive('formSubmit', function($parse) {
    return {
      restrict: 'A',
      link: function($scope, element, attrs) {
        var submitFn = $parse(attrs.formSubmit);
        element[0].onsubmit = function() {
          return submitFn($scope);
        };
      }
    };
  });

  angularCommon.directive('clmButtons', [function () {
    return {
      scope : {
        acceptFn : '&',
        acceptDisabled : '&',
        cancelFn : '&',
        cancelDisabled : '&'
      },
      template : '<button class="btn" type="button" ng-click="decline()" ng-disabled="disabled || cancelDisabled()">{{cancelText}}</button> ' +
                 '<button class="btn btn-primary" type="button" ng-click="accept()" ng-disabled="disabled || acceptDisabled()">{{acceptText}}</button>',
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
        template: '<div ng-show="error != null" class="alert alert-error clm-error"><p><strong>Error</strong></p>' +
            '<p><span>An error occurred loading data. </span>' +
            '<span ng-switch on="error.status">' +
            '<span ng-switch-when="0">(Unable to reach CLM server)</span>' +
            '<span ng-switch-default>({{getDetails()}})</span>' +
            '</span></p>' +
            '<p><button type="button" class="btn btn-danger" ng-click="reload()">Reload</button></p></div>',
        scope: {
          error: '=loadError',
          reload: '&reload'
        },
        link: function($scope) {
          $scope.getDetails = function() {
            return messages.getHttpErrorMessage($scope.error);
          };
        }
      };
    }
  ]);

  /**
   * Full width closeable bootstrap alerts built from an array
   */
  angularCommon.directive('clmAlerts', [
    function() {
      return {
        restrict: 'A',
        priority: 99,
        template: '<div alert ng-repeat="alert in alerts" type="alert.type" close="closeAlert($index)">{{alert.msg}}</div>',
        scope: {
          alerts: '=clmAlerts'
        },
        link: function($scope) {
          $scope.closeAlert = function(index) {
            $scope.alerts.splice(index, 1);
          };
        }
      };
    }
  ]);

  /**
   * Ensure that a given value is all alphanumeric characters.
   */
  angularCommon.directive('alphaNumeric', [
    'regexFactory', function(regexFactory) {
      var alphaNumericRegex = new RegExp('[^-' + regexFactory.allLetters().source + '0-9 ]', 'i');
      return {
        require: 'ngModel',
        restrict: 'A',
        link: function(scope, elem, attr, ctrl) {
          var validator = function(value) {
            var passed = !value || !value.match(alphaNumericRegex);
            ctrl.$setValidity('alphaNumeric', passed);
            return passed ? value : undefined;
          };
          ctrl.$parsers.push(validator);
          // Allows validation to be invoked by code or user input
          scope.$watch(attr.ngModel, function(newValue) {
            if (typeof newValue !== 'undefined') {
              validator(newValue);
            }
          });
        }
      };
    }
  ]);

  /**
   * Ensure that the value of the input matches the value from another specified input
   */
  angularCommon.directive('match', function() {
    return {
      restrict: 'A',
      require: 'ngModel',
      scope: false,
      priority: 99,
      link: function(scope, elm, attrs, ctrl) {
        function validate (newVal) {
          var match = !elm.val() || attrs.match === elm.val();

          ctrl.$setValidity('match', match);

          return match ? newVal : undefined;
        }

        ctrl.$parsers.unshift(validate);
        attrs.$observe('match', validate);
      }
    };
  });

  /**
   * Ensure that a given value does not :
   * - have leading whitespace
   * - have trailing whitespace
   * - contain double spaces or tabs
   */
  angularCommon.directive('hasWhitespace', [
    '$parse', '$timeout', function($parse, $timeout) {
      return {
        require: 'ngModel',
        restrict: 'A',
        link: function(scope, elem, attr, ctrl) {
          function checkWhitespace(value) {
            if (typeof value === 'undefined') {
              if (elem.data('editable')) {
                value = elem.data('editable').input.$input.val();
              }
              else {
                value = elem.val();
              }
            }
            var whitespacePass = value.match(/^ | {2,}|\t| $/);
            suggestionModel.assign(scope,
                (value || '').replace(/ {2,}/g, ' ').replace(/^ | $/g, '').replace(/\t/g, ''));
            ctrl.$setValidity('spaces', !whitespacePass);
            return whitespacePass ? value : undefined;
          }

          var failed = null;
          var suggestionModel = $parse(attr.hasWhitespace);
          elem.on('keyup', function() {
            if (failed) {
              scope.$apply(function() {
                failed = checkWhitespace();
              });
            }
          });
          elem.on('blur', function() {
            scope.$apply(function() {
              failed = checkWhitespace();
            });
          });
          elem.on('click.editable', function() {
            $timeout(function() {
              if (elem.data('editable')) {
                elem.data('editable').input.$input.on('keyup', function() {
                  if (failed) {
                    scope.$apply(function() {
                      failed = checkWhitespace();
                    });
                  }
                });
                elem.data('editable').input.$input.on('blur', function() {
                  scope.$apply(function() {
                    failed = checkWhitespace();
                  });
                });
              }
            }, 100);
            return true;
          });

          // Allows validation to be invoked by code or user input
          scope.$watch(attr.ngModel, function(newValue) {
            newValue = newValue || '';
            checkWhitespace(newValue);
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
          switch (item.name) {
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

  /**
   * Common component for threat drop downs
   */
  angularCommon.directive('threatBox', function() {
    return {
      restrict: 'A',
      scope: {
        selected: '='
      },
      template: '<span class="threat-level-dropdown dropdown">' +
          '<a class="btn dropdown-toggle threat-level-{{selected}}" data-toggle="dropdown" href="#">' +
          '{{selected}} <span class="caret"></span>' +
          '</a>' +
          '<ul class="dropdown-menu">' +
          '<li ng-repeat="threatLevel in threatLevels">' +
          '<a ng-click="select(10 - $index)" class="threat-level-{{10 - $index}}">{{ threatLevel }}</a>' +
          '</li>' +
          '</ul>' +
          '</span>',
      link: function(scope, element, attrs) {
        scope.threatLevels = [10, 9, 8, 7, 6, 5, 4, 3, 2, attrs.one || 1, attrs.zero || 0];
        scope.select = function(threat) {
          scope.selected = threat;
        };
      }
    };
  });

  angularCommon.directive('labelDropDown', function() {
    return {
      restrict: 'A',
      scope: {
        model: '=ngModel',
        options: '=',
        multiple: '='
      },
      require: 'ngModel',
      template: '<span class="label-drop-down dropdown"><a class="btn dropdown-toggle clmLabel-dropdown {{ selectedLabel.color }}Label" data-toggle="dropdown" href="#">' +
          '{{ selectedLabel.label }}<span class="caret pull-right"></span>' +
          '</a>' +
          '<ul class="dropdown-menu">' +
          '<li ng-repeat="label in options">' +
          '<a ng-click="selectLabel(label)" class="clmLabel-dropdown {{ label.color }}Label" tooltip="{{ label.description }}" tooltip-placement="right">{{ label.label }}</a>' +
          '</li>' +
          '</ul>' +
          '</span>',
      link: function(scope) {
        for (var i = 0; i < scope.options.length; i++) {
          var option = scope.options[i];
          if (option.id === scope.model) {
            scope.selectedLabel = option;
            break;
          }
        }

        scope.selectLabel = function(label) {
          scope.selectedLabel = label;
          scope.model = label.id;
        };
      }
    };
  });

  /**
   * Delay allowing overflow on accordion elements to preserve the expand/collapse animation effect
   */
  angularCommon.directive('delayedOverflow', ['$timeout', function($timeout) {
    return {
      restrict: 'A',
      // Since accordion creates its own scope, an attribute needs to be used rather than scope. Because of this,
      // attribute should take a rendered value {{ ... }} rather than a scope field name
      link: function(scope, element, attrs) {
        attrs.$observe('delayedOverflow', function() {
          if (attrs.delayedOverflow === 'true') {
            $timeout(function() {
              element.css('overflow', 'visible');
            }, 100);
          } else {
            element.css('overflow', 'hidden');
          }
        });
      }
    };
  }]);

  angularCommon.directive('focusInput', ['$parse', function($parse) {
    return {
      link: function(scope, element, attrs) {
        var model = $parse(attrs.focusInput);
        scope.$watch(model, function(value) {
          if(value) {
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
              $http.get(val, { cache: $templateCache }).success(function(response) {
                if (changeCounter === counter) {
                  if (childScope) {
                    childScope.$destroy();
                  }
                  childScope = scope.$new();
                  element.html(response);
                  $compile(element.contents())(childScope);
                  childScope.$emit('$includeContentLoaded');
                }
              }).error(function() {
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

  angularCommon.directive('firefoxInputClick', function() {
    return {
      link: function(scope, element) {
        // Firefox v21 and below have a bug where clicking a file input label does not open the file input dialog
        // https://bugzilla.mozilla.org/show_bug.cgi?id=838695
        if (navigator.userAgent.indexOf('Firefox') !== -1 &&
            navigator.userAgent.substring(navigator.userAgent.lastIndexOf('/') + 1) < 22) {
          element.on('click', function() {
            angular.element('#' + element.attr('for')).click();
          });
        }
      }
    };
  });

  /**
   * Render four tiles representing threat levels. By default will only render tiles for set values, unless 'always-show="true"'
   * is specified.
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
      template: '<span ng-show="critical || alwaysShow" class="{{baseClass}}" ng-class="{\'threat-chiclet-critical\': critical }" ' +
        'ng-style="style">{{ critical || "" }}</span>' +
        '<span ng-show="severe || alwaysShow" class="{{baseClass}}" ng-class="{\'threat-chiclet-severe\': severe }" ' +
        'ng-style="style">{{ severe || "" }}</span>' +
        '<span ng-show="moderate || alwaysShow" class="{{baseClass}}" ng-class="{\'threat-chiclet-moderate\': moderate }" ' +
        'ng-style="style">{{ moderate || "" }}</span>' +
        '<span ng-show="none || alwaysShow" class="{{baseClass}}" ng-class="{\'threat-chiclet-none\': none }" ' +
        'ng-style="style">{{ none || "" }}</span>',
      link: function(scope, element, attrs) {
        scope.style = {margin: attrs.margin || '2px'};
        scope.alwaysShow = attrs.alwaysShow || false;
        scope.baseClass = attrs.baseClass || 'threat-chiclet';
      }
    };
  });

  angularCommon.directive('refreshButton', ['$timeout', '$parse', function($timeout, $parse) {
    var timer, degree = 0;

    function rotate(element) {
      element.css({ transform: 'rotate(' + degree + 'deg)'});
      timer = $timeout(function() {
        ++degree;
        rotate(element);
      }, 15);
    }

    function refreshComplete(scope) {
      $timeout.cancel(timer);
      scope.refreshing = false;
    }

    return {
      scope: {
        refreshTooltip: '='
      },
      template: '<a class="btn btn-mini btn-bottom" ng-show="!refreshing"><i class="icon-refresh"></i></a>' +
          '<a class="btn btn-mini" ng-show="refreshing" tooltip="{{refreshTooltip}}" tooltip-placement="bottom"><i class="icon-refresh"></i></a>',
      link: function(scope, element, attr) {
        var deferredFunction = $parse(attr.refreshButton);
        scope.refreshing = false;
        element.bind('click', function() {
          if (scope.refreshing) {
            return;
          }
          scope.refreshing = true;
          rotate(element.find('i'));
          deferredFunction(scope.$parent).then(function() {
            refreshComplete(scope);
          }, function() {
            refreshComplete(scope);
          });
        });
      }
    };
  }]);

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
  
  angularCommon.service('Dialog', ['$modal', function ($modal) {
    function wrapClick(fn, scope) {
      return function () {
        scope.$close();
        if (fn) {
          fn();
        }
      };
    }
    return {
      open : function (config) {
        config = angular.extend({
          backdrop: 'static',
          keyboard : false,
          template: '<div class="modal-header"><button type="button" class="close" ng-click="$close()">&times</button><h3>{{title}}</h3></div>' +
              '<div class="modal-body"><p>{{body}}</p></div>' +
              '<div class="modal-footer">' +
                '<button ng-repeat="button in buttons" ng-class="{\'btn-danger\' : button.type == \'danger\',\'btn-primary\' : button.type == \'primary\'}" class="btn" ng-click="button.click()">{{button.name}}</button>' +
              '</div>',
          controller: ['$scope', function(scope) {
            scope.buttons = config.buttons;
            scope.title = config.title;
            scope.body = config.body;

            angular.forEach(scope.buttons, function (button) {
              button.click = wrapClick(button.click, scope);
            });
          }]
        }, config);

        return $modal.open(config);
      }
    };
  }]);

  /**
  * Angular directive for bootstrap-multiselect
  */
  angularCommon.directive('multiSelect', ['$timeout', function($timeout) {
    return {
      scope: {
        items: '=',
        selectedIds: '='
      },
      template: '<select multiple="multiple"><option ng-repeat="item in items" value="{{item.id}}">{{ item.name }}</option></select>',
      link: function(scope, element) {
        var selectElement = element.children('select');
        function updateItems() {
          $timeout(function() {
            selectElement.multiselect('rebuild');
          }, 0);
        }
        function updateSelections() {
          if (!scope.selectedIds) {
            return;
          }
          for (var i = 0; i < scope.items.length; i++) {
            if (scope.selectedIds.indexOf(scope.items[i].id) > -1) {
              selectElement.multiselect('select', scope.items[i].id);
            } else {
              selectElement.multiselect('deselect', scope.items[i].id);
            }
          }
        }
        scope.$watch('items', updateItems);
        scope.$watch('selectedIds', updateSelections);
        selectElement.multiselect({
          onChange: function(element, checked) {
            if (!scope.selectedIds) {
              return;
            }
            if (!checked) {
              for (var i = 0; i < scope.selectedIds.length; i++) {
                if (scope.selectedIds[i] === element.attr('value')) {
                  scope.selectedIds.splice(i, 1);
                }
              }
            } else {
              scope.selectedIds.push(element.attr('value'));
            }
          }
        });
      }
    };
  }]);
}());

(function() {
  'use strict';

  var services = angular.module('CommonServices', []);

  services.service('Messages', function() {
    return {
      getHttpErrorMessage: function(args) {
        if (!args) {
          return;
        }
        if (angular.isArray(args) || args.toString() === '[object Arguments]') {
          args = {
            status: args[1],
            data: args[0],
            headers: args.length >= 3 ? args[2] : null
          };
        }
        var message = '',
            headers = args.headers ? args.headers() : null;
        if (args.status === 0 || args.status >= 1000) {
          message = 'Unable to reach CLM server';
        }
        else if (args.data && (!headers || !headers['content-type'] || headers['content-type'].indexOf('text/html') === -1)) {
          message = args.data;
        }
        // Angular misses statusText (cf. https://github.com/angular/angular.js/pull/2665), so at least ensure message for typical proxy errors
        else if (args.status === 502) {
          message = 'Bad Gateway';
        }
        else if (args.status === 503) {
          message = 'Service Unavailable';
        }
        else if (args.status === 504) {
          message = 'Gateway Timeout';
        }
        else {
          message = 'Error ' + args.status;
        }
        return message;
      }
    };
  });

  services.filter('ago', function() {
    return function(date) {
      var ago = '',
          diff,
          unit,
          val;

      if (!date) {
        return ago;
      }
      diff = new Date().getTime() - date;

      if (diff > 12 * 30 * 24 * 60 * 60 * 1000) {
        val = diff / (12 * 30 * 24 * 60 * 60 * 1000);
        unit = 'Year';
      }
      else if (diff > 30 * 24 * 60 * 60 * 1000) {
        val = diff / (30 * 24 * 60 * 60 * 1000);
        unit = 'Month';
      }
      else if (diff > 24 * 60 * 60 * 1000) {
        val = diff / (24 * 60 * 60 * 1000);
        unit = 'Day';
      }
      else if (diff > 60 * 60 * 1000) {
        val = diff / (60 * 60 * 1000);
        unit = 'Hour';
      }
      else if (diff > 60 * 1000) {
        val = diff / (60 * 1000);
        unit = 'Minute';
      }
      else {
        return 'Seconds Ago';
      }
      val = Math.floor(val);
      if (val > 1) {
        unit += 's';
      }
      return val + ' ' + unit + ' Ago';
    };
  });

  /**
   * Filter strings to fit within a set length, padding the end with ellipsis.
   * Default length is 25, but can be overridden.
   * i.e.
   * {{ value | truncate:20 }}
   *
   * Generally if you are filtering text, it's to ensure that it fits within some boundary element. CSS rules for that
   * element should take into account the possibility of increased font sizes on client machines and prefer to specify
   * boundary sizes in em.
   */
  services.filter('truncate', function () {
      return function (text, length) {
        var end = '...';
        if (isNaN(length)){
          length = 25;
        }
        if (text.length <= length) {
          return text;
        }
        else {
          return String(text).substring(0, length-end.length) + end;
        }
      };
    });

  services.service('BaseUrl', [
    function() {
      return {
        get: function() {
          var baseSegments = ['/policy-assets/', '/application-assets/', '/assets/', '/security-assets/', '/report-assets/', '/rest/report/'],
              idx = -1;

          for (var i = 0; i < baseSegments.length; i++) {
            idx = window.location.href.indexOf(baseSegments[i]);
            if (idx !== -1) {
              break;
            }
          }
          
          // check if we are in a report
          if (idx < 0) {
            idx = window.location.href.indexOf('/rest/report/');
          }

          if (idx > -1) {
            return window.location.href.substring(0, idx);
          }
          return '';
        }
      };
    }
  ]);

  services.service('ownerChange', ['$parse', function ($parse) {
    return {
      getEventHandler: function(scope, applicableCollection) {
        return function(eventArgs, changeEvent) {
          jQuery.each(changeEvent.changes, function(index, change) {
            switch (change.field) {
              case 'name':
                angular.forEach($parse(applicableCollection)(scope), function(item) {
                  if (item.ownerId === changeEvent.ownerId) {
                    item.ownerName = change.newValue;
                  }
                });
                break;
              case 'organizationId':
                scope.doLoad();
                return false;
            }
          });
        };
      }
    };
  }]);
}());
