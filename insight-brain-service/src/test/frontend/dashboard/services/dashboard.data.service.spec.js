describe('dashboard.data.service.spec', function() {
  var $httpBackend, dashboardDataService, CLMLocations;

  beforeEach(module('dashboard.module'));

  beforeEach(inject(function($injector) {
    $httpBackend = $injector.get('$httpBackend');
    dashboardDataService = $injector.get('dashboard.data.service');
    CLMLocations = $injector.get('CLMLocations');
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation(false);
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('getNewestRisks()', function() {
    it('returns data on success', function() {
      var result, filter = {filterParam: 'filter value'},
          data = {
            dashboardResults: [
              {
                hash: 'f60e9504841ba867a692',
                displayName: {
                  parts: [
                    {field: 'any', value: 'foo'},
                    {value: ' : '},
                    {field: 'any', value: 'bar'}
                  ]
                },
                stageDetails: [
                  {
                    stageTypeId: "stage-release",
                    time: 123456789
                  }, {
                    stageTypeId: "build",
                    time: 0
                  }
                ]
              },
              {
                hash: '1249e25aebb15358bedd'
              }
            ],
            numResults: 2
          };
      $httpBackend.expectPOST(CLMLocations.getNewestRisksUrl(), filter).respond(data);

      dashboardDataService.getNewestRisks(filter).then(function(data) {
        result = data[0];
      });

      $httpBackend.flush();
      expect(result[0].hash).toBe('f60e9504841ba867a692');
      expect(result[0].gavName).toBe('foo : bar');
      expect(result[0].stagereleaseTime).toBe(123456789);
      expect(result[0].buildTime).toBe(null);
      expect(result[1].hash).toBe('1249e25aebb15358bedd');
      expect(result[1].gavName).toBe('Unknown');
    });

    it('saves and updates result count', function() {
      var filter = {filterParam: 'filter value'},
          data = {dashboardResults: [], numResults: 5};
      $httpBackend.whenPOST(CLMLocations.getNewestRisksUrl()).respond(data);

      dashboardDataService.getNewestRisks(filter);
      $httpBackend.flush();

      expect(dashboardDataService.latestResultCounts.newestRisk).toBe(5);
      expect(dashboardDataService.latestResultCounts.componentRisk).toBe(undefined);
      expect(dashboardDataService.latestResultCounts.applicationRisk).toBe(undefined);

      // does not update if filter is not changed
      dashboardDataService.latestResultCounts.componentRisk = 3;
      dashboardDataService.latestResultCounts.applicationRisk = 2;
      dashboardDataService.getNewestRisks(filter);
      $httpBackend.flush();

      expect(dashboardDataService.latestResultCounts.newestRisk).toBe(5);
      expect(dashboardDataService.latestResultCounts.componentRisk).toBe(3);
      expect(dashboardDataService.latestResultCounts.applicationRisk).toBe(2);

      // resets all counts if called with different filter
      dashboardDataService.getNewestRisks({filterParam: 'different filter value'});
      $httpBackend.flush();

      expect(dashboardDataService.latestResultCounts.newestRisk).toBe(5);
      expect(dashboardDataService.latestResultCounts.componentRisk).toBe(undefined);
      expect(dashboardDataService.latestResultCounts.applicationRisk).toBe(undefined);
    });

  });

  describe('getApplicationRisks()', function() {
    function createRisk(total, critical, severe, moderate, low) {
      return {
        totalRisk: total,
        criticalRisk: critical,
        severeRisk: severe,
        moderateRisk: moderate,
        lowRisk: low
      };
    }

    it('returns data on success', function() {
      var originalRisks = [{
            applicationName: 'application1',
            applicationId: 'app1',
            totalApplicationRisk: createRisk(5, 4, 3, 2, 1),
            stages: []
          }, {
            applicationName: 'application2',
            applicationId: 'app2',
            totalApplicationRisk: createRisk(6, 0),
            stages: []
          }],
          filter = {
            filterParam: 'filter value'
          },
          spy = jasmine.createSpy("response");

      $httpBackend.expectPOST(CLMLocations.getApplicationRisksUrl(), filter).respond({
        dashboardResults: [
          {
            applicationName: 'application1',
            applicationId: 'app1',
            totalApplicationRisk: createRisk(5, 4, 3, 2, 1),
            stages: []
          }, {
            applicationName: 'application2',
            applicationId: 'app2',
            totalApplicationRisk: createRisk(6, 0),
            stages: []
          }
        ],
        numResults: 2
      });

      dashboardDataService.getApplicationRisks(filter).then(spy);

      $httpBackend.flush();
      expect(spy).toHaveBeenCalledWith([originalRisks, [1, 2, 3, 4, 5, 6]]);
      expect(dashboardDataService.latestResultCounts.applicationRisk).toBe(2);
    });

    it('saves and updates result count', function() {
      var filter = {filterParam: 'filter value'},
          data = { dashboardResults: [], numResults: 5 };
      $httpBackend.whenPOST(CLMLocations.getApplicationRisksUrl()).respond(data);

      dashboardDataService.getApplicationRisks(filter);
      $httpBackend.flush();

      expect(dashboardDataService.latestResultCounts.newestRisk).toBe(undefined);
      expect(dashboardDataService.latestResultCounts.componentRisk).toBe(undefined);
      expect(dashboardDataService.latestResultCounts.applicationRisk).toBe(5);

      // does not update if filter is not changed
      dashboardDataService.latestResultCounts.newestRisk = 3;
      dashboardDataService.latestResultCounts.componentRisk = 2;
      dashboardDataService.getApplicationRisks(filter);
      $httpBackend.flush();

      expect(dashboardDataService.latestResultCounts.newestRisk).toBe(3);
      expect(dashboardDataService.latestResultCounts.componentRisk).toBe(2);
      expect(dashboardDataService.latestResultCounts.applicationRisk).toBe(5);

      // resets all counts if called with different filter
      dashboardDataService.getApplicationRisks({filterParam: 'different filter value'});
      $httpBackend.flush();

      expect(dashboardDataService.latestResultCounts.newestRisk).toBe(undefined);
      expect(dashboardDataService.latestResultCounts.componentRisk).toBe(undefined);
      expect(dashboardDataService.latestResultCounts.applicationRisk).toBe(5);
    });
  });

  describe('getComponentRisks()', function() {
    var components, filter = {filterParam: 'filter value'};
    it('populates component name', function() {
      var series, data = {
        dashboardResults: [
          {
            hash: 'f60e9504841ba867a692',
            displayName: {
              parts: [
                {field: 'any', value: 'foo'},
                {value: ' : '},
                {field: 'any', value: 'bar'}
              ]
            },
            score: 12
          },
          {
            hash: '1249e25aebb15358bedd',
            scoreSevere: 8
          }
        ],
        numResults: 2
      };

      $httpBackend.expectPOST(CLMLocations.getComponentRisksUrl(), filter).respond(data);

      dashboardDataService.getComponentRisks(filter).then(function(data) {
        components = data[0];
        series = data[1];
      });

      $httpBackend.flush();
      expect(components[0].hash).toBe('f60e9504841ba867a692');
      expect(components[0].name).toBe('foo : bar');
      expect(components[1].hash).toBe('1249e25aebb15358bedd');
      expect(components[1].name).toBe('Unknown');

      expect(series).toEqual([12, 8]);
    });

    it('saves and updates result count', function() {
      var filter = {filterParam: 'filter value'},
          data = { dashboardResults: [], numResults: 5 };
      $httpBackend.whenPOST(CLMLocations.getComponentRisksUrl()).respond(data);

      dashboardDataService.getComponentRisks(filter);
      $httpBackend.flush();

      expect(dashboardDataService.latestResultCounts.newestRisk).toBe(undefined);
      expect(dashboardDataService.latestResultCounts.componentRisk).toBe(5);
      expect(dashboardDataService.latestResultCounts.applicationRisk).toBe(undefined);

      // does not update if filter is not changed
      dashboardDataService.latestResultCounts.newestRisk = 3;
      dashboardDataService.latestResultCounts.applicationRisk = 2;
      dashboardDataService.getComponentRisks(filter);
      $httpBackend.flush();

      expect(dashboardDataService.latestResultCounts.newestRisk).toBe(3);
      expect(dashboardDataService.latestResultCounts.componentRisk).toBe(5);
      expect(dashboardDataService.latestResultCounts.applicationRisk).toBe(2);

      // resets all counts if called with different filter
      dashboardDataService.getComponentRisks({filterParam: 'different filter value'});
      $httpBackend.flush();

      expect(dashboardDataService.latestResultCounts.newestRisk).toBe(undefined);
      expect(dashboardDataService.latestResultCounts.componentRisk).toBe(5);
      expect(dashboardDataService.latestResultCounts.applicationRisk).toBe(undefined);
    });
  });
});
