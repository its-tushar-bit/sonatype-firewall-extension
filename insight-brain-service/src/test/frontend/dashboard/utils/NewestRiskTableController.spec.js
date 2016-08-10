describe('NewestRiskTableController.spec', function() {

  var scope;

  beforeEach(module('dashboard.utils'));

  afterEach(inject(function($httpBackend) {
    scope.$destroy();
    $httpBackend.verifyNoOutstandingExpectation(false);
    $httpBackend.verifyNoOutstandingRequest();
  }));

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
        "displayName": {
          parts: [
            {field: "Group", value: "foo"},
            {value: " : "},
            {field: "Artifact", value: "bar"},
            {value: " : "},
            {field: "Version", value: "1.0"}
          ]
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
    $httpBackend.expectGET(CLMLocations.getDashboardStageUrl()).respond(MockData.getDashboardStageData());
    $controller('NewestRiskTableController', {$scope: scope});
    $httpBackend.flush();
  }));

  it('Modifies the loaded stages to remove develop and rename Stage-Release', function() {
    expect(scope.stageTypes.length).toBe(4);
    expect(scope.stageTypes[1].shortName).toBe('Stage');
  });

  it('Enhances the available data to aid sorting by row', function() {
    var risk = scope.data[0];
    expect(risk.stagereleaseTime).toBe(risk.stageDetails[1].time);
    expect(risk.releaseTime).toBe(risk.stageDetails[2].time);
    expect(risk.buildTime).toBe(risk.stageDetails[0].time);
    expect(risk.operateTime).toBeNull();
    expect(risk.gavName).toBe('foo : bar : 1.0');
  });
});
