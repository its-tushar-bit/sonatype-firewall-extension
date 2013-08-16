var clmTimestamp = '';

describe('ApplicationController', function () {
  var scope, httpBackend, rootScope, state, mockApplication, _provide;

  beforeEach(module('ApplicationModule', function($provide) {
    $provide.value('ApplicationId', {
      encoded: function () {
        return 'bom1-12345678';
      }
    }
    );
    $provide.factory('hudson', ['$http', function($http){
      return $http;
    }]);
  }));

  beforeEach(inject(function ($httpBackend, $rootScope, $controller, $state, CLMAppLocations) {
    httpBackend = $httpBackend;
    rootScope = $rootScope;

    $state.current.name = 'management.application';

    var applicationsData = ApplicationMockData.getApplicationsData();
    mockApplication = applicationsData[0];
    httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getEntitiesUrl())).respond(applicationsData);

    scope = $rootScope.$new();
    state = $state;

    $controller('applicationController', { $scope: scope, $state: state });

    httpBackend.flush();
  }));

  it('loads applications.', function () {
    expect(scope.applications).not.toBeUndefined();
    expect(scope.applications.length).toEqual(1);
    expect(scope.applications[0].publicId).toEqual('bom1-12345678');
  });

  it('switches applications.', inject(function ($timeout) {
    expect(scope.selectedApplication).toEqual(null);
    scope.$apply(function () {
      state.params.applicationPublicId = 'bom1-12345678';
    });
    $timeout.flush();
    expect(scope.selectedApplication).not.toBeUndefined();
    expect(scope.selectedApplication.publicId).toEqual('bom1-12345678');
  }));

  it('switch to new application', inject(function ($timeout) {
    expect(scope.selectedApplication).toEqual(null);
    scope.$apply(function () {
      state.params.applicationPublicId = '_new_';
    });
    $timeout.flush();
    expect(scope.selectedApplication).not.toBeUndefined();
    expect(scope.selectedApplication.publicId).toEqual(null);
  }));

  it('passes through alerts', inject(function($state, $httpBackend) {
    $httpBackend.expectGET('../assets/management.html?').respond('<div></div>');
    $httpBackend.expectGET('../application-assets/components/application-navigator.html?').respond('<div></div>');
    $httpBackend.expectGET('../application-assets/components/application-editor.html?').respond('<div></div>');

    $state.transitionTo('management.application.view');

    $httpBackend.flush();

    expect($state.current.data.passThroughAlerts).not.toBeUndefined();
    expect($state.current.data.passThroughAlerts.length).toEqual(0);
    $state.current.data.passThroughAlerts.push({ type: 'error', msg: 'apptest'});

    $httpBackend.expectGET('../policy-assets/components/policy/policy.html?').respond('<div></div>');

    $state.transitionTo('management.application.view.policies', { applicationPublicId: 'publicID' });

    $httpBackend.flush();

    expect($state.current.data.passThroughAlerts).not.toBeUndefined();
    expect($state.current.data.passThroughAlerts.length).toEqual(1);
    expect($state.current.data.passThroughAlerts[0].msg).toEqual('apptest');
    expect($state.current.data.passThroughAlerts[0].type).toEqual('error');
  }));
});

describe('ApplicationEditorController', function () {
  var scope, httpBackend, rootScope, state, mockApplication, originalMockApplication, mockOrganization, revertSpy, getOriginalSpy, saveSpy;
  
  beforeEach(module('ApplicationModule', 'OrganizationModule', function($provide) {
    $provide.factory('hudson', ['$http', function($http){
      return $http;
    }]);
  }));
  
  beforeEach(inject(function ($httpBackend, $rootScope, $controller, $state, CLMLocations, CLMAppLocations) {
    httpBackend = $httpBackend;
    rootScope = $rootScope;

    $state.current.name = 'management.application';

    var applicationsData = ApplicationMockData.getApplicationsData();
    mockApplication = applicationsData[0];
    httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getEntitiesUrl())).respond(applicationsData);

    var organizationData = OrganizationMockData.getGETResponse();
    mockOrganization = organizationData[0];
    httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getOrganizationsUrl())).respond(organizationData);

    scope = $rootScope.$new();
    state = $state;

    mockApplication.$getOriginal = function () {
      return originalMockApplication
    };
    mockApplication.$revert = function () {
      return angular.extend(mockApplication, originalMockApplication)
    };
    mockApplication.$save = function () {
      return { then: angular.noop }
    };
    getOriginalSpy = spyOn(mockApplication, '$getOriginal').andCallThrough();
    revertSpy = spyOn(mockApplication, '$revert').andCallThrough();
    saveSpy = spyOn(mockApplication, '$save').andCallThrough();

    originalMockApplication = angular.copy(mockApplication);

    scope.applications = [mockApplication];
    scope.selectedApplication = mockApplication;

    $controller('applicationEditorController', { $scope: scope, $state: state });

    httpBackend.flush();
  }));

  it('generates an icon', function () {
    scope.generateIcon();

    expect(scope.robotHash).not.toBeUndefined();
    expect(scope.robotHash).not.toEqual('');
    expect(scope.hasRobotSource).toBeTruthy();

    // After first robohash is generated using the name, a random should be created next
    var robotHash = scope.robotHash;
    scope.generateIcon();
    expect(scope.robotHash).not.toEqual(robotHash);
  });

  it('gets org name from id', function () {
    var name = scope.getOrganizationName(mockOrganization.id);
    expect(name).toEqual(mockOrganization.name);
  });

  it('updates an applications organization', function () {
    scope.changeOrganization(mockOrganization);
    expect(scope.selectedApplication.organizationId).toEqual(mockOrganization.id);
  });

  it('checks if the form is dirty', function () {
    var isDirty = scope.isFormDirty();

    expect(getOriginalSpy).toHaveBeenCalled();
    expect(isDirty).not.toBeTruthy();

    var originalOrgId = scope.selectedApplication.organizationId;
    scope.changeOrganization(mockOrganization);
    isDirty = scope.isFormDirty();

    expect(getOriginalSpy).toHaveBeenCalled();
    expect(isDirty).toBeTruthy();

    scope.selectedApplication.organizationId = originalOrgId;
    isDirty = scope.isFormDirty();

    expect(getOriginalSpy).toHaveBeenCalled();
    expect(isDirty).not.toBeTruthy();

    var originalName = scope.selectedApplication.name;
    scope.selectedApplication.name = "newName";
    isDirty = scope.isFormDirty();

    expect(getOriginalSpy).toHaveBeenCalled();
    expect(isDirty).toBeTruthy();

    scope.selectedApplication.name = originalName;
    isDirty = scope.isFormDirty();

    expect(getOriginalSpy).toHaveBeenCalled();
    expect(isDirty).not.toBeTruthy();

    scope.generateIcon();
    isDirty = scope.isFormDirty();

    expect(getOriginalSpy).toHaveBeenCalled();
    expect(isDirty).toBeTruthy();
  });

  it('cancels edits', inject(function ($httpBackend) {
    scope.changeOrganization(mockOrganization);
    scope.selectedApplication.name = "newName";
    scope.generateIcon();

    expect(scope.hasRobotSource).toBeTruthy();
    expect(scope.iconChanged).toBeTruthy();

    $httpBackend.expectGET('../assets/management.html?').respond('<div></div>');
    $httpBackend.expectGET('../application-assets/components/application-navigator.html?').respond('<div></div>');

    scope.cancel();

    expect(revertSpy).toHaveBeenCalled();

    expect(angular.equals(scope.selectedApplication, originalMockApplication)).toBeTruthy();
    expect(scope.hasRobotSource).not.toBeTruthy();
    expect(scope.iconChanged).not.toBeTruthy();
  }));

  it('saves an application', inject(function ($httpBackend, CLMAppLocations) {
    scope.applicationEditor = {};
    scope.applicationEditor.$valid = true;

    scope.changeOrganization(mockOrganization);
    scope.selectedApplication.name = "newName";
    scope.generateIcon();

    $httpBackend.expectPUT(CLMAppLocations.getEntitiesUrl()).respond(mockApplication);

    var hasFormData = window.FormData;
    window.FormData = false;

    scope.save();

    expect(saveSpy).toHaveBeenCalled();

    window.FormData = hasFormData;
  }));

  it('Can delete an application', inject(function (CLMAppLocations) {
    httpBackend.expectDELETE(CLMAppLocations.getEntityUrl(mockApplication.publicId)).respond({});
    httpBackend.expectGET('../assets/management.html?').respond('<div></div>');
    httpBackend.expectGET('../application-assets/components/application-navigator.html?').respond('<div></div>');

    expect(angular.element('#deleteApplicationModal').css('display')).toBeUndefined();

    scope.confirmDeleteApplication(mockApplication);

    expect(scope.deletedEnabled).toBeTruthy();
    expect(angular.element('#deleteApplicationModal').css('display')).not.toBe('none');

    scope.deleteApplication();

    httpBackend.flush();

    expect(angular.element('#deleteApplicationModal').css('display')).toBeUndefined();
    expect(scope.applications.length).toEqual(0);
    expect(scope.deletedEnabled).toBeFalsy();
  }));

  it('Can respond to errors when trying to delete an application', inject(function (CLMAppLocations) {
    var spy = spyOn(rootScope, '$broadcast').andReturn({defaultPrevented: false});

    httpBackend.expectDELETE(CLMAppLocations.getEntityUrl(mockApplication.publicId)).respond(400);

    expect(angular.element('#deleteApplicationModal').css('display')).toBeUndefined();

    scope.confirmDeleteApplication(mockApplication);

    expect(scope.deletedEnabled).toBeTruthy();
    expect(angular.element('#deleteApplicationModal').css('display')).not.toBe('none');

    scope.deleteApplication();

    httpBackend.flush();

    expect(angular.element('#deleteApplicationModal').css('display')).toBeUndefined();
    expect(spy).toHaveBeenCalledWith('showServerError', jasmine.any(Object));
    expect(scope.applications.length).toEqual(1);
    expect(scope.deletedEnabled).toBeFalsy();
  }));

  it('Refreshes the list of applications when informed that an organization has been deleted', inject(function (CLMAppLocations, applicationStore) {
    var applicationStoreSpy = spyOn(applicationStore, 'refresh');
    rootScope.$broadcast('organizations.delete');
    expect(applicationStoreSpy).toHaveBeenCalled()
  }));
});