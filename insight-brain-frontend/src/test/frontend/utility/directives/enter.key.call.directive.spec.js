/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityDirectivesModule from '../../../../main/frontend/utility/directives/utility.directives.module';

describe('enter.key.call.directive.js', function () {
  var element, scope;

  beforeEach(angular.mock.module(utilityDirectivesModule.name));

  beforeEach(inject(function ($rootScope, $compile) {
    scope = $rootScope.$new();
    element = $compile('<input enter-key-call="doSomething($event)"></div>')(
      scope
    );
    scope.$digest();

    scope.doSomething = function () {};
  }));

  it('enter', function () {
    spyOn(scope, 'doSomething').and.callThrough();
    var event = jQuery.Event('keydown');
    event.keyCode = 13;
    element.trigger(event);
    expect(scope.doSomething).toHaveBeenCalled();
  });
});
