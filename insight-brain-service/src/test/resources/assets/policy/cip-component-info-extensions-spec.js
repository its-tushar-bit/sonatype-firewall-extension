/* global window */
var applicationId = 'appId', CLM = {
  path: '../brain/'
};
describe('CIP Component info extensions tests', function() {
  var $scope;
  
  angular.module('Hudson', []).factory('hudson', ['$http', function($http) {
    return $http;
  }]);
  
  beforeEach(module('ComponentInfo'));

  beforeEach(inject(function($rootScope, $compile, $controller, $httpBackend) {
    $scope = $rootScope.$new();
    $controller('ComponentInfoController', {
      $scope: $scope
    });
    var node = $("<table id='infoPanelArtifactTable'><tr></tr></table>");
    node.appendTo('body');
    $compile(node)($scope);
  }));

  afterEach(function() {
    $('#infoPanelArtifactTable').remove();
    $('#componentExistingWaiverModal').remove();
  });

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  //TODO: commented out because weird stuff is happening, the http requests aren't getting shoveled
  //through the mock httpBackend...
  xit('Validate button and modal injected', inject(function($httpBackend) {
    $httpBackend.expectGET('../brain/rest/policyWaiver/application/appId/component/1234').respond([{
      id: "id",
      hash: "1234",
      policyId: "policyId",
      constraintId: null,
      ownerId: "ownerId",
      comment: "some comment",
      createTime: 1375366539817
    }]);
    $httpBackend.expectGET('../brain/rest/policy/application/appId/applicable').respond({
      "policiesByOwner": [{
        "ownerId": "ownerId",
        "ownerName": "ownerName",
        "ownerType": "application",
        "policies": [{
          "id": "policyId",
          "name": "policyName",
          "ownerId": "ownerId",
          "enabled": true,
          "threatLevel": 5,
          "constraints": [{
            "id": "constraintId",
            "name": "constraintName",
            "enabled": true,
            "operator": "AND",
            "conditions": [{
              "conditionTypeId": "AgeInDays",
              "operator": "older than",
              "value": "1825"
            }]
          }],
          "actions": {}
        }]
      }]
    });

    $.event.trigger("artifactInfoPanelLoading", {
      gav: {
        hash: '1234'
      }
    });

    expect($('button[data-target="#componentExistingWaiverModal"]').length).toEqual(1);

    expect($('#componentExistingWaiverModal').length).toEqual(0);

    $('button[data-target="#componentExistingWaiverModal"]').trigger('click');

    $httpBackend.flush();

    expect($('#componentExistingWaiverModal').length).toEqual(1);
    expect($scope.hash).toEqual('1234');
    expect($scope.applicationId).toEqual('appId');

    var values = $('#componentExistingWaiverModal').find('td');

    expect(values.length).toEqual(4);

    expect(values[0].val()).toEqual('policyId');
    expect(values[1].val()).toEqual('8/1/2013');
    expect(values[2].val()).toEqual('test');
    expect(values[3].val()).toEqual('asdfdsa');
  }));
});