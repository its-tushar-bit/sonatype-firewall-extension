describe('dashboard.data.service.spec', function() {
  var $httpBackend, dashboardDataService, CLMLocations;

  beforeEach(module('dashboard.utils'));

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
          data = [
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
      ];
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

      $httpBackend.expectPOST(CLMLocations.getApplicationRisksUrl(), filter).respond([{
        applicationName: 'application1',
        applicationId: 'app1',
        totalApplicationRisk: createRisk(5,4,3,2,1),
        stages: []
      }, {
        applicationName: 'application2',
        applicationId: 'app2',
        totalApplicationRisk: createRisk(6, 0),
        stages: []
      }]);

      dashboardDataService.getApplicationRisks(filter).then(spy);

      $httpBackend.flush();
      expect(spy).toHaveBeenCalledWith([originalRisks, [1, 2, 3, 4, 5, 6]]);
    });
  });

  describe('getComponentRisks()', function() {
    var components, filter = {filterParam: 'filter value'};
    it('populates component name', function() {
      var series, data = [
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
      ];

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
  });

  describe('deleteFilterNames()', function() {
    it('properly parses multiple errors', function() {
      $httpBackend.expectPOST(CLMLocations.getDashboardDeleteFiltersUrl()).respond(500, [
        {
          "name": "Test1",
          "errorMessage": "foo",
          "status": 404
        },
        {
          "name": "Test2",
          "errorMessage": "bar",
          "status": 500
        }
      ]);

      dashboardDataService.deleteSavedFilters(['Test1', 'Test2']).then(function() {
        throw 'promise should have been rejected';
      }).catch(function(error) {
        expect(error).toEqual(['Filter Test1, foo', 'Filter Test2, bar']);
      });

      $httpBackend.flush();
    });

    it('properly parses single error', function() {
      $httpBackend.expectPOST(CLMLocations.getDashboardDeleteFiltersUrl()).respond(404, "not found");

      dashboardDataService.deleteSavedFilters(['Test1']).then(function() {
        throw 'promise should have been rejected';
      }).catch(function(error) {
        expect(error).toEqual(['not found']);
      });

      $httpBackend.flush();
    });
  });
});
