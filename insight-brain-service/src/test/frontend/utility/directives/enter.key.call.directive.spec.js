describe('enter.key.call.directive.js', function() {
  var element,
      scope;

  beforeEach(module('utility.directives'));

  beforeEach(inject(function($rootScope, $compile) {
    scope = $rootScope.$new();
    element = $compile('<input enter-key-call="doSomething($event)"></div>')(scope);
    scope.$digest();

    scope.doSomething = function() {};
  }));

  it('enter', function() {
    spyOn(scope, 'doSomething').andCallThrough();
    var event = jQuery.Event('keydown');
    event.keyCode = 13;
    element.trigger(event);
    expect(scope.doSomething).toHaveBeenCalled();
  });
});
