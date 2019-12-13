/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import angularCommonModule from '../../../main/frontend/util/AngularCommon';
import utilityServicesModule from '../../../main/frontend/utility/services/utility.services.module';

describe('AngularCommon', function() {
  var scope, compile, httpBackend, regex;

  beforeEach(angular.mock.module(angularCommonModule.name, utilityServicesModule.name, function($provide) {
    var $state = {
      current: {
        name: 'dashboard.component',
        data: {
          crumb: 'Component Details'
        }
      },
      get: function(name) {
        if (name === 'dashboard') {
          return {
            name: 'dashboard',
            data: {
              crumb: 'Dashboard'
            }
          };
        }
      },
      href: angular.noop
    };
    $provide.value('$state', $state);
  }));

  beforeEach(inject(function($httpBackend, $rootScope, $compile, regexFactory) {
    scope = $rootScope.$new();
    compile = $compile;
    httpBackend = $httpBackend;
    regex = regexFactory;
    scope.mockModel = {
      name: null
    };
  }));

  afterEach(function() {
    httpBackend.verifyNoOutstandingExpectation();
    httpBackend.verifyNoOutstandingRequest();
  });

  it('provides regex to match unicode characters', function() {
    var allLettersRegex = new RegExp('[' + regex.allLetters().source + ']');
    expect('a'.match(allLettersRegex)).toBeTruthy();
    expect('ñ'.match(allLettersRegex)).toBeTruthy();
    expect('Ҙ'.match(allLettersRegex)).toBeTruthy();
    expect('長'.match(allLettersRegex)).toBeTruthy();
    expect('!'.match(allLettersRegex)).not.toBeTruthy();
    expect('$'.match(allLettersRegex)).not.toBeTruthy();
  });

  it('validates valid name characters controls', function() {
    var element = compile('<ng-form id="form" name="form"><input id="control" name="control" type="text" ' +
        'ng-model="alpha" valid-name-characters /></ng-form>')(scope);
    scope.$digest();
    scope.$apply(function() {
      scope.alpha = '!!!!';
    });
    expect(element.find('input').val()).toEqual('!!!!');
    expect(scope.form.control.$error.validNameCharacters).toBeTruthy();
    scope.$apply(function() {
      scope.alpha = 'foo';
    });
    expect(scope.form.control.$error.validNameCharacters).not.toBeTruthy();
  });

  it('isDuplicate should respect casesensitive param', function() {
    var elm = angular.element(
        '<form name="form">' +
        '<input ng-model="app.name" name="name" ' +
               'is-Duplicate is-Duplicate-Array="applications" is-Duplicate-Id-Field="name" ' +
               'is-Duplicate-Case-Sensitive="false">' +
        '</input>' +
        '</form>'

    );
    scope.app = {name: null};
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

  describe('"ago" filter', function() {
    var ago;
    beforeEach(inject(function($filter) {
      ago = $filter('ago');
    }));
    var testCases = [
      {
        input: function() {
          return new Date();
        },
        expected: 'seconds ago'
      },
      {
        input: function() {
          return new Date().getTime();
        },
        expected: 'seconds ago'
      },
      {
        input: function() {
          var today = new Date();
          return new Date(today.getFullYear() - 2, today.getMonth(), today.getDate());
        },
        expected: '2 years ago'
      },
      {
        input: function() {
          var today = new Date();
          return new Date(today.getFullYear(), today.getMonth(), today.getDate() - 3 * 30, today.getHours() - 6);
        },
        expected: '3 months ago'
      },
      {
        input: function() {
          var today = new Date();
          return new Date(today.getFullYear(), today.getMonth(), today.getDate() - 10, today.getHours() - 6);
        },
        expected: '10 days ago'
      },
      {
        input: function() {
          var today = new Date();
          return new Date(today.getTime() - (23 * 60 + 30) * 60 * 1000);
        },
        expected: '23 hours ago'
      },
      {
        input: function() {
          var today = new Date();
          return new Date(today.getFullYear(), today.getMonth(), today.getDate(), today.getHours(),
              today.getMinutes() - 58);
        },
        expected: '5[8|9] minutes ago'
      },
      {
        input: function() {
          var today = new Date();
          return new Date(today.getFullYear(), today.getMonth(), today.getDate(), today.getHours(),
              today.getMinutes() - 1);
        },
        expected: '[1|2] minute[s]? ago'
      },
      {
        input: function() {
          var today = new Date();
          return new Date(today.getFullYear() + 100, today.getMonth(), today.getDate());
        },
        expected: 'seconds ago'
      },
      {
        input: function() {
          return null;
        },
        expected: ''
      },
      {
        input: function() {
          return undefined;
        },
        expected: ''
      },
      {
        input: function() {
          return '';
        },
        expected: ''
      }
    ];
    function validateFilter(input, expected) {
      it('should filter to: ' + expected, function() {
        expect(ago(input())).toMatch(expected);
      });
    }
    for (var i = 0; i < testCases.length; i++) {
      var testCase = testCases[i];
      validateFilter(testCase.input, testCase.expected);
    }
  });

  describe('"terseAgo" filter', function() {
    var ago;
    beforeEach(inject(function($filter) {
      ago = $filter('terseAgo');
    }));
    var testCases = [
      {
        input: function() {
          return new Date();
        },
        expected: '1min'
      },
      {
        input: function() {
          return new Date().getTime();
        },
        expected: '1min'
      },
      {
        input: function() {
          var today = new Date();
          return new Date(today.getFullYear() - 2, today.getMonth(), today.getDate());
        },
        expected: '2y'
      },
      {
        input: function() {
          var today = new Date();
          return new Date(today.getFullYear(), today.getMonth(), today.getDate() - 3 * 30, today.getHours() - 6);
        },
        expected: '3m'
      },
      {
        input: function() {
          var today = new Date();
          return new Date(today.getFullYear(), today.getMonth(), today.getDate() - 10, today.getHours() - 6);
        },
        expected: '10d'
      },
      {
        input: function() {
          var today = new Date();
          return new Date(today.getTime() - (23 * 60 + 30) * 60 * 1000);
        },
        expected: '23h'
      },
      {
        input: function() {
          var today = new Date();
          return new Date(today.getFullYear(), today.getMonth(), today.getDate(), today.getHours(),
              today.getMinutes() - 58);
        },
        expected: '5[8|9]min'
      },
      {
        input: function() {
          var today = new Date();
          return new Date(today.getFullYear(), today.getMonth(), today.getDate(), today.getHours(),
              today.getMinutes() - 1);
        },
        expected: '[1|2]min'
      },
      {
        input: function() {
          var today = new Date();
          return new Date(today.getFullYear() + 100, today.getMonth(), today.getDate());
        },
        expected: '1min'
      },
      {
        input: function() {
          return null;
        },
        expected: ''
      },
      {
        input: function() {
          return undefined;
        },
        expected: ''
      },
      {
        input: function() {
          return '';
        },
        expected: ''
      }
    ];
    function validateFilter(input, expected) {
      it('should filter to: ' + expected, function() {
        expect(ago(input())).toMatch(expected);
      });
    }
    for (var i = 0; i < testCases.length; i++) {
      var testCase = testCases[i];
      validateFilter(testCase.input, testCase.expected);
    }
  });

  describe('agoLastDay filter', function() {
    var filter, filteredAnswer = 'Less than a day ago';
    beforeEach(inject(function($filter) {
      filter = $filter('agoLastDay');
    }));

    it('Should filter anything in the last few seconds', function() {
      expect(filter('seconds ago')).toBe(filteredAnswer);
    });
    it('Should filter anything in the last few minutes', function() {
      expect(filter('59 minutes ago')).toBe(filteredAnswer);
    });
    it('Should filter anything in the last few hours', function() {
      expect(filter('1 hour ago')).toBe(filteredAnswer);
    });
  });

  describe('chiclet directive', function() {
    var compileElement = function(template) {
          var element = angular.element(template);
          compile(element)(scope);
          angular.element('body').append(element);
          scope.$digest();
          return element;
        },
        chicletElement = null;

    afterEach(function () {
      chicletElement.remove();
    });

    it('should show all defined chiclets with a default margin', function() {
      chicletElement = compileElement('<div chiclets critical="1" severe="1" moderate="1" none="1"></div>');
      expect(chicletElement.scope().$$childTail.style.margin).toBe('2px');
      var chiclets = chicletElement.find('span');
      expect(chiclets.length).toBe(4);
      angular.forEach(chiclets, function(chiclet) {
        expect($(chiclet).text()).toBe('1');
        expect($(chiclet).is(':visible')).toBeTruthy();
      });
    });
    it('should show all chiclets even if not set when "alwaysShow" specified', function() {
      chicletElement = compileElement('<div chiclets always-show="true"></div>');
      var chiclets = chicletElement.find('span');
      expect(chiclets.length).toBe(4);
      angular.forEach(chiclets, function(chiclet) {
        expect($(chiclet).text()).toBe('');
        expect($(chiclet).is(':visible')).toBeTruthy();
      });
    });
    it('should show nothing if there is no chiclet data', function() {
      chicletElement = compileElement('<div chiclets></div>');
      var chiclets = chicletElement.find('span');
      expect(chiclets.length).toBe(4);
      angular.forEach(chiclets, function(chiclet) {
        expect($(chiclet).is(':visible')).toBeFalsy();
      });
    });
    it('should set a default margin if one is not specified', function() {
      chicletElement = compileElement('<div chiclets critical="1" severe="1" moderate="1" none="1"></div>');
      var chiclets = chicletElement.find('span');
      expect(chiclets.length).toBe(4);
      angular.forEach(chiclets, function(chiclet) {
        expect($(chiclet).css('margin-top')).toBe('2px');
      });
    });
    it('should respect a provided margin', function() {
      chicletElement = compileElement('<div chiclets critical="1" severe="1" moderate="1" none="1" margin="5cm">' +
          '</div>');
      expect(chicletElement.scope().$$childTail.style.margin).toBe('5cm');
    });
  });

  describe('"truncate" filter', function() {
    var truncate;
    beforeEach(inject(function($filter) {
      truncate = $filter('truncate');
    }));
    it('Should filter strings longer than 25 characters by default', function() {
      var truncated = truncate('A string longer than 25 characters');
      expect(truncated).toBe('A string longer than 2...');
      expect(truncated.length).toBe(25);
    });
    it('Should allow for specifying a length', function() {
      var truncated = truncate('A string longer than 30 characters', 30);
      expect(truncated).toBe('A string longer than 30 cha...');
      expect(truncated.length).toBe(30);
    });
    it('Should not filter strings that fit in its set length', function() {
      var truncated = truncate('bumfuzzled');
      expect(truncated).toBe('bumfuzzled');
      expect(truncated.length).toBe(10);
    });
  });

  describe('multiSelect vs-repeat', function() {
    it('supports vs-repeat', inject(function($compile) {
      var element = angular.element('<div multi-select use-vs-repeat items="tags" selected-ids="appliedTagIds"></div>');
      $compile(element)(scope);
      scope.$digest();

      var dropdownList = element.find('.dropdown-scroll');
      expect(dropdownList.attr('vs-repeat')).toBeDefined();
      expect(dropdownList.attr('vs-repeat')).toBe('');
    }));
  });

  describe('multiSelect', function () {
    var directiveScope = null;

    beforeEach(inject(function ($compile) {
      var element = angular.element('<div  multi-select items="tags" selected-ids="appliedTagIds"></div>');
      scope.tags = [{ id: 'foo', name: 'Foo' }, { id: 'bar', name: 'Bar' }];
      scope.appliedTagIds = [];
      scope.$apply(function () {
        $compile(element)(scope);
      });
      directiveScope = scope.$$childHead;
    }));

    it('can handle getText being called before the watcher on selectedIds', function () {
      directiveScope.selectedIds = ['bar'];
      delete directiveScope.selected;
      expect(directiveScope.getText()).toEqual('None selected');
    });

    it('getText', function () {
      expect(directiveScope.getText()).toEqual('None selected');
      scope.$apply(function () {
        scope.appliedTagIds = ['bar'];
      });
      expect(directiveScope.getText()).toEqual('Bar');
      scope.$apply(function () {
        scope.appliedTagIds = ['foo', 'bar'];
      });
      expect(directiveScope.getText()).toEqual('Foo, Bar');
    });

    it('updateSelectedIds', function () {
      expect(scope.appliedTagIds).toEqual([]);

      directiveScope.$apply(function () {
        directiveScope.selected['foo'] = true; // normally checkbox model does this
        directiveScope.updateSelectedIds('foo');
      });
      expect(scope.appliedTagIds).toEqual(['foo']);

      directiveScope.$apply(function () {
        directiveScope.selected['bar'] = true; // normally checkbox model does this
        directiveScope.updateSelectedIds('bar');
      });
      expect(scope.appliedTagIds).toEqual(['foo', 'bar']);

      directiveScope.$apply(function () {
        directiveScope.selected['bar'] = false; // normally checkbox model does this
        directiveScope.updateSelectedIds('bar');
      });
      expect(scope.appliedTagIds).toEqual(['foo']);
    });
  });

  describe('LastSelectedOrganization', function() {
    it('Set/Get/Clear', inject(function(LastSelectedOrganization) {
      expect(LastSelectedOrganization.get()).toEqual({});
      LastSelectedOrganization.set({
        id: 'anid',
        name: 'aname'
      });
      expect(LastSelectedOrganization.get()).toEqual({
        id: 'anid',
        name: 'aname'
      });
      LastSelectedOrganization.clear();
      expect(LastSelectedOrganization.get()).toEqual({});
    }));
  });

  describe('threatClass', function() {
    let $compile;

    beforeEach(inject(function(_$compile_) {
      $compile = _$compile_;
    }));

    it('applies threat classes', function() {
      const allClasses = ['ignore', 'none', 'moderate', 'severe', 'critical'],
          expectedResults = [{
            clazz: 'ignore',
            threatLevels: [0]
          }, {
            clazz: 'none',
            threatLevels: [1]
          }, {
            clazz: 'moderate',
            threatLevels: [2, 3]
          }, {
            clazz: 'severe',
            threatLevels: [4, 5, 6, 7]
          }, {
            clazz: 'critical',
            threatLevels: [8, 9, 10]
          }];

      angular.forEach(expectedResults, function(result) {
        angular.forEach(result.threatLevels, function(threatLevel) {
          scope.threatLevel = threatLevel;
          var element = angular.element('<div threat-class="threatLevel"></div>');
          element = $compile(element)(scope);
          scope.$digest();

          const elementClassAttr = element.attr('class');
          expect(elementClassAttr).toContain(result.clazz);

          allClasses.filter(c => c !== result.clazz).forEach(function(cls) {
            expect(elementClassAttr).not.toContain(cls);
          });
        });
      });
    });

    it('updates threat classes in response to scope changes', function() {
      scope.threatLevel = 2;
      const element = $compile('<div threat-class="threatLevel"></div>')(scope);
      scope.$digest();

      expect(element.attr('class')).toContain('moderate');

      scope.threatLevel = 6;
      scope.$digest();

      expect(element.attr('class')).toContain('severe');
      expect(element.attr('class')).not.toContain('moderate');
    });
  });

  describe('safeDivide', function() {
    var safeDivide;
    beforeEach(inject(function($filter) {
      safeDivide = $filter('safeDivide');
    }));

    it('divides two numbers', function() {
      expect(safeDivide(1, 2)).toBe(0.5);
    });

    it('safely divides by zero', function() {
      expect(safeDivide(1, 0)).toBe(0);
    });
  });

  describe('clmAlerts', function() {
    var element, scope, compile;

    beforeEach(inject(function($compile, $rootScope) {
      scope = $rootScope.$new();
      compile = $compile;
    }));

    it('defaults to showing a closeable warning message if no other type is specified', function() {
      element = angular.element('<div clm-alerts="errors"></div>');
      element = compile(element)(scope);
      scope.$apply(function() {
        scope.errors = [{msg: 'Foo'}];
      });
      expect(element.find('.alert-error').length).toBe(0);
      expect(element.find('.alert-warning').length).toBe(1);
      expect(element.find('button').length).toBe(1);
    });

    it('displays all errors configured in the scope', function() {
      element = angular.element('<div clm-alerts="errors"></div>');
      element = compile(element)(scope);
      scope.$apply(function() {
        scope.errors = [
          {type: 'error', msg: 'Foo'},
          {type: 'info', msg: 'Foo'},
          {type: 'success', msg: 'Foo'}
        ];
      });
      expect(element.find('.alert-error').length).toBe(1);
      expect(element.find('.alert-info').length).toBe(1);
      expect(element.find('.alert-success').length).toBe(1);
      expect(element.find('.alert-warning').length).toBe(0);
    });

    it('hides the "close" button if configured to', function() {
      element = angular.element('<div clm-alerts="errors" no-close="true"></div>');
      element = compile(element)(scope);
      scope.$apply(function() {
        scope.errors = [{msg: 'Foo'}];
      });
      expect(element.find('button').length).toBe(0);
    });
  });

  describe('dashboard "fileName" filter', function() {
    var fileNameFilter;
    beforeEach(inject(function($filter) {
      fileNameFilter = $filter('fileName');
    }));
    var testCases = [
      {
        input: function() {
          return '/';
        },
        expected: '/'
      },
      {
        input: function() {
          return '//';
        },
        expected: ''
      },
      {
        input: function() {
          return '///';
        },
        expected: ''
      },
      {
        input: function() {
          return 'test/path/fileName';
        },
        expected: 'fileName'
      },
      {
        input: function() {
          return '/test/path/fileName';
        },
        expected: 'fileName'
      },
      {
        input: function() {
          return 'test/path/fileName/';
        },
        expected: 'fileName'
      },
      {
        input: function() {
          return '/test/path/fileName/';
        },
        expected: 'fileName'
      },
      {
        input: function() {
          return '/fileName';
        },
        expected: 'fileName'
      },
      {
        input: function() {
          return 'fileName/';
        },
        expected: 'fileName'
      },
      {
        input: function() {
          return 'fileName';
        },
        expected: 'fileName'
      },
      {
        input: function() {
          return null;
        },
        expected: 'null'
      },
      {
        input: function() {
          return '';
        },
        expected: ''
      }
    ];
    function validateFilter(input, expected) {
      it('should filter to: ' + expected, function() {
        if (expected) {
          expect(fileNameFilter(input())).toMatch(expected);
        }
        else {
          expect(fileNameFilter(input())).toEqual(expected);
        }
      });
    }
    for (var i = 0; i < testCases.length; i++) {
      var testCase = testCases[i];
      validateFilter(testCase.input, testCase.expected);
    }
  });

  describe('breadcrumb', function() {
    var scope,
        $rootScope;

    beforeEach(inject(function(_$rootScope_, $compile) {
      $rootScope = _$rootScope_;
      var parentScope = $rootScope.$new();

      $compile(angular.element('<div breadcrumb></div>'))(parentScope);
      parentScope.$digest();
      scope = parentScope.$$childHead;
    }));

    it('builds list of parent states', function() {
      $rootScope.$broadcast('$stateChangeSuccess', { name: 'dashboard.component' }, undefined, { name: '' });

      expect(scope.states.length).toBe(2);
      expect(scope.states[0].state).toBe('dashboard.overview.violations');
      expect(scope.states[1].state).toBe('dashboard.component');
    });

    it('maintains previous parent states when navigating away', function() {
      $rootScope.$broadcast('$stateChangeSuccess', {name: 'dashboard.component'}, undefined,
          {name: 'dashboard.overview.components'});

      expect(scope.states.length).toBe(2);
      expect(scope.states[0].state).toBe('dashboard.overview.components');
      expect(scope.states[1].state).toBe('dashboard.component');
    });
  });

  describe('Modal', function() {
    var $modal,
        Modal;

    beforeEach(inject(function(_$modal_, _Modal_) {
      $modal = _$modal_;
      Modal = _Modal_;

      spyOn($modal, 'open');
    }));

    it('augments and then passes to $modal the config object that it receives', function() {
      var config = { a: 1, b: '2' };

      Modal.open(config);

      expect($modal.open).toHaveBeenCalledWith({
        a: 1,
        b: '2',
        windowClass: 'iq-modal',
        backdropClass: 'iq-modal-backdrop',
        animation: false
      });
    });

    it('does not overwrite conflicting values from the config object it receives', function() {
      var config = {
        a: 1,
        b: '2',
        windowClass: 'iq-modal-test',
        backdropClass: 'iq-modal-backdrop-test',
        animation: true
      };

      Modal.open(config);

      expect($modal.open).toHaveBeenCalledWith({
        a: 1,
        b: '2',
        windowClass: 'iq-modal-test',
        backdropClass: 'iq-modal-backdrop-test',
        animation: true
      });
    });
  });
});
