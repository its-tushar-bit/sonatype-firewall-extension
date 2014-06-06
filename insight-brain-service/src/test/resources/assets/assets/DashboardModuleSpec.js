describe('DashboardModule', function() {
  function startsWith(url) {
    return new RegExp('^' + url + '\?.*');
  }

  var scope, tags = [
    {
      id: "tagid1",
      organizationId: 'orgId1',
      name: "TagOne",
      nameLowercaseNoWhitespace: "tagone",
      description: "Tag One Description"
    },
    {
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
  ], commonFilters = {
    applicationPublicIds: ['1', '2'],
    policyThreatTypes: ['3', '4'],
    stageTypeIds: ['5', '6'],
    applicationTagIds: ['7', '8'],
    policyThreatLevel: [3, 9]
  }, commonFilterQuery = '?applicationPublicIds=1&applicationPublicIds=2&policyThreatCategories=3,' +
    '4&policyThreatLevelRange=3,9&stageIds=5&stageIds=6&tagIds=7&tagIds=8';

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

  describe('stageTypeSort', function () {
    it('sort by id', inject(function ($filter) {
      var result = $filter('stageTypeSort')([{ id : 'operate' }, { id : 'build' }, { id : 'release' }, { id : 'stage-release' }]);
      expect(result[0].id).toEqual('build');
      expect(result[1].id).toEqual('stage-release');
      expect(result[2].id).toEqual('release');
      expect(result[3].id).toEqual('operate');
    }));
    it('sort by stageTypeId', inject(function ($filter) {
      var result = $filter('stageTypeSort')([{
        id : 'build',
        stageTypeId : 'operate'
      }, {
        id : 'operate',
        stageTypeId : 'build'
      }, {
        id : 'stage-release',
        stageTypeId : 'release'
      }, {
        id : 'release',
        stageTypeId : 'stage-release'
      }]);
      expect(result[0].stageTypeId).toEqual('build');
      expect(result[1].stageTypeId).toEqual('stage-release');
      expect(result[2].stageTypeId).toEqual('release');
      expect(result[3].stageTypeId).toEqual('operate');
    }));
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
            };
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
      scope.filters = commonFilters;

      $httpBackend.expectGET(CLMLocations.getDashboardViewingSummaryUrl() + commonFilterQuery).respond();
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
    var stageTypes = [
      {"name": "Build", "id": "build"},
      {"name": "Develop", "id": "develop"},
      {"name": "Release", "id": "release"},
      {"name": "Stage Release", "id": "stage-release"},
      {"name": "Operate", "id": "operate"}
    ], stageDetails = [
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
    ];

    beforeEach(inject(function($rootScope, $controller, $httpBackend, CLMLocations) {
      scope = $rootScope.$new();
      scope.data = [
        {
          "applicationPublicId": "appPublicId",
          "applicationName": "appName",
          "threatLevel": 10,
          "time": 1401149547140,
          "policyId": "policyId",
          "policyName": "Policy",
          "hash": "foobar1",
          "gav": {
            "groupId": "foo",
            "artifactId": "bar",
            "version": "1.0"
          },
          "pathnames": ["foobar.jar"],
          "stageDetails": [
            {
              "stageTypeId": "build",
              "time": 1385755537775,
              "actionTypeId": "warn",
              "scanId": "scan1"
            },
            {
              "stageTypeId": "stage-release",
              "time": 1401133522035,
              "actionTypeId": "warn",
              "scanId": "scan2"
            },
            {
              "stageTypeId": "release",
              "time": 1401149547140,
              "actionTypeId": "fail",
              "scanId": "scan3"
            },
            {
              "stageTypeId": "operate",
              "time": 0,
              "actionTypeId": null,
              "scanId": null
            }
          ]
        }
      ];
      $httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(stageTypes);
      $controller('NewestRiskTableController', { $scope: scope });
      $httpBackend.flush();
    }));

    it('Modifies the loaded stages to remove develop and rename Stage-Release', function(){
      expect(scope.stageTypes.length).toBe(4);
      expect(scope.stageTypes[2].name).toBe('Stage');
    });

    it('Enhances the available data to aid sorting by row', function(){
      var risk = scope.data[0];
      expect(risk.stagereleaseTime).toBe(risk.stageDetails[1].time);
      expect(risk.releaseTime).toBe(risk.stageDetails[2].time);
      expect(risk.buildTime).toBe(risk.stageDetails[0].time);
      expect(risk.operateTime).toBeNull();
    });
  });

  describe('sparkline', function() {
    var compile, scope;

    beforeEach(inject(function($compile, $rootScope) {
      compile = $compile;
      scope = $rootScope.$new();
    }));

    it('sparkline should have reasonable defaults', function() {
      var element = angular.element('<div sparkline></div>');
      element = compile(element)(scope);

      var svg = element.find('svg');
      expect(svg).toBeDefined();
      expect(+svg.attr('width')).toBe(100);
      expect(+svg.attr('height')).toBe(25);
    });

    it('sparkline should respect size configuration', function() {
      var element = angular.element('<div sparkline style="width:100px; height:200px"></div>');
      element = compile(element)(scope);

      var svg = element.find('svg');
      expect(svg).toBeDefined();
      expect(+svg.attr('width')).toBe(100);
      expect(+svg.attr('height')).toBe(200);
    });

    it('sparkline should render the line and fill for the base color', function() {
      var element = angular.element('<div sparkline data="[0,1,2,1,2]"></div>');
      element = compile(element)(scope);

      // expect each point, plus the 'move to' zero path command, plus each point on the base of the fill
      var fill = element.find('.fill.base');
      expect(fill.attr('d').split(',').length).toBe(9);

      // expect each point, plus the 'move to' zero path command
      var line = element.find('.line.base');
      expect(line.attr('d').split(',').length).toBe(5);
    });

    it('sparkline should render the line and fill for the trailing color', function() {
      var element = angular.element('<div sparkline data="[0,1,2,1,2]"></div>');
      element = compile(element)(scope);

      // expect each point, plus the 'move to' zero path command, plus each point on the base of the fill
      var fill = element.find('.fill.green');
      expect(fill.attr('d').split(',').length).toBe(5);

      // expect each point, plus the 'move to' zero path command
      var line = element.find('.line.green');
      expect(line.attr('d').split(',').length).toBe(3);
    });

    it('sparkline renders trailing colors inverted when inverse is enabled', function() {
      var element = angular.element('<div sparkline data="[0,1,2,1,2]" inverse-green="true"></div>');
      element = compile(element)(scope);

      var fill = element.find('.fill.green');
      expect(fill.length).toBe(0);

      fill = element.find('.fill.red');
      expect(fill.length).toBe(1);
    });
  });

  describe('Dashboard component match summary', function() {
    var scope, data = {
      exact: 13,
      similar: 18,
      unknown: 1843,
      total: 1874
    }, expectedData = {
      exact: 13,
      similar: 18,
      unknown: 1843,
      total: 1874,
      items: [{
        count : 13,
        colorCss : 'match-exact',
        label : 'Exact Match'
      }, {
        count : 18,
        colorCss : 'match-partial',
        label : 'Similar Match'
      }, {
        count : 1843,
        colorCss : 'match-none',
        label : 'Unknown'
      }]
    }, url;

    beforeEach(inject(function($rootScope, $httpBackend, CLMLocations) {
      scope = $rootScope.$new();
      scope.filters = commonFilters;
      $httpBackend.expectGET('dashboard-component-match-results').respond('<div></div>');
      url = CLMLocations.getDashboardComponentMatchSummaryUrl() + commonFilterQuery;
    }));

    it('Data loaded from server into model properly', inject(function($compile, $httpBackend) {
      $httpBackend.expectGET(url).respond(data);
      $compile(angular.element('<div dashboard-component-match-results filters="filters"></div>'))(scope);
      $httpBackend.flush();
      expect(scope.$$childHead.data).toEqual(expectedData);
      expect(scope.$$childHead.error).toBeNull();
    }));

    it('Error propogated to scope', inject(function($compile, $httpBackend) {
      $httpBackend.expectGET(url).respond(404, 'You screwed up');
      $compile(angular.element('<div dashboard-component-match-results filters="filters"></div>'))(scope);
      $httpBackend.flush();
      expect(scope.$$childHead.error).toBeDefined();
    }));
  });

  describe('Value bar chart with both positive and negative values', function(){
    var element;
    beforeEach(inject(function($rootScope, $compile) {
      scope = $rootScope.$new();
      scope.barData = [-50, 0, 50];
      element = $compile(angular.element('<div value-bars data="barData" style="height: 50px;width: 100px;"></div>'))(scope);
    }));

    it('creates an SVG element based on the data', function(){
      expect(element.find('svg')).toBeTruthy();
    });

    it('creates a bar for each of the data points', function(){
      expect(element.find('svg').find('rect').length).toBe(scope.barData.length);
    });

    it('sets the correct style and size for values below zero', function(){
      var negativeValue = angular.element(element.find('svg').find('rect')[0]);
      expect(negativeValue.attr('class')).toBe('bar negative');
      expect(negativeValue.attr('height')).toBe('25'); //half of chart below zero
      expect(negativeValue.attr('y')).toBe('25');  //starts in the middle between high/low
    });

    it('sets the correct style and size for zero values', function(){
      var zero = angular.element(element.find('svg').find('rect')[1]);
      expect(zero.attr('class')).toBe('bar negative');
      expect(zero.attr('height')).toBe('0'); //no height
      expect(zero.attr('y')).toBe('25');  //starts in the middle
    });

    it('sets the correct style and size for positive values', function(){
      var positiveValue = angular.element(element.find('svg').find('rect')[2]);
      expect(positiveValue.attr('class')).toBe('bar positive');
      expect(positiveValue.attr('height')).toBe('25'); //half of chart above zero
      expect(positiveValue.attr('y')).toBe('0');  //starts at the top
    });

  });

  describe('Value bar chart with only positive values', function(){
    var element;
    beforeEach(inject(function($rootScope, $compile) {
      scope = $rootScope.$new();
      scope.barData = [0, 25, 50];
      element = $compile(angular.element('<div value-bars data="barData" style="height: 50px;width: 100px;"></div>'))(scope);
    }));

    it('sets the correct style and size for zero values', function(){
      var zero = angular.element(element.find('svg').find('rect')[0]);
      expect(zero.attr('class')).toBe('bar negative');
      expect(zero.attr('height')).toBe('0'); //no height
      expect(parseFloat(zero.attr('y'))).toBeCloseTo(50, 0.25);
    });

    it('sets the correct style and size for intermediate positive value', function(){
      var positiveValue = angular.element(element.find('svg').find('rect')[1]);
      expect(positiveValue.attr('class')).toBe('bar positive');
      expect(positiveValue.attr('height')).toBe('25'); //entire height
      expect(positiveValue.attr('y')).toBe('25');  //starts in the middle
    });

    it('sets the correct style and size for maximum positive value', function(){
      var positiveValue = angular.element(element.find('svg').find('rect')[2]);
      expect(positiveValue.attr('class')).toBe('bar positive');
      expect(positiveValue.attr('height')).toBe('50'); //entire height
      expect(positiveValue.attr('y')).toBe('0');  //starts at the top
    });

  });

  describe('Policy Summary table', function(){
    var url, policySummaryData = {
      "newCounts": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
      "fixedCounts": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
      "unresolvedCounts": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]
    };

    beforeEach(inject(function($rootScope, $httpBackend, CLMLocations) {
      scope = $rootScope.$new();
      scope.filters = commonFilters;
      $httpBackend.expectGET('dashboard-policy-summary').respond('<div></div>');
      url = CLMLocations.getPolicySummaryUrl() + commonFilterQuery;
    }));

    it('Data loaded from server  properly', inject(function($compile, $httpBackend) {
      $httpBackend.expectGET(url).respond(policySummaryData);
      $compile(angular.element('<div dashboard-policy-summary filters="filters"></div>'))(scope);
      $httpBackend.flush();
      assertPolicySummaryBlock('New', 12, 11, policySummaryData.newCounts,
        [1, 3, 6, 10, 15, 21, 28, 36, 45, 55, 66, 78], true,
        scope.$$childHead.policySummaryData[0]);
      assertPolicySummaryBlock('Fixed', 12, 11, policySummaryData.fixedCounts,
        [1, 3, 6, 10, 15, 21, 28, 36, 45, 55, 66, 78], false,
        scope.$$childHead.policySummaryData[1]);
      assertPolicySummaryBlock('Unresolved', 12, 11, policySummaryData.unresolvedCounts,
        [1, 3, 6, 10, 15, 21, 28, 36, 45, 55, 66, 78], true,
        scope.$$childHead.policySummaryData[2]);

      expect(scope.$$childHead.error).toBeFalsy();
    }));

    it('Error propogated to scope', inject(function($compile, $httpBackend) {
      $httpBackend.expectGET(url).respond(404, 'You screwed up');
      $compile(angular.element('<div dashboard-policy-summary filters="filters"></div>'))(scope);
      $httpBackend.flush();
      expect(scope.$$childHead.error).toBeDefined();
    }));

    function assertPolicySummaryBlock(name, counts, delta, barchartData, sparklineData, inverseGreen, policySummaryBlock){
      expect(policySummaryBlock.name).toEqual(name);
      expect(policySummaryBlock.counts).toEqual(counts);
      expect(policySummaryBlock.delta).toEqual(delta);
      expect(policySummaryBlock.barChartData).toEqual(barchartData);
      expect(policySummaryBlock.sparklineData).toEqual(sparklineData);
      expect(policySummaryBlock.inverseGreen).toEqual(inverseGreen);
    }
  });

  describe('windowEventsFactory', function() {
    var wEventsFactory,
      window,
      scope;

    beforeEach(function() {
      module(function($provide) {
        $provide.value('$window', (function() {
          return {
            resize: angular.noop
          };
        })());
      });
    });

    beforeEach(inject(function(windowEventsFactory, $rootScope, $window) {
      wEventsFactory = windowEventsFactory;
      scope = $rootScope.$new();
      window = angular.element($window);
    }));

    describe('addResizeHandler', function() {
      it('invokes a callback when an element width resizes', function() {
        var element = angular.element('<div></div>');
        element.width(1);
        element.height(1);
        var callback = jasmine.createSpy();

        wEventsFactory.addResizeHandler(scope, element, callback);
        element.width(2);
        window.resize();

        expect(callback).toHaveBeenCalled();
      });

      it('invokes a callback when an element height resizes', function() {
        var element = angular.element('<div></div>');
        element.width(1);
        element.height(1);
        var callback = jasmine.createSpy();

        wEventsFactory.addResizeHandler(scope, element, callback);
        element.height(2);
        window.resize();

        expect(callback).toHaveBeenCalled();
      });

      it('does not callback when element is not resized', function() {
        var element = angular.element('<div></div>');
        element.width(1);
        element.height(1);
        var callback = jasmine.createSpy();

        wEventsFactory.addResizeHandler(scope, element, callback);
        window.resize();

        expect(callback).not.toHaveBeenCalled();
      });

      it('does not callback when scope is disposed', function() {
        var element = angular.element('<div></div>');
        element.width(1);
        element.height(1);
        var callback = jasmine.createSpy();

        wEventsFactory.addResizeHandler(scope, element, callback);
        scope.$destroy();

        element.width(2);
        window.resize();

        expect(callback).not.toHaveBeenCalled();
      });
    });
    
    describe('modalHelp', function() {

      var divFoo, divBar, divBaz, scope, modal;
      
      beforeEach(inject(function ($rootScope, $compile, $modal) {
        modal = $modal;
        spyOn(modal, 'open');
        
        scope = $rootScope.$new();
        var page = angular.element('<html><script type="text/ng-template" id="foo"><div>Foo</div></script><script type="text/ng-template" id="bar"><div>Bar</div></script>' +
                '<div id="divFoo" modal-help="foo">click</div><div id="divBar" modal-help="bar" modal-help-trigger="mouseover">mouseover</div><div id="divBaz" modal-help="bar" modal-help-trigger="mouseover" modal-help-class="test-class">mouseover</div></html>');
        $compile(page)(scope);
        
        divFoo = page[2];
        divBar = page[3];
        divBaz = page[4];
      }));

      it('click on foo div to open foo modal', function () {
        expect(divFoo).toBeDefined();
        
        angular.element(divFoo).click();
        
        expect(modal.open).toHaveBeenCalled();
      });
      
      it('mouseover on bar div to open bar modal', function () {
        expect(divBar).toBeDefined();
        
        angular.element(divBar).mouseover();
        
        expect(modal.open).toHaveBeenCalled();
      });
      
      it('mouseover on baz div to open bar modal that has a custom class', function () {
        expect(divBaz).toBeDefined();
        
        angular.element(divBaz).mouseover();
        
        expect(modal.open).toHaveBeenCalledWith({templateUrl: 'bar', windowClass: 'test-class'});
      });
    });
  });
});