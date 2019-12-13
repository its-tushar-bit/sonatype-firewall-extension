/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityDirectivesModule from '../../../../main/frontend/utility/directives/utility.directives.module';

describe('pad.to.top.directive.spec.js', function() {
  beforeEach(angular.mock.module(utilityDirectivesModule.name));

  var $interval,
      scope,
      container,
      element,
      topTarget;

  beforeEach(inject(function($rootScope, _$interval_) {
    scope = $rootScope.$new();
    $interval = _$interval_;

    spyOn($interval, 'cancel').and.callThrough();
  }));

  describe('When element as top target', function() {
    beforeEach(inject(function($compile) {
      container = $compile(angular.element(
          '<div id="container" style="height:100px;"><div pad-to-top style="height:10px;' +
          'margin-bottom:1px;"><div id="innerElement" style="height:10px;">' +
          '</div></div></div>'))(scope);
      topTarget = element = container.children();

      angular.element('body').append(container);
    }));

    afterEach(function() {
      angular.element('#container').remove();
      scope.$destroy();
    });

    testPadToTop();
  });

  describe('When inner element as top target', function() {
    beforeEach(inject(function($compile) {
      container = $compile(angular.element(
          '<div id="container" style="height:100px;"><div pad-to-top="#innerElement" style="height:10px;' +
          'margin-bottom:1px;"><div id="innerElement" style="height:10px;">' +
          '</div></div></div>'))(scope);
      element = container.children();
      topTarget = element.children();

      angular.element('body').append(container);
    }));

    afterEach(function() {
      angular.element('#container').remove();
      scope.$destroy();
    });

    testPadToTop();
  });

  function testPadToTop() {
    it('Properly Updates Padding', function() {
      expect(element.css('margin-bottom')).toBe('1px');
      expect($interval.cancel).not.toHaveBeenCalled();
      $interval.flush(200);

      expect($interval.cancel).toHaveBeenCalled();
      expect(element.css('margin-bottom')).toBe((100 - 10) + 'px');
    });

    describe('Refresh after interval', function() {
      beforeEach(function() {
        $interval.flush(200);
      });

      it('with container height change', function() {
        container.css('height', '200px');
        $interval.flush(1000);

        expect(element.css('margin-bottom')).toBe((200 - 10) + 'px');
      });

      it('with top target outer height change', function() {
        topTarget.css('padding-top', '20px');
        $interval.flush(1000);

        expect(element.css('margin-bottom')).toBe((100 - 10 - 20) + 'px');
      });

      it('with no change', function() {
        var originalMargin = element.css('margin-bottom');
        $interval.flush(1000);

        expect(element.css('margin-bottom')).toBe(originalMargin);
      });

      it('is stopped after scope destroy', function() {
        var originalMargin = element.css('margin-bottom');
        topTarget.css('padding-top', '20px');
        scope.$destroy();
        $interval.flush(1000);

        expect(element.css('margin-bottom')).toBe(originalMargin);
      });
    });
  }
});
