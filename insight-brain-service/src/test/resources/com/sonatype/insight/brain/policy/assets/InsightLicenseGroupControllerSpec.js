var clmTimestamp = '';

describe('InsightLicenseGroupController', function() {
	var scope, mockGroup;
	
	function toRegExp( getUrl ) {
		return new RegExp( getUrl + '\\?timestamp=[0-9]+' );
	}
	
	angular.module('Hudson', []).factory('hudson', ['$http', function($http){
		return $http;
	}]);
	
	beforeEach(module('LicenseGroup', 'CLMLocation', 'Hudson'));
	beforeEach(inject(function($httpBackend, $rootScope, $controller, CLMLocations) {
		CLMLocations.appId = 'myAppId';
		
		mockGroup = LicenseGroupMockData.getLicenseGroupData()[0];
		$httpBackend.whenGET(toRegExp(CLMLocations.getLicensesUrl())).respond(LicenseGroupMockData.getLicensesData());
		$httpBackend.whenGET(toRegExp(CLMLocations.getLicenseGroupsUrl())).respond(LicenseGroupMockData.getLicenseGroupData());
		$httpBackend.whenGET(toRegExp(CLMLocations.getLicenseGroupLicensesUrl(mockGroup))).respond(LicenseGroupMockData.getLicenseGroupLicensesData());
		
		scope = $rootScope.$new();
		
		$controller('InsightLicenseGroupController', {$scope: scope});
		
		$httpBackend.flush();
    }));
	
	it('shows the GUI', function() {
		expect(scope.features.licenseGroup).toBeTruthy();
	});
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
	it('updates the license group.', inject(function($httpBackend, $rootScope, $controller, hudson, CLMLocations) {
		$httpBackend.expectPOST(CLMLocations.getLicenseGroupsUrl()).respond(LicenseGroupMockData.getLicenseGroupData());
		$httpBackend.expectPOST(CLMLocations.getLicenseGroupLicensesUrl(mockGroup)).respond(LicenseGroupMockData.getLicenseGroupLicensesData());
		
		scope.editLicenseGroup(mockGroup);
		
		$controller('InsightLicenseGroupEditorController', {$scope: scope, hudson: hudson});
		
		scope.licenseGroupEditor = { $isValid: true };
		
		scope.saveClick();
	}));
	it('shows the Delete modal', function() {
		scope.confirmDeleteLicenseGroup(mockGroup);
		expect(scope.deletedEnabled).toBeTruthy();
	});
	it('deletes a group.', inject(function($httpBackend, CLMLocations) {
		$httpBackend.expectDELETE(CLMLocations.getDeleteLicenseGroupUrl(mockGroup)).respond({});
		
		scope.confirmDeleteLicenseGroup(mockGroup);
		scope.deleteLicenseGroup();
		
	}));
});