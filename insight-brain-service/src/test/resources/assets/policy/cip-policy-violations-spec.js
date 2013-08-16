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
    var scope, _http;

    beforeEach(module('PolicyViolations'));
    // setup our http backend to return what we want
    beforeEach(inject(function($rootScope, $controller, $http, $httpBackend,$location) {
        _http = $httpBackend;
        scope = $rootScope.$new();
        //simply so we don't have to worry about comparing urls against ../../../../.././ etc etc
        $location.url('/sonatype-clm-report/');
        _http.whenGET(SpecUtil.toRegExp('policyalerts.json')).respond({
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
        _http.whenGET('../brain/rest/policy/actionType').respond({});
        $controller('PolicyViolationsController', {
            $scope : scope,
            global : {},
            hudson : $http,
            PolicyViolationData : {
                hash : "1",
                appId : "appId"
            }
        });
        _http.flush();
    }));

    afterEach(inject(function($httpBackend) {
        $httpBackend.verifyNoOutstandingExpectation();
    }));

    it('Test waive policy at org level', function() {
      _http.whenGET('../brain/rest/policyWaiver/application/appId/applicable/context/policyId').respond({
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

        scope.waiveComponent({
            id : 'policyId'
        });

        _http.flush();

        scope.waiver.selectedTarget = 'orgId$$organization';
        scope.waiverComment = 'this is my comment!';

        _http.whenPOST('../brain/rest/policyWaiver/organization/orgId', {
            hash : "1",
            policyId : "policyId",
            comment : "this is my comment!"
        }).respond({});

        scope.acceptWaiveComponent();

        _http.flush();
    });

    it('Test waive policy at app level', function() {
      _http.whenGET('../brain/rest/policyWaiver/application/appId/applicable/context/policyId').respond({
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

        scope.waiveComponent({
            id : 'policyId'
        });

        _http.flush();

        scope.waiver.selectedTarget = 'appId$$application';
        scope.waiverComment = 'this is my comment!';

        _http.whenPOST('../brain/rest/policyWaiver/application/appId', {
            hash : "1",
            policyId : "policyId",
            comment : "this is my comment!"
        }).respond({});

        scope.acceptWaiveComponent();

        _http.flush();
    });
});