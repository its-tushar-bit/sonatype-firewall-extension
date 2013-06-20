describe('ProprietaryConfiguration', function () {
    'use strict';
    
    var scope,
        controller;

    beforeEach(module('ProprietaryConfiguration', 'CLMLocation'));
    afterEach(function () {
        scope.$destroy();
        scope = null;
    });

    describe('Load', function () {
        it('Success', inject(function ($controller, $rootScope, $httpBackend, CLMLocations) {
            scope = $rootScope.$new();
            $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond({ packages : ['foo']});
            controller = $controller('ProprietaryConfigurationController', { $scope : scope });
            $httpBackend.flush();

            expect(scope.packages).toEqual(['foo']);
            expect(scope.loadError).toBeUndefined();
        }));

        it('Error', inject(function ($controller, $rootScope, $httpBackend, CLMLocations) {
            scope = $rootScope.$new();
            $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond(500, 'A Random Error');
            controller = $controller('ProprietaryConfigurationController', { $scope : scope });
            $httpBackend.flush();
            expect(scope.packages).toBeUndefined();
            expect(scope.loadError).toEqual('Error: 500 A Random Error');
        }));
    });

    it('Reset', inject(function ($controller, $rootScope, $httpBackend, CLMLocations) {
        scope = $rootScope.$new();

        $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond({ packages : ['foo']});
        controller = $controller('ProprietaryConfigurationController', { $scope : scope });
        $httpBackend.flush();

        expect(scope.packages).toEqual(['foo']);
        scope.packages.push('bar');
        expect(scope.proprietary.packages).toEqual(['foo']);

        scope.reset();
        expect(scope.packages).toEqual(['foo']);
    }));

    describe('Save', function () {
        it('Success', inject(function ($controller, $rootScope, $httpBackend, CLMLocations) {
            scope = $rootScope.$new();
            $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond({ packages : ['foo']});
            controller = $controller('ProprietaryConfigurationController', { $scope : scope });
            $httpBackend.flush();

            scope.packages.push('bar');

            $httpBackend.expectPUT(CLMLocations.getProprietaryConfig()).respond(204);
            scope.save();
            expect(scope.saving).toEqual(true);
            $httpBackend.flush();

            expect(scope.saving).toEqual(false);
            expect(scope.packages).toEqual(['foo', 'bar']);
            expect(scope.proprietary.packages).toEqual(['foo', 'bar']);
        }));

        it('Error', inject(function ($controller, $rootScope, $httpBackend, CLMLocations) {
            scope = $rootScope.$new();
            $httpBackend.expectGET(CLMLocations.getProprietaryConfig()).respond({ packages : ['foo']});
            controller = $controller('ProprietaryConfigurationController', { $scope : scope });
            $httpBackend.flush();

            scope.packages.push('bar');

            $httpBackend.expectPUT(CLMLocations.getProprietaryConfig()).respond(500, 'A Random Error');
            scope.save();
            $httpBackend.flush();
            
            expect(scope.proprietary.packages).toEqual(['foo']);
            expect(scope.packages).toEqual(['foo', 'bar']);
            expect(scope.error).toEqual('Error: 500 A Random Error');
        }));
    });
});