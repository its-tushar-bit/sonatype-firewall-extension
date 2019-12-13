/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityDirectivesModule from '../../../../main/frontend/utility/directives/utility.directives.module';

describe('submit.validation.directive.spec.js', function() {
  var $compile,
      scope;

  beforeEach(angular.mock.module(utilityDirectivesModule.name));

  beforeEach(inject(function($rootScope, _$compile_) {
    $compile = _$compile_;
    scope = $rootScope.$new();
  }));

  it('is disabled when not dirty and displays correct submit message', function() {
    var element = $compile('<form name="form"><button submit-validation submit-dirty="dirty" ' +
        'submit-type="submit"></button></form>')(scope).children(0);
    spyOn($.fn, 'tooltip').and.returnValue(element);

    scope.dirty = false;
    scope.$digest();

    expect(element.hasClass('disabled')).toBeTruthy();
    expect(element.attr('title')).toEqual('Unable to submit: fields with invalid or missing data.');
  });

  it('is disabled when not valid and displays correct update message', function() {
    var element = $compile('<form name="form"><button submit-validation submit-dirty="dirty" ' +
        'submit-type="update"></button></form>')(scope).children(0);
    spyOn($.fn, 'tooltip').and.returnValue(element);

    scope.dirty = false;
    scope.$digest();

    expect(element.hasClass('disabled')).toBeTruthy();
    expect(element.attr('title')).toEqual('There are no changes to update.');

    scope.dirty = true;
    scope.form.$setValidity(false);
    scope.$digest();
    expect(element.attr('title')).toEqual('Unable to update: fields with invalid or missing data.');
  });

  it('is enabled when valid and dirty', function() {
    var element = $compile('<form name="form"><button submit-validation submit-dirty="dirty" ' +
        'submit-type="submit"></button></form>')(scope).children(0);
    spyOn($.fn, 'tooltip').and.returnValue(element);

    scope.dirty = true;
    scope.$digest();

    expect(element.hasClass('disabled')).toBeFalsy();
    expect($.fn.tooltip).toHaveBeenCalledWith('destroy');
  });

  it('calls preventDefault on the click event when invalid and not dirty', function() {
    var elementStr =
          '<form name="form"><button submit-validation submit-dirty="dirty" submit-type="submit"></button></form>',
        element = $compile(elementStr)(scope).children(0),
        preventDefaultSpy = jasmine.createSpy('preventDefault'),
        evt = { type: 'click', preventDefault: preventDefaultSpy };

    scope.dirty = false;
    scope.form.$setValidity(false);
    scope.$digest();

    element.trigger(evt);
    expect(evt.preventDefault).toHaveBeenCalled();
  });

  it('calls preventDefault on the click event when valid and not dirty', function() {
    var elementStr =
          '<form name="form"><button submit-validation submit-dirty="dirty" submit-type="submit"></button></form>',
        element = $compile(elementStr)(scope).children(0),
        preventDefaultSpy = jasmine.createSpy('preventDefault'),
        evt = { type: 'click', preventDefault: preventDefaultSpy };

    scope.dirty = false;
    scope.$digest();

    element.trigger(evt);
    expect(evt.preventDefault).toHaveBeenCalled();
  });

  it('calls preventDefault on the click event when invalid and dirty', function() {
    var elementStr =
          '<form name="form"><button submit-validation submit-dirty="dirty" submit-type="submit"></button></form>',
        element = $compile(elementStr)(scope).children(0),
        preventDefaultSpy = jasmine.createSpy('preventDefault'),
        evt = { type: 'click', preventDefault: preventDefaultSpy };

    scope.dirty = true;
    scope.form.$setValidity(false);
    scope.$digest();

    element.trigger(evt);
    expect(evt.preventDefault).toHaveBeenCalled();
  });

  it('does not call preventDefault on the click event when valid and dirty', function() {
    var elementStr =
          '<form name="form"><button submit-validation submit-dirty="dirty" submit-type="submit"></button></form>',
        element = $compile(elementStr)(scope).children(0),
        preventDefaultSpy = jasmine.createSpy('preventDefault'),
        evt = { type: 'click', preventDefault: preventDefaultSpy };

    scope.dirty = true;
    scope.$digest();

    element.trigger(evt);
    expect(evt.preventDefault).not.toHaveBeenCalled();
  });
});
