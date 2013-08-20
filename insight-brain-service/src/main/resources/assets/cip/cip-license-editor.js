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
			function buildAppContext(appContext) {
				$scope.hierarchy.push(appContext);
				angular.forEach(appContext.children, function (child) {
					buildAppContext(child);
				});
			}
			var savedState;

			$scope.doLoad = function () {
				$scope.error = null;

				var promises = [];
				// List of licenses
				promises.push($http.get(CLM.path + 'rest/license'));
				// Application's hierarchy
				promises.push($http.get(CLM.path + 'rest/application/' + applicationId + '/applicable/context'));
				// Current override state
				promises.push($http.get(CLM.path + 'rest/licenseOverride/application/' + applicationId + '/' +
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

					$scope.licenses = {};
					angular.forEach(results[0].data, function (license) {
						$scope.licenses[license.id] = license;
					});

					$scope.hierarchy = [];
					buildAppContext(results[1].data);

					savedState = results[2].data;
					$scope.component = results[3].data;

					$scope.selectableLicenses = {};
					angular.forEach($scope.component.declaredLicenseIds, function (license) {
						$scope.selectableLicenses[license] = $scope.licenses[license];
					});
					angular.forEach($scope.component.observedLicenseIds, function (license) {
						$scope.selectableLicenses[license] = $scope.licenses[license];
					});

					$scope.reset();
				}, function () {
					$scope.error = arguments[0];
				});
			};

			$scope.save = function () {
				$scope.saving = true;
				var licenseOverride = {
					artifactId : SelectedComponent.artifactId,
					groupId : SelectedComponent.groupId,
					version : SelectedComponent.version,
					status : savedState.status,
					comment : savedState.comment,
					overriddenLicenses : []
				};
				if (savedState.status === 'Overridden' || savedState.status === 'Selected') {
					licenseOverride.overriddenLicenses.push(savedState.licenseId);
				}
				hudson.post(CLM.path + 'rest/licenseOverride/' + savedState.scope.type + '/' + savedState.scope.id, licenseOverride).success(function () {
					$scope.saving = false;
					savedState = angular.copy($scope.currentLicense);
				}).error(function () {
					$scope.alert = Messages.getHttpErrorMessage(arguments);
					$scope.saving = false;
				});
			};

			$scope.reset = function () {
				//$scope.savedState = angular.copy(savedState) || {};
				$scope.savedState = {};
			};

			$scope.statuses = [ { value : 'Open', label : 'Open' }, { value : 'Acknowledged', label : 'Acknowledged' },
								{ value : 'Overridden', label : 'Overridden' }, { value : 'Selected', label : 'Selected' },
								{ value : 'Confirmed', label : 'Confirmed' }];

			$scope.$watch('savedState.status', function (val) {
				if (!$scope.savedState) {
					return;
				}
				if (val === null || val === 'Open' || val === 'Selected') {
				} else {
					$scope.savedState.licenseId = null;
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