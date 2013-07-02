describe('AngularCommon', function () {
  var scope, compile, httpBackend, regex, mockModel;

  beforeEach(module('AngularCommon'));
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
    httpBackend.expectGET('../assets/components/errorModal.html').respond("<div id='errorModal'></div>");
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
});