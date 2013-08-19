/*global window*/
var InsightDatatable = {
    getActiveTable : function() {
        return {
            dataView : {
                getItems : function() {
                    return [];
                }
            }
        };
    }
};

describe('CIP Policy Waiver tests', function() {
    var _scope, _addScope, _viewScope;

    beforeEach(module('PolicyViolations', function($provide) {
      $provide.factory('hudson', ['$http', function($http){
        return $http;
      }]);
    }));
    // setup our http backend to return what we want
    beforeEach(inject(function($rootScope, $controller, $httpBackend) {
        _scope = $rootScope.$new();
        $httpBackend.expectGET(SpecUtil.toRegExp('policyalerts.json')).respond({
            aaData : [ {
                trigger : {
                    policyId : "policyId",
                    policyName : "name",
                    threatLevel : 5,
                    componentFacts : [ {
                        groupId : "bsh",
                        artifactId : "bsh",
                        version : "1.3.0",
                        hash : "1fed35193d56470f46c0",
                        constraintFacts : [ {
                            constraintId : "c7ad07e00c4948c59651cce82163e50a",
                            constraintName : "test3",
                            operatorName : "AND",
                            conditionFacts : [ {
                                conditionTypeId : "AgeInDays",
                                summary : "Age older than 1825",
                                reason : "Age was 7 years, 8 months and 17 days"
                            }, {
                                conditionTypeId : "AgeInDays",
                                summary : "Age older than 730",
                                reason : "Age was 7 years, 8 months and 17 days"
                            } ]
                        } ]
                    } ]
                },
                actions : []
            } ]
        });
        $httpBackend.expectGET('../brain/rest/policy/actionType').respond({});
        $controller('PolicyViolationsController', {
            $scope : _scope,
            global : {},
            PolicyViolationData : {
                hash : "1",
                appId : "appId"
            }
        });
        _addScope = $rootScope.$new();
        $controller('AddWaiverController', {
          $scope : _addScope,
          PolicyViolationData : {
            hash : "1",
            appId : "appId"
          }
        });
        _viewScope = $rootScope.$new();
        $controller('ViewWaiverController', {
          $scope: _viewScope,
          global : {},
          PolicyViolationData : {
              hash : "1",
              appId : "appId"
          }
        });
        $httpBackend.flush();
    }));

    afterEach(inject(function($httpBackend) {
        $httpBackend.verifyNoOutstandingExpectation();
    }));
    
    afterEach(function(){
      _scope.$destroy();
      _addScope.$destroy();
      _viewScope.$destroy();
    });

    it('Test waive policy at org level', inject(function($httpBackend) {
      $httpBackend.expectGET('../brain/rest/policyWaiver/application/appId/applicable/context/policyId').respond({
            id : 'orgId',
            name : 'org',
            type : 'organization',
            children : [{
                id : 'appId',
                name : 'app',
                type : 'application',
                children : null
          }]
        });

        _scope.waiveComponent({
          id: 'policyId'
        });
        _addScope.setupModal();
        $httpBackend.flush();

        _addScope.waiver.selectedTarget = 'orgId$$organization';
        _addScope.waiverComment = 'this is my comment!';

        $httpBackend.expectPOST('../brain/rest/policyWaiver/organization/orgId', {
            hash : "1",
            policyId : "policyId",
            comment : "this is my comment!"
        }).respond({});

        _addScope.acceptWaiveComponent();

        $httpBackend.flush();
    }));

    it('Test waive policy at app level', inject(function($httpBackend) {
      $httpBackend.expectGET('../brain/rest/policyWaiver/application/appId/applicable/context/policyId').respond({
            id : 'orgId',
            name : 'org',
            type : 'organization',
            children : [{
                id : 'appId',
                name : 'app',
                type : 'application',
                children : null
          }]
        });

        _scope.waiveComponent({
          id: 'policyId'
        });
        _addScope.setupModal();
        $httpBackend.flush();

        _addScope.waiver.selectedTarget = 'appId$$application';
        _addScope.waiverComment = 'this is my comment!';

        $httpBackend.expectPOST('../brain/rest/policyWaiver/application/appId', {
            hash : "1",
            policyId : "policyId",
            comment : "this is my comment!"
        }).respond({});

        _addScope.acceptWaiveComponent();

        $httpBackend.flush();
    }));
    
    it('Validate data in scope', inject(function($httpBackend) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/policyWaiver/application/appId/component/1')).respond({
        waiversByOwner: [{
          ownerId: "appId",
          ownerName: "ownerName",
          ownerType: "application",
          waivers: [{
            id: "id",
            hash: "1234",
            policyId: "policyId",
            policyName: "policyName",
            constraintId: null,
            ownerId: "appId",
            comment: "some comment",
            createTime: 1375366539817
          }]
        }]
      });
      _viewScope.setupModal();
      $httpBackend.flush();
      
      expect(_viewScope.waivers.length).toEqual(1);
      expect(_viewScope.waivers[0].id).toEqual("id");
      expect(_viewScope.waivers[0].hash).toEqual("1234");
      expect(_viewScope.waivers[0].policyId).toEqual("policyId");
      expect(_viewScope.waivers[0].policyName).toEqual("policyName");
      expect(_viewScope.waivers[0].constraintId).toEqual(null);
      expect(_viewScope.waivers[0].ownerId).toEqual("appId");
      expect(_viewScope.waivers[0].comment).toEqual("some comment");
      expect(_viewScope.waivers[0].createTime).toEqual(1375366539817);
    }));

    it('Validate delete waiver', inject(function($httpBackend) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/policyWaiver/application/appId/component/1')).respond({
        waiversByOwner: [{
          ownerId: "appId",
          ownerName: "ownerName",
          ownerType: "application",
          waivers: [{
            id: "id",
            hash: "1234",
            policyId: "policyId",
            policyName: "policyName",
            constraintId: null,
            ownerId: "appId",
            comment: "some comment",
            createTime: 1375366539817
          }]
        }]
      });
      _viewScope.setupModal();
      $httpBackend.flush();
      
      $httpBackend.expectDELETE(CLM.path + 'rest/policyWaiver/application/appId/id').respond(200);
      _viewScope.remove(_viewScope.waivers[0]);
      expect(_viewScope.confirmDelete).toEqual(_viewScope.waivers[0]);

      _viewScope.removeWaiver();
      $httpBackend.flush();
      expect(_viewScope.confirmDelete).toEqual(null);
      expect(_viewScope.waivers.length).toEqual(0);
    }));
});