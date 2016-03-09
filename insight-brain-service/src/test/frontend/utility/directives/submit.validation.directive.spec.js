describe('submit.validation.directive.spec.js', function() {
  var $compile,
      scope;

  beforeEach(module('utility.directives'));

  beforeEach(inject(function($rootScope, _$compile_) {
    $compile = _$compile_;
    scope = $rootScope.$new();
  }));

  it('is disabled when not dirty and displays correct submit message', function() {
    var element = $compile('<form name="form"><button submit-validation submit-dirty="dirty" submit-type="submit"></button></form>')(scope).children(0);
    spyOn($.fn, 'tooltip').andReturn(element);

    scope.dirty = false;
    scope.$digest();

    expect(element.hasClass('disabled')).toBeTruthy();
    expect(element.attr('title')).toEqual('Unable to submit: fields with invalid or missing data.');
  });

  it('is disabled when not valid and displays correct update message', function() {
    var element = $compile('<form name="form"><button submit-validation submit-dirty="dirty" submit-type="update"></button></form>')(scope).children(0);
    spyOn($.fn, 'tooltip').andReturn(element);

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
    var element = $compile('<form name="form"><button submit-validation submit-dirty="dirty" submit-type="submit"></button></form>')(scope).children(0);
    spyOn($.fn, 'tooltip').andReturn(element);

    scope.dirty = true;
    scope.$digest();

    expect(element.hasClass('disabled')).toBeFalsy();
    expect($.fn.tooltip).toHaveBeenCalledWith('destroy');
  });
});
