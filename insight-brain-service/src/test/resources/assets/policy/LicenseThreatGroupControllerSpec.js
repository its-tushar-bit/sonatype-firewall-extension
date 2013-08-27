var clmTimestamp = '';

describe('LicenseThreatGroup', function() {
	var scope, mockGroup;
	
  beforeEach(module('LicenseThreatGroup', 'CLMLocation', function($provide) {
    $provide.value('ApplicationId', {
      encoded : function () {
        return 'bom1-12345678';
      }
    });
    $provide.value('OrganizationId', {
      encoded: function () {
        return null;
      }
    });
    $provide.factory('hudson', ['$http', function($http){
      return $http;
    }]);
  }));

	beforeEach(inject(function($httpBackend, $rootScope, $controller, CLMLocations, CLMAppLocations, licenseGroupStore) {
		$httpBackend.whenGET(SpecUtil.toRegExp(CLMLocations.getLicensesUrl())).respond(LicenseGroupMockData.getLicensesData());
		$httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getApplicableLicenseGroupsUrl())).respond(LicenseGroupMockData.getApplicableLicenseGroupData());
		$httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getLicenseGroupsUrl())).respond(LicenseGroupMockData.getLicenseGroupData());
		$httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getLicenseGroupsUrl('9999999c07584e57945f04890c672e99', 'organization'))).respond(LicenseGroupMockData.getLicenseGroupData());
		$httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getLicenseGroupLicensesUrl(LicenseGroupMockData.getLicenseGroupData()[0]))).respond(LicenseGroupMockData.getLicenseGroupLicensesData());
		licenseGroupStore.get().then(function (data) {
			mockGroup = data[0];
		});
		scope = $rootScope.$new();

		$controller('LicenseThreatGroupController', {$scope: scope});

		$httpBackend.flush();
	}));

  afterEach(inject(function($httpBackend){
    scope.$destroy();
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

	describe('LicenseThreatGroupController', function () {
		it('loads licenses.', function() {
			expect(scope.allLicenses).not.toBeUndefined();
			expect(scope.allLicenses.length).toEqual(3);
		});
		it('sorts the licenses.', function() {
			expect(scope.allLicenses[0].id).toEqual('AAL');
		});
		it('loads license groups.', function() {
			expect(scope.applicableLicenseGroups).not.toBeUndefined();
			expect(scope.applicableLicenseGroups.length).toEqual(2);
			expect(scope.applicableLicenseGroups[0].licenseThreatGroups).not.toBeUndefined();
			expect(scope.applicableLicenseGroups[0].licenseThreatGroups.length).toEqual(1);
			expect(scope.applicableLicenseGroups[0].editable).toEqual(true);
			expect(scope.applicableLicenseGroups[1].licenseThreatGroups).not.toBeUndefined();
			expect(scope.applicableLicenseGroups[1].licenseThreatGroups.length).toEqual(1);
			expect(scope.applicableLicenseGroups[1].editable).toEqual(false);
			expect(scope.licenseGroups).not.toBeUndefined();
			expect(scope.licenseGroups).toEqual(scope.applicableLicenseGroups[0].licenseThreatGroups);
		});
		it('loads license group licenses.', function() {
			expect(scope.applicableLicenseGroups[0].licenseThreatGroups[0].licenses).not.toBeUndefined();
			expect(scope.applicableLicenseGroups[0].licenseThreatGroups[0].licenses.length).toEqual(2);
			expect(scope.applicableLicenseGroups[1].licenseThreatGroups[0].licenses).not.toBeUndefined();
			expect(scope.applicableLicenseGroups[1].licenseThreatGroups[0].licenses.length).toEqual(2);
		});
		it('loads the group editor.', function() {
			scope.editLicenseGroup(mockGroup);
			expect(scope.ltgEditorMap[mockGroup.id]).toEqual(true);
		});
		it('updates the license group.', inject(function($httpBackend, $rootScope, $controller, CLMAppLocations) {
			$httpBackend.expectPUT(SpecUtil.toRegExp(CLMAppLocations.getLicenseGroupsUrl())).respond(LicenseGroupMockData.getLicenseGroupData());
			$httpBackend.expectPUT(SpecUtil.toRegExp(CLMAppLocations.getLicenseGroupLicensesUrl(mockGroup))).respond(LicenseGroupMockData.getLicenseGroupLicensesData());

			scope.editLicenseGroup(mockGroup);

      var childScope = scope.$new();
      $controller('LicenseThreatGroupEditorController', {$scope: childScope});
      childScope.selectedGroup = mockGroup;
      childScope.hide = angular.noop; //normally this impl provided by a directive
      spyOn(childScope, 'hide');
      childScope.licenseGroupEditor = { $valid: true };

      childScope.saveClick();

      $httpBackend.flush();
      expect(childScope.hide).toHaveBeenCalled();
    }));
		it('shows the Delete modal', function() {
			scope.confirmDeleteLicenseGroup(mockGroup);
			expect(scope.deletedEnabled).toBeTruthy();
		});
		it('deletes a group.', inject(function($httpBackend, CLMAppLocations) {
			$httpBackend.expectDELETE(CLMAppLocations.getDeleteLicenseGroupUrl(mockGroup)).respond({});

			scope.confirmDeleteLicenseGroup(mockGroup);
			scope.deleteLicenseGroup();

      $httpBackend.flush();
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
		beforeEach(inject(function($compile, $httpBackend) {
			var element = angular.element('<div id="LicenseThreatGroupEditorController" ltg-editor="licenseGroup"></div>'),
				parentScope = scope.$new();

			$httpBackend.expectGET('ltgInlineEditor').respond('');
			$compile(element)(parentScope);
			$httpBackend.flush();
			parentScope.$apply(function () {
				parentScope.licenseGroup = mockGroup;
			});
			editorScope = parentScope.$$childHead;
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