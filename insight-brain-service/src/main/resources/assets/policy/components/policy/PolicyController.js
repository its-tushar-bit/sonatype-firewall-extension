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

	policyModule.controller('PolicyController', ['$scope', '$location', '$http', 'hudson', '$timeout', '$rootScope', '$q', 'PolicyStore', 'ActionStore', function ($scope, $location, $http, hudson, $timeout, $rootScope, $q, policyStore, actionStore) {

		function capitalize(text) {
			if (text && text.length > 1) {
				return text.substring(0, 1).toUpperCase() + text.substring(1);
			}
			return text;
		}
		$scope.location = $location;

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

		function handleHttpError(headerText, bodyText, status) {
			$scope.httpError = {
				body : status === 0 ? 'Unable to connect to server.' : bodyText,
				header : headerText
			};
			$('#httpErrorModal').modal('show');
		}

		function reset() {
			delete $scope.state.policyChanged;
			delete $scope.state.policyWatchStopFn;
			delete $scope.state.currentPolicy;
		}

		function postLoad() {
			reset();
		}

		function viewConfirmation(header, body, declineText, acceptText, acceptFn, declineFn) {
			$scope.state.confirm = {
				header : header,
				body : body,
				declineText : declineText,
				acceptText : acceptText,
				acceptFn : function () {
					delete $scope.state.confirm;
					$('#confirmationModal').modal('hide');
					acceptFn();
				},
				declineFn : function () {
					delete $scope.state.confirm;
					$('#confirmationModal').modal('hide');
					declineFn();
				}
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

		$q.all([policyStore.get().get(), actionStore.get()]).then(function (results) {
			$scope.state = {
				policyList : results[0],
				actionStageList : results[1][1]
			};
			postLoad();
		}, function (error) {
			handleHttpError('Policy Initialization Error', error.data, error.status);
		});
	}]);
}());
