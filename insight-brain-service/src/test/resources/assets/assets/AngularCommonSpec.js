describe('AngularCommon', function () {
  var scope, compile, httpBackend, regex, mockModel, form;

  beforeEach(module('AngularCommon', 'CommonServices'));
  beforeEach(inject(function ($httpBackend, $rootScope, $compile, regexFactory, $timeout) {
    scope = $rootScope.$new();
    compile = $compile;
    httpBackend = $httpBackend;
    regex = regexFactory;
    timeout = $timeout;
    scope.mockModel = {
      name: null
    };
  }));

  it('implements errorModal directive', function () {
    httpBackend.expectGET('../assets/components/errorModal.html?').respond("<div id='errorModal'></div>");
    var element = compile("<div error-Modal></div>")(scope);
    expect(element).not.toBeUndefined();
  });

  it('provides regex to match unicode characters', function () {
    var allLettersRegex = new RegExp('[' + regex.allLetters().source + ']');
    expect('a'.match(allLettersRegex)).toBeTruthy();
    expect('ñ'.match(allLettersRegex)).toBeTruthy();
    expect('Ҙ'.match(allLettersRegex)).toBeTruthy();
    expect('長'.match(allLettersRegex)).toBeTruthy();
    expect('!'.match(allLettersRegex)).not.toBeTruthy();
    expect('$'.match(allLettersRegex)).not.toBeTruthy();
  });

  it('validates alpha numeric controls', function() {
    var element = compile("<ng-form id='form' name='form'><input id='control' name='control' type='text' ng-model='alpha' alpha-numeric /></ng-form>")(scope);
    scope.$digest();
    scope.$apply(function() {
      scope.alpha = '!!!!';
    });
    expect(element.find('input').val()).toEqual('!!!!');
    expect(scope.form.control.$error.alphaNumeric).toBeTruthy();
    scope.$apply(function() {
      scope.alpha = 'foo';
    });
    expect(scope.form.control.$error.alphaNumeric).not.toBeTruthy();
  });

  it('Messages', inject(function (Messages) {
    expect(Messages.getHttpErrorMessage(['Internal Error', 500, null, null])).toEqual('500 - Internal Error');
    expect(Messages.getHttpErrorMessage(
        ['Unable to reach CLM server', 0, null, null])).toEqual('Unable to reach CLM server');

    expect(Messages.getHttpErrorMessage({ data: 'Internal Error', status: 500 })).toEqual('500 - Internal Error');
    expect(Messages.getHttpErrorMessage({ data: 'Bogus String', status: 0 })).toEqual('Unable to reach CLM server');
  }));

  it('X-editable directive should be respected if applied as an attribute', function () {
    var elm = angular.element(
        "<a ng-model='mockModel.name' xeditable href='javascript:;' data-emptytext='Name' " +
            "data-type='text' style='margin-bottom: 5px;'></a>");
    compile(elm)(scope);
    scope.$digest();

    expect(elm).not.toBeUndefined();
    expect(elm.hasClass('editable')).toBeFalsy();

    timeout.flush();

    expect(elm.hasClass('editable')).toBeTruthy();
  });

  it('X-editable directive should be ignored if not applied as an attribute', function () {
    var elm = angular.element(
        "<xeditable ng-model='mockModel.name' data-emptytext='Name' " +
            "data-type='text' style='margin-bottom: 5px;'></xeditable>");
    compile(elm)(scope);
    scope.$digest();

    expect(elm).not.toBeUndefined();
    expect(elm.hasClass('editable')).toBeFalsy();

    try {
      timeout.flush();
      fail();
    }
    catch (err) {
      //expected since we haven't queued any activity
    }

    expect(elm.hasClass('editable')).toBeFalsy();
  });

  it('X-editable allows for adjustable width', function () {
    var element = angular.element("<a ng-model='mockModel.name' xeditable adjustable='true' href='javascript:;' />");
    compile(element)(scope);
    scope.$digest();

    timeout.flush();

    element.click();
    expect(element.data('editable').input.$input).not.toBeUndefined();
    element.data('editable').input.$input.width(90);
    element.data('editable').input.$input.val('very very long string to go into the width of the control');
    element.data('editable').input.$input.keyup();
    expect(element.data('editable').input.$input.width()).toBeGreaterThan(90);
  });

  it('X-editable with required attribute updates backing model on keyup', function () {
    var element = angular.element("<a ng-model='mockModel.name' xeditable required href='javascript:;' />");
    compile(element)(scope);
    scope.$digest();

    timeout.flush();

    element.click();
    expect(element.data('editable').input.$input).not.toBeUndefined();
    element.data('editable').input.$input.val('Some typed string');

    expect(scope.mockModel.name).toBe(null);

    element.data('editable').input.$input.keyup();
    element.data('editable').input.$input.val('Some more editing');

    expect(scope.mockModel.name).toBe('Some typed string');

    element.data('editable').input.$input.val('Even more editing');
    element.data('editable').input.$input.keyup();

    expect(scope.mockModel.name).toBe('Even more editing');
  });

  it('X-editable without required attribute does not update model on keyup', function () {
    var element = angular.element("<a ng-model='mockModel.name' xeditable href='javascript:;' />");
    compile(element)(scope);
    scope.$digest();

    timeout.flush();

    element.click();
    expect(element.data('editable').input.$input).not.toBeUndefined();
    element.data('editable').input.$input.val('Some typed string');

    expect(scope.mockModel.name).toBe(null);

    element.data('editable').input.$input.keyup();
    element.data('editable').input.$input.val('Some more editing');

    expect(scope.mockModel.name).toBe(null);

    element.data('editable').input.$input.val('Even more editing');
    element.data('editable').input.$input.keyup();

    expect(scope.mockModel.name).toBe(null);
  });

  it('isDuplicate should respect casesensitive param', function () {
    var elm = angular.element(
        "<form name='form'>" +
            "<input ng-model='app.name' name='name' " +
            " is-Duplicate is-Duplicate-Array='applications' is-Duplicate-Id-Field='name' is-Duplicate-Case-Sensitive='false'>" +
            "</input>" +
        "</form>"

    );
    scope.app = {name: null}
    scope.applications = [
      {name: 'a'}
    ];
    compile(elm)(scope);
    scope.$digest();
    scope.form.name.$setViewValue('A');
    expect(scope.form.name.$valid).toBe(false);

    scope.form.name.$setViewValue('a');
    expect(scope.form.name.$valid).toBe(false);

    scope.form.name.$setViewValue('b');
    expect(scope.form.name.$valid).toBe(true);
  });
});
