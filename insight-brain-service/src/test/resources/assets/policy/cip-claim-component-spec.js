/*global window*/
var InsightDatatable = {
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
    var scope, _http;

    beforeEach(module('ClaimComponent'));
    // setup our http backend to return what we want
    beforeEach(inject(function($rootScope, $controller, $http, $httpBackend, $location) {
        _http = $httpBackend;
        scope = $rootScope.$new();
        //simply so we don't have to worry about comparing urls against ../../../../.././ etc etc
        $location.url('/sonatype-clm-report/');
        $controller('ClaimComponentController', {
            $scope : scope,
            global : {},
            hudson : $http,
            CurrentData : function() {
                return {
                    hash : "1",
                    createTime : 1
                };
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
        
        scope.claimForm = {
            $valid: true
        };

        _http.expectPOST('../brain/rest/component/identified').respond({});
        scope.claimSubmit();
        _http.flush();
    });
});