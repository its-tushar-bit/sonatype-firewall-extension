/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp, window, document */
(function () {
	'use strict';

	var reportModule = angular.module('Report', ['CLMLocation', 'ui.compat', 'AngularCommon', 'CommonServices'], ['$stateProvider', function ($stateProvider) {
		$stateProvider.state('report', {
			url: '/reports/{publicId}/{stageId}',
			controller: 'ReportController',
			templateUrl: '../application-assets/components/report.html'
		});
	}]);

	reportModule.controller('ReportController', ['$scope', '$state', '$http', '$q', 'CLMLocations', function ($scope, $state, $http, $q, clmLocations) {
		$scope.doLoad = function () {
			var appListPromise = $http.get(clmLocations.getApplicationUrl($state.params.publicId), {
					params: { timestamp: new Date().getTime() }
				}),
				actionStagePromise = $http.get(clmLocations.getActionStageUrl(), {
					params: { timestamp: new Date().getTime() }
				});
			$scope.error = null;

			$q.all([appListPromise, actionStagePromise]).then(function (results) {
				var stageId = $state.params.stageId;
				for (var stageTypeId in results[0].data.policyEvaluations) {
					if (stageTypeId === stageId) {
						$scope.policyEvaluation = results[0].data.policyEvaluations[stageTypeId];
						break;
					}
				}
				$scope.application = results[0].data;
				$scope.reportUrl = '../rest/report/' + encodeURIComponent($state.params.publicId) + '/' + encodeURIComponent($scope.policyEvaluation.scanId) + '/embedReport/index.html';

				for (var i = 0; i < results[1].data.length; i++) {
					if (results[1].data[i].id == $scope.policyEvaluation.stage.stageTypeId) {
						$scope.policyEvaluation.stage.stageName = results[1].data[i].name;
						break;
					}
				}
			}, function () {
				$scope.error = arguments[0];
			});
		};
		$scope.doLoad();
	}]);

	reportModule.directive('expandableIframe', function () {
		return {
			template: "<iframe ng-src='{{url}}' width='100%' height='1000px' border='0' frameborder='0' scrolling='yes' style='overflow:auto;'/>",
			scope : {
				url : '=expandableIframe'
			},
			link : function (scope, element, attrs) {
				var resizeTimeoutId;

				function setDimensions() {
					var iframe = angular.element('iframe');
					if (!iframe || iframe.length === 0) {
						clearTimeout(resizeTimeoutId);
						return;
					}
					var windowHeight = $(window).height(),
					containerTop = iframe.offset().top,
					bottomPadding = 20,
					height = Math.max(400, windowHeight - containerTop - bottomPadding);

					iframe.css({ 'height': height + 'px' });
				}

				function dedupe() {
					clearTimeout(resizeTimeoutId);
					resizeTimeoutId = setTimeout(setDimensions, 100);
				}

				setTimeout(setDimensions, 100);
				window.onresize = dedupe;
				scope.$on('$destroy', function () {
					clearTimeout(resizeTimeoutId);
				});
			}
		};
	});
}());
