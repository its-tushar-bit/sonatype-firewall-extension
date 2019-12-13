/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityDirectivesModule from '../../../../main/frontend/utility/directives/utility.directives.module';

describe('middle.click.directive.js', function() {
  var compile,
      element,
      event,
      scope;

  beforeEach(angular.mock.module(utilityDirectivesModule.name));

  beforeEach(inject(function($rootScope, $compile) {
    scope = $rootScope.$new();
    compile = $compile;

    scope.doSomething = function() {};
    spyOn(scope, 'doSomething').and.callThrough();
  }));

  it('does not call ng-click action without middle-click', function() {
    element = compile('<a href="test" ng-click="doSomething($event)"></a>')(scope);
    scope.$digest();
    triggerMiddleClick(element);
    expect(scope.doSomething).not.toHaveBeenCalled();
  });

  it('calls ng-click action with middle-click', function() {
    element = compile('<a href="test" ng-click="doSomething($event)" middle-click></a>')(scope);
    scope.$digest();
    triggerMiddleClick(element);
    expect(scope.doSomething).toHaveBeenCalledWith(event);
  });

  function triggerMiddleClick(element) {
    // the event type is determined by the browser... not all browsers support auxclick
    var eventType = 'onauxclick' in document.documentElement ? 'auxclick' : 'mousedown';
    event = jQuery.Event(eventType);
    event.which = 2;
    element.trigger(event);
  }
});
