(function () {
	function getAppliedLicenseOverrides(appStatus, appLicense, orgStatus, orgLicense) {
		var overrides = {
			licenseOverridesByOwner : [{
				ownerId : 'app1',
				ownerName : 'Application',
				ownerType : 'application',
				licenseOverride : {
					id : 'app1override',
					ownerId : 'app1',
					groupId : 'org.groupid',
					artifactId : 'artifactid',
					version : '1',
					status : appStatus,
					licenseId : appLicense,
					comment : ''
				}
			},{
				ownerId : 'org1',
				ownerName : 'Organization',
				ownerType : 'organization',
				licenseOverride : {
					id : 'org1override',
					ownerId : 'org1',
					groupId : 'org.groupid',
					artifactId : 'artifactid',
					version : '1',
					status : orgStatus,
					licenseId : orgLicense,
					comment : ''
				}
			}]
		};
		if (appStatus === null) {
			overrides.licenseOverridesByOwner[0].licenseOverride = null;
		}
		if (orgStatus === null) {
			overrides.licenseOverridesByOwner[1].licenseOverride = null;
		}
		return overrides;
	}
	function getLicenseWithThreats() {
		var x = {
			declaredlicenses : [],
			observedlicenses : []
		};
		x.declaredlicenses.push({
			threat : 4,
			license : LicenseGroupMockData.getLicensesData()[0] 
		});
		x.observedlicenses.push({
			threat : 9,
			license : LicenseGroupMockData.getLicensesData()[1]
		});
		return x;
	}

	describe('CIP License Editor', function () {
		beforeEach(module('LicenseEditor', function ($provide) {
			$provide.value('SelectedComponent', {
				groupId : 'org.groupid',
				artifactId : 'artifactid',
				version : '1'
			});
		    $provide.factory('hudson', ['$http', function($http){
		        return $http;
		      }]);
		}));

		var scope;

		beforeEach(inject(function ($rootScope) {
			scope = $rootScope.$new();
		}));

		afterEach(function () {
			scope.$destroy();
		});

		describe('Two Overrides', function () {
			beforeEach(inject(function ($controller, $httpBackend, SelectedComponent) {
				$httpBackend.expectGET(CLM.path + 'rest/license').respond(LicenseGroupMockData.getLicensesData());

				$httpBackend.expectGET(CLM.path + 'rest/licenseOverride/application/' + applicationId + '/applied/' +
										SelectedComponent.groupId + '/' + SelectedComponent.artifactId + '/' + 
										SelectedComponent.version).respond(getAppliedLicenseOverrides('ACKNOWLEDGED', null, 'OVERRIDDEN', "AFL"));

				$httpBackend.expectGET(CLM.path + 'rest/ci/component/details/licenses/' + applicationId + '?artifactId=' + SelectedComponent.artifactId +
								'&groupId=' + SelectedComponent.groupId + '&version=' + SelectedComponent.version).respond(getLicenseWithThreats());

				$controller('LicenseEditorController', {
					$scope : scope
				});
				$httpBackend.flush();
			}));

			it('Default Selection', function () {
				expect(scope.override.status).toEqual('ACKNOWLEDGED');
				expect(scope.override.ownerId).toEqual('app1');
				expect(scope.override.licenseId).toEqual(null);

				expect(scope.statuses.length).toEqual(6);
				expect(scope.statuses[5]).toEqual({
					value : 'DELETE',
					label : 'Inherit Status (OVERRIDDEN)'
				});
			});

			it('Delete', inject(function ($httpBackend) {
				$httpBackend.expectDELETE(CLM.path + 'rest/licenseOverride/application/app1/app1override').respond(204);
				scope.$apply(function () {
					scope.override.status = 'DELETE';
					scope.save();
				});
				$httpBackend.flush();				
				expect(scope.statuses.length).toEqual(5);
				expect(scope.override.status).toEqual('OVERRIDDEN');
				expect(scope.override.ownerId).toEqual('org1');
				expect(scope.override.licenseId).toEqual('AFL');
			}));
		});

		describe("No Overrides", function () {
			beforeEach(inject(function ($controller, $httpBackend, SelectedComponent) {
				$httpBackend.expectGET(CLM.path + 'rest/license').respond(LicenseGroupMockData.getLicensesData());

				$httpBackend.expectGET(CLM.path + 'rest/licenseOverride/application/' + applicationId + '/applied/' +
										SelectedComponent.groupId + '/' + SelectedComponent.artifactId + '/' + 
										SelectedComponent.version).respond(getAppliedLicenseOverrides(null, null, null, null));

				$httpBackend.expectGET(CLM.path + 'rest/ci/component/details/licenses/' + applicationId + '?artifactId=' + SelectedComponent.artifactId +
								'&groupId=' + SelectedComponent.groupId + '&version=' + SelectedComponent.version).respond(getLicenseWithThreats());

				$controller('LicenseEditorController', {
					$scope : scope
				});
				$httpBackend.flush();
			}));

			it('Default Selection', function () {
				expect(scope.override.status).toEqual('OPEN');
				expect(scope.override.ownerId).toEqual('app1');
				expect(scope.override.licenseId).toEqual(null);
				expect(scope.statuses.length).toEqual(5);
			});

			it('Save', inject(function ($httpBackend, SelectedComponent) {
				$httpBackend.expectPOST(CLM.path + 'rest/licenseOverride/organization/org1').respond(function (method, url, data, headers) {
					var post = angular.fromJson(data);
					expect(post).toEqual({
						id : null,
						ownerId : 'org1',
						artifactId : SelectedComponent.artifactId,
						groupId : SelectedComponent.groupId,
						version : SelectedComponent.version,
						status : 'ACKNOWLEDGED',
						licenseId : null
					});
					post.id = 'saveOverrideId';
					return [200, post, headers];
				});
				scope.$apply(function () {
					scope.override.status = 'ACKNOWLEDGED';
					scope.override.ownerId = 'org1';
					scope.save();
				});
				$httpBackend.flush();
			}));
		});
	});
}());