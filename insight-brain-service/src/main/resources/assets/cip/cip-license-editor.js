/**
 * @license Copyright (c) 2013 Sonatype, Inc. All rights reserved. Includes the
 * third-party code listed at
 * http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 * trademark of Sonatype, Inc.
 */
/* global angular */
(function () {

	function BrainLicenseEditorTab(node, options) {
		this.node = node;
		this.options = options;
	}

	BrainLicenseEditorTab.prototype = new Insight.InformationPanelPlugin();

	BrainLicenseEditorTab.prototype.destroy = function () {
		if (this.node) {
			this.node.empty();
		}
	};
	BrainLicenseEditorTab.prototype.getTitle = function () {
		return 'Licenses';
	};
	BrainLicenseEditorTab.prototype.isVisible = function () {
		return !((freemium && !this.options.sampleData) || this.gav.matchState === 'unknown' || this.gav.identificationSource === 'Manual');
	};

	BrainLicenseEditorTab.prototype.create = function () {
		var timestamp = new Date().getTime(), 
			container = $('<div clm-include="\'' + CLM.path + 'cip/cip-license-editor.html\'"></div>'),
			me = this;

		me.node.empty();
		container.appendTo(this.node);

		angular.module('componentProvider' + timestamp, []).service('SelectedComponent', function() {
			return me.gav;
		});

		angular.bootstrap(container[0], [ 'LicenseEditor', 'componentProvider' + timestamp, 'AngularCommon', 'Hudson' ]);
	};

	function load() {
		var licenseEditor = angular.module('LicenseEditor', ['CommonServices']),
			licenses = null;

		licenseEditor.service('Licenses', ['$q', function ($q) {
			return {
				get : function () {
					var deferred = $q.deferred();
					if (licenses !== null) {
						deferred.resolve(licenses);
					} else {
						$http.get('').success(function () {}).then(deferred);
					}
					return deferred.promise;
				}
			};
		}]);

		licenseEditor.controller('LicenseEditorController', ['$scope', '$q', '$http', 'hudson', 'Messages', 'SelectedComponent', function ($scope, $q, $http, hudson, Messages, SelectedComponent) {
			var savedState;

			$scope.doLoad = function () {
				$scope.error = null;

				var promises = [];
				// List of licenses
				promises.push($http.get(CLM.path + 'rest/license'));
				// Current override state
				promises.push($http.get(CLM.path + 'rest/licenseOverride/application/' + applicationId + '/applied/' +
								SelectedComponent.groupId + '/' + SelectedComponent.artifactId + '/' + SelectedComponent.version));
				// Component licenses
				promises.push($http.get(CLM.path + 'rest/ci/component/details/' + applicationId, {
					params : {
						'artifactId' : SelectedComponent.artifactId,
						'groupId' : SelectedComponent.groupId,
						'version' : SelectedComponent.version,
						'hash' : SelectedComponent.hash,
						'matchState' : SelectedComponent.matchState,
						'instanceId' : SelectedComponent.instanceId
					}
				}));

				// TODO License list ought to link to Category + ThreatLevel (Highest?)
				$q.all(promises).then(function (results) {
					var licenses = results[0].data,
						currentOverride = results[1].data,
						component = results[2].data;

					$scope.licenses = {};
					angular.forEach(licenses, function (license) {
						$scope.licenses[license.id] = license;
					});

					$scope.hierarchy = angular.copy(currentOverride.licenseOverridesByOwner);

					savedState = currentOverride;
					$scope.reset();

					$scope.component = component;

					$scope.selectableLicenses = {};
					angular.forEach($scope.component.declaredLicenseIds, function (license) {
						$scope.selectableLicenses[license] = $scope.licenses[license];
					});
					angular.forEach($scope.component.observedLicenseIds, function (license) {
						$scope.selectableLicenses[license] = $scope.licenses[license];
					});

				}, function () {
					$scope.error = arguments[0];
				});
			};

			$scope.save = function () {
				$scope.saving = true;

				var licenseOverride = {
						id : null,
						ownerId : null,
						artifactId : SelectedComponent.artifactId,
						groupId : SelectedComponent.groupId,
						version : SelectedComponent.version,
						status : $scope.override.status.toUpperCase(),
						licenseId : null,
						comment : $scope.override.comment
					},
					owner = null;

				// Only set license for Override or Select states 
				if (licenseOverride.status === 'Overridden' || licenseOverride.status === 'Selected') {
					licenseOverride.overriddenLicenses.push($scope.licenseId);
				}

				// Find owner
				for (var i=0; i<$scope.hierarchy.length; i++) {
					if ($scope.hierarchy[i].ownerId === $scope.override.scope.ownerId) {
						owner = $scope.hierarchy[i];
						break;
					}
				}
				licenseOverride.ownerId = owner.ownerId;

				hudson.post(CLM.path + 'rest/licenseOverride/' + owner.ownerType + '/' + owner.ownerId, licenseOverride).success(function (data) {
					$scope.saving = false;
					for (var i=0; i<$scope.hierarchy.length; i++) {
						if ($scope.hierarchy[i].ownerId === data.ownerId) {
							$scope.hierarchy[i].licenseOverride = data;
							break;
						}
					}
					$scope.reset();
				}).error(function () {
					$scope.alert = Messages.getHttpErrorMessage(arguments);
					$scope.saving = false;
				});
			};

			$scope.reset = function () {
				$scope.override = {
					status : null,
					scope : null,
					licenseId : null
				};
				if (savedState && savedState.licenseOverridesByOwner) {
					for (var i=0; i<savedState.licenseOverridesByOwner.length; i++) {
						if (savedState.licenseOverridesByOwner[i].licenseOverride) {
							$scope.override.status = savedState.licenseOverridesByOwner[i].licenseOverride.status;
							$scope.override.scope = savedState.licenseOverridesByOwner[i].ownerId;

							if ($scope.override.scope === 'Overridden' || $scope.override.scope === 'Selected') {
								$scope.override.licenseId = savedState.licenseOverridesByOwner[i].licenseOverride.licenseId;
							} else {
								$scope.override.licenseId = null;
							}
							return;
						}
					}
				}
			};

			$scope.statuses = [ { value : 'Open', label : 'Open' }, { value : 'Acknowledged', label : 'Acknowledged' },
								{ value : 'Overridden', label : 'Overridden' }, { value : 'Selected', label : 'Selected' },
								{ value : 'Confirmed', label : 'Confirmed' }];

			$scope.$watch('status', function (val) {
				if (val !== null && val !== 'Open' && val !== 'Selected') {
					$scope.licenseId = null;
				}
			});

			$scope.doLoad();
		}]);
	}

	var  timeout = null;
	function checkAngular() {
		if (window.angular) {
			if (Insight && Insight.InformationPanelPlugins) {
				var index = -1;
				$.each(Insight.InformationPanelPlugins, function (candidateIndex, plugin) {
					if (plugin.name === 'LicenseEditorTab') {
						index = candidateIndex;
					}
				});
				if (index > -1) {
					Insight.InformationPanelPlugins[index] = BrainLicenseEditorTab;
				} else {
					// XXX This may not be ideal if other plugins need to do this.
					Insight.InformationPanelPlugins.splice(4, 0, BrainLicenseEditorTab);
				}
			}
			load();
		} else {
			timeout = setTimeout(checkAngular, 50);
		}
	}
	checkAngular();
}());