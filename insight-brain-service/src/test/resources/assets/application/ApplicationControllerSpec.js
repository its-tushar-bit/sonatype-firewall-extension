var clmTimestamp = '';

describe('ApplicationController', function() {
  var parentScope, scope, httpBackend, rootScope, state, mockApplication, _provide;

  beforeEach(module('ApplicationModule', function($provide) {
    $provide.value('ApplicationId', {
      encoded: function() {
        return 'bom1-12345678';
      }
    });
    $provide.value('OrganizationId', {
      encoded: function() {
        return null;
      }
    });
  }));

  beforeEach(inject(function($httpBackend, $rootScope, $controller, $state, CLMAppLocations) {
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

  afterEach(function() {
    httpBackend.verifyNoOutstandingExpectation();
    httpBackend.verifyNoOutstandingRequest();
    scope.$destroy();
  });

  it('loads applications.', function() {
    expect(scope.applications).not.toBeUndefined();
    expect(scope.applications.length).toEqual(1);
    expect(scope.applications[0].publicId).toEqual('bom1-12345678');
  });

  it('passes through alerts', inject(function($state, $httpBackend) {
    $httpBackend.expectGET('../assets/management.html?').respond('<div></div>');
    $httpBackend.expectGET('../application-assets/components/application-navigator.html?').respond('<div></div>');
    $httpBackend.expectGET('../application-assets/components/aoeditor.html?').respond('<div></div>');

    $state.transitionTo('management.application.view', {
      applicationPublicId : '_new_'
    });

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

describe('ApplicationEditorController', function() {
  var parentScope, scope, httpBackend, rootScope, state, mockApplication, originalMockApplication, mockOrganization, getOriginalSpy;

  beforeEach(module('ApplicationModule', 'OrganizationModule'));

  describe('Missing Application', function () {
    beforeEach(inject(function ($controller, $rootScope) {
      var state = {
        params : {
          applicationPublicId : 'foo'
        },
        current : {}
      };
      scope = $rootScope.$new();
      $controller('applicationEditorController', {
        $scope: scope,
        $state: state,
        selectedApplication : null
      });
    }));

    it('Simple', function () {
      expect(scope.ao).toBeDefined();
      expect(scope.ao.selected).toBeFalsy();
      expect(scope.ao.getPublicId()).toEqual('foo');
    });
  });

  describe('New Application', function () {
    beforeEach(inject(function($httpBackend, $rootScope, $controller, $state, CLMLocations, CLMAppLocations, ApplicationStore) {
      rootScope = $rootScope;

      $state.current.name = 'management.application';

      var applicationsData = ApplicationMockData.getApplicationsData();
      mockApplication = applicationsData[0];
      $httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getEntitiesUrl())).respond(applicationsData);

      $httpBackend.expectGET(CLMLocations.getActionTypeUrl()).respond(PolicyMockData.getActionTypeData());
      $httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(MockData.getActionStageData());

      var organizationData = OrganizationMockData.getGETResponse();
      mockOrganization = organizationData[0];
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getOrganizationsUrl())).respond(organizationData);

      parentScope = $rootScope.$new();
      scope = parentScope.$new();
      state = $state;

      var selectedApplication = ApplicationStore.create();

      parentScope.applications = [selectedApplication];
      parentScope.applicationIconTimestamp = {}
      $controller('applicationEditorController', { $scope: scope, $state: state, selectedApplication : selectedApplication });

      $httpBackend.flush();
    }));

    it('Sets Organization Name', function() {
      scope.setOrganization(mockOrganization);
      expect(scope.getOrganizationName()).toEqual(mockOrganization.name);
    });
  });

  describe('Existing Application No Org', function () {
    beforeEach(inject(function($httpBackend, $rootScope, $controller, $state, ApplicationStore, CLMLocations) {
      rootScope = $rootScope;

      $state.current.name = 'management.application';

      parentScope = $rootScope.$new();
      scope = parentScope.$new();
      state = $state;

      var selectedApplication = ApplicationStore.create();

      parentScope.applications = [selectedApplication];
      parentScope.applicationIconTimestamp = {}

      var organizationData = OrganizationMockData.getGETResponse();
      mockOrganization = organizationData[0];
      $httpBackend.expectGET(CLMLocations.getActionTypeUrl()).respond(PolicyMockData.getActionTypeData());
      $httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(MockData.getActionStageData());
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getOrganizationsUrl())).respond(organizationData);

      $controller('applicationEditorController', { $scope: scope, $state: state, selectedApplication : selectedApplication });
      $httpBackend.flush();
    }));

    it('Organizations List Retrieved', inject(function($httpBackend, CLMLocations, CLMAppLocations) {
      var applicationsData = ApplicationMockData.getApplicationsData();
      mockApplication = applicationsData[0];

      expect(scope.organizations).toBeDefined();
      expect(scope.organizations.length).toEqual(OrganizationMockData.getGETResponse().length);
    }));
  });

  describe('Existing Application', function () {
    beforeEach(inject(function($httpBackend, $rootScope, $controller, $state, CLMLocations, CLMAppLocations, ApplicationStore) {
      httpBackend = $httpBackend;
      rootScope = $rootScope;

      $state.current.name = 'management.application';

      var applicationsData = ApplicationMockData.getApplicationsData();
      mockApplication = applicationsData[0];
      httpBackend.whenGET(SpecUtil.toRegExp(CLMAppLocations.getEntitiesUrl())).respond(applicationsData);

      httpBackend.expectGET(CLMLocations.getActionTypeUrl()).respond(PolicyMockData.getActionTypeData());
      httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(MockData.getActionStageData());

      var applicationSummaryData = ApplicationMockData.getApplicationSummaryData();
      httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getApplicationSummaryUrl(mockApplication.publicId))).respond(applicationSummaryData);

      parentScope = $rootScope.$new();
      scope = parentScope.$new();
      scope.appPublicIdForm = {
        $save : angular.noop
      };
      scope.aoEditorName = {
        $save : angular.noop
      };
      state = $state;

      var selectedApplication = ApplicationStore.create();
      selectedApplication.$new = false;
      selectedApplication.$updateOriginal(mockApplication);
      originalMockApplication = angular.copy(selectedApplication);

      getOriginalSpy = spyOn(selectedApplication, '$getOriginal').andReturn(originalMockApplication);

      parentScope.applications = [selectedApplication];
      parentScope.applicationIconTimestamp = {}
      $controller('applicationEditorController', { $scope: scope, $state: state, selectedApplication : selectedApplication });

      httpBackend.flush();
    }));

    afterEach(function() {
      httpBackend.verifyNoOutstandingExpectation();
      httpBackend.verifyNoOutstandingRequest();
      parentScope.$destroy();
    });

    it('getOrganization with null selectedApplication', function () {
      scope.selectedApplication = null;
      expect(scope.getOrganizationName()).toEqual("Select Organization");
    });

    it('does not cancel edits when changing between tabs', function() {
      scope.selectedApplication.name = 'new_name';
      e = scope.$broadcast('pageChangeAccepted', 'application/' + scope.selectedApplication.publicId);
      expect(scope.selectedApplication.name).toEqual('new_name');

      e = scope.$broadcast('pageChangeAccepted', 'organization/');
      expect(scope.selectedApplication.name).toEqual(originalMockApplication.name);
    });

    it('generates an icon', function() {
      scope.generateIcon();

      expect(scope.robotHash).not.toBeUndefined();
      expect(scope.robotHash).not.toEqual('');
      expect(scope.hasRobotSource).toBeTruthy();

      // After first robohash is generated using the name, a random should be created next
      var robotHash = scope.robotHash;
      scope.generateIcon();
      expect(scope.robotHash).not.toEqual(robotHash);
    });

    it('updates an applications organization', function() {
      scope.setOrganization(mockOrganization);
      expect(scope.selectedApplication.organizationId).toEqual(mockOrganization.id);
    });

    it('checks if the form is dirty', function() {
      var isDirty = scope.isFormDirty();

      expect(getOriginalSpy).toHaveBeenCalled();
      expect(isDirty).not.toBeTruthy();

      var originalOrgId = scope.selectedApplication.organizationId;
      scope.setOrganization(mockOrganization);
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

    it('cancels edits', inject(function($httpBackend) {
      var revertSpy = spyOn(scope.selectedApplication, '$revert').andCallThrough();
      scope.selectedApplication.name = "newName";
      scope.generateIcon();

      expect(scope.hasRobotSource).toBeTruthy();
      expect(scope.iconChanged).toBeTruthy();

      scope.cancel();

      expect(revertSpy).toHaveBeenCalled();

      expect(angular.equals(scope.selectedApplication, originalMockApplication)).toBeTruthy();
      expect(scope.hasRobotSource).not.toBeTruthy();
      expect(scope.iconChanged).not.toBeTruthy();
    }));

    it('saves an application', inject(function(CLMAppLocations) {
      var saveSpy = spyOn(scope.selectedApplication, '$save').andCallThrough();
      scope.aoEditor = {
        $valid : true
      };

      scope.setOrganization(mockOrganization);
      scope.selectedApplication.name = "newName";
      scope.generateIcon();

      window.FormData = false;

      httpBackend.expectPUT(SpecUtil.toRegExp(CLMAppLocations.getEntitiesUrl(mockApplication.publicId))).respond(ApplicationMockData.getApplicationSummaryData());

      scope.save();

      httpBackend.flush();

      expect(saveSpy).toHaveBeenCalled();
    }));

    it('Can delete an application', inject(function(CLMAppLocations, CLMLocations, $modal, $q) {
      var modalDeferred = $q.defer(),
          modalSpy = spyOn($modal, 'open').andReturn({
            result : modalDeferred.promise
          });

      httpBackend.expectGET('../assets/management.html?').respond('<div></div>');
      httpBackend.expectGET('../application-assets/components/application-navigator.html?').respond('<div></div>');

      expect(angular.element('#deleteApplicationModal').css('display')).toBeUndefined();

      scope.confirmDelete();
      expect(modalSpy).toHaveBeenCalled();
      expect(modalSpy.mostRecentCall.args[0].resolve.selected()).toEqual(scope.selectedApplication);
      expect(modalSpy.mostRecentCall.args[0].templateUrl).toEqual('delete-app-modal');

      modalDeferred.resolve({});
      httpBackend.flush();
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

      $httpBackend.expectPOST(CLMLocations.evaluatePolicyUrl(mockApplication.publicId,
              mockApplication.policyEvaluations.build.scanId)).respond(policyResponse);

      scope.reEvaluatePolicy(mockApplication.policyEvaluations.build);

      $httpBackend.flush();

      expect(mockApplication.policyEvaluationsResults.build.affectedComponentCount).toEqual(policyResponse.affectedComponentCount);
      expect(mockApplication.policyEvaluationsResults.build.criticalComponentCount).toEqual(policyResponse.criticalComponentCount);
      expect(mockApplication.policyEvaluationsResults.build.severeComponentCount).toEqual(policyResponse.severeComponentCount);
      expect(mockApplication.policyEvaluationsResults.build.moderateComponentCount).toEqual(policyResponse.moderateComponentCount);
    }));

    it('Can respond to errors when trying to delete an application', inject(function(CLMAppLocations, CLMLocations, $modal, $q) {
      var spy = spyOn(scope, '$broadcast'),
          modalDeferred = $q.defer(),
          serverFailure = ['Foo', 400, null, null],
          modalSpy = spyOn($modal, 'open').andReturn({
            result : modalDeferred.promise
          });

      expect(angular.element('#deleteApplicationModal').css('display')).toBeUndefined();

      scope.confirmDelete();
      expect(modalSpy).toHaveBeenCalled();

      scope.$apply(function () {
        modalDeferred.reject(serverFailure);
      });

      expect(spy).toHaveBeenCalledWith('showServerError', serverFailure);
    }));

    it('Refreshes the list of applications when informed that an organization has been deleted',
            inject(function(CLMAppLocations, ApplicationStore) {
              var ApplicationStoreSpy = spyOn(ApplicationStore, 'refresh');
              rootScope.$broadcast('organizations.delete');
              expect(ApplicationStoreSpy).toHaveBeenCalled()
            }));

    it('displays confirmation dialog', function() {
      scope.selectedApplication.name = 'new_name';
      var e = scope.$broadcast('pageChangeStarted');
      expect(e.defaultPrevented).toBeTruthy();

      scope.selectedApplication.name = originalMockApplication.name;
      e = scope.$broadcast('pageChangeStarted');
      expect(e.defaultPrevented).not.toBeTruthy();

      scope.generateIcon();
      var e = scope.$broadcast('pageChangeStarted');
      expect(e.defaultPrevented).toBeTruthy();

      scope.cancel();
      e = scope.$broadcast('pageChangeStarted');
      expect(e.defaultPrevented).not.toBeTruthy();

      scope.selectedApplication.name = 'new_name';
      e = scope.$broadcast('pageChangeStarted', 'application/' + scope.selectedApplication.publicId);
      expect(e.defaultPrevented).not.toBeTruthy();
    });

    it('broadcasts changes to owner data', inject(function(CLMAppLocations) {
      var broadcastSpy = spyOn(scope, '$broadcast');
      scope.aoEditor = {
        $valid : true
      };

      scope.selectedApplication.organizationId = 'new_org_id';

      httpBackend.expectPUT(SpecUtil.toRegExp(CLMAppLocations.getEntitiesUrl(mockApplication.publicId))).respond(ApplicationMockData.getApplicationSummaryData());

      scope.save();

      httpBackend.flush();

      expect(broadcastSpy).toHaveBeenCalledWith('ownerChanged', { ownerId : '78c1d44c07584e57945f04890c672e82', changes : [ { field : 'organizationId', newValue : 'new_org_id' } ] });
      broadcastSpy.reset();

      scope.selectedApplication.organizationId = originalMockApplication.organizationId;
      scope.selectedApplication.name = 'new_name';

      httpBackend.expectPUT(SpecUtil.toRegExp(CLMAppLocations.getEntitiesUrl(mockApplication.publicId))).respond(ApplicationMockData.getApplicationSummaryData());

      scope.save();

      httpBackend.flush();

      expect(broadcastSpy).toHaveBeenCalledWith('ownerChanged', { ownerId : '78c1d44c07584e57945f04890c672e82', changes : [ { field : 'name', newValue : 'new_name' } ] });
    }));
  });

});

describe('ContactController', function () {
  var scope;

  beforeEach(module('ApplicationModule', function($provide) {
    $provide.value('ApplicationId', {
      encoded: function() {
        return 'bom1-12345678';
      }
    });
    $provide.value('OrganizationId', {
      encoded: function() {
        return null;
      }
    });
  }));

  beforeEach(inject(function ($rootScope, $controller) {
    scope = $rootScope.$new();
    scope.$close = jasmine.createSpy('closeSpy');
    $controller('ContactController', { $scope: scope });
  }));
  afterEach(function () {
    scope.$destroy();
  });

  it('Error', function () {
    scope.setQueryResults(null, "Failure");
    expect(scope.error).toEqual("Failure");
  });

  it('Query Error', function () {
    scope.setQueryResults(null, "Failure");
    expect(scope.error).toEqual("Failure");
    expect(scope.queryResults).toEqual(null);
  });

  it('Query Results+Error', function () {
    scope.setQueryResults([{ id : 'bar' }], "Failure");
    expect(scope.error).toEqual("Failure");
    expect(scope.queryResults).toEqual([{ id : 'bar' }]);
  });

  it('Query Results', function () {
    scope.setQueryResults([{ id : 'bar' }]);
    expect(scope.error).toEqual(null);
    expect(scope.queryResults).toEqual([{ id : 'bar' }]);
  });

  it('Select a user', function () {
    scope.selectUser({ id : 'bar' });
    expect(scope.$close).toHaveBeenCalledWith({ id : 'bar' });
  });
});