describe('dashboardFilterService', function() {
  var $httpBackend, dashboardFilterService, CLMLocations;

  beforeEach(module('dashboard.module'));

  beforeEach(inject(function($injector) {
    $httpBackend = $injector.get('$httpBackend');
    dashboardFilterService = $injector.get('dashboardFilterService');
    CLMLocations = $injector.get('CLMLocations');
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation(false);
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('deleteFilterNames()', function() {
    it('properly parses multiple errors', function() {
      $httpBackend.expectPOST(CLMLocations.getDashboardDeleteFiltersUrl()).respond(500, [
        {
          'name': 'Test1',
          'errorMessage': 'foo',
          'status': 404
        },
        {
          'name': 'Test2',
          'errorMessage': 'bar',
          'status': 500
        }
      ]);

      dashboardFilterService.deleteSavedFilters(['Test1', 'Test2']).then(function() {
        throw 'promise should have been rejected';
      }).catch(function(error) {
        expect(error).toEqual(['Filter Test1, foo', 'Filter Test2, bar']);
      });

      $httpBackend.flush();
    });

    it('properly parses single error', function() {
      $httpBackend.expectPOST(CLMLocations.getDashboardDeleteFiltersUrl()).respond(404, 'not found');

      dashboardFilterService.deleteSavedFilters(['Test1']).then(function() {
        throw 'promise should have been rejected';
      }).catch(function(error) {
        expect(error).toEqual(['not found']);
      });

      $httpBackend.flush();
    });
  });

  describe('filterToJson()', function() {
    var filter = {
      organizations: {'orgId1': true, 'orgId2': true},
      policyTypes: {'QUALITY': true, 'OTHER': true, 'SECURITY': true},
      stages: {'release': true, 'stage-release': true, 'build': true},
      categories: {'tagId1': true, 'tagId2': true, 'null': true},
      applications: {'applicationIdZ': true, 'applicationIdA': true, 'applicationIdQ': true, 'applicationIdR': true},
      policyViolationStates: {'OPEN': true, 'WAIVED': true},
      age: {maxDaysOld: 90},
      policyThreatLevels: [3, 6]
    };

    it('creates proper filter json representation', function() {
      var filterJson = dashboardFilterService.filterToJson(filter);
      expect(filterJson.organizationFilters).toEqual(['orgId1', 'orgId2']);
      expect(filterJson.policyThreatCategoryFilters).toEqual(['QUALITY', 'OTHER', 'SECURITY']);
      expect(filterJson.stageTypeFilters).toEqual(['release', 'stage-release', 'build']);
      expect(filterJson.tagFilters).toEqual(['tagId1', 'tagId2', null]);
      expect(filterJson.applicationFilters).toEqual(['applicationIdZ', 'applicationIdA', 'applicationIdQ', 'applicationIdR']);
      expect(filterJson.policyViolationStates).toEqual(['OPEN', 'WAIVED']);
      expect(filterJson.maxDaysOld).toEqual(90);
      expect(filterJson.minPolicyThreatLevel).toEqual(3);
      expect(filterJson.maxPolicyThreatLevel).toEqual(6);
    });
  });
});
