var clmTimestamp = '';

describe('LicenseThreatGroupController', function() {
	var scope, mockGroup;

	function toRegExp( getUrl ) {
		return new RegExp( getUrl + '\\?timestamp=[0-9]+' );
	}

    angular.module('ApplicationId',[]).service('ApplicationId', function () {
		return {
			encoded : 'bom1-12345678'
		};
    });

	angular.module('Hudson', []).factory('hudson', ['$http', function($http){
		return $http;
	}]);

	beforeEach(module('LicenseThreatGroup', 'CLMLocation', 'Hudson', 'ApplicationId'));
	beforeEach(inject(function($httpBackend, $rootScope, $controller, CLMLocations, CLMAppLocations) {

		mockGroup = LicenseGroupMockData.getLicenseGroupData()[0];
		$httpBackend.whenGET(toRegExp(CLMLocations.getLicensesUrl())).respond(LicenseGroupMockData.getLicensesData());
		$httpBackend.whenGET(toRegExp(CLMAppLocations.getLicenseGroupsUrl())).respond(LicenseGroupMockData.getLicenseGroupData());
		$httpBackend.whenGET(toRegExp(CLMAppLocations.getLicenseGroupLicensesUrl(mockGroup))).respond(LicenseGroupMockData.getLicenseGroupLicensesData());

		scope = $rootScope.$new();

		$controller('LicenseThreatGroupController', {$scope: scope});

		$httpBackend.flush();
    }));

	it('loads licenses.', function() {
		expect(scope.allLicenses).not.toBeUndefined();
		expect(scope.allLicenses.length).toEqual(3);
	});
	it('sorts the licenses.', function() {
		expect(scope.allLicenses[0].id).toEqual('AAL');
	});
	it('loads license groups.', function() {
		expect(scope.licenseGroups).not.toBeUndefined();
		expect(scope.licenseGroups.length).toEqual(1);
	});
	it('loads license group licenses.', function() {
		expect(scope.licenseGroups[0].licenses).not.toBeUndefined();
		expect(scope.licenseGroups[0].licenses.length).toEqual(2);
	});
	it('loads the group editor.', function() {
		scope.editLicenseGroup(mockGroup);
		expect(scope.selectedGroup).not.toBeUndefined();
		expect(scope.licenses).not.toBeUndefined();
		expect(scope.licenses.length).toEqual(3);
	});
	it('adds the isApplied property and sets accordingly.', function() {
		scope.editLicenseGroup(mockGroup);
		expect(scope.licenses[0].isApplied).toBeFalsy();
	});
	it('updates the license group.', inject(function($httpBackend, $rootScope, $controller, hudson, CLMAppLocations) {
		$httpBackend.expectPOST(CLMAppLocations.getLicenseGroupsUrl()).respond(LicenseGroupMockData.getLicenseGroupData());
		$httpBackend.expectPOST(CLMAppLocations.getLicenseGroupLicensesUrl(mockGroup)).respond(LicenseGroupMockData.getLicenseGroupLicensesData());

		scope.editLicenseGroup(mockGroup);

		$controller('LicenseThreatGroupEditorController', {$scope: scope, hudson: hudson});

		scope.licenseGroupEditor = { $isValid: true };

		scope.saveClick();
	}));
	it('shows the Delete modal', function() {
		scope.confirmDeleteLicenseGroup(mockGroup);
		expect(scope.deletedEnabled).toBeTruthy();
	});
	it('deletes a group.', inject(function($httpBackend, CLMAppLocations) {
		$httpBackend.expectDELETE(CLMAppLocations.getDeleteLicenseGroupUrl(mockGroup)).respond({});

		scope.confirmDeleteLicenseGroup(mockGroup);
		scope.deleteLicenseGroup();

	}));
});