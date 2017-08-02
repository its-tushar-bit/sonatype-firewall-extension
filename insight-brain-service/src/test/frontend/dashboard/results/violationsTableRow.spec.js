describe('violationsTableRow.spec', function() {

  var scope,
      getVm,
      mockWindow,
      mockState,
      violationsTableRow;

  var riskDataMultipleStages =
  {
    "stageDetails": [
      {
        "stageTypeId": "build",
        "stageTypeName": null,
        "time": 1492397030700,
        "actionTypeId": null,
        "scanId": "164c8128857b4d62a8649350ced3f9a6"
      },
      {
        "stageTypeId": "stage-release",
        "stageTypeName": null,
        "time": 1492397082307,
        "actionTypeId": null,
        "scanId": "5d6a9955588f482a9e48d2d93f2236f0"
      },
      {
        "stageTypeId": "release",
        "stageTypeName": null,
        "time": 1492397030701,
        "actionTypeId": null,
        "scanId": null
      },
      {
        "stageTypeId": "operate",
        "stageTypeName": null,
        "time": 1492397030702,
        "actionTypeId": null,
        "scanId": null
      }
    ]
  };

  var riskDataSingleStage =
  {
    "stageDetails": [
      {
        "stageTypeId": "build",
        "stageTypeName": null,
        "time": null,
        "actionTypeId": null,
        "scanId": "164c8128857b4d62a8649350ced3f9a6"
      },
      {
        "stageTypeId": "stage-release",
        "stageTypeName": null,
        "time": null,
        "actionTypeId": null,
        "scanId": "5d6a9955588f482a9e48d2d93f2236f0"
      },
      {
        "stageTypeId": "release",
        "stageTypeName": null,
        "time": null,
        "actionTypeId": null,
        "scanId": null
      },
      {
        "stageTypeId": "operate",
        "stageTypeName": null,
        "time": 1492397030702,
        "actionTypeId": null,
        "scanId": null
      }
    ]
  };

  beforeEach(module('dashboard.module', 'legacyConfiguration', function($provide) {
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

      getVm = function(risk) {
        scope.risk = risk;

        var element = $compile('<tr violations-table-row risk="risk"></tr>')(scope);
        scope.$digest();

        return element.controller('violationsTableRow');
      }
    }
  ]));

  describe('ViolationsTableRowComponent', function() {
    beforeEach(inject([
      '$httpBackend', function($httpBackend) {
        violationsTableRow = getVm(riskDataMultipleStages);
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
      expect(mockWindow.open).toHaveBeenCalledWith('report.appId.scanId', '_blank');
    });

    it('Gets the latest report', function() {
      expect(violationsTableRow.latestReport).toEqual(riskDataMultipleStages.stageDetails[1])
    });
  });

  describe('ViolationsTableRowComponent with single report', function() {
    it('Gets the report', function() {
      inject([
        '$httpBackend', function($httpBackend) {
          violationsTableRow = getVm(riskDataSingleStage);
          $httpBackend.flush();
        }
      ]);
      expect(violationsTableRow.latestReport).toEqual(riskDataSingleStage.stageDetails[3])
    });
  });

});
