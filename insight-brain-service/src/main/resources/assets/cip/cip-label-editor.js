/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, window, CLM, setTimeout */
(function () {
    'use strict';

	$.extend(true, window, {
		'Insight' : {
			'LabelEditor' : function (node, applicationId, hash) {
				var timestamp = (new Date()).getTime(),
					container = $('<div clm-include="\'' + CLM.path + 'cip/cip-label-editor.html\'"></div>');
				node.empty();
				container.appendTo(node);

				angular.module('labelEditor' + timestamp, []).service('ComponentLabelEditorGAV', function () {
					return {
						applicationId : applicationId,
						hash : hash
					};
				});
				angular.bootstrap(container[0], ['ComponentLabelEditor', 'labelEditor' + timestamp, 'AngularCommon']);
			}
		}
	});

	var labelsApp = angular.module('ComponentLabelEditor', []);

	labelsApp.controller('LabelsController', ['$http', '$scope', 'ComponentLabelEditorGAV', function ($http, $scope, componentLabelEditorGAV) {
		var componentLabelsUrl = CLM.path + 'rest/label/component/application/' + componentLabelEditorGAV.applicationId + '/' + componentLabelEditorGAV.hash;

		function errorFn(data, status, headersFn, config) {
			$scope.alerts.push({
				type : 'error',
				msg : messages.getHttpErrorMessage({ status: status,  data: data })
			});
		}

		function persist(labelArr, color) {
			$http.put(componentLabelsUrl, { labels : labelArr, color : color}).success(function (data) {
				$scope.itemLabels = data;
				$scope.reloadAppLabels(); // Only really necessary if someone adds a brand new label, and removes it in the same session
			}).error(errorFn);
		}

		$scope.alerts = [];

		$scope.reloadLabels = function () {
			$http.get(componentLabelsUrl, { params : { timestamp : new Date().getTime() } }).success(function (data) {
				$scope.itemLabels = data;
			}).error(errorFn);
		};
		$scope.reloadLabels();

		$scope.reloadAppLabels = function () {
			$http.get(CLM.path + 'rest/label/application/' + componentLabelEditorGAV.applicationId, { params : { inherit : 'true', timestamp : new Date().getTime() } }).success(function (data) {
				$scope.availableLabels = data;
			}).error(errorFn);
		};
		$scope.reloadAppLabels(); // do initial load

		$scope.color = null;
		$scope.colors = [{ color : null, text : '(Unset)' },
						{ color : 'white', text : 'White' },
						{ color : 'grey', text : 'Grey' },
						{ color : 'black', text : 'Black' },
						{ color : 'green', text : 'Green' },
						{ color : 'yellow', text : 'Yellow' },
						{ color : 'orange', text : 'Orange' },
						{ color : 'red', text : 'Red' },
						{ color : 'blue', text : 'Blue' }];

		$scope.removeLabel = function (label) {
			var updatedLabels = [];
			angular.forEach($scope.itemLabels, function (candidate, key) {
				if (candidate.label !== label.label) {
					updatedLabels.push(candidate.label);
				}
			});
			persist(updatedLabels);
		};

		$scope.addLabel = function (label) {
			var updatedLabels = [],
				duplicate = false;

			angular.forEach($scope.itemLabels, function (candidate, key) {
				if (candidate.label.toLowerCase() === label.label.toLowerCase()) {
					duplicate = true;
				}
				updatedLabels.push(candidate.label);
			});

			if (!duplicate) {
				updatedLabels.push(label.label);
				persist(updatedLabels);
			}
		};

		$scope.isWhite = function (label) {
			return label.color === "green" || label.color === "black" || label.color === "orange" || label.color === "red" || label.color === "blue";
		};

		$scope.isApplied = function (label) {
			var duplicate = false;
			angular.forEach($scope.itemLabels, function (candidate, key) {
				duplicate = duplicate || (candidate.label === label.label);
				return !duplicate;
			});
			return !duplicate;
		};
	}]);

  /**
   * Enables tipsy tooltip on an element(with fixed parameters)
   */
  labelsApp.directive('tip', function () {
    return function (scope, element, attrs) {
      $(element).tipsy({fade: true, gravity: $.fn.tipsy.autoWE, html: true, opacity: 1.0, delayOut: 0});
    };
  });

	labelsApp.directive('spinner', function () {
		var properties = ['-ms-transform', '-webkit-transform', '-moz-transform', 'transform'];

		function setElement(element, value) {
			angular.forEach(properties, function (prop, key) {
				element.css(prop, value);
			});
			return element;
		}

		return function (scope, element, attrs) {
			element.bind('click', function (e) {
				setElement(element, '').prop('rotate', null).animate({ rotate : '+360'}, {
					step : function (now, fx) {
						now = now % 360;
						setElement(element, 'rotate(' + now + 'deg)');
					}
				});
			});
		};
	});
}());