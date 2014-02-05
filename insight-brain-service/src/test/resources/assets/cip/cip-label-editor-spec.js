/*global window*/
describe('ComponentLabelEditor tests', function() {
  var scope, addScope, removeScope, $http;

  angular.module('TestGavProvider', []).service('ComponentLabelEditorGAV', function() {
    return {
      hash: '3102cdd0edd5a05afe00',
      applicationId: 'bom1-12345678'
    };
  });

  beforeEach(module('ComponentLabelEditor', 'TestGavProvider'));

  //setup our http backend to return what we want
  beforeEach(inject(function($rootScope, $controller, $httpBackend) {
    $http = $httpBackend;

    $httpBackend.expectGET(SpecUtil.toRegExp('../brain/rest/label/component/application/bom1-12345678/3102cdd0edd5a05afe00')).
        respond({"labelsByOwner": [
          {"ownerId": "orgOwnerId", "ownerName": "orgName", "ownerType": "organization", "labels": [
            {"id": "one", "ownerId": "orgOwnerId", "label": "one", "labelLowercase": "one", "description": "one", "color": "red"}
          ]}
        ]});
    $httpBackend.expectGET(SpecUtil.toRegExp('../brain/rest/label/application/bom1-12345678/applicable')).
        respond({"labelsByOwner": [
          {"ownerId": "orgOwnerId", "ownerName": "orgName", "ownerType": "organization", "labels": [
            {"id": "one", "ownerId": "orgOwnerId", "label": "one", "labelLowercase": "one", "description": "one", "color": "red"}
          ]},
          {"ownerId": "appOwnerId", "ownerName": "appName", "ownerType": "application", "labels": [
            {"id": "two", "ownerId": "appOwnerId", "label": "two", "labelLowercase": "two", "description": "two", "color": "blue"}
          ]}
        ]});

    scope = $rootScope.$new();
    $controller('LabelsController', {$scope: scope, global: {}});
    addScope = scope.$new();
    $controller('LabelAddController', {$scope: addScope, global: {}});
    removeScope = scope.$new();
    $controller('LabelRemoveController', {$scope: removeScope, global: {}});
    $httpBackend.flush();
  }));

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  it('Test Add Application scoped Label', function() {
    $http.expectPOST(SpecUtil.toRegExp('../brain/rest/label/component/application/bom1-12345678/3102cdd0edd5a05afe00'),
        {"id": "two", "ownerId": "appId", "label": "two", "labelLowercase": "two", "description": "two", "color": "blue", "ownerType": "application", "ownerName": "test"}).respond(
        []);
    $http.expectGET(SpecUtil.toRegExp('../brain/rest/label/component/application/bom1-12345678/3102cdd0edd5a05afe00')).
        respond({"labelsByOwner": [
          {"ownerId": "orgOwnerId", "ownerName": "orgName", "ownerType": "organization", "labels": [
            {"id": "one", "ownerId": "orgOwnerId", "label": "one", "labelLowercase": "one", "description": "one", "color": "red"}
          ]},
          {"ownerId": "appOwnerId", "ownerName": "appName", "ownerType": "application", "labels": [
            {"id": "two", "ownerId": "appOwnerId", "label": "two", "labelLowercase": "two", "description": "two", "color": "blue"}
          ]}
        ]});
    $http.expectGET(SpecUtil.toRegExp('../brain/rest/label/application/bom1-12345678/applicable')).
        respond({"labelsByOwner": [
          {"ownerId": "orgOwnerId", "ownerName": "orgName", "ownerType": "organization", "labels": [
            {"id": "one", "ownerId": "orgOwnerId", "label": "one", "labelLowercase": "one", "description": "one", "color": "red"}
          ]},
          {"ownerId": "appOwnerId", "ownerName": "appName", "ownerType": "application", "labels": [
            {"id": "two", "ownerId": "appOwnerId", "label": "two", "labelLowercase": "two", "description": "two", "color": "blue"}
          ]}
        ]});

    scope.addLabel({"id": "two", "ownerId": "appId", "label": "two", "labelLowercase": "two", "description": "two", "color": "blue", "ownerType": "application", "ownerName": "test"});
    $http.flush();
    expect(scope.itemLabels.length).toEqual(2);
  });

  it('Test Add Organization scoped Label', function() {
    $http.expectPOST(SpecUtil.toRegExp('../brain/rest/label/component/organization/orgOwnerId/3102cdd0edd5a05afe00'),
        {"id": "one", "ownerId": "orgOwnerId", "label": "one", "labelLowercase": "one", "description": "one", "color": "red", "ownerType": "organization", "ownerName": "orgName"}).respond(
        []);

    scope.addLabel({"id": "one", "ownerId": "orgOwnerId", "label": "one", "labelLowercase": "one", "description": "one", "color": "red", "ownerType": "organization", "ownerName": "orgName"});
    addScope.label = {
      selectedOwner: 'orgOwnerId$$organization'
    }
    addScope.accept();
    $http.flush();
  });

  it('Test Remove', function() {
    expect(scope.itemLabels.length).toEqual(1);
    $http.expectDELETE(SpecUtil.toRegExp('../brain/rest/label/component/organization/orgOwnerId/3102cdd0edd5a05afe00/one')).respond([]);
    scope.removeLabel({"id": "one", "ownerId": "orgOwnerId", "label": "one", "labelLowercase": "one", "description": "one", "color": "red", "ownerId": "orgOwnerId", "ownerName": "orgName", "ownerType": "organization"});
    removeScope.accept();
    $http.flush();
  });

  it('Test Filter', function() {
    scope.itemLabels = [
      { "label": "foo", "color": "black"},
      { "label": "asdf"},
      { "label": "bar"}
    ];
    expect(scope.isApplied({ "label": "bbb"})).toEqual(true);
    expect(scope.isApplied({ "label": "foo"})).toEqual(false);
  });

  it('reloads both applied and applicable labels upon refresh', function() {
    $http.expectGET(SpecUtil.toRegExp('../brain/rest/label/component/application/bom1-12345678/3102cdd0edd5a05afe00')).
        respond({"labelsByOwner": []});
    $http.expectGET(SpecUtil.toRegExp('../brain/rest/label/application/bom1-12345678/applicable')).
        respond({"labelsByOwner": []});
    scope.loadLabelData();
    $http.flush();
  });
});