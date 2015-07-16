describe('ApplicationController', function() {
  var scope, httpBackend, rootScope, state, mockApplication;

  beforeEach(module('ApplicationModule', 'HttpInterceptors', function($provide) {
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
    $controller('ContactController', { $scope: scope, contextId : 'foo', contextType : 'application' });
  }));
  afterEach(function () {
    scope.$destroy();
  });

  it('Error', function () {
    scope.setQueryResults(null, "Failure");
    expect(scope.alerts[0].msg).toEqual("Failure");
  });

  it('Query Error', function () {
    scope.setQueryResults(null, "Failure");
    expect(scope.alerts[0].msg).toEqual("Failure");
    expect(scope.queryResults).toEqual(null);
  });

  it('Query Results+Error', function () {
    scope.setQueryResults([{ id : 'bar' }], "Failure");
    expect(scope.alerts[0].msg).toEqual("Failure");
    expect(scope.queryResults).toEqual([{ id : 'bar' }]);
  });

  it('Query Results', function () {
    scope.setQueryResults([{ id : 'bar' }]);
    expect(scope.alerts.length).toEqual(0);
    expect(scope.queryResults).toEqual([{ id : 'bar' }]);
  });

  it('Select a user', function () {
    scope.selectUser({ id : 'bar' });
    expect(scope.$close).toHaveBeenCalledWith({ id : 'bar' });
  });
});