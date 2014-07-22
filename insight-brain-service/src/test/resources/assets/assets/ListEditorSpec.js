describe('ListEditor', function() {
  'use strict';

  function createDirective(element, scopeTemplate) {
    var e = null;
    inject(function($compile, $rootScope, $httpBackend) {
      scope = $rootScope.$new();
      angular.extend(scope, scopeTemplate);

      $httpBackend.expectGET('../assets/components/list-editor/list-editor.html?').respond(template);
      e = $compile(element)(scope);
      $httpBackend.flush();
    });
    return e;
  }

  function setInput(inputElement, val) {

    inputElement.val(val);

    var evt = document.createEvent('HTMLEvents');
    inject(function($sniffer) {
      evt.initEvent(($sniffer.hasEvent('input')) ? 'input' : 'change', false, false);
    });
    inputElement[0].dispatchEvent(evt);
  }

  var scope,
      template = SpecUtil.getTemplate('assets/components/list-editor/list-editor.html');

  beforeEach(module('ListEditor'));

  afterEach(inject(function($httpBackend) {
    if (scope) {
      scope.$destroy();
    }
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  it('Validation', function() {
    var valid = true,
        template = {
          myList: [],
          placeHolder: 'some text',
          validator: function() {
            return valid;
          }
        },
        element = createDirective('<form name="form">' +
          '<div list-editor validator="validator" entries="myList" place-holder="placeHolder"></div>' +
          '</form>', template),
        input = angular.element('input', element),
        form = element.scope().form;

    expect(input.length).not.toEqual(0);
    setInput(input, 'foo');
    expect(form.$valid).toBe(true);

    valid = false;
    setInput(input, 'bar');
    expect(form.$valid).toBe(false);
    expect(form.neditor.$error.validInput).toBeTruthy();
  });

  it('Uniqueness', function() {
    var valid = true,
        template = {
          myList: [],
          setError: jasmine.createSpy('setError'),
          placeHolder: 'some text',
          validator: function() {
            return valid;
          }
        },
        element = createDirective('<form name="form">' +
          '<div list-editor validator="validator" entries="myList" place-holder="placeHolder"></div>' +
          '</form>', template),
        input = angular.element('input', element),
        form = element.scope().form;

    setInput(input, 'foo');
    input.trigger('submit');
    expect(template.myList).toEqual(['foo']);

    setInput(input, 'foo');
    expect(form.$valid).toBe(false);
    expect(form.neditor.$error.unique).toBeTruthy();
  });

  it('Add', function() {
    var valid = true,
        template = {
          myList: [],
          setError: jasmine.createSpy('setError'),
          placeHolder: 'some text',
          validator: function() {
            return valid;
          }
        },
        element = createDirective('<div list-editor validator="validator" entries="myList" place-holder="placeHolder"></div>',
            template),
        input = angular.element('input', element);

    setInput(input, 'foo');
    input.trigger('submit');
    expect(template.myList).toEqual(['foo']);
  });

  it('Remove', function() {
    var valid = true,
        template = {
          myList: [],
          setError: jasmine.createSpy('setError'),
          placeHolder: 'some text',
          validator: function() {
            return valid;
          }
        },
        element = createDirective('<form name="form">' +
          '<div list-editor validator="validator" entries="myList" place-holder="placeHolder"></div>' +
          '</form>', template),
        input = angular.element('input', element),
        form = element.scope().form;

    setInput(input, 'foo');
    input.trigger('submit');
    expect(template.myList).toEqual(['foo']);

    setInput(input, 'foo');
    expect(form.$valid).toBe(false);
    expect(form.neditor.$error.unique).toBeTruthy();
    angular.element('.btn-mini', element).click();
    expect(template.myList).toEqual([]);
    expect(form.$valid).toBe(true);
  });

  // See https://issues.sonatype.org/browse/CLM-844
  var reset = [
    { valid: true, name: 'Valid' },
    { valid: false, name: 'Invalid' }
  ];
  $.each(reset, function(index, item) {
    it('Reset + ' + item.name, function() {
      var template = {
            myList: ['bar'],
            setError: jasmine.createSpy('setError'),
            placeHolder: 'some text',
            validator: function() {
              return item.valid ? null : 'invalid entry';
            }
          },
          element = createDirective('<div list-editor validator="validator" entries="myList" place-holder="placeHolder"></div>',
              template),
          input = angular.element('input', element);

      setInput(input, 'foo');
      scope.$digest();
      expect(scope.$$childHead.neditor.$valid).toEqual(item.valid);

      spyOn(scope, 'validator').andCallThrough();
      scope.$apply(function() {
        scope.myList = ['asdf'];
      });
      expect(scope.validator).toHaveBeenCalledWith('foo');
      expect(scope.$$childHead.neditor.$valid).toEqual(item.valid);
    });
  });
});