describe('dashboard.results.directives.spec', function() {

  var directives = [
    {
      prefix: 'violations',
      serviceMethod: 'getNewestRisks'
    }, {
      prefix: 'applications',
      serviceMethod: 'getApplicationRisks'
    }, {
      prefix: 'components',
      serviceMethod: 'getComponentRisks'
    }
  ];

  var dashboardDataServiceMock,
      maskControllerMock,
      $state;

  beforeEach(module('dashboard.utils', 'legacyConfiguration', function($provide) {
    $provide.service('dashboard.data.service', function() {
      return dashboardDataServiceMock;
    });
    $provide.service('$state', function() {
      $state = jasmine.createSpyObj('state', ['go', 'is']);
      return $state;
    });
  }));

  angular.forEach(directives, function(directive) {
    describe(directive.prefix + '-results directive', function() {
      var $q,
          scope,
          directiveScope;

      beforeEach(inject(function($controller, $compile, $httpBackend, $rootScope, _$q_) {
        $q = _$q_;
        scope = $rootScope.$new();
        scope.maxResults = 123;
        scope.filtersAreDirty = false;
        directiveScope = scope.$new();

        dashboardDataServiceMock = jasmine.createSpyObj('dashboardDataService', [directive.serviceMethod]);
        maskControllerMock = jasmine.createSpyObj('maskController', ['activateMask', 'removeMask']);

        scope.maskController = maskControllerMock;

        $compile(angular.element('<div ' + directive.prefix + '-results></div>'))(scope);
        scope.$digest();
      }));

      it('Filter Set', function() {
        dashboardDataServiceMock[directive.serviceMethod].and.returnValue($q.resolve(['foo']));
        scope.$apply(function() {
          scope.needsAcknowledgement = false;
          scope.filters = {
            applicationIds: ['foo'],
            policyThreatTypes: [],
            stageTypeIds: [],
            applicationTagIds: [],
            policyThreatLevel: [0, 10]
          };
        });
        expect(directiveScope.data).toEqual('foo');

        // Filter is changed
        dashboardDataServiceMock[directive.serviceMethod].and.returnValue($q.resolve(['bar']));
        scope.$apply(function() {
          scope.filters = angular.copy(scope.filters);
          scope.filters.applicationIds = ['bar'];
        });
        expect(directiveScope.data).toEqual('bar');
      });

      it('Drops Requests That Don\'t Match', function() {
        var deferred1 = $q.defer([]);
        var deferred2 = $q.defer([]);
        dashboardDataServiceMock[directive.serviceMethod].and.returnValue(deferred1.promise);
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
        dashboardDataServiceMock[directive.serviceMethod].and.returnValue(deferred2.promise);
        scope.$apply(function() {
          scope.filters = angular.copy(scope.filters);
          scope.filters.applicationIds = ['bar'];
        });

        deferred1.resolve(['foo']);
        scope.$apply();
        expect(directiveScope.data).toBeFalsy();

        deferred2.resolve(['bar']);
        scope.$apply();
        expect(directiveScope.data).toEqual('bar');
      });

      it('Errors', function() {
        dashboardDataServiceMock[directive.serviceMethod].and.returnValue($q.reject('foo'));
        scope.$apply(function() {
          scope.filters = {
            applicationIds: ['foo'],
            policyThreatTypes: [],
            stageTypeIds: [],
            applicationTagIds: [],
            policyThreatLevel: [0, 10]
          };
        });
        expect(directiveScope.error).toBeTruthy();
        expect(directiveScope.data).toBeFalsy();
      });

      it('goToComponentDetails() uses proper sate and hash', function() {
        scope.goToComponentDetails({hash: 'test-hash'});
        expect($state.go).toHaveBeenCalledWith('dashboard.component', {hash: 'test-hash'});
      });

      it('Does not fetch data if acknowledgement is needed', function() {
        dashboardDataServiceMock[directive.serviceMethod].and.returnValue($q.resolve(['foo']));
        scope.$apply(function() {
          scope.needsAcknowledgement = true;
          scope.filters = {
            applicationIds: ['foo'],
            policyThreatTypes: [],
            stageTypeIds: [],
            applicationTagIds: [],
            policyThreatLevel: [0, 10]
          };
        });

        expect(dashboardDataServiceMock[directive.serviceMethod]).not.toHaveBeenCalled();
        expect(directiveScope.data).toBeUndefined();
      });

      it('watches the filtersAreDirty property and calls the correct functions on the maskController', function() {
        function updateDirty(isDirty) {
          scope.$apply(function(scope) {
            scope.filtersAreDirty = isDirty;
          });
        }

        expect(maskControllerMock.removeMask.calls.count()).toBe(1);
        expect(maskControllerMock.activateMask.calls.count()).toBe(0);

        updateDirty(true);
        expect(maskControllerMock.removeMask.calls.count()).toBe(1);
        expect(maskControllerMock.activateMask.calls.count()).toBe(1);

        updateDirty(false);
        expect(maskControllerMock.removeMask.calls.count()).toBe(2);
        expect(maskControllerMock.activateMask.calls.count()).toBe(1);

        // no change - shouldn't make an additional call
        updateDirty(false);
        expect(maskControllerMock.removeMask.calls.count()).toBe(2);
        expect(maskControllerMock.activateMask.calls.count()).toBe(1);
      });
    });
  });

});
