var clmTimestamp = '';

describe('ApplicationController', function () {
  var scope, httpBackend, rootScope, state, mockApplication;

  beforeEach(module('ApplicationModule'));
  beforeEach(module(function ($provide) {
    $provide.value('ApplicationId', {
      encoded: function () {
        return 'bom1-12345678';
      }
    }
    );
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

  beforeEach(module('ApplicationModule', 'OrganizationModule'));
  beforeEach(inject(function ($httpBackend, $rootScope, $controller, $state, CLMLocations, CLMAppLocations) {
    httpBackend = $httpBackend;
    rootScope = $rootScope;

    $state.current.name = 'management.application';

    var applicationsData = ApplicationMockData.getApplicationsData();
    mockApplication = applicationsData[0];
    httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getEntitiesUrl())).respond(applicationsData);

    httpBackend.expectGET(CLMLocations.getActionTypeUrl()).respond(PolicyMockData.getActionTypeData());
    httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(MockData.getActionStageData());

    var organizationData = OrganizationMockData.getGETResponse();
    mockOrganization = organizationData[0];
    httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getOrganizationsUrl())).respond(organizationData);

    var applicationSummaryData = ApplicationMockData.getApplicationSummaryData();
    httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getApplicationSummaryUrl(mockApplication.publicId))).respond(applicationSummaryData);

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

    $httpBackend.expectGET('../assets/management.html?').respond('<div></div>');
    $httpBackend.expectGET('../application-assets/components/application-navigator.html?').respond('<div></div>');

    scope.cancel();

    expect(revertSpy).toHaveBeenCalled();

    expect(angular.equals(scope.selectedApplication, originalMockApplication)).toBeTruthy();
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

  it('deletes an application', inject(function (CLMAppLocations) {
    httpBackend.expectDELETE(CLMAppLocations.getEntityUrl(mockApplication.publicId)).respond({});

    scope.confirmDeleteApplication(mockApplication);
    scope.deleteApplication();
  }));

  it('shows report summary.', function() {
    expect(scope.state.actionStageList.length).toEqual(MockData.getActionStageData().length);
  });

  it('reevaluates policy', inject(function($httpBackend, CLMLocations) {
    var policyResponse = ApplicationMockData.getPolicyEvaluationData();
    var mockApplication = {
      publicId: 'publicId',
      policyEvaluations: {
        build: {
          scanId: 'scanId',
          stage: {
            stageTypeId: 'build'
          }
        }
      },
      policyEvaluationsResults: {
        build: {}
      }
    };
    scope.applicationSummary = mockApplication;

    $httpBackend.expectPOST(CLMLocations.evaluatePolicyUrl(mockApplication.publicId, mockApplication.policyEvaluations.build.scanId)).respond(policyResponse);

    scope.reEvaluatePolicy(mockApplication.policyEvaluations.build);

    $httpBackend.flush();

    expect(mockApplication.policyEvaluationsResults.build.affectedComponentCount).toEqual(policyResponse.affectedComponentCount);
    expect(mockApplication.policyEvaluationsResults.build.criticalComponentCount).toEqual(policyResponse.criticalComponentCount);
    expect(mockApplication.policyEvaluationsResults.build.severeComponentCount).toEqual(policyResponse.severeComponentCount);
    expect(mockApplication.policyEvaluationsResults.build.moderateComponentCount).toEqual(policyResponse.moderateComponentCount);
  }));
});