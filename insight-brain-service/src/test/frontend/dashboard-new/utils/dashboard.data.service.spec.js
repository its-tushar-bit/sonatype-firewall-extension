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
    it('returns data on success', function() {
      var risks, filter = {filterParam: 'filter value'};
      $httpBackend.expectPOST(CLMLocations.getApplicationRisksUrl(), filter)
          .respond(['application1', 'application2']);

      dashboardDataService.getApplicationRisks(filter).then(function(data) {
        risks = data[0];
      });

      $httpBackend.flush();
      expect(risks).toEqual(['application1', 'application2']);
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
});
