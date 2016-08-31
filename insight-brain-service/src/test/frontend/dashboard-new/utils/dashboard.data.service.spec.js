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
      var result, filter = {filterParam: 'filter value'};
      $httpBackend.expectPOST(CLMLocations.getNewestRisksUrl(), filter)
          .respond(['violation1', 'violation2']);

      dashboardDataService.getNewestRisks(filter).then(function(data) {
        result = data;
      });

      $httpBackend.flush();
      expect(result).toEqual(['violation1', 'violation2']);
    });
  });

  describe('getApplicationRisks()', function() {
    it('returns data on success', function() {
      var result, filter = {filterParam: 'filter value'};
      $httpBackend.expectPOST(CLMLocations.getApplicationRisksUrl(), filter)
          .respond(['application1', 'application2']);

      dashboardDataService.getApplicationRisks(filter).then(function(data) {
        result = data;
      });

      $httpBackend.flush();
      expect(result).toEqual(['application1', 'application2']);
    });
  });

  describe('getComponentRisks()', function() {
    var result, filter = {filterParam: 'filter value'};
    it('populates component name', function() {
      var data = [
        {
          hash: 'f60e9504841ba867a692',
          displayName: {
            parts: [
              {field: 'any', value: 'foo'},
              {value: ' : '},
              {field: 'any', value: 'bar'}
            ]
          }
        },
        {
          hash: '1249e25aebb15358bedd'
        }
      ];

      $httpBackend.expectPOST(CLMLocations.getComponentRisksUrl(), filter).respond(data);

      dashboardDataService.getComponentRisks(filter).then(function(components) {
        result = components;
      });

      $httpBackend.flush();
      expect(result[0].hash).toBe('f60e9504841ba867a692');
      expect(result[0].name).toBe('foo : bar');
      expect(result[1].hash).toBe('1249e25aebb15358bedd');
      expect(result[1].name).toBe('Unknown');
    });
  });
});
