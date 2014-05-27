describe('DashboardModule', function() {
  function startsWith(url) {
    return new RegExp('^' + url + '\?.*');
  }

  var scope;
  var newestViolations = [
    {
      applicationPublicId: "fooID",
      applicationName: "Foo App",
      gav: {
        groupId: "org.apache.geronimo.framework",
        artifactId: "geronimo-security",
        version: "2.1"
      },
      hash: "848d7549ef7ec13ce546",
      id: "5e833e5982534083a035019c07d66507",
      policyEvaluationId: "2cca57dc20b947e999cc348f83da1e5b",
      policyId: "f219cdc1b9bd4bd089343dcdc542e757",
      policyName: "Bar Policy",
      threatCategory: "OTHER",
      threatLevel: 10,
      pathnames: ["geronimo-security.jar"],
      "stageDetails": [
        {
          "stageTypeId": "release",
          "time": 1401149547140,
          "actionTypeId": "fail",
          "scanId": "d8cbb9196c2d475991e5fbdcdf96e345"
        },
        {
          "stageTypeId": "build",
          "time": 1385755537775,
          "actionTypeId": "warn",
          "scanId": "175427dcaa88418f8e310eea03233ec1"
        },
        {
          "stageTypeId": "stage-release",
          "time": 1401133522035,
          "actionTypeId": "warn",
          "scanId": "c2bdf85b6292489abfe93882153880f5"
        },
        {
          "stageTypeId": "operate",
          "time": 0,
          "actionTypeId": null,
          "scanId": null
        }
      ]
    }
  ], tags = [
   {
     id: "tagid1",
     organizationId: 'orgId1',
     name: "TagOne",
     nameLowercaseNoWhitespace: "tagone",
     description: "Tag One Description"
   }, {
      id: "tagid2",
      organizationId: 'orgId2',
      name: "TagTwo",
      nameLowercaseNoWhitespace: "tagtwo",
      description: "Tag Two Description"
    }
  ], applications = [
    {
      id: 'applicationId1',
      publicId: 'applicationPublicId1',
      name: 'ApplicationOne',
      organizationId: 'orgId1'
    }
  ], organizations = [
    {
      id: 'orgId1',
      name: 'OrganizationOne'
    },
    {
      id: 'orgId2',
      name: 'OrganizationTwo'
    }
  ];

  beforeEach(module('DashboardModule'));

  afterEach(inject(function($httpBackend) {
    if (scope) {
      scope.$destroy();
    }
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  describe('dashboardController', function() {
    beforeEach(inject(function($rootScope, $controller) {
      scope = $rootScope.$new();

      scope.$apply(function () {
        scope.filters = {
          applicationPublicIds: [],
          policyThreatTypes: [],
          stageTypeIds: [],
          applicationTagIds: [],
          policyThreatLevel: [0,10]
        };
      });
      $controller('DashboardController', { $scope: scope });
    }));

    it('Reacts to filter changes', function() {
      scope.$apply(function () {
        scope.filters = {
          applicationPublicIds: ['foo'],
          policyThreatTypes: [],
          stageTypeIds: [],
          applicationTagIds: [],
          policyThreatLevel: [0,10]
        };
      });
    });
  });

  describe('dashboardFilter', function () {
    var directiveScope,
        stageTypeData;

    beforeEach(inject(function($rootScope, $compile, $templateCache, $httpBackend, CLMLocations) {
      scope = $rootScope.$new();

      stageTypeData = [{
        id: 'type1',
        name: 'Type 1'
      },{
        id: 'type2',
        name: 'Type 2'
      }];

      $templateCache.put('dashboard-filter', '<div></div>');
      $httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(stageTypeData);
      $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(applications);
      $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(organizations);
      $httpBackend.expectGET(CLMLocations.getApplicationTagsUrl()).respond(tags);
      $httpBackend.expectGET(CLMLocations.getDashboardFilters()).respond({
        policyThreatCategoryFilters: ['SECURITY','OTHER'],
        stageTypeFilters: ['type1','type2'],
        tagFilters: ['tag1','tag2'],
        applicationFilters: ['app1','app2'],
        minPolicyThreatLevel: 3,
        maxPolicyThreatLevel: 6
      });

      $compile(angular.element('<div dashboard-filter="filters" toggle="foo"></div>'))(scope);
      $httpBackend.flush();
      directiveScope = scope.$$childHead;
      directiveScope.$digest();
      expect(directiveScope.filtersLoaded).toBeTruthy();
    }));

    it('persisted data in scope', function() {
      expect(scope.filters.policyThreatTypes).toEqual(['SECURITY','OTHER']);
      expect(scope.filters.stageTypeIds).toEqual(['type1','type2']);
      expect(scope.filters.applicationTagIds).toEqual(['tag1','tag2']);
      expect(scope.filters.applicationPublicIds).toEqual(['app1','app2']);
      expect(scope.filters.policyThreatLevel).toEqual([3,6]);
    });

    it('loads applications', function() {
      expect(directiveScope.applications.length).toBe(applications.length);
      expect(directiveScope.applications[0].id).toBe(applications[0].id);
    });

    it('loads stage types', function() {
      expect(directiveScope.stageTypes.length).toBe(stageTypeData.length);
      expect(directiveScope.stageTypes[0].id).toBe(stageTypeData[0].id);
      expect(directiveScope.stageTypes[1].id).toBe(stageTypeData[1].id);
    })

    it('loads application tags', function() {
      expect(directiveScope.applicationTags.length).toBe(tags.length);
      expect(directiveScope.applicationTags[0].id).toBe(tags[0].id);
      expect(directiveScope.applicationTags[0].owner).toBe(organizations[0].name);
    });

    function expectFilterPUT($httpBackend, CLMLocations, applicationFilters, policyThreatCategoryFilters, stageTypeFilters,
                       tagFilters, minPolicyThreatLevel, maxPolicyThreatLevel)
    {
      $httpBackend.expectPUT(CLMLocations.getDashboardFilters(), {
        applicationFilters: applicationFilters !== undefined ? applicationFilters : ['app1','app2'],
        policyThreatCategoryFilters: policyThreatCategoryFilters !== undefined ? policyThreatCategoryFilters : ['SECURITY','OTHER'],
        stageTypeFilters: stageTypeFilters !== undefined ? stageTypeFilters : ['type1', 'type2'],
        tagFilters: tagFilters !== undefined ? tagFilters : ['tag1','tag2'],
        minPolicyThreatLevel: minPolicyThreatLevel !== undefined ? minPolicyThreatLevel : 3,
        maxPolicyThreatLevel: maxPolicyThreatLevel !== undefined ? maxPolicyThreatLevel : 6
      }).respond(null);
    }

    it('filters policy violations by application', inject(function($httpBackend, CLMLocations) {
      directiveScope.dirtyFilter.applicationPublicIds = ['fooID'];
      expectFilterPUT($httpBackend, CLMLocations, ['fooID']);
      directiveScope.applyFilter();
      $httpBackend.flush();

      expect(scope.filters.applicationPublicIds.length).toBe(1);
    }));

    it('filters policy violations by policy threat type', inject(function($httpBackend, CLMLocations) {
      directiveScope.dirtyFilter.policyThreatTypes = ['SECURITY2', 'OTHER2'];
      expectFilterPUT($httpBackend, CLMLocations, undefined, ['SECURITY2','OTHER2']);
      directiveScope.applyFilter();
      $httpBackend.flush();

      expect(scope.filters.policyThreatTypes).toEqual(['SECURITY2', 'OTHER2']);
    }));

    it('filters policy violations by stage type', inject(function($httpBackend, CLMLocations) {
      directiveScope.dirtyFilter.stageTypeIds = ['type3', 'type4'];
      expectFilterPUT($httpBackend, CLMLocations, undefined, undefined, ['type3','type4']);
      directiveScope.applyFilter();
      $httpBackend.flush();

      expect(scope.filters.stageTypeIds).toEqual(['type3', 'type4']);
    }));

    it('filters policy violations by application tag', inject(function($httpBackend, CLMLocations) {
      directiveScope.dirtyFilter.applicationTagIds = ['fooID'];
      expectFilterPUT($httpBackend, CLMLocations, undefined, undefined, undefined, ['fooID']);
      directiveScope.applyFilter();
      $httpBackend.flush();

      expect(scope.filters.applicationTagIds).toEqual(['fooID']);
    }));

    it('filters policy violations by policy threat level', inject(function($httpBackend, CLMLocations) {
      directiveScope.dirtyFilter.policyThreatLevel = [2,7];
      expectFilterPUT($httpBackend, CLMLocations, undefined, undefined, undefined, undefined, 2, 7);
      directiveScope.applyFilter();
      $httpBackend.flush();

      expect(scope.filters.policyThreatLevel).toEqual([2,7]);
    }));

    it('cancels filters', function() {
      scope.filters.applicationPublicIds = [];
      scope.filters.policyThreatTypes = [];
      scope.filters.stageTypeIds = [];
      scope.filters.applicationTagIds = [];

      directiveScope.$apply(function () {
        directiveScope.dirtyFilter.applicationPublicIds = ['fooID'];
        directiveScope.dirtyFilter.policyThreatTypes = ['security'];
        directiveScope.dirtyFilter.stageTypeIds = ['type1'];
        directiveScope.dirtyFilter.applicationTagIds = ['tagID'];
        directiveScope.cancelFilter();
      });

      expect(scope.filters.applicationPublicIds.length).toBe(0);
      expect(scope.filters.policyThreatTypes.length).toBe(0);
      expect(scope.filters.stageTypeIds.length).toBe(0);
      expect(scope.filters.applicationTagIds.length).toBe(0);

      expect(directiveScope.dirtyFilter.applicationPublicIds.length).toBe(0);
      expect(directiveScope.dirtyFilter.policyThreatTypes.length).toBe(0);
      expect(directiveScope.dirtyFilter.stageTypeIds.length).toBe(0);
      expect(directiveScope.dirtyFilter.applicationTagIds.length).toBe(0);
    });

    it('converts from application.publicId to application.name', function(){
      expect(directiveScope.applicationNameFor('applicationPublicId1')).toBe('ApplicationOne');
    });

    it('converts from policyThreatCategory.id to policyThreatCategory.name', function(){
      expect(directiveScope.policyThreatTypeNameFor('SECURITY')).toBe('Security');
    });

    it('converts from stageType.id to stageType.name', function(){
      expect(directiveScope.stageTypeNameFor('type1')).toBe('Type 1');
    });
  });

  describe('dashboard "fileName" filter', function() {
    var fileNameFilter;
    beforeEach(inject(function($filter) {
      fileNameFilter = $filter('fileName');
    }));
    var testCases = [
      { input: function() {
        return '/';
      }, expected: '' },
      { input: function() {
        return '//';
      }, expected: '' },
      { input: function() {
        return '///';
      }, expected: '' },
      { input: function() {
        return 'test/path/fileName';
      }, expected: 'fileName' },
      { input: function() {
        return '/test/path/fileName';
      }, expected: 'fileName' },
      { input: function() {
        return 'test/path/fileName/';
      }, expected: 'fileName' },
      { input: function() {
        return '/test/path/fileName/';
      }, expected: 'fileName' },
      { input: function() {
        return '/fileName';
      }, expected: 'fileName' },
      { input: function() {
        return 'fileName/';
      }, expected: 'fileName' },
      { input: function() {
        return 'fileName';
      }, expected: 'fileName' },
      { input: function() {
        return null;
      }, expected: null },
      { input: function() {
        return '';
      }, expected: '' }
    ];
    function validateFilter(input, expected) {
      it('should filter to: ' + expected, function() {
        expect(fileNameFilter(input())).toMatch(expected);
      });
    }
    for (var i = 0; i < testCases.length; i++) {
      var testCase = testCases[i];
      validateFilter(testCase.input, testCase.expected);
    }
  });

  describe('Risk Table Controllers', function() {
    var controllers = [{
      prefix: 'application',
      urlFn: 'getApplicationRisksUrl',
      data: [
        {
          totalApplicationRisk: {
            totalRisk:11,
            criticalRisk:5,
            severeRisk:3,
            moderateRisk:2,
            lowRisk:1
          }
        },
        {
          totalApplicationRisk: {
            totalRisk:48,
            criticalRisk:17,
            severeRisk:13,
            moderateRisk:11,
            lowRisk:7
          }
        },
      ]
    }, {
      prefix: 'component',
      urlFn: 'getComponentRisksUrl',
      data: [{
        score:11,
        scoreCritical:5,
        scoreSevere:3,
        scoreModerate:2,
        scoreLow:1
      }, {
        score:48,
        scoreCritical:17,
        scoreSevere:13,
        scoreModerate:11,
        scoreLow:7
      }]
    }];

    angular.forEach(controllers, function(controller) {
      describe(controller.prefix + 'RiskTable', function() {
        var scope;

        beforeEach(inject(function($rootScope, $controller) {
          scope = $rootScope.$new();

          scope.data = controller.data;
          $controller(controller.prefix + 'RiskTable', { $scope: scope });
        }));

        it('calculates maximum risk', function() {
          expect(scope.totalRisk).toBe(48);
          expect(scope.criticalRisk).toBe(17);
          expect(scope.severeRisk).toBe(13);
          expect(scope.moderateRisk).toBe(11);
          expect(scope.lowRisk).toBe(7);
        });
      });
    })
  })

  describe('Risk Table Directives', function () {
    var directives = [{
      prefix : 'newest',
      urlFn : 'getNewestRisksUrl'
    }, {
      prefix : 'application',
      urlFn : 'getApplicationRisksUrl'
    }, {
      prefix : 'component',
      urlFn : 'getComponentRisksUrl'
    }];

    angular.forEach(directives, function (directive) {
      describe(directive.prefix + 'RiskTable', function () {
        var directiveScope;

        beforeEach(inject(function ($controller, $compile, $httpBackend, $rootScope) {
          scope = $rootScope.$new();
          scope.maxResults = 123;
          directiveScope = scope.$new();

          $httpBackend.expectGET('dashboard-table').respond('<div></div>');
          $compile(angular.element('<div ' + directive.prefix + '-risk-table></div>'))(scope);
          scope.$digest();
          $httpBackend.flush();

          $httpBackend.verifyNoOutstandingRequest();
        }));

        it('Filter Set', inject(function (CLMLocations, $httpBackend) {
          $httpBackend.expectGET(startsWith(CLMLocations[directive.urlFn]())).respond('foo');
          scope.$apply(function () {
            scope.filters =  {
              applicationPublicIds: ['foo'],
              policyThreatTypes: [],
              stageTypeIds: [],
              applicationTagIds: [],
              policyThreatLevel: [0,10]
            };;
          });
          $httpBackend.flush();
          expect(directiveScope.data).toEqual('foo');

          // Filter is changed
          $httpBackend.expectGET(startsWith(CLMLocations[directive.urlFn]())).respond('bar');
          scope.$apply(function () {
            scope.filters = angular.copy(scope.filters);
            scope.filters.applicationPublicIds = ['bar'];
          });
          $httpBackend.flush();
          expect(directiveScope.data).toEqual('bar');
        }));

        it('Drops Requests That Don\'t Match', inject(function (CLMLocations, $httpBackend) {
          $httpBackend.expectGET(startsWith(CLMLocations[directive.urlFn]())).respond('foo');
          scope.$apply(function () {
            scope.filters =  {
              applicationPublicIds: ['foo'],
              policyThreatTypes: [],
              stageTypeIds: [],
              applicationTagIds: [],
              policyThreatLevel: [0,10]
            };
          });

          // Before the request completes the user alters the filter again
          $httpBackend.expectGET(startsWith(CLMLocations[directive.urlFn]())).respond('bar');
          scope.$apply(function () {
            scope.filters = angular.copy(scope.filters);
            scope.filters.applicationPublicIds = ['bar'];
          });
          $httpBackend.flush();
          expect(directiveScope.data).toEqual('bar');
        }));

        it('Errors', inject(function (CLMLocations, $httpBackend) {
          $httpBackend.expectGET(startsWith(CLMLocations[directive.urlFn]())).respond(500, 'foo');
          scope.$apply(function () {
            scope.filters =  {
              applicationPublicIds: ['foo'],
              policyThreatTypes: [],
              stageTypeIds: [],
              applicationTagIds: [],
              policyThreatLevel: [0,10]
            };;
          });
          $httpBackend.flush();
          expect(directiveScope.error).toBeTruthy();
          expect(directiveScope.data).toBeFalsy();
        }));
      });
    });
  });

  describe('Dashboard view summary', function() {
    var scope;

    beforeEach(inject(function($rootScope, $httpBackend) {
      scope = $rootScope.$new();
      $httpBackend.expectGET('dashboard-view-summary').respond('<div></div>');
    }));

    it('Predefined filters sent in GET request', inject(function($compile, $httpBackend, CLMLocations) {
      scope.filters = {
        applicationPublicIds: ['1', '2'],
        policyThreatTypes: ['3', '4'],
        stageTypeIds: ['5', '6'],
        applicationTagIds: ['7', '8'],
        policyThreatLevel: [3, 9]
      };

      $httpBackend.expectGET(CLMLocations.getDashboardViewingSummaryUrl() +
          '?applicationPublicIds=1&applicationPublicIds=2&policyThreatCategories=3,4&policyThreatLevelRange=3,9&stageIds=5&stageIds=6&tagIds=7&tagIds=8').respond();
      $compile(angular.element('<div dashboard-view-summary filters="filters"></div>'))(scope);
      $httpBackend.flush();
      expect(scope.$$childHead.error).toBeNull();
    }));

    it('Data loaded from server into model properly', inject(function($compile, $httpBackend, CLMLocations) {
      var data = {
        totalApplications: 13,
        totalPolicies: 18,
        totalComponents: 1843,
        matchedApplications: 2,
        matchedPolicies: 4,
        matchedComponents: 300
      };

      $httpBackend.expectGET(CLMLocations.getDashboardViewingSummaryUrl()).respond(data);
      $compile(angular.element('<div dashboard-view-summary filters="filters"></div>'))(scope);
      $httpBackend.flush();
      expect(scope.$$childHead.data).toEqual(data);
      expect(scope.$$childHead.error).toBeNull();
    }));

    it('Error propogated to scope', inject(function($compile, $httpBackend, CLMLocations) {
      $httpBackend.expectGET(CLMLocations.getDashboardViewingSummaryUrl()).respond(404, 'You screwed up');
      $compile(angular.element('<div dashboard-view-summary filters="filters"></div>'))(scope);
      $httpBackend.flush();
      expect(scope.$$childHead.error).toBeDefined();
    }));
  });

  describe('breadcrumb', function() {
    var scope;

    beforeEach(inject(function($rootScope) {
      scope = $rootScope.$new();
    }));

    it('builds list of parent states', inject(function($state, $compile, $httpBackend) {
      $httpBackend.expectGET('../dashboard-assets/dashboard.html?').respond('<div></div>');
      $httpBackend.expectGET('../dashboard-assets/component.html?').respond('<div></div>');
      scope.$apply(function() {
        $state.go('dashboard.component');
      });
      $httpBackend.flush();

      $compile(angular.element('<div breadcrumb></div>'))(scope);
      scope.$digest();

      expect(scope.states.length).toBe(2);
      expect(scope.states[0].state).toBe('dashboard.overview');
      expect(scope.states[1].state).toBe('dashboard.component');
    }));
  });

  describe('sortable', function() {
    var barScope, fooScope;

    beforeEach(inject(function ($rootScope, $compile) {
      scope = $rootScope.$new();
      $compile('<div sortable sortable-direction="true" sortable-field="bar">' +
              '<span sort-column="foo">foo</span><span sort-column="bar" sort-inverse="true">bar</span></div>')(scope);
      fooScope = scope.$$childHead;
      barScope = scope.$$childHead.$$nextSibling.$$nextSibling;
    }));

    it('tests', function () {
      expect(fooScope.isUp()).toBeFalsy();
      expect(fooScope.isDown()).toBeFalsy();
      expect(barScope.isUp()).toBeTruthy();
      expect(barScope.isDown()).toBeFalsy();
      expect(scope.getSortField()).toEqual('bar');
      expect(scope.getSortReverse()).toBeFalsy();

      fooScope.$apply(function () {
        barScope.setSort();
      });
      expect(fooScope.isUp()).toBeFalsy();
      expect(fooScope.isDown()).toBeFalsy();
      expect(barScope.isUp()).toBeFalsy();
      expect(barScope.isDown()).toBeTruthy();
      expect(scope.getSortField()).toEqual('bar');
      expect(scope.getSortReverse()).toBeTruthy();

      fooScope.$apply(function () {
        fooScope.setSort();
      });
      expect(fooScope.isUp()).toBeFalsy();
      expect(fooScope.isDown()).toBeTruthy();
      expect(barScope.isUp()).toBeFalsy();
      expect(barScope.isDown()).toBeFalsy();
      expect(scope.getSortField()).toEqual('foo');
      expect(scope.getSortReverse()).toBeFalsy();

      fooScope.$apply(function () {
        fooScope.setSort();
      });
      expect(fooScope.isUp()).toBeTruthy();
      expect(fooScope.isDown()).toBeFalsy();
      expect(barScope.isUp()).toBeFalsy();
      expect(barScope.isDown()).toBeFalsy();
      expect(scope.getSortField()).toEqual('foo');
      expect(scope.getSortReverse()).toBeTruthy();
    });

  });

  describe('NewestRiskTableController', function() {
    var stageDetails;
    beforeEach(inject(function($rootScope, $controller) {
      scope = $rootScope.$new();
      scope.data = newestViolations;
      stageDetails = scope.data[0].stageDetails;
      $controller('NewestRiskTableController', { $scope: scope });
    }));

    it('Marks one of the stage details as "latest"', function() {
      expect(stageDetails.length).toBe(4);
      expect(stageDetails[0].latest).toBeTruthy();
      expect(stageDetails[1].latest).toBeFalsy();
      expect(stageDetails[2].latest).toBeFalsy();
      expect(stageDetails[3].latest).toBeFalsy();
    });

    if('Can sort stageDetails by stageTypeId', function() {
      expect(scope.stageTypeSort(stageDetails[0])).toBe(2);
      expect(scope.stageTypeSort(stageDetails[1])).toBe(0);
      expect(scope.stageTypeSort(stageDetails[2])).toBe(1);
      expect(scope.stageTypeSort(stageDetails[3])).toBe(3);
    });
  });
});