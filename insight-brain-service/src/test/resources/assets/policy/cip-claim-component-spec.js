/*global window*/
var CLM = {
        path : '../brain/'
    },
    InsightDatatable = {
        getActiveTable : function () {
            return {
                dataView : {
                    getItems : function () {
                        return [];
                    }
                }
            };
        }
    };

describe('CIP Claim Component tests', function() {
    var scope, $http;

    beforeEach(module('ClaimComponent'));
    // setup our http backend to return what we want
    beforeEach(inject(function($rootScope, $controller, $httpBackend) {
        $http = $httpBackend;
        scope = $rootScope.$new();
        $controller('ClaimComponentController', {
            $scope : scope,
            global : {},
            CurrentHash : function() {
                return 1;
            }
        });
    }));

    afterEach(inject(function($httpBackend) {
        $httpBackend.verifyNoOutstandingExpectation();
    }));

    it('Test Claim Component', function() {
        expect(scope.formValid()).toEqual(false);
        scope.claimData.groupId = 'groupid';
        expect(scope.formValid()).toEqual(false);
        scope.claimData.artifactId = 'artifactid';
        expect(scope.formValid()).toEqual(false);
        scope.claimData.version = 'version';
        expect(scope.formValid()).toEqual(true);

        $http.expectPOST('../brain/rest/component/identified').respond({});
        scope.claimClick();
        $http.flush();
    });
});