/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
/*jslint plusplus: true */
(function () {
	'use strict';

	var policyModule = angular.module('Policy', ['Hudson', 'PolicyEditor']);

	policyModule.controller('InsightPolicyController', ['$scope', 'global', '$http', 'hudson', '$timeout', 'CLMLocations', '$rootScope', '$q', 'PolicyStore', 'ActionStore', function ($scope, global, $http, hudson, $timeout, clmLocations, $rootScope, $q, policyStore, actionStore) {

		function capitalize(text) {
		    if (text && text.length > 1) {
		        return text.substring(0,1).toUpperCase() + text.substring(1);
		    }
		    return text;
		}

		$scope.getActionCount = function (policy) {
			var actionCount = 0;
			angular.forEach(policy.actions, function (value, key) {
				if (value.length > 0) {
					actionCount++;
				}
			});
			return actionCount;
		};
		$scope.getActions = function (policy) {
			var actions = '';
			angular.forEach(policy.actions, function (value, key) {
				var j, currentStageText = '', formattedName;
				if (value.length > 0) {
					if (actions.length > 0) {
						actions += ', ';
					}

					for (j = 0; j < $scope.state.actionStageList.length; j++) {
						if ($scope.state.actionStageList[j].id == key) {
							currentStageText += $scope.state.actionStageList[j].name + ': ';
							break;
						}
					}

					for (j = 0; j < value.length; j++) {
						formattedName = capitalize(value[j].actionTypeId);
						if (currentStageText.indexOf(formattedName) < 0) {
						    if (j > 0) {
	                            currentStageText += '/';
	                        }
						    currentStageText += formattedName;
						}
					}

					if (currentStageText) {
					    actions += currentStageText;
					}
				}
			});
			return actions;
		};

		function showHttpMask(bodyText) {
			$scope.state.httpMaskBody = bodyText;
			$('#httpMaskModal').modal('show');
		}

		function hideHttpMask() {
			$('#httpMaskModal').modal('hide');
		}

		function handleHttpError(headerText, bodyText, status) {
			hideHttpMask();
			$scope.state.httpErrorBody = status === 0 ? 'Unable to connect to server.' : bodyText;
			$scope.state.httpErrorHeader = headerText;
			$('#httpErrorModal').modal('show');
		}

		function reset() {
			delete $scope.state.policyChanged;
			delete $scope.state.policyWatchStopFn;
			delete $scope.state.currentPolicy;
			delete $scope.state.showAddPolicyScreen;
		}

		function postLoad() {
			reset();
			hideHttpMask();
		}

		function viewConfirmation(header, body, declineText, acceptText, acceptFn, declineFn) {
			$scope.state.confirmationHeader = header;
			$scope.state.confirmationBody = body;
			$scope.state.confirmationDeclineText = declineText;
			$scope.state.confirmationAcceptText = acceptText;
			$scope.state.confirmationDeclineFn = function () {
				$('#confirmationModal').modal('hide');
				declineFn();
			};
			$scope.confirmationAccept = function () {
				$('#confirmationModal').modal('hide');
				acceptFn();
			};
			$('#confirmationModal').modal('show');
		}

		function hidePolicy() {
			if ($scope.state.policyWatchStopFn) {
				$scope.state.policyWatchStopFn();
			}
			reset();
		}


		$scope.viewRemovePolicy = function (policy) {
			viewConfirmation("Delete Policy?",
				"Are you sure you want to delete the Policy named '" + policy.name + "'?  This action is not reversible.",
				'Cancel',
				'Delete',
				function () {
					policy.$delete();
				}, angular.noop);
		};

		$rootScope.$on('tabChange', function (event, args) {
			if (args[0].indexOf('policy') >= 0 && $scope.state.policyChanged) {
				event.preventDefault();
				event.stopPropagation();
				viewConfirmation("Unsaved Changes", "Navigating away will lose changes to the current policy.  Do you want to do this?", 'Yes', 'No', null, function () {
					args[1]();
				});
			}
		});


		$scope.state = global;
		$q.all([policyStore.get(), actionStore.get()]).then(function (results) {
			$scope.state.policyList = results[0];
			$scope.state.actionStageList = results[1][1];
			postLoad();
		}, function (data, status, headers, config) {
			handleHttpError('Policy Initialization Error', data, status);
		});

		showHttpMask('Loading data from server...');
	}]);
}());
