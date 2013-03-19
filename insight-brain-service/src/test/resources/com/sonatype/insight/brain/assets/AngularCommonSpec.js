describe('AngularCommon', function () {
    var scope, compile, httpBackend;

    beforeEach(module('AngularCommon'));
    beforeEach(inject(function ($httpBackend, $rootScope, $compile) {
        scope = $rootScope.$new();
        compile = $compile;
        httpBackend = $httpBackend;
    }));

    it('implements errorModal directive', function () {
        httpBackend.expectGET('../assets/components/errorModal.html').respond("<div id='errorModal'></div>");
        var element = compile('<div error-Modal></div>')(scope);
        expect(element).not.toBeUndefined();
    });
});