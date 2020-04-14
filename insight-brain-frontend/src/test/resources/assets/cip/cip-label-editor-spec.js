/*global window*/
window.Insight = window.Insight || {};
describe('cip.label.editor tests', function() {
  var scope;

  beforeEach(module('cip.label.editor', 'TestComponentProvider'));

  //setup our http backend to return what we want
  beforeEach(inject(function($rootScope, $controller, $httpBackend) {
  }));

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
    scope.$destroy();
  }));

  describe('LabelsController', function () {
    beforeEach(inject(function ($rootScope, $httpBackend, $controller) {
      scope = $rootScope.$new();

      $httpBackend.expectGET(SpecUtil.toRegExp('../brain/rest/label/component/application/bom1-12345678/3102cdd0edd5a05afe00')).
          respond({"labelsByOwner": [
            {"ownerId": "orgOwnerId", "ownerName": "orgName", "ownerType": "organization", "labels": [
              {"id": "one", "ownerId": "orgOwnerId", "label": "one", "description": "one", "color": "red"}
            ]}
          ]});
      $httpBackend.expectGET(SpecUtil.toRegExp('../brain/api/v2/labels/application/bom1-12345678/applicable')).
          respond({"labelsByOwner": [
            {"ownerId": "orgOwnerId", "ownerName": "orgName", "ownerType": "organization", "labels": [
              {"id": "one", "ownerId": "orgOwnerId", "label": "one", "description": "one", "color": "red"}
            ]},
            {"ownerId": "appOwnerId", "ownerName": "appName", "ownerType": "application", "labels": [
              {"id": "two", "ownerId": "appOwnerId", "label": "two", "description": "two", "color": "blue"}
            ]}
          ]});
      $controller('LabelsController', {$scope: scope, global: {}});

      $httpBackend.flush();
    }));

    it('reloads both applied and applicable labels upon refresh', inject(function($httpBackend) {
      $httpBackend.expectGET(SpecUtil.toRegExp('../brain/rest/label/component/application/bom1-12345678/3102cdd0edd5a05afe00')).
          respond({"labelsByOwner": []});
      $httpBackend.expectGET(SpecUtil.toRegExp('../brain/api/v2/labels/application/bom1-12345678/applicable')).
          respond({"labelsByOwner": []});
      scope.doLoad();
      $httpBackend.flush();
      
    }));

    it('Test Filter', function() {
      scope.itemLabels = [
        { "label": "foo", "color": "black"},
        { "label": "asdf"},
        { "label": "bar"}
      ];
      expect(scope.isApplied({ "label": "bbb"})).toEqual(true);
      expect(scope.isApplied({ "label": "foo"})).toEqual(false);
    });

    it('Test Add Application scoped Label', inject(function($httpBackend) {
      $httpBackend.expectPOST(SpecUtil.toRegExp('../brain/rest/label/component/application/bom1-12345678/3102cdd0edd5a05afe00'),
              {"id": "two", "ownerId": "appId", "label": "two", "description": "two", "color": "blue"}).respond(
                      []);
      $httpBackend.expectGET(SpecUtil.toRegExp('../brain/rest/label/component/application/bom1-12345678/3102cdd0edd5a05afe00')).
          respond({"labelsByOwner": [
            {"ownerId": "orgOwnerId", "ownerName": "orgName", "ownerType": "organization", "labels": [
              {"id": "one", "ownerId": "orgOwnerId", "label": "one", "description": "one", "color": "red"}
            ]},
            {"ownerId": "appOwnerId", "ownerName": "appName", "ownerType": "application", "labels": [
              {"id": "two", "ownerId": "appOwnerId", "label": "two", "description": "two", "color": "blue"}
            ]}
          ]});
      $httpBackend.expectGET(SpecUtil.toRegExp('../brain/api/v2/labels/application/bom1-12345678/applicable')).
          respond({"labelsByOwner": [
            {"ownerId": "orgOwnerId", "ownerName": "orgName", "ownerType": "organization", "labels": [
              {"id": "one", "ownerId": "orgOwnerId", "label": "one", "description": "one", "color": "red"}
            ]},
            {"ownerId": "appOwnerId", "ownerName": "appName", "ownerType": "application", "labels": [
              {"id": "two", "ownerId": "appOwnerId", "label": "two", "description": "two", "color": "blue"}
            ]}
          ]});

      scope.addLabel({"id": "two", "ownerId": "appId", "label": "two", "description": "two", "color": "blue", "ownerType": "application", "ownerName": "test"});
      $httpBackend.flush();
      expect(scope.itemLabels.length).toEqual(2);
    }));
  });
  
  describe('LabelAddController', function () {
    beforeEach(inject(function ($rootScope, $controller) {
      scope = $rootScope.$new();
      scope.$close = angular.noop;
      spyOn(scope, '$close');

      $controller('LabelAddController', {
        $scope: scope,
        global: {},
        label: {
          "id": "one",
          "ownerId": "orgOwnerId",
          "label": "one",
          "description": "one",
          "color": "red",
          "ownerType": "organization",
          "ownerName": "orgName"
        }
      });
    }));

    it('Test Add Organization scoped Label', inject(function($httpBackend) {
      expect(scope.labelLoading).toBeTruthy();
      $httpBackend.expectGET(SpecUtil.toRegExp('api/v2/labels/application/bom1-12345678/applicable/context/one')).respond({
        id: 'orgOwnerId',
        name: 'orgName',
        type: 'organization',
        children: [{
          id: 'appOwnerId',
          name: 'appName',
          type: 'application',
          children: null
        }]
      });
      $httpBackend.flush();
      expect(scope.labelLoading).toBeFalsy();
      expect(scope.labelOwners[0].type).toEqual('application');
      expect(scope.labelOwners[1].type).toEqual('organization');

      scope.label = {
        selectedOwner: 'orgOwnerId$$organization'
      };

      $httpBackend.expectPOST(
              SpecUtil.toRegExp('../brain/rest/label/component/organization/orgOwnerId/3102cdd0edd5a05afe00'), {
                "id": "one",
                "ownerId": "orgOwnerId",
                "label": "one",
                "description": "one",
                "color": "red"
              }).respond([]);

      scope.accept();
      expect(scope.$close).not.toHaveBeenCalled();
      $httpBackend.flush();
      expect(scope.$close).toHaveBeenCalled();
    }));
  });

  describe('LabelRemoveController', function () {
    beforeEach(inject(function ($rootScope, $controller) {
      scope = $rootScope.$new();
      scope.$close = angular.noop;
      spyOn(scope, '$close');

      $controller('LabelRemoveController', {
        $scope: scope,
        global: {},
        label: {
          "id": "one",
          "ownerId": "orgOwnerId",
          "label": "one",
          "description": "one",
          "color": "red",
          "ownerName": "orgName",
          "ownerType": "organization"
        }
      });
    }));

    it('Test Remove', inject(function($httpBackend) {
      $httpBackend.expectDELETE(SpecUtil.toRegExp('../brain/rest/label/component/organization/orgOwnerId/3102cdd0edd5a05afe00/one')).respond([]);
      scope.accept();
      expect(scope.$close).not.toHaveBeenCalled();
      $httpBackend.flush();
      expect(scope.$close).toHaveBeenCalled();
    }));
  });
});
