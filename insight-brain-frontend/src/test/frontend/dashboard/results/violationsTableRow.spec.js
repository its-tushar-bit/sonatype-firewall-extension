import dashboardResultsModule from '../../../../main/frontend/dashboard/results/module';
import legacyConfigurationModule from '../../../../main/frontend/LegacyConfigurationModule';

describe('violationsTableRow.spec', function() {

  var scope,
      getVm,
      mockWindow,
      mockState,
      violationsTableRow;

  var riskData =
      {
        'stageTypeId': 'stage-release',
        'actionTypeId': null,
        'scanId': '5d6a9955588f482a9e48d2d93f2236f0'
      };

  beforeEach(angular.mock.module(dashboardResultsModule.name, legacyConfigurationModule.name, function($provide) {
    mockWindow = jasmine.createSpyObj('$window', ['open']);
    mockState = {href: angular.noop};
    $provide.value('$window', mockWindow);
    $provide.value('$state', mockState);
  }));

  afterEach(inject(function($httpBackend) {
    scope.$destroy();
    $httpBackend.verifyNoOutstandingExpectation(false);
    $httpBackend.verifyNoOutstandingRequest();
  }));

  beforeEach(inject([
    '$q', '$rootScope', '$compile', '$httpBackend', 'CLMLocations', '$templateCache',
    function($q, $rootScope, $compile, $httpBackend, CLMLocations, $templateCache) {
      scope = $rootScope.$new();
      $httpBackend.expectGET(CLMLocations.getDashboardStageUrl()).respond(MockData.getDashboardStageData());
      $templateCache.put('violations-table-row', '<td/>');

      getVm = function() {
        scope.risk = riskData;

        var element = $compile('<tr violations-table-row risk="risk"></tr>')(scope);
        scope.$digest();

        return element.controller('violationsTableRow');
      };
    }
  ]));

  describe('ViolationsTableRowComponent', function() {
    beforeEach(inject([
      '$httpBackend', function($httpBackend) {
        violationsTableRow = getVm();
        $httpBackend.flush();
      }
    ]));

    it('Modifies the loaded stages to remove develop and rename Stage-Release', function() {
      expect(Object.keys(violationsTableRow.stageTypes).length).toBe(4);
      expect(violationsTableRow.stageTypes['stage-release'].shortName).toBe('Stage');
    });

    it('Does not attempt to open a report without scan id', function() {
      violationsTableRow.openReport('appId');
      expect(mockWindow.open).not.toHaveBeenCalled();
    });

    it('Opens the right report when supplied with valid params', function() {
      spyOn(mockState, 'href').and.callFake(function(state, params) {
        return state + '.' + params.publicId + '.' + params.scanId;
      });
      violationsTableRow.openReport('appId', 'scanId');
      expect(mockWindow.open).toHaveBeenCalledWith('applicationReport.policy.appId.scanId', '_blank');
    });

    it('Gets the latest report', function() {
      expect(violationsTableRow.risk).toEqual(riskData);
    });
  });
});
