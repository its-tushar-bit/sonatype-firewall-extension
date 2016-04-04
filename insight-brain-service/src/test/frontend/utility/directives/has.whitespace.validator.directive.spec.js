describe('has.whitespace.validator.directive.spec.js', function() {
  var scope;

  beforeEach(module('utility.directives'));

  beforeEach(inject(function($compile, $rootScope) {
    scope = $rootScope.$new();
  }));

  it('validates whitespace controls', inject(function($compile) {
    $compile('<ng-form name="form"><input name="control" type="text" ng-model="whitespace" has-whitespace-validator ng-trim="false" /></ng-form>')(scope);
    scope.$digest();
    scope.$apply(function() {
      scope.whitespace = '1234 ';
    });
    expect(scope.form.control.$error.spaces).toBeTruthy();
    scope.$apply(function() {
      scope.whitespace = ' 1234';
    });
    expect(scope.form.control.$error.spaces).toBeTruthy();
    scope.$apply(function() {
      scope.whitespace = '12  34';
    });
    expect(scope.form.control.$error.spaces).toBeTruthy();
    scope.$apply(function() {
      scope.whitespace = '1234';
    });
    expect(scope.form.control.$error.spaces).not.toBeTruthy();
  }));

  it('validates whitespace requires ng-trim to be false', inject(function($compile) {
    var html = '<ng-form><input name="control" type="text" ng-model="whitespace" has-whitespace-validator ng-trim="true" /></ng-form>';
    var error = new Error('has-whitespace-validator directive requires that the ngTrim attribute be set to false');
    function wrapper() {
      $compile(html)(scope);
    }
    expect(wrapper).toThrow(error);
    html = '<ng-form><input name="control" type="text" ng-model="whitespace" has-whitespace-validator /></ng-form>';
    expect(wrapper).toThrow(error);
  }));
});
