describe('Tests for the OrganizationController', function() {

  beforeEach(module('OrganizationModule', 'HttpInterceptors', function($provide) {
    $provide.value('OrganizationId', {
      encoded: function() {
        return '1';
      }
    });
    $provide.value('ApplicationId', {
      encoded: function() {
        return;
      }
    });
  }));

  describe('OrganizationController', function() {
    var scope, httpBackend, rootScope, state, mockOrganization;

    beforeEach(inject(function($httpBackend, $rootScope, $controller, $state, CLMAppLocations) {
      httpBackend = $httpBackend;
      rootScope = $rootScope;

      $state.current.name = 'management.organization';

      var organizationsData = OrganizationMockData.getGETResponse();
      mockOrganization = organizationsData[0];
      httpBackend.expectGET(SpecUtil.toRegExp(CLMAppLocations.getEntitiesUrl())).respond(organizationsData);

      scope = $rootScope.$new();

      scope.aoEditorName = {
        $save : angular.noop
      };
      state = $state;

      $controller('OrganizationController', {
        $scope: scope,
        $state: state
      });

      httpBackend.flush();
    }));

    afterEach(function() {
      httpBackend.verifyNoOutstandingExpectation();
      httpBackend.verifyNoOutstandingRequest();
      scope.$destroy();
    });

    it('loads organizations.', function() {
      expect(scope.organizations).not.toBeUndefined();
      expect(scope.organizations.length).toEqual(3);
      expect(scope.organizations[0].id).toEqual('1');
      expect(scope.organizations[0].name).toEqual('org1');
      expect(scope.organizations[1].id).toEqual('2');
      expect(scope.organizations[1].name).toEqual('org2');
      expect(scope.organizations[2].id).toEqual('3');
      expect(scope.organizations[2].name).toEqual('org3');
    });
  });
});
