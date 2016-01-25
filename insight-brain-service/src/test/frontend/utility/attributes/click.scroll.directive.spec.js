describe('click.scroll.directive.spec.js', function() {
  var element;

  beforeEach(module('utility'));

  beforeEach(function() {
    module(function($provide) {
      $provide.value('$uiViewScroll', jasmine.createSpy());
    });
  });

  beforeEach(inject(function($compile, $rootScope) {
    scope = $rootScope.$new();

    element = $compile('<button click-scroll></button>')(scope);
  }));

  it('Directive calls $uiViewScroll on element click', inject(function($uiViewScroll) {
    element.trigger('click');

    expect($uiViewScroll).toHaveBeenCalled();
    expect($uiViewScroll.calls[0].args[0][0]).toEqual(element[0]);
  }));
});
