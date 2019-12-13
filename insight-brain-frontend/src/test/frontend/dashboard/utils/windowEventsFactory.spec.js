/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import dashboardUtilsModule from '../../../../main/frontend/dashboard/utils/dashboard.utils.module';

describe('windowEventsFactory.spec', function() {
  var wEventsFactory,
      window,
      scope;

  beforeEach(angular.mock.module(dashboardUtilsModule.name, function($provide) {
    $provide.value('$window', {
      resize: angular.noop
    });
  }));

  beforeEach(inject(function(windowEventsFactory, $rootScope, $window) {
    wEventsFactory = windowEventsFactory;
    scope = $rootScope.$new();
    window = angular.element($window);
  }));

  describe('addResizeHandler', function() {
    it('invokes a callback when an element width resizes', function() {
      var element = angular.element('<div></div>');
      element.width(1);
      element.height(1);
      var callback = jasmine.createSpy();

      wEventsFactory.addResizeHandler(scope, element, callback);
      element.width(2);
      window.resize();

      expect(callback).toHaveBeenCalled();
    });

    it('invokes a callback when an element height resizes', function() {
      var element = angular.element('<div></div>');
      element.width(1);
      element.height(1);
      var callback = jasmine.createSpy();

      wEventsFactory.addResizeHandler(scope, element, callback);
      element.height(2);
      window.resize();

      expect(callback).toHaveBeenCalled();
    });

    it('does not callback when element is not resized', function() {
      var element = angular.element('<div></div>');
      element.width(1);
      element.height(1);
      var callback = jasmine.createSpy();

      wEventsFactory.addResizeHandler(scope, element, callback);
      window.resize();

      expect(callback).not.toHaveBeenCalled();
    });

    it('does not callback when scope is disposed', function() {
      var element = angular.element('<div></div>');
      element.width(1);
      element.height(1);
      var callback = jasmine.createSpy();

      wEventsFactory.addResizeHandler(scope, element, callback);
      scope.$destroy();

      element.width(2);
      window.resize();

      expect(callback).not.toHaveBeenCalled();
    });
  });
});
