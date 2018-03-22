describe('gettingStarted component', function() {
  beforeEach(module('gettingStartedModule'));

  var vm,
      $q,
      $scope,
      CLMLocations,
      currentUserDeferred,
      $httpBackend,
      $rootScope,
      permissionServiceMock;

  beforeEach(inject(function(_$q_, _$httpBackend_, _$rootScope_, $componentController, _CLMLocations_) {
    $q = _$q_;
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    $rootScope = _$rootScope_;
    $scope = _$rootScope_.$new();
    currentUserDeferred = $q.defer();
    permissionServiceMock = jasmine.createSpyObj('permissionServiceMock', ['getValidPermissions']);

    vm = $componentController('gettingStarted', {
      'CurrentUser': currentUserDeferred.promise,
      $rootScope: $rootScope,
      PermissionService: permissionServiceMock
    });
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
    $scope.$destroy();
  });

  describe('$onInit()', function() {

    it('does not do anything if application is not licensed', function() {
      $rootScope.licensed = false;

      vm.$onInit();

      expect(vm.error).toBeUndefined();
      expect(vm.validPermissions).toBeUndefined();
      expect(vm.shouldDisplayChangePassword).toBeUndefined();
      expect(vm.isDefaultUser).toBeUndefined();
      expect(vm.license).toBeUndefined();
    });

    it('sets variables up correctly on successful requests', function() {
      $rootScope.licensed = true;

      var permissionsDeferred = $q.defer();
      permissionServiceMock.getValidPermissions.and.returnValue(permissionsDeferred.promise);

      $httpBackend.whenGET(CLMLocations.getLicenseSummaryUrl()).respond('license value');
      $httpBackend.whenGET(CLMLocations.getIsAdminDefaultPasswordChanged()).respond('true');
      currentUserDeferred.resolve({username: 'admin'});

      vm.$onInit();

      permissionsDeferred.resolve(['CONFIGURE_SYSTEM', 'ADD_APPLICATION']);
      $scope.$digest();
      $httpBackend.flush();

      expect(vm.error).toBeUndefined();
      expect(vm.validPermissions).toEqual(['CONFIGURE_SYSTEM', 'ADD_APPLICATION']);
      expect(vm.shouldDisplayChangePassword).toBe(false);
      expect(vm.isDefaultUser).toBe(true);
      expect(vm.license).toBe('license value');
    });

    it('sets validPermissions but does not retrieve any data if has no admin permission', function() {
      $rootScope.licensed = true;

      var permissionsDeferred = $q.defer();
      permissionServiceMock.getValidPermissions.and.returnValue(permissionsDeferred.promise);

      vm.$onInit();

      permissionsDeferred.resolve(['ADD_APPLICATION']);
      $scope.$digest();

      expect(vm.validPermissions).toEqual(['ADD_APPLICATION']);
      expect(vm.shouldDisplayChangePassword).toBeUndefined();
      expect(vm.isDefaultUser).toBeUndefined();
      expect(vm.license).toBeUndefined();
    });

    it('resets error', function() {
      $rootScope.licensed = true;
      vm.error = 'Error';

      var permissionsDeferred = $q.defer();
      permissionServiceMock.getValidPermissions.and.returnValue(permissionsDeferred.promise);

      vm.$onInit();

      permissionsDeferred.resolve([]);
      $scope.$digest();

      expect(vm.error).toBeUndefined();
    });

    it('sets error if getValidPermissions request fails', function() {
      $rootScope.licensed = true;

      var permissionsDeferred = $q.defer();
      permissionServiceMock.getValidPermissions.and.returnValue(permissionsDeferred.promise);

      vm.$onInit();
      permissionsDeferred.reject('get Permissions Error');
      $scope.$digest();

      expect(vm.error).toBe('get Permissions Error');
    });

    it('sets error if isAdminDefaultPasswordChanged request fails', function() {
      $rootScope.licensed = true;

      var permissionsDeferred = $q.defer();
      permissionServiceMock.getValidPermissions.and.returnValue(permissionsDeferred.promise);

      $httpBackend.whenGET(CLMLocations.getLicenseSummaryUrl()).respond({});
      $httpBackend.whenGET(CLMLocations.getIsAdminDefaultPasswordChanged()).respond(500);

      vm.$onInit();

      permissionsDeferred.resolve(['CONFIGURE_SYSTEM']);
      $scope.$digest();
      $httpBackend.flush();

      expect(vm.error).not.toBeNull();
      expect(vm.error.status).toBe(500);
    });
  });

  describe('isLoading()', function() {
    it('returns true if permissions are not loaded', function() {
      vm.validPermissions = undefined;
      expect(vm.isLoading()).toBe(true);
    });

    it('returns false if permissions are loaded and has no admin permission', function() {
      vm.validPermissions = [];
      expect(vm.isLoading()).toBe(false);
    });

    it('returns true if has admin permission, but license is not loaded', function() {
      vm.validPermissions = ['CONFIGURE_SYSTEM'];
      vm.license = undefined;
      expect(vm.isLoading()).toBe(true);
    });

    it('returns false if has admin permission and license is loaded', function() {
      vm.validPermissions = ['CONFIGURE_SYSTEM'];
      vm.license = {};
      expect(vm.isLoading()).toBe(false);
    });

    it('returns false if failed to get permissions', function() {
      vm.error = {};
      expect(vm.isLoading()).toBe(false);
    });

    it('returns false if failed to get license', function() {
      vm.validPermissions = ['CONFIGURE_SYSTEM'];
      vm.error = {};
      expect(vm.isLoading()).toBe(false);
    });
  });

  describe('isDataLoaded()', function() {
    it('returns false if permissions are not loaded', function() {
      vm.validPermissions = undefined;
      expect(vm.isDataLoaded()).toBe(false);
    });

    it('returns true if permissions are loaded and has no admin permission', function() {
      vm.validPermissions = [];
      expect(vm.isDataLoaded()).toBe(true);
    });

    it('returns false if has admin permission, but license is not loaded', function() {
      vm.validPermissions = ['CONFIGURE_SYSTEM'];
      vm.license = undefined;
      expect(vm.isDataLoaded()).toBe(false);
    });

    it('returns true if has admin permission and license is loaded', function() {
      vm.validPermissions = ['CONFIGURE_SYSTEM'];
      vm.license = {};
      expect(vm.isDataLoaded()).toBe(true);
    });
  });

  describe('isAuthorizedToViewSystemSetup()', function() {
    it('returns true if has only CONFIGURE_SYSTEM permission', function() {
      vm.validPermissions = ['CONFIGURE_SYSTEM'];
      expect(vm.isAuthorizedToViewSystemSetup()).toBe(true);
    });

    it('returns true if has only ADD_APPLICATION permission', function() {
      vm.validPermissions = ['ADD_APPLICATION'];
      expect(vm.isAuthorizedToViewSystemSetup()).toBe(true);
    });

    it('returns false if has no permission', function() {
      vm.validPermissions = [];
      expect(vm.isAuthorizedToViewSystemSetup()).toBe(false);
    });
  });
});
