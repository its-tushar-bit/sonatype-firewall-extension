var clmTimestamp = '';

describe('OrganizationController', function() {
    var scope, httpBackend, rootScope, clmLocations, compile, sniffer, organizationStore;

    function toRegExp(url) {
      return new RegExp(url + '\\?timestamp=[0-9]+');
    }

    angular.module('Hudson', []).factory('hudson', ['$http', function($http){
		return $http;
    }]);

    beforeEach(module('Organization', 'AngularCommon', 'CLMLocation'));

    beforeEach(inject(function($httpBackend, $rootScope, $controller, hudson, CLMLocations, regexFactory, $compile, $sniffer, OrganizationStore) {
        httpBackend = $httpBackend;
        rootScope = $rootScope;
        clmLocations = CLMLocations;
        compile = $compile;
        sniffer = $sniffer;
        organizationStore = OrganizationStore;

        httpBackend.expectGET(toRegExp(CLMLocations.getOrganizationsUrl())).respond(OrganizationMockData.getGETResponse());

        scope = $rootScope.$new();

        $controller('OrganizationController', {
            $scope : scope,
            hudson : hudson
        });
        
        $controller('OrganizationEditorController', {
            $scope : scope,
            hudson : hudson,
            regexFactory : regexFactory
        });

        httpBackend.flush();
    }));

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

    it('switches organizations', inject(function($timeout) {
        expect(scope.selectedOrganization).toEqual(null);
        scope.$apply(function() {
            scope.$state.params.organizationId = '2';
        });
        $timeout.flush();
        expect(scope.selectedOrganization).not.toBeUndefined();
        expect(scope.selectedOrganization.name).toEqual('org2');
    }));

    it('adds an organization', function() {
        scope.$state.params.organizationId = '_new_';
        scope.selectedOrganization = organizationStore.create();
        scope.selectedOrganization.name = 'name';
        scope.organizationEditor = {}

        httpBackend.expectPOST(toRegExp(clmLocations.getOrganizationsUrl()), {
            id : null,
            name : 'name'
        }).respond(OrganizationMockData.getPOSTResponse('name'));

		var hasFormData = window.FormData;
		window.FormData = false;
        
        scope.saveClick();
        
        window.FormData = hasFormData;
        
        httpBackend.flush();
    });
});
