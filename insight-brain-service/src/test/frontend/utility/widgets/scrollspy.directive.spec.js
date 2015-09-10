describe('scrollspy.directive.spec.js', function(){
  var element, spy;
  beforeEach(module('utility'));

  beforeEach(inject(function($rootScope){
    controllerScope = $rootScope.$new();
    element = angular.element('<div><div id="pills"><ul class="nav nav-pills">' +
    '<li><a data-toggle="pill" data-target="#pill1" ng-href="">1</a></li></ul></div>' +
    '<div id="scroller" scrollspy="#pills"><div id="pill1"></div></div>');
  }));

  it('Validate scrollspy is initialized prpoerly', inject(function($compile) {
    spy = spyOn($.fn, 'scrollspy');
    expect(spy).not.toHaveBeenCalled();
    $compile(element)(controllerScope);
    expect(spy).toHaveBeenCalled();
  }));

  it('Validate pill click causes scroll', inject(function($compile, $timeout) {
    //have to add to dom for click events to be processed
    angular.element('body').append(element);
    $compile(element)(controllerScope);
    spy = spyOn($.fn, 'scrollTop');
    expect(spy).not.toHaveBeenCalled();
    element.find('#pills .nav li > a').click();
    $timeout.flush();
    //once to get and once to set
    expect(spy.callCount).toBe(2);

    //remove the element we added to the dom
    element.remove();
  }));
});
