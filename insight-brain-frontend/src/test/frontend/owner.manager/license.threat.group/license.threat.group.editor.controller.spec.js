/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';
import LicenseResourceMockData from '../mock.data/license.resource.mock.data';
import LicenseThreatGroupResourceMockData from '../mock.data/licenseThreatGroup.resource.mock.data';

describe('license.threat.group.editor.controller.spec.js', function() {

  beforeEach(angular.mock.module(ownerManagerModule.name, function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  var vm,
      $q,
      scope,
      $timeout,
      $httpBackend,
      $state = {
        go: jasmine.createSpy('go')
      },
      deleteServiceResourceDefer,
      mockDeleteService,
      CLMLocations,
      CLMContextLocations,
      SameOwnerStateNavigationService = {
        goEdit: angular.noop
      },
      mockLicenseGroupStore = StoreUtils().createMockStore('licenseGroupStore'),
      mockLTG = ResourceUtils().createMockResource();

  beforeEach(inject(function($rootScope, _$q_, _$timeout_, _$httpBackend_, _CLMLocations_, _CLMContextLocations_) {
    scope = $rootScope.$new();
    $timeout = _$timeout_;
    $q = _$q_;
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    CLMContextLocations = _CLMContextLocations_;

    deleteServiceResourceDefer = $q.defer();

    mockDeleteService = {
      deleteResource: function() {
        return deleteServiceResourceDefer.promise;
      }
    };
  }));

  it('Creates new on load', inject(function($controller) {
    vm = $controller('license.threat.group.editor.controller', {$scope: scope});

    $httpBackend.whenGET(CLMLocations.getLicensesUrl()).respond(LicenseResourceMockData.getLicensesUrl());
    $httpBackend.whenGET(CLMContextLocations.getApplicableLicenseGroupsUrl())
        .respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());
    $httpBackend.flush();
    $timeout.flush();

    expect(vm.dirtyLTG).toBeDefined();
    expect(vm.dirtyLTG.$new).toBe(true);
  }));

  it('Captures siblings', inject(function($controller) {
    vm = $controller('license.threat.group.editor.controller', {$scope: scope});

    $httpBackend.whenGET(CLMLocations.getLicensesUrl()).respond(LicenseResourceMockData.getLicensesUrl());
    $httpBackend.whenGET(CLMContextLocations.getApplicableLicenseGroupsUrl())
        .respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());
    $timeout.flush();
    $httpBackend.flush();

    expect(vm.siblings.length).toBe(1);
    expect(vm.siblings).toContain(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl()
        .licenseThreatGroupsByOwner[0].licenseThreatGroups[0]);
  }));

  it('Updates siblings list after creating new', inject(function($controller) {
    vm = $controller('license.threat.group.editor.controller', {$scope: scope});
    spyOn(vm, 'isLTGDirty').and.returnValue(true);

    $httpBackend.whenGET(CLMLocations.getLicensesUrl()).respond(LicenseResourceMockData.getLicensesUrl());
    $httpBackend.whenGET(CLMContextLocations.getApplicableLicenseGroupsUrl())
        .respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());
    $timeout.flush();
    $httpBackend.flush();

    mockLTG.$new = true;
    mockLTG.licenses = [];
    mockLTG.isDirty = function() {
      return true;
    };

    vm.dirtyLTG = mockLTG;
    vm.ltgEditor = {
      $valid: true,
      $setPristine: angular.noop
    };
    vm.ltgEditorMask = {wrap: SpecUtil.promiseWrapper($q)};

    vm.save();
    mockLTG.resolveSave();
    $timeout.flush();
    $timeout(function() {
    }, 1000); // mask delay = 0.8s
    $timeout.flush();

    expect(vm.siblings.length).toBe(2);
    expect(vm.siblings).toContain(mockLTG);
  }));

  it('Finds match with URL parameter', inject(function($controller) {
    vm = $controller('license.threat.group.editor.controller',
        {$scope: scope, $stateParams: {licenseThreatGroupId: '456'}});
    mockLTG.id = '456';
    mockLTG.licenses = [];

    $httpBackend.whenGET(CLMLocations.getLicensesUrl()).respond(LicenseResourceMockData.getLicensesUrl());
    $httpBackend.whenGET(CLMContextLocations.getApplicableLicenseGroupsUrl())
        .respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());
    mockLicenseGroupStore.resolveGet([mockLTG, {id: '123'}]);

    $timeout.flush();
    $httpBackend.flush();

    expect(vm.dirtyLTG.$clone).toHaveBeenCalled();
    expect(vm.dirtyLTG.id).toBe('456');
  }));

  it('Errors if no match found', inject(function($controller) {
    vm = $controller('license.threat.group.editor.controller',
        {$scope: scope, $stateParams: {licenseThreatGroupId: '456'}});

    $httpBackend.whenGET(CLMLocations.getLicensesUrl()).respond(LicenseResourceMockData.getLicensesUrl());
    $httpBackend.whenGET(CLMContextLocations.getApplicableLicenseGroupsUrl())
        .respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());
    mockLicenseGroupStore.resolveGet([{id: '123'}, {id: '124'}]);

    $timeout.flush();
    $httpBackend.flush();

    expect(vm.dirtyLTG).toBeUndefined();
    expect(vm.loadError).toBe('Unable to locate License Threat Group.');
  }));

  it('Unsuccessful save sets error message', inject(function($controller) {
    vm = $controller('license.threat.group.editor.controller', {$scope: scope});

    $httpBackend.whenGET(CLMLocations.getLicensesUrl()).respond(LicenseResourceMockData.getLicensesUrl());
    $httpBackend.whenGET(CLMContextLocations.getApplicableLicenseGroupsUrl())
        .respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());

    $timeout.flush();
    $httpBackend.flush();

    mockLTG.licenses = [];
    mockLTG.isDirty = function() {
      return true;
    };

    vm.dirtyLTG = mockLTG;
    vm.ltgEditor = {
      $valid: true
    };
    vm.ltgEditorMask = {wrap: SpecUtil.promiseWrapper($q)};

    vm.save();
    mockLTG.rejectSave('dagnabbit');

    $timeout.flush();
    expect(vm.submitError).toBe('dagnabbit');
  }));

  it('After delete goes to create new license threat group', inject(function($controller) {
    spyOn(SameOwnerStateNavigationService, 'goEdit');
    vm = $controller('license.threat.group.editor.controller', {
      $scope: scope,
      SameOwnerStateNavigationService: SameOwnerStateNavigationService,
      $stateParams: {licenseThreatGroupId: '1'},
      DeleteModalService: mockDeleteService
    });

    $httpBackend.whenGET(CLMLocations.getLicensesUrl()).respond(LicenseResourceMockData.getLicensesUrl());
    $httpBackend.whenGET(CLMContextLocations.getApplicableLicenseGroupsUrl())
        .respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());

    mockLTG.id = '1';
    mockLTG.licenses = [];
    mockLicenseGroupStore.resolveGet([mockLTG]);
    $timeout.flush();
    $httpBackend.flush();

    vm.deleteLTG();
    deleteServiceResourceDefer.resolve();
    $timeout.flush();

    expect(SameOwnerStateNavigationService.goEdit).toHaveBeenCalledWith('create-license-threat-group');
    SpecUtil.expectStateChangeNotPrevented(scope);
  }));

  it('After last app LTG delete goes to summary page', inject(function($controller) {
    vm = $controller('license.threat.group.editor.controller', {
      $scope: scope,
      SameOwnerStateNavigationService: SameOwnerStateNavigationService,
      $stateParams: {licenseThreatGroupId: '1', applicationPublicId: '123'},
      $state: $state,
      DeleteModalService: mockDeleteService
    });

    vm.isApp = true;

    $httpBackend.whenGET(CLMLocations.getLicensesUrl()).respond(LicenseResourceMockData.getLicensesUrl());
    $httpBackend.whenGET(CLMContextLocations.getApplicableLicenseGroupsUrl())
        .respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());

    mockLTG.id = '1';
    mockLTG.licenses = [];
    mockLicenseGroupStore.resolveGet([mockLTG]);
    $timeout.flush();
    $httpBackend.flush();

    vm.deleteLTG();
    deleteServiceResourceDefer.resolve();
    $timeout.flush();

    expect($state.go).toHaveBeenCalledWith('management.view.application', {applicationPublicId: '123'});
  }));

  it('Picks the licenses that are already included with the LTG', inject(function($controller) {
    var licenses = LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl().licenseThreatGroupsByOwner[0]
        .licenseThreatGroups[0].licenses;
    vm = $controller('license.threat.group.editor.controller',
        {$scope: scope, $stateParams: {licenseThreatGroupId: '1'}});

    $httpBackend.whenGET(CLMLocations.getLicensesUrl()).respond(LicenseResourceMockData.getLicensesUrl());
    $httpBackend.whenGET(CLMContextLocations.getApplicableLicenseGroupsUrl())
        .respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());

    mockLTG.id = '1';
    mockLTG.licenses = licenses;
    mockLicenseGroupStore.resolveGet([mockLTG]);
    $timeout.flush();
    $httpBackend.flush();

    // All licenses should be picked
    vm.availableLicenses.forEach(function(license) {
      expect(license.picked).toBeTruthy();
    });
  }));

  describe('Page Changes', function() {
    beforeEach(inject(function($controller) {
      vm = $controller('license.threat.group.editor.controller', {$scope: scope});

      $httpBackend.whenGET(CLMLocations.getLicensesUrl()).respond(LicenseResourceMockData.getLicensesUrl());
      $httpBackend.whenGET(CLMContextLocations.getApplicableLicenseGroupsUrl())
          .respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());
      $httpBackend.flush();
      $timeout.flush();

      vm.dirtyLTG = mockLTG;
    }));

    it('clean', function() {
      vm.dirtyLTG.isDirty = jasmine.createSpy('isDirty').and.returnValue(false);

      SpecUtil.expectStateChangeNotPrevented(scope);
      expect(vm.dirtyLTG.isDirty).toHaveBeenCalled();
    });

    it('dirty', function() {
      vm.dirtyLTG.isDirty = jasmine.createSpy('isDirty').and.returnValue(true);

      SpecUtil.expectStateChangePrevented(scope);
      expect(vm.dirtyLTG.isDirty).toHaveBeenCalled();
    });
  });
});
