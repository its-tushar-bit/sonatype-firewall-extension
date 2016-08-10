describe('riskTable.directives.spec', function() {

  var directives = [
    {
      prefix: 'newest',
      urlFn: 'getNewestRisksUrl'
    }, {
      prefix: 'application',
      urlFn: 'getApplicationRisksUrl'
    }, {
      prefix: 'component',
      urlFn: 'getComponentRisksUrl'
    }
  ];

  beforeEach(module('dashboard.utils'));

  angular.forEach(directives, function(directive) {
    describe(directive.prefix + 'RiskTable', function() {
      var scope, directiveScope;

      afterEach(inject(function($httpBackend) {
        scope.$destroy();
        $httpBackend.verifyNoOutstandingExpectation(false);
        $httpBackend.verifyNoOutstandingRequest();
      }));

      beforeEach(inject(function($controller, $compile, $httpBackend, $rootScope) {
        scope = $rootScope.$new();
        scope.maxResults = 123;
        directiveScope = scope.$new();

        $httpBackend.expectGET('dashboard-table').respond('<div></div>');
        $compile(angular.element('<div ' + directive.prefix + '-risk-table></div>'))(scope);
        scope.$digest();
        $httpBackend.flush();

        $httpBackend.verifyNoOutstandingRequest();
      }));

      it('Filter Set', inject(function(CLMLocations, $httpBackend) {
        $httpBackend.expectPOST(CLMLocations[directive.urlFn]()).respond('foo');
        scope.$apply(function() {
          scope.filters = {
            applicationIds: ['foo'],
            policyThreatTypes: [],
            stageTypeIds: [],
            applicationTagIds: [],
            policyThreatLevel: [0, 10]
          };
        });
        $httpBackend.flush();
        expect(directiveScope.data).toEqual('foo');

        // Filter is changed
        $httpBackend.expectPOST(CLMLocations[directive.urlFn]()).respond('bar');
        scope.$apply(function() {
          scope.filters = angular.copy(scope.filters);
          scope.filters.applicationIds = ['bar'];
        });
        $httpBackend.flush();
        expect(directiveScope.data).toEqual('bar');
      }));

      it('Drops Requests That Don\'t Match', inject(function(CLMLocations, $httpBackend) {
        $httpBackend.expectPOST(CLMLocations[directive.urlFn]()).respond('foo');
        scope.$apply(function() {
          scope.filters = {
            applicationIds: ['foo'],
            policyThreatTypes: [],
            stageTypeIds: [],
            applicationTagIds: [],
            policyThreatLevel: [0, 10]
          };
        });

        // Before the request completes the user alters the filter again
        $httpBackend.expectPOST(CLMLocations[directive.urlFn]()).respond('bar');
        scope.$apply(function() {
          scope.filters = angular.copy(scope.filters);
          scope.filters.applicationIds = ['bar'];
        });
        $httpBackend.flush();
        expect(directiveScope.data).toEqual('bar');
      }));

      it('Errors', inject(function(CLMLocations, $httpBackend) {
        $httpBackend.expectPOST(CLMLocations[directive.urlFn]()).respond(500, 'foo');
        scope.$apply(function() {
          scope.filters = {
            applicationIds: ['foo'],
            policyThreatTypes: [],
            stageTypeIds: [],
            applicationTagIds: [],
            policyThreatLevel: [0, 10]
          };
        });
        $httpBackend.flush();
        expect(directiveScope.error).toBeTruthy();
        expect(directiveScope.data).toBeFalsy();
      }));

      it('Derives Policy Threat Level Categories from Filter', inject(function(CLMLocations, $httpBackend) {
        for (var i = 0; i <= 10; i++) {
          $httpBackend.expectPOST(CLMLocations[directive.urlFn]()).respond('foo');
          scope.$apply(function() {
            scope.filters = {
              applicationIds: [],
              policyThreatTypes: [],
              stageTypeIds: [],
              applicationTagIds: [],
              policyThreatLevel: [i, i]
            };
          });
          $httpBackend.flush();
          expect(directiveScope.policyThreatLevelCategories).toEqual({
            critical: 8 <= i && i <= 10,
            severe: 4 <= i && i < 8,
            moderate: 2 <= i && i < 4,
            low: i < 2
          });
        }
      }));
    });
  });
});
