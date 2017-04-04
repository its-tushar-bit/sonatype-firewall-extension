describe('dashboard.results.controller.spec', function() {
  beforeEach(module('dashboard.module'));
  var scope, CLMLocations, EventNameConstant, stateMock;

  beforeEach(inject(function($rootScope, $controller, $injector, _CLMLocations_) {
    CLMLocations = _CLMLocations_;
    EventNameConstant = $injector.get('event.name.constant');
    scope = $rootScope.$new();

    stateMock = {
      current: {
        data: {}
      }
    };
    $controller('dashboard.results.controller', {$scope: scope, $state: stateMock});
  }));

  describe('initialisation', function() {
    it('sets maxResults and maxDaysOld', function() {
      expect(scope.maxResults).toBe(100);
      expect(scope.maxDaysOld).toBe(30);
    });

    it('listens to update filter event', function() {
      var filterJson = {
        organizationFilters: ['org1', 'org2'],
        applicationFilters: ['app1', 'app2'],
        policyThreatCategoryFilters: ['category1', 'category2'],
        stageTypeFilters: ['stage1'],
        tagFilters: ['tag1'],
        policyViolationStates: ['OPEN', 'WAIVED'],
        maxDaysOld: 90,
        minPolicyThreatLevel: 3,
        maxPolicyThreatLevel: 8
      };

      expect(scope.filters).toBeUndefined();
      scope.$broadcast(EventNameConstant.UPDATE_DASHBOARD_FILTERS, filterJson);
      expect(scope.filters).toBe(filterJson);
      expect(scope.maxDaysOld).toEqual(filterJson.maxDaysOld);
    });
  });

  it('getViewTitle() uses state.data.title', function() {
    stateMock.current.data.title = 'Foo';
    expect(scope.getViewTitle()).toBe('Foo');
    stateMock.current.data.title = 'Bar';
    expect(scope.getViewTitle()).toBe('Bar');
  });

  describe('getExportUrl()', function() {
    it('throws error when state is not one of the dashboard views', function() {
      stateMock.current.name = 'Foo';
      expect(scope.getExportUrl).toThrowError('Export is not supported for state Foo');
    });

    it('uses violations export URl when on violations view', function() {
      stateMock.current.name = 'dashboard.overview.violations';
      expect(scope.getExportUrl()).toBe(CLMLocations.getNewestRisksExportUrl());
    });

    it('uses components export URl when on components view', function() {
      stateMock.current.name = 'dashboard.overview.components';
      expect(scope.getExportUrl()).toBe(CLMLocations.getComponentRisksExportUrl());
    });

    it('uses applications export URl when on applications view', function() {
      stateMock.current.name = 'dashboard.overview.applications';
      expect(scope.getExportUrl()).toBe(CLMLocations.getApplicationRisksExportUrl());
    });
  });

  it('getFilterJson() converts filters to json string', function() {
    scope.$broadcast(EventNameConstant.UPDATE_DASHBOARD_FILTERS, {
      organizationFilters: [],
      applicationFilters: ['app1', 'app2'],
      policyThreatCategoryFilters: [],
      stageTypeFilters: [],
      tagFilters: [],
      minPolicyThreatLevel: 3,
      maxPolicyThreatLevel: 8
    });
    var json = '{"applicationIds":["app1","app2"],"stageIds":[],"tagIds":[],"policyThreatLevelRange":"3,8"}';
    expect(scope.getFilterJson()).toBe(json);
  });

});
