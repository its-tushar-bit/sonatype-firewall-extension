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
    var scope, $http;

    beforeEach(module('PolicyViolations'));
    // setup our http backend to return what we want
    beforeEach(inject(function($rootScope, $controller, $httpBackend,$location) {
        $http = $httpBackend;
        scope = $rootScope.$new();
        //simply so we don't have to worry about comparing urls against ../../../../.././ etc etc
        $location.url('/sonatype-clm-report/');
        $http.whenGET(SpecUtil.toRegExp('policyalerts.json')).respond({
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
        $http.whenGET('../brain/rest/policy/actionType').respond({});
        $controller('PolicyViolationsController', {
            $scope : scope,
            global : {},
            PolicyViolationData : {
                hash : "1",
                appId : "appId"
            }
        });
        $http.flush();
    }));

    afterEach(inject(function($httpBackend) {
        $httpBackend.verifyNoOutstandingExpectation();
    }));

    it('Test waive policy at org level', function() {
        $http.whenGET('../brain/rest/policyWaiver/application/appId/applicable/context/policyId').respond({
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

        $http.flush();

        scope.waiver.selectedTarget = 'orgId$$organization';
        scope.waiverComment = 'this is my comment!';

        $http.whenPOST('../brain/rest/policyWaiver/organization/orgId', {
            hash : "1",
            policyId : "policyId",
            comment : "this is my comment!"
        }).respond({});

        scope.acceptWaiveComponent();

        $http.flush();
    });

    it('Test waive policy at app level', function() {
        $http.whenGET('../brain/rest/policyWaiver/application/appId/applicable/context/policyId').respond({
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

        $http.flush();

        scope.waiver.selectedTarget = 'appId$$application';
        scope.waiverComment = 'this is my comment!';

        $http.whenPOST('../brain/rest/policyWaiver/application/appId', {
            hash : "1",
            policyId : "policyId",
            comment : "this is my comment!"
        }).respond({});

        scope.acceptWaiveComponent();

        $http.flush();
    });
});