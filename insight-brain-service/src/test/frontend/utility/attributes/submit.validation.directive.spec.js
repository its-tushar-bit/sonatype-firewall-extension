describe('submit.validation.directive.spec.js', function() {
  var $compile,
      scope;

  beforeEach(module('utility'));

  beforeEach(inject(function($rootScope, _$compile_) {
    $compile = _$compile_;
    scope = $rootScope.$new();
  }));

  it('is disabled when not valid and displays correct submit message', function() {
    var element = $compile('<form name="form"><button submit-validation="valid" submit-type="submit"></button></form>')(scope).children(0);
    spyOn($.fn, 'tooltip').andReturn(element);

    scope.valid = false;
    scope.$digest();

    expect(element.hasClass('disabled')).toBeTruthy();
    expect(element.attr('title')).toEqual('Unable to submit with invalid or missing fields.');
  });

  it('is disabled when not valid and displays correct update message', function() {
    var element = $compile('<form name="form"><button submit-validation="valid" submit-type="update"></button></form>')(scope).children(0);
    spyOn($.fn, 'tooltip').andReturn(element);

    scope.valid = false;
    scope.$digest();

    expect(element.hasClass('disabled')).toBeTruthy();
    expect(element.attr('title')).toEqual('There are no changes to update.');

    scope.form.$setDirty();
    scope.$digest();
    expect(element.attr('title')).toEqual('Unable to update with invalid or missing fields.');
  });

  it('is enabled when valid', function() {
    var element = $compile('<form name="form"><button submit-validation="valid" submit-type="submit"></button></form>')(scope).children(0);
    spyOn($.fn, 'tooltip').andReturn(element);

    scope.valid = true;
    scope.$digest();

    expect(element.hasClass('disabled')).toBeFalsy();
    expect($.fn.tooltip).toHaveBeenCalledWith('destroy');
  });
});
