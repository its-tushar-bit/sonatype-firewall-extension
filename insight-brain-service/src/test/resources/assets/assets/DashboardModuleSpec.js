describe('DashboardModule', function() {
  function startsWith(url) {
    return new RegExp('^' + url + '\?.*');
  }

  var scope, commonFilters = {
    applicationIds: ['1', '2'],
    policyThreatTypes: ['3', '4'],
    stageTypeIds: ['5', '6'],
    applicationTagIds: ['7', '8'],
    policyThreatLevel: [3, 9]
  }, commonFilterQuery = '?applicationIds=1&applicationIds=2&policyThreatCategories=3,' +
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
          applicationIds: [],
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
          applicationIds: ['foo'],
          policyThreatTypes: [],
          stageTypeIds: [],
          applicationTagIds: [],
          policyThreatLevel: [0,10]
        };
      });
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

  describe('stageFilter', function () {
    it('empty filter', inject(function ($filter) {
      var stageList = [{ id : 'operate' }, { id : 'build' }, { id : 'release' }, { id : 'stage-release' }],
          result;

      // null filter
      result = $filter('stageFilter')(stageList);
      expect(result).toEqual(stageList);

      // empty filter
      result = $filter('stageFilter')(stageList, { stageTypeIds : [] });
      expect(result).toEqual(stageList);
    }));

    it('filter', inject(function ($filter) {
      var stageList = [{ id : 'build' }, { id : 'stage-release' }, { id : 'release' }, { id : 'operate' }],
          result;

      result = $filter('stageFilter')(stageList, { stageTypeIds : ['release', 'build'] });
      expect(result).toEqual([{ id : 'build' }, { id : 'release' }]);
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
              applicationIds: ['foo'],
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
            scope.filters.applicationIds = ['bar'];
          });
          $httpBackend.flush();
          expect(directiveScope.data).toEqual('bar');
        }));

        it('Drops Requests That Don\'t Match', inject(function (CLMLocations, $httpBackend) {
          $httpBackend.expectGET(startsWith(CLMLocations[directive.urlFn]())).respond('foo');
          scope.$apply(function () {
            scope.filters =  {
              applicationIds: ['foo'],
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
            scope.filters.applicationIds = ['bar'];
          });
          $httpBackend.flush();
          expect(directiveScope.data).toEqual('bar');
        }));

        it('Errors', inject(function (CLMLocations, $httpBackend) {
          $httpBackend.expectGET(startsWith(CLMLocations[directive.urlFn]())).respond(500, 'foo');
          scope.$apply(function () {
            scope.filters =  {
              applicationIds: ['foo'],
              policyThreatTypes: [],
              stageTypeIds: [],
              applicationTagIds: [],
              policyThreatLevel: [0,10]
            };
          });
          $httpBackend.flush();
          expect(directiveScope.error).toBeTruthy();
          expect(directiveScope.data).toBeFalsy();
        }));

        it('Derives Policy Threat Level Categories from Filter', inject(function (CLMLocations, $httpBackend) {
          for (var i = 0; i <= 10; i++) {
            $httpBackend.expectGET(startsWith(CLMLocations[directive.urlFn]())).respond('foo');
            scope.$apply(function () {
              scope.filters =  {
                applicationIds: [],
                policyThreatTypes: [],
                stageTypeIds: [],
                applicationTagIds: [],
                policyThreatLevel: [i,i]
              };
            });
            $httpBackend.flush();
            expect(directiveScope.policyThreatLevelCategories).toEqual({
              critical: 8 <= i && i <= 10, 
              severe : 4 <= i && i < 8, 
              moderate : 2 <= i && i < 4, 
              low : i < 2
            });
          }
        }));
      });
    });
  });

  describe('Dashboard view summary', function() {
    var scope;

    beforeEach(inject(function($rootScope, $httpBackend) {
      scope = $rootScope.$new();
      scope.filters={};
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
      expect(scope.states[0].state).toBe('dashboard.overview.newest-risk');
      expect(scope.states[1].state).toBe('dashboard.component');
    }));
  });

  describe('sortable', function() {
    var barScope, fooScope, invertedScope;

    beforeEach(inject(function ($rootScope, $compile) {
      scope = $rootScope.$new();
      $compile('<div sortable="bar">' +
        '<span sort-columns="foo">foo</span>' +
        '<span sort-columns="-bar">bar</span>' +
        '<span sort-columns="-foobar" sort-inverted="true">foobar</span>' +
        '</div>')(scope);
      fooScope = scope.$$childHead;
      barScope = scope.$$childHead.$$nextSibling.$$nextSibling;
      invertedScope = barScope.$$nextSibling.$$nextSibling;
    }));

    it('tests', function () {
      // by default we should sort by the top-level sortable attributes
      expect(fooScope.isUp()).toBeFalsy();
      expect(fooScope.isDown()).toBeFalsy();
      expect(invertedScope.isUp()).toBeFalsy();
      expect(invertedScope.isDown()).toBeFalsy();
      expect(barScope.isUp()).toBeTruthy();
      expect(barScope.isDown()).toBeFalsy();
      expect(scope.getSortField()).toEqual(['bar']);

      // when sorting by a specific child, should respect its initial sort-columns
      fooScope.$apply(function () {
        barScope.setSort();
      });
      expect(fooScope.isUp()).toBeFalsy();
      expect(fooScope.isDown()).toBeFalsy();
      expect(invertedScope.isUp()).toBeFalsy();
      expect(invertedScope.isDown()).toBeFalsy();
      expect(barScope.isUp()).toBeFalsy();
      expect(barScope.isDown()).toBeTruthy(); //DESC sort is expected for '-bar'
      expect(scope.getSortField()).toEqual(['-bar']);

      //default sort in this case is ASC for 'foo'
      fooScope.$apply(function () {
        fooScope.setSort();
      });
      expect(fooScope.isUp()).toBeTruthy();
      expect(fooScope.isDown()).toBeFalsy();
      expect(invertedScope.isUp()).toBeFalsy();
      expect(invertedScope.isDown()).toBeFalsy();
      expect(barScope.isUp()).toBeFalsy();
      expect(barScope.isDown()).toBeFalsy();
      expect(scope.getSortField()).toEqual(['foo']);

      //setting the sort a second time should reverse the operator and icons
      fooScope.$apply(function () {
        fooScope.setSort();
      });
      expect(fooScope.isUp()).toBeFalsy();
      expect(fooScope.isDown()).toBeTruthy();
      expect(invertedScope.isUp()).toBeFalsy();
      expect(invertedScope.isDown()).toBeFalsy();
      expect(barScope.isUp()).toBeFalsy();
      expect(barScope.isDown()).toBeFalsy();
      expect(scope.getSortField()).toEqual(['-foo']);

      //sorting on a field which is marked as sort-inverted should invert all expectations
      fooScope.$apply(function () {
        invertedScope.setSort();
      });

      expect(fooScope.isUp()).toBeFalsy();
      expect(fooScope.isDown()).toBeFalsy();
      expect(invertedScope.isUp()).toBeTruthy();
      expect(invertedScope.isDown()).toBeFalsy();
      expect(barScope.isUp()).toBeFalsy();
      expect(barScope.isDown()).toBeFalsy();
      expect(scope.getSortField()).toEqual(['-foobar']);

      //sorting on a field which is marked as sort-inverted should invert all expectations
      fooScope.$apply(function () {
        invertedScope.setSort();
      });

      expect(fooScope.isUp()).toBeFalsy();
      expect(fooScope.isDown()).toBeFalsy();
      expect(invertedScope.isUp()).toBeFalsy();
      expect(invertedScope.isDown()).toBeTruthy();
      expect(barScope.isUp()).toBeFalsy();
      expect(barScope.isDown()).toBeFalsy();
      expect(scope.getSortField()).toEqual(['foobar']);
    });

  });

  describe('sortable with secondary sort', function() {
    var barScope, fooScope;

    beforeEach(inject(function ($rootScope, $compile) {
      scope = $rootScope.$new();
      $compile('<div sortable="bar,foo">' +
        '<span sort-columns="foo">foo</span><span sort-columns="-bar,foo">bar</span></div>')(scope);
      fooScope = scope.$$childHead;
      barScope = scope.$$childHead.$$nextSibling.$$nextSibling;
    }));

    it('tests', function () {
      expect(scope.getSortField()).toEqual(['bar','foo']);

      fooScope.$apply(function () {
        barScope.setSort();
      });

      expect(scope.getSortField()).toEqual(['-bar','foo']);
    });
  });


  describe('NewestRiskTableController', function() {
    var stageTypes = [
      {"name": "Build", "id": "build"},
      {"name": "Develop", "id": "develop"},
      {"name": "Release", "id": "release"},
      {"name": "Stage Release", "id": "stage-release"},
      {"name": "Operate", "id": "operate"}
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
      expect(risk.gavName).toBe('foo:bar:1.0');
    });
  });

  describe('sparkline', function() {
    var compile, scope;

    beforeEach(inject(function($compile, $rootScope) {
      compile = $compile;
      scope = $rootScope.$new();
    }));

    it('sparkline will have class', function() {
      var element = angular.element('<div sparkline></div>');
      element = compile(element)(scope);

      var svg = element.find('svg');
      expect(svg).toBeDefined();
      expect(svg.attr('class')).toBe('chart');
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
      var fill = element.find('.fill.up');
      expect(fill.attr('d').split(',').length).toBe(5);

      // expect each point, plus the 'move to' zero path command
      var line = element.find('.line.up');
      expect(line.attr('d').split(',').length).toBe(3);
    });

    it('sparkline renders trailing colors inverted when inverse is enabled', function() {
      var element = angular.element('<div sparkline data="[0,1,2,1,2]" inverse-green="true"></div>');
      element = compile(element)(scope);

      var fill = element.find('.fill.down');
      expect(fill.length).toBe(0);

      fill = element.find('.fill.up');
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
    var element,
        height = 25;
    beforeEach(inject(function($rootScope, $compile) {
      scope = $rootScope.$new();
      scope.barData = [-50, 0, 50];
      element = $compile(angular.element('<div value-bars data="barData"></div>'))(scope);
    }));

    it('creates an SVG element based on the data', function(){
      expect(element.find('svg')).toBeTruthy();
    });

    it('creates a bar for each of the data points', function(){
      expect(element.find('svg').find('rect').length).toBe(scope.barData.length);
    });

    it('sets the correct style and size for values below zero', function(){
      var negativeValue = angular.element(element.find('svg').find('rect')[0]);
      expect(negativeValue.attr('class')).toBe('bar down');
      expect(negativeValue.attr('height')).toEqual('' + height / 2); //half of chart below zero
      expect(negativeValue.attr('y')).toEqual('' + height / 2);  //starts in the middle between high/low
    });

    it('sets the correct style and size for zero values', function(){
      var zero = angular.element(element.find('svg').find('rect')[1]);
      expect(zero.attr('class')).toBe('bar down');
      expect(zero.attr('height')).toBe('0'); //no height
      expect(zero.attr('y')).toEqual('' + height / 2);  //starts in the middle
    });

    it('sets the correct style and size for positive values', function(){
      var positiveValue = angular.element(element.find('svg').find('rect')[2]);
      expect(positiveValue.attr('class')).toBe('bar up');
      expect(positiveValue.attr('height')).toEqual('' + height / 2); //half of chart above zero
      expect(positiveValue.attr('y')).toBe('0');  //starts at the top
    });

    it('sets the correct width for the baseline', function(){
      var first = angular.element(element.find('svg').find('rect')[0]);
      var last = angular.element(element.find('svg').find('rect')[scope.barData.length - 1]);
      var baseline = angular.element(element.find('svg').find('line')[0]);
      expect(baseline.attr('x1')).toEqual(first.attr('x'));
      expect(baseline.attr('x2')).toEqual((parseInt(last.attr('x')) + parseInt(last.attr('width'))).toFixed());
    });

  });

  describe('Value bar chart with only positive values', function(){
    var element,
        height = 25;
    beforeEach(inject(function($rootScope, $compile) {
      scope = $rootScope.$new();
      scope.barData = [0, 25, 50];
      element = $compile(angular.element('<div value-bars data="barData"></div>'))(scope);
    }));

    it('sets the correct style and size for zero values', function(){
      var zero = angular.element(element.find('svg').find('rect')[0]);
      expect(zero.attr('class')).toBe('bar down');
      expect(zero.attr('height')).toBe('0'); //no height
      expect(parseFloat(zero.attr('y'))).toBe(height - 0.5); //baseline is fudged so it doesn't render outside the svg element
    });

    it('sets the correct style and size for intermediate positive value', function(){
      var positiveValue = angular.element(element.find('svg').find('rect')[1]);
      expect(positiveValue.attr('class')).toBe('bar up');
      expect(positiveValue.attr('height')).toBe('' + height / 2); //entire height
      expect(positiveValue.attr('y')).toBe('' + height / 2);  //starts in the middle
    });

    it('sets the correct style and size for maximum positive value', function(){
      var positiveValue = angular.element(element.find('svg').find('rect')[2]);
      expect(positiveValue.attr('class')).toBe('bar up');
      expect(positiveValue.attr('height')).toBe('' + height); //entire height
      expect(positiveValue.attr('y')).toBe('0');  //starts at the top
    });

  });

  describe('Policy Summary table', function() {
    var url, policySummaryData = {
      totalNew: 100,
      totalFixed: 48,
      totalWaived: 2,
      currentUnresolved: 50,
      weeklyDeltaNew: [1, 1, 2, 0, 3, 0, 1, 5, 2, 1, 0, 1],
      weeklyDeltaFixed: [1, 0, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0],
      weeklyDeltaUnresolved: [0, 0, 1, -1, 2, -1, -1, 5, 2, 1, -1, 1],
      weeklyDeltaWaived: [0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0]
    };

    beforeEach(inject(function($rootScope, $httpBackend, CLMLocations) {
      scope = $rootScope.$new();
      scope.filters = commonFilters;
      $httpBackend.expectGET('dashboard-policy-summary').respond('<div></div>');
      url = CLMLocations.getPolicySummaryUrl() + commonFilterQuery;
    }));

    it('Data loaded from server properly', inject(function($compile, $httpBackend) {
      $httpBackend.expectGET(url).respond(policySummaryData);
      $compile(angular.element('<div dashboard-policy-summary filters="filters"></div>'))(scope);
      $httpBackend.flush();
      assertPolicySummaryBlock('Discovered', 100, 17, policySummaryData.weeklyDeltaNew,
        [83, 84, 85, 87, 87, 90, 90, 91, 96, 98, 99, 99, 100], undefined,
        scope.$$childHead.policySummaryData[3]);
      assertPolicySummaryBlock('Fixed', 48, 7, policySummaryData.weeklyDeltaFixed,
          [41, 42, 42, 43, 44, 45, 46, 47, 47, 47, 47, 48, 48], true,
        scope.$$childHead.policySummaryData[2]);
      assertPolicySummaryBlock('Waived', 2, 2, policySummaryData.weeklyDeltaWaived,
          [0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2], false,
          scope.$$childHead.policySummaryData[1]);
      assertPolicySummaryBlock('Pending', 50, 8, policySummaryData.weeklyDeltaUnresolved,
        [42, 42, 42, 43, 42, 44, 43, 42, 47, 49, 50, 49, 50], false,
        scope.$$childHead.policySummaryData[0]);

      expect(scope.$$childHead.error).toBeFalsy();
    }));

    it('Error propogated to scope', inject(function($compile, $httpBackend) {
      $httpBackend.expectGET(url).respond(404, 'You screwed up');
      $compile(angular.element('<div dashboard-policy-summary filters="filters"></div>'))(scope);
      $httpBackend.flush();
      expect(scope.$$childHead.error).toBeDefined();
    }));

    function assertPolicySummaryBlock(name, counts, delta, barchartData, sparklineData, naturalOrder,
                                      policySummaryBlock)
    {
      expect(policySummaryBlock.name).toEqual(name);
      expect(policySummaryBlock.counts).toEqual(counts);
      expect(policySummaryBlock.delta).toEqual(delta);
      expect(policySummaryBlock.barChartData).toEqual(barchartData);
      expect(policySummaryBlock.sparklineData).toEqual(sparklineData);
      expect(policySummaryBlock.naturalOrder).toEqual(naturalOrder);
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
  });

  describe('modalHelp', function() {

    var divFoo, divBar, divBaz, scope, modal;

    beforeEach(inject(function($rootScope, $compile, $modal) {
      modal = $modal;
      spyOn(modal, 'open');

      scope = $rootScope.$new();
      var page = angular.element('<html><script type="text/ng-template" id="foo"><div>Foo</div></script>'
              + '<script type="text/ng-template" id="bar"><div>Bar</div></script>'
              + '<div id="divFoo" modal-help="foo">click</div>'
              + '<div id="divBar" modal-help="bar" modal-help-trigger="mouseover">mouseover</div>'
              + '<div id="divBaz" modal-help="bar" modal-help-trigger="mouseover"'
              + 'modal-help-class="test-class">mouseover</div></html>');
      $compile(page)(scope);

      divFoo = page[2];
      divBar = page[3];
      divBaz = page[4];
    }));

    it('click on foo div to open foo modal', function() {
      expect(divFoo).toBeDefined();

      angular.element(divFoo).click();

      expect(modal.open).toHaveBeenCalled();
    });

    it('mouseover on bar div to open bar modal', function() {
      expect(divBar).toBeDefined();

      angular.element(divBar).mouseover();

      expect(modal.open).toHaveBeenCalled();
    });

    it('mouseover on baz div to open bar modal that has a custom class', function() {
      expect(divBaz).toBeDefined();

      angular.element(divBaz).mouseover();

      expect(modal.open).toHaveBeenCalled()
      expect(modal.open.mostRecentCall.args[0].templateUrl).toBe('bar');
      expect(modal.open.mostRecentCall.args[0].windowClass).toBe('test-class');
    });

    it('plays space invaders', inject(function($httpBackend, CLMLocations) {
      expect(divFoo).toBeDefined();

      angular.element(divFoo).click();

      var modalScope = scope.$$childHead.$$childHead;

      $httpBackend.expectGET(CLMLocations.getComponentRisksUrl()).respond([]);
      modalScope.invade();
      $httpBackend.flush();
    }));
  });

  describe('dashboard "emptyToEnd" filter', function() {
    var emptyToEnd, data = [
        { key: null },
        { key: 'value'},
        { key: null }
      ], expectedResult = [
        { key: 'value' },
        { key: null },
        { key: null }
      ]
      ;
    beforeEach(inject(function($filter) {
      emptyToEnd = $filter('emptyToEnd');
    }));

    it('should filter all null values to the end of an array of objects', function() {
      expect(emptyToEnd(data, 'key')).toEqual(expectedResult);
    });

    it('should filter all null values to the end when given a compound key', function() {
      expect(emptyToEnd(data, ['key','key2'])).toEqual(expectedResult);
    });
  });

  describe('Pathname Popover', function() {

    var divElement, scope;
    
    beforeEach(inject(function ($rootScope, $compile) {
      jasmine.Clock.useMock();
      
      scope = $rootScope.$new();
      var page = angular
              .element('<div pathnames-popover="[\'pathname1\', \'pathname2\']">empty div</div>');
      $compile(page)(scope);
      
      divElement = angular.element(page[0]);
      
      scope.$digest();
    }));
    
    afterEach(function () {
      $('.popover').remove();
    });
    
    it('popover displayed when hovering over div', function () {   
      spyOn($.fn, 'popover').andCallThrough();
      spyOn($.fn, 'is').andReturn(true);
      
      // Mouse enter and hover.
      divElement.mouseover();
      jasmine.Clock.tick(51);
      expect(divElement.popover).toHaveBeenCalledWith('show');
      
      // Ensure the contents are correct, just the first pathname.
      var popover = $('.popover');
      expect(popover.html()).toContain('pathname1');
      expect(popover.html()).not.toContain('pathname2');
      
      // Mouse leave.
      // Set the 'is' check to false because leaving the element (div)
      // checks that we are hovering over the popover, which we want to return false.
      $.fn.is.andReturn(false);
      divElement.mouseleave();
      jasmine.Clock.tick(101);      
      expect(divElement.popover).toHaveBeenCalledWith('hide');
    });
    
    it('popover functions modally', function () {      
      spyOn($.fn, 'popover').andCallThrough();
      spyOn($.fn, 'is').andReturn(true);
      
      // Mouse enter and hover.
      divElement.mouseover();
      jasmine.Clock.tick(51);
      expect(divElement.popover).toHaveBeenCalledWith('show');
      
      // Hover over the popover instead of the element.
      var popover = $('.popover');
      divElement.mouseleave();
      popover.mouseover();
      // Skip forward 101 ms to ensure that the close fires from leaving the element.
      jasmine.Clock.tick(101);
      expect($.fn.is.callCount).toEqual(2);
      // Even though the close event fired the popover is still open because we are hovering over it.
      expect($('.popover').length).toEqual(1);
      // Leave the popover.
      popover.mouseleave();
      expect(divElement.popover).toHaveBeenCalledWith('hide');
    });
  });
});