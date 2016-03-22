describe('scrollspy.directive.spec.js', function(){
  var spy;
  beforeEach(module('utility'));

  function getFullElement() {
    var el = angular.element('<div id="toRemove"><div id="pills"><ul class="nav nav-pills">' +
    '<li><a data-toggle="pill" data-target="#pill1" ng-href="">1</a></li></ul></div>' +
    '<div id="scroller" scrollspy="#pills"><div id="pill1"></div></div>');
    angular.element('body').append(el);
    return el;
  }

  function getPartialElement() {
    var el = angular.element('<div id="toRemove"><div id="scroller" scrollspy="#pills"><div id="pill1"></div></div>');
    angular.element('body').append(el);
    return el;
  }

  beforeEach(inject(function($rootScope){
    controllerScope = $rootScope.$new();
  }));

  afterEach(function(){
    controllerScope.$destroy();
    angular.element('#toRemove').remove();
  });

  it('Validate scrollspy is initialized prpoerly', inject(function($compile) {
    spy = spyOn($.fn.scrollspy, 'Constructor');
    expect(spy).not.toHaveBeenCalled();
    $compile(getFullElement())(controllerScope);
    expect(spy).toHaveBeenCalled();
  }));

  it('Validate pill click causes scroll', inject(function($compile, $timeout) {
    var element = getFullElement();
    $compile(element)(controllerScope);
    spy = spyOn($.fn, 'scrollTop');
    expect(spy).not.toHaveBeenCalled();
    element.find('#pills .nav li > a').click();
    $timeout.flush();
    //once to get and once to set
    expect(spy.callCount).toBe(2);
  }));

  it('Validate scrollspy applied when dom inserted after initialization', inject(function($compile, $timeout){
    spy = spyOn($.fn.scrollspy, 'Constructor');
    var element = getPartialElement();
    expect(spy).not.toHaveBeenCalled();
    $compile(element)(controllerScope);
    expect(spy).not.toHaveBeenCalled();
    angular.element(element.children()[0]).prepend(angular.element('<div id="pills"><ul class="nav nav-pills"><li>' +
    '<a data-toggle="pill" data-target="#pill1" ng-href="">1</a></li></ul></div>'));
    $timeout.flush();
    expect(spy).toHaveBeenCalled();
  }));

  it('Validate scrollspy applied when http queue empties, after initialization', inject(function($compile, $timeout, $http){
    spy = spyOn($.fn.scrollspy, 'Constructor');
    //just need content in array, all we are checking in src is the array length
    $http.pendingRequests.push('something');
    var element = getFullElement();
    expect(spy).not.toHaveBeenCalled();
    $compile(element)(controllerScope);
    expect(spy).not.toHaveBeenCalled();
    $timeout.flush();
    expect(spy).not.toHaveBeenCalled();
    $http.pendingRequests.splice(0,1);
    $timeout.flush();
    expect(spy).toHaveBeenCalled();
  }));
});
