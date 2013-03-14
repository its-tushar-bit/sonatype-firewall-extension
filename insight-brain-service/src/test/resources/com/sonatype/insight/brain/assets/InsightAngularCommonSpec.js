describe('InsightAngularCommon', function () {
    var scope, compile;

    beforeEach(module('InsightAngularCommon'));
    beforeEach(inject(function ($rootScope, $controller, $compile) {
        scope = $rootScope.$new();
        compile = $compile;
    }));

    it('replaces implements errorModal template', function () {
        var element = compile('<div error-Modal></div>');
        expect(element.find('div').length).toEqual(3);
        expect(element.attr('class')).toEqual('modal hide');
    });

    it('can be shown', function () {
        var element = compile('<div error-Modal></div>');
        scope.showError('foo');
        expect(element.attr('class')).toEqual('modal hide in');
    });
});