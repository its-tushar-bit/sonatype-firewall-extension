describe('dashboardApp', function() {
  var scope, state, licenseCheckedSpy;

  beforeEach(module('DashboardModule'));
  beforeEach(module(function($provide) {
    licenseCheckedSpy = jasmine.createSpy('then');
    $provide.value('licenseChecker', {
      check: function() {
        return {
          then: licenseCheckedSpy
        }
      }
    });
  }));
  afterEach(function() {
    scope.$destroy();
  });
  beforeEach(inject(function($rootScope, $state, $controller) {
    expect(licenseCheckedSpy).toHaveBeenCalled();

    scope = $rootScope.$new();
    state = $state;

    $controller('dashboardController', { $scope: scope, $state: state });
  }));

  it('Validate location change event is broadcast properly', inject(function($rootScope) {
    var successStart = false, successAccept = false;
    $rootScope.$on('pageChangeStarted', function(event, destination) {
      successStart = true;
    });
    $rootScope.$on('pageChangeAccepted', function(event, destination) {
      successAccept = true;
    });

    $rootScope.$broadcast('$locationChangeStart', 'http://www.cnn.com', 'http://www.google.com');

    waitsFor(function() {
      return successStart;
    }, "pageChangeStarted event not properly retrieved", 1000);
    waitsFor(function() {
      return successAccept;
    }, "pageChangeAccepted event not properly retrieved", 1000);
  }));
});

describe('ManagementModule', function() {
  var scope;

  beforeEach(module('OrganizationModule', 'ApplicationModule', 'AngularCommon'));
  beforeEach(inject(function($rootScope, $state, $controller, commonCodeFactory) {
    scope = $rootScope.$new();

    $controller('ManagementController', { $scope: scope, $state: $state, commonCodeFactory: commonCodeFactory });
  }));
  afterEach(function() {
    scope.$destroy();
  });

  it('Lists Org before App', function() {
    expect(scope.managementPanes).not.toBeUndefined();
    expect(scope.managementPanes.length).toEqual(3);
    expect(scope.managementPanes[0].name).toEqual('Organizations');
    expect(scope.managementPanes[1].name).toEqual('Applications');
    expect(scope.managementPanes[2].name).toEqual('Configuration');
  });
});
