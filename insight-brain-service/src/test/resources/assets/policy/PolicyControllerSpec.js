var clmBuildTimestamp = '';

describe('PolicyController tests', function() {
    function toRegExp(getUrl) {
        return new RegExp(getUrl + '\\?timestamp=[0-9]+');
    }
    var scope;

    beforeEach(module('Policy'));
	beforeEach(module(function($provide) {
		$provide.value('ApplicationId', {
				encoded : function () {
					return 'bom1-12345678';
				}
			}
		);
	}));
	beforeEach(module(function($provide) {
		$provide.value('Hudson', ['$http', function($http) {
				return $http;
			}]
		);
	}));
    // setup our http backend to return what we want
    beforeEach(inject(function($httpBackend, $rootScope, $controller, CLMLocations, CLMAppLocations, $state) {
		$state.current.name = "management.application";

        $httpBackend.expectGET(CLMLocations.getActionTypeUrl()).respond(PolicyMockData.getActionTypeData());
        $httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(MockData.getActionStageData());
        $httpBackend.expectGET(toRegExp(CLMAppLocations.getEntitiesUrl())).respond(ApplicationMockData.getApplicationsData()[0]);
        $httpBackend.expectGET(toRegExp(CLMAppLocations.getPolicyUrl())).respond(PolicyMockData.getPolicyData());

        // inject the controller
        scope = $rootScope.$new();

        $controller('PolicyController', {
            $scope : scope,
            global : {}
        });
        $httpBackend.flush();
    }));

    it('Test initial data state', function() {
        expect(scope.state.actionStageList.length).toEqual(MockData.getActionStageData().length);
        expect(scope.state.policyList.length).toEqual(PolicyMockData.getPolicyData().length);
    });

    it('Test Summary', function() {
        expect(scope.getActionCount(scope.state.policyList[0])).toEqual(1);
        expect(scope.getActions(scope.state.policyList[0])).toEqual('Build: Fail');

        expect(scope.getActionCount(scope.state.policyList[1])).toEqual(1);
        expect(scope.getActions(scope.state.policyList[1])).toEqual('Build: Fail');

        expect(scope.getActionCount(scope.state.policyList[2])).toEqual(0);
        expect(scope.getActions(scope.state.policyList[2])).toEqual('');

        expect(scope.getActionCount(scope.state.policyList[3])).toEqual(0);
        expect(scope.getActions(scope.state.policyList[3])).toEqual('');

        expect(scope.getActionCount(scope.state.policyList[4])).toEqual(0);
        expect(scope.getActions(scope.state.policyList[4])).toEqual('');
    });

    it('Test remove policy', inject(function(CLMAppLocations, $httpBackend) {
        expect(scope.state.policyList.length).toEqual(5);
        scope.viewRemovePolicy(scope.state.policyList[0]);

        expect(scope.state.policyList[0].id).toEqual('053e89a476b34d7dac5d97665d2d241e');
        expect(scope.state.confirm.header).toEqual('Delete Policy?');
        expect(scope.state.confirm.body).toEqual('Are you sure you want to delete the Policy named \'asdffffrfff\'?  This action is not reversible.');
        expect(scope.state.confirm.declineText).toEqual('Cancel');
        expect(scope.state.confirm.acceptText).toEqual('Delete');
        expect(scope.state.confirm.acceptFn).not.toBeNull();
        expect(scope.state.confirm.declineFn).not.toBeNull();

        scope.state.confirm.declineFn(); // Cancel delete dialog
        expect(scope.state.policyList.length).toEqual(5);

        scope.viewRemovePolicy(scope.state.policyList[0]);

        expect(scope.state.policyList[0].id).toEqual('053e89a476b34d7dac5d97665d2d241e');
        expect(scope.state.confirm.header).toEqual('Delete Policy?');
        expect(scope.state.confirm.body).toEqual('Are you sure you want to delete the Policy named \'asdffffrfff\'?  This action is not reversible.');
        expect(scope.state.confirm.declineText).toEqual('Cancel');
        expect(scope.state.confirm.acceptText).toEqual('Delete');
        expect(scope.state.confirm.accept).not.toBeNull();
        expect(scope.state.confirm.declineFn).not.toBeNull();

        $httpBackend.expectDELETE(CLMAppLocations.getPolicyUrl() + '/' + scope.state.policyList[0].id).respond(200);

        scope.state.confirm.acceptFn();

        $httpBackend.flush();

        expect(scope.state.policyList.length).toEqual(4);
        expect(scope.state.policyList[0].id).toEqual('ec21b3ee9f31447c9e40913d91776593');
    }));
    
	
	it('reevaluates policy', inject(function($httpBackend, CLMLocations) {
		var policyResponse = PolicyMockData.getPolicyEvaluationData();
		var mockApplication = {
				publicId: 'publicId',
				policyEvaluations: {
					build: {
						scanId: 'scanId',
						stage: {
	                		stageTypeId: 'build'
	                	}
					}
				},
				policyEvaluationsResults: {
					build: {}
				}
		};
		
		$httpBackend.expectPOST(CLMLocations.evaluatePolicyUrl(mockApplication.publicId, mockApplication.policyEvaluations.build.scanId)).respond(policyResponse);
		
		scope.reEvaluatePolicy(mockApplication, mockApplication.policyEvaluations.build);
		
		$httpBackend.flush();
		
		expect(mockApplication.policyEvaluationsResults.build.affectedComponentCount).toEqual(policyResponse.affectedComponentCount);
		expect(mockApplication.policyEvaluationsResults.build.criticalComponentCount).toEqual(policyResponse.criticalComponentCount);
		expect(mockApplication.policyEvaluationsResults.build.severeComponentCount).toEqual(policyResponse.severeComponentCount);
		expect(mockApplication.policyEvaluationsResults.build.moderateComponentCount).toEqual(policyResponse.moderateComponentCount);
	}));
});
