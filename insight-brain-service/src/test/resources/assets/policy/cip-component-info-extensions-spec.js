/* global window */
var CLM = {
  path: '../brain/'
}, applicationId = 'appId';

describe('CIP Component info extensions tests', function() {
  beforeEach(module('ComponentInfo'));

  afterEach(function() {
    $('#infoPanelArtifactTable').remove();
    $('#componentExistingWaiverModal').remove();
  });

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
  }));

  it('Validate button and modal injected', inject(function($httpBackend, $rootScope, $controller, $compile) {
    var node = $("<table id='infoPanelArtifactTable'><tr></tr></table>");
    node.appendTo('body');

    var $scope = $rootScope.$new();
    $controller('ComponentInfoController', {
      $scope: $scope
    });

    // required since angular mocks dumps all bindings between tests
    $scope.rebind();

    $.event.trigger("artifactInfoPanelLoading", {
      gav: {
        hash: '1234'
      },
      scope: $scope
    });

    expect($('button[data-target="#componentExistingWaiverModal"]').length).toEqual(1);
    expect($('#componentExistingWaiverModal').length).toEqual(1);
    
    $compile($('#componentExistingWaiverModal'))($scope);

    $httpBackend.expectGET(CLM.path + 'rest/policyWaiver/application/appId/component/1234').respond([{
      id: "id",
      hash: "1234",
      policyId: "policyId",
      constraintId: null,
      ownerId: "ownerId",
      comment: "some comment",
      createTime: 1375366539817
    }]);
    $httpBackend.expectGET(CLM.path + 'rest/policy/application/appId/applicable').respond({
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

    $('button[data-target="#componentExistingWaiverModal"]').trigger('click');

    $httpBackend.flush();

    $scope.$digest();

    expect($('#componentExistingWaiverModal').length).toEqual(1);
    expect($scope.hash).toEqual('1234');
    expect($scope.applicationId).toEqual('appId');

    var values = $('#componentExistingWaiverModal').find('td');

    expect(values.length).toEqual(4);

    expect($(values[0]).text()).toEqual('policyName');
    expect($(values[1]).text()).toEqual('8/1/2013');
    expect($(values[2]).text()).toEqual('ownerName');
    expect($(values[3]).text()).toEqual('some comment');

    $scope.close();
    //don't wait, junk the thing now
    $('#componentExistingWaiverModal').trigger('hidden.bs.modal');
    
    expect($('#componentExistingWaiverModal').length).toEqual(0);
  }));
});