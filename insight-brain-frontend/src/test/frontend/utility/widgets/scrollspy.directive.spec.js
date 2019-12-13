/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityModule from '../../../../main/frontend/utility/utility.module';
import utilityServicesModule from '../../../../main/frontend/utility/services/utility.services.module';

describe('scrollspy.directive.spec.js', function() {
  var spy, controllerScope;

  beforeEach(angular.mock.module(utilityModule.name, utilityServicesModule.name, function($provide) {
    $provide.service('stable.body.service', function() {
      return { whenStable: function(f) { f(); }};
    });
  }));

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

  beforeEach(inject(function($rootScope) {
    controllerScope = $rootScope.$new();
  }));

  afterEach(function() {
    controllerScope.$destroy();
    angular.element('#toRemove').remove();
  });

  it('Validate scrollspy is initialized properly', inject(function($compile, $timeout) {
    spy = spyOn($.fn.scrollspy, 'Constructor');
    expect(spy).not.toHaveBeenCalled();
    $compile(getFullElement())(controllerScope);
    $timeout.flush();
    expect(spy).toHaveBeenCalled();
  }));

  it('Validate pill click causes scroll', inject(function($compile, $timeout) {
    var element = getFullElement();
    $compile(element)(controllerScope);
    $timeout.flush();
    var spy = spyOn($.fn, 'animate');
    expect(spy).not.toHaveBeenCalled();
    element.find('#pills .nav li > a').click();
    $timeout.flush();

    expect(spy.calls.count()).toBe(1);
    expect(spy.calls.count()).toBe(1);
  }));

  it('Validate scrollspy applied when dom inserted after initialization', inject(function($compile, $timeout) {
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

  it('Validate events are handled as expected',
      inject(['$compile', '$rootScope', 'event.name.constant', '$timeout',
        function($compile, $rootScope, EventNameConstant, $timeout) {
          var scrollspyObj = {
            refresh: jasmine.createSpy()
          };

          spyOn($.fn.scrollspy, 'Constructor').and.returnValue(scrollspyObj);
          spyOn($.fn, 'animate');

          $compile(getFullElement())(controllerScope);
          $timeout.flush();

          expect(scrollspyObj.refresh).not.toHaveBeenCalled();
          $rootScope.$broadcast(EventNameConstant.UPDATE_SCROLLSPY);
          expect(scrollspyObj.refresh).toHaveBeenCalled();
          expect($.fn.animate).not.toHaveBeenCalled();
          scrollspyObj.refresh.calls.reset();
          $.fn.animate.calls.reset();
          expect($.fn.animate).not.toHaveBeenCalled();
          $rootScope.$broadcast(EventNameConstant.UPDATE_SCROLLSPY, {resetScroll: true});
          expect(scrollspyObj.refresh).not.toHaveBeenCalled();
          expect($.fn.animate).toHaveBeenCalled();
        }
      ])
  );
});
