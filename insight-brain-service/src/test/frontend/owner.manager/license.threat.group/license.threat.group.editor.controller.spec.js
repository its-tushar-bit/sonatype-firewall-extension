describe('license.threat.group.editor.controller.spec.js', function() {

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  beforeEach(module('ResourceModule'));

  var vm,
      scope,
      $timeout,
      $httpBackend,
      deleteServiceResourceDefer,
      mockDeleteService,
      CLMLocations,
      CLMAppLocations,
      SameOwnerStateNavigationService = {
        goEdit: angular.noop
      },
      mockLicenseGroupStore = StoreUtils().createMockStore('licenseGroupStore'),
      mockLTG = ResourceUtils().createMockResource();

  beforeEach(inject(function($rootScope, $q, _$timeout_, _$httpBackend_, _CLMLocations_, _CLMAppLocations_) {
    scope = $rootScope.$new();
    $timeout = _$timeout_;
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    CLMAppLocations = _CLMAppLocations_;

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
    $httpBackend.whenGET(CLMAppLocations.getApplicableLicenseGroupsUrl()).respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());
    $httpBackend.flush();
    $timeout.flush();

    expect(vm.dirtyLTG).toBeDefined();
    expect(vm.dirtyLTG.$new).toBe(true);
  }));

  it('Captures siblings', inject(function($controller) {
    vm = $controller('license.threat.group.editor.controller', {$scope: scope});

    $httpBackend.whenGET(CLMLocations.getLicensesUrl()).respond(LicenseResourceMockData.getLicensesUrl());
    $httpBackend.whenGET(CLMAppLocations.getApplicableLicenseGroupsUrl()).respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());
    $timeout.flush();
    $httpBackend.flush();

    expect(vm.siblings.length).toBe(1);
    expect(vm.siblings).toContain(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl().licenseThreatGroupsByOwner[0].licenseThreatGroups[0]);
  }));

  it('Updates siblings list after creating new', inject(function($controller) {
    spyOn(vm, 'isLTGDirty').andReturn(true);
    vm = $controller('license.threat.group.editor.controller', {$scope: scope});

    $httpBackend.whenGET(CLMLocations.getLicensesUrl()).respond(LicenseResourceMockData.getLicensesUrl());
    $httpBackend.whenGET(CLMAppLocations.getApplicableLicenseGroupsUrl()).respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());
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
    $httpBackend.whenGET(CLMAppLocations.getApplicableLicenseGroupsUrl()).respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());
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
    $httpBackend.whenGET(CLMAppLocations.getApplicableLicenseGroupsUrl()).respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());
    mockLicenseGroupStore.resolveGet([{id: '123'}, {id: '124'}]);

    $timeout.flush();
    $httpBackend.flush();

    expect(vm.dirtyLTG).toBeUndefined();
    expect(vm.loadError).toBe('Unable to locate License Threat Group.');
  }));

  it('Unsuccessful save sets error message', inject(function($controller) {
    vm = $controller('license.threat.group.editor.controller', {$scope: scope});

    $httpBackend.whenGET(CLMLocations.getLicensesUrl()).respond(LicenseResourceMockData.getLicensesUrl());
    $httpBackend.whenGET(CLMAppLocations.getApplicableLicenseGroupsUrl()).respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());

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
    $httpBackend.whenGET(CLMAppLocations.getApplicableLicenseGroupsUrl()).respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());

    mockLTG.id = '1';
    mockLTG.licenses = [];
    mockLicenseGroupStore.resolveGet([mockLTG]);
    $timeout.flush();
    $httpBackend.flush();

    vm.deleteLTG();
    deleteServiceResourceDefer.resolve();
    $timeout.flush();

    expect(SameOwnerStateNavigationService.goEdit).toHaveBeenCalledWith('create-license-threat-group');
  }));

  it('Picks the licenses that are already included with the LTG', inject(function($controller) {
    var licenses = LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl().licenseThreatGroupsByOwner[0].licenseThreatGroups[0].licenses;
    vm = $controller('license.threat.group.editor.controller',
        {$scope: scope, $stateParams: {licenseThreatGroupId: '1'}});

    $httpBackend.whenGET(CLMLocations.getLicensesUrl()).respond(LicenseResourceMockData.getLicensesUrl());
    $httpBackend.whenGET(CLMAppLocations.getApplicableLicenseGroupsUrl()).respond(LicenseThreatGroupResourceMockData.getApplicableLicenseGroupsUrl());

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
});
