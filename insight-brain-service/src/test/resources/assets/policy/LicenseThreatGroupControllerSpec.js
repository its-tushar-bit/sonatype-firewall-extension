var clmTimestamp = '';

describe('LicenseThreatGroup', function() {
	var scope, mockGroup;

	angular.module('Hudson', []).factory('hudson', ['$http', function($http){
		return $http;
	}]);

	beforeEach(module('LicenseThreatGroup', 'CLMLocation'));
	beforeEach(module(function($provide) {
		$provide.value('ApplicationId', {
				encoded : function () {
					return 'bom1-12345678';
				}
			}
		);
	}));

	beforeEach(inject(function($httpBackend, $rootScope, $controller, CLMLocations, CLMAppLocations, licenseGroupStore) {
		$httpBackend.whenGET(SpecUtil.toRegExp(CLMLocations.getLicensesUrl())).respond(LicenseGroupMockData.getLicensesData());
		$httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getLicenseGroupsUrl())).respond(LicenseGroupMockData.getLicenseGroupData());
		$httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getLicenseGroupLicensesUrl(LicenseGroupMockData.getLicenseGroupData()[0]))).respond(LicenseGroupMockData.getLicenseGroupLicensesData());
		licenseGroupStore.get().then(function (data) {
			mockGroup = data[0];
		});
		scope = $rootScope.$new();

		$controller('LicenseThreatGroupController', {$scope: scope});

		$httpBackend.flush();
	}));

	afterEach(function () {
		scope.$destroy();
	});

	describe('LicenseThreatGroupController', function () {
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
			expect(scope.selectedGroup.licenses).not.toBeUndefined();
			expect(scope.selectedGroup.licenses.length).toEqual(2);
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

	describe('LicenseThreatGroupEditorController', function () {
		var editorScope = null;
		function getSelectedGroupLicenses() {
			var selectedLicenses = [];
			angular.forEach(editorScope.selectedGroupLicenses, function (value, key) {
				if (value) {
					selectedLicenses.push(key);
				}
			});
			return selectedLicenses;
		}
		beforeEach(inject(function($httpBackend, $rootScope, $controller, CLMLocations, CLMAppLocations, licenseGroupStore) {
			editorScope = scope.$new();
			$controller('LicenseThreatGroupEditorController', {$scope: editorScope});
			scope.$apply(function () {
				scope.editLicenseGroup(mockGroup);
			});
		}));
		it('Selected', function () {
			expect(getSelectedGroupLicenses().length).toEqual(2);
			expect(editorScope.selectedGroupLicenses['AFL-UNSPECIFIED']).toEqual(true);
			expect(editorScope.selectedGroupLicenses['AAL']).toEqual(true);
		});
		it('Add License', function () {
			// editor
			editorScope.$apply(function () {
				editorScope.addLicense(LicenseGroupMockData.getLicensesData()[1]);
			});
			// Original not modified
			expect(mockGroup.licenses.length).toEqual(2);

			expect(editorScope.selectedGroup.licenses.length).toEqual(3);
			expect(getSelectedGroupLicenses().length).toEqual(3);
			expect(editorScope.selectedGroupLicenses['AFL-1.2']).toEqual(true);
			// TODO Test save
		});
		it('Remove License', function () {

			// editor
			editorScope.$apply(function () {
				editorScope.removeLicense(LicenseGroupMockData.getLicensesData()[2]);
			});
			// Original not modified
			expect(mockGroup.licenses.length).toEqual(2);

			expect(editorScope.selectedGroup.licenses.length).toEqual(1);
			expect(getSelectedGroupLicenses().length).toEqual(1);
			expect(editorScope.selectedGroupLicenses['AFL-UNSPECIFIED']).toEqual(true);
			expect(editorScope.selectedGroupLicenses['AAL']).toEqual(null);
		});
	});
});