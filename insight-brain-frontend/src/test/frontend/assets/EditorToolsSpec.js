/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import editorToolsModule from '../../../main/frontend/EditorTools';
import { httpInterceptors } from '../../../main/frontend/utilAngular/HttpInterceptors';

describe('EditorToolsSpec', function () {
  var scope = null;

  beforeEach(
    angular.mock.module(editorToolsModule.name, httpInterceptors.name, function ($provide) {
      $provide.value('ApplicationId', {
        encoded: function () {
          return 'bom1-12345678';
        },
      });
      $provide.value('OrganizationId', {
        encoded: function () {
          return null;
        },
      });
      $provide.value('selectedApplication', {
        publicId: 'bom1-12345678',
      });
    })
  );

  beforeEach(inject(function ($rootScope) {
    scope = $rootScope.$new();
    scope.$close = angular.noop;
  }));

  afterEach(function () {
    if (scope) {
      scope.$destroy();
    }
  });

  describe('clmEditable', function () {
    var scope, directiveScope, element;

    beforeEach(inject(function ($rootScope, $compile) {
      scope = $rootScope.$new();
      angular.extend(scope, {
        selected: {
          name: '',
          id: null,
        },
        siblings: [],
        eForm: {},
      });
      element = $compile(
        '<div clm-editable ' +
          'model="selected" ' +
          'model-field="name" ' +
          'e-form="eForm" ' +
          'empty-text="Enter Name" ' +
          'whitespace-check="true" ' +
          'invalid="$invalid" ' +
          'duplicate-array="siblings" ' +
          'duplicate-id-field="id"></div>'
      )(scope);
      angular.element('body').append(element);
      directiveScope = scope.$$childHead;
    }));

    afterEach(function () {
      scope.$destroy();
      element.remove();
    });

    it('Name Validation', function () {
      directiveScope.check('');
      scope.$digest();
      expect(scope.$invalid).not.toBeTruthy();

      expect(directiveScope.check('Foo  Bar')).toEqual('No double spaces or tabs in name');
      scope.$digest();
      expect(scope.$invalid).toBeTruthy();

      expect(directiveScope.check('Foo')).toBeFalsy();
      scope.$digest();
      expect(scope.$invalid).not.toBeTruthy();

      expect(directiveScope.check('._ -')).toBeFalsy();
      scope.$digest();
      expect(scope.$invalid).not.toBeTruthy();

      expect(directiveScope.check('Foo&Bar')).toEqual('Use valid characters: alphanumeric, "_", ".", "-", or spaces');
      scope.$digest();
      expect(scope.$invalid).toBeTruthy();
    });

    describe('Duplicate Checking', function () {
      it('same id', function () {
        scope.$apply(function () {
          scope.siblings.push({
            id: 'bar',
            name: 'foo',
          });
          scope.selected.id = 'bar';
        });

        directiveScope.check('foo');
        scope.$digest();
        expect(scope.$invalid).not.toBeTruthy();
      });

      it('different entries', function () {
        scope.$apply(function () {
          scope.siblings.push({
            id: 'asdf',
            name: 'foo',
          });
          scope.selected.id = 'bar';
        });

        expect(directiveScope.check('foo')).toEqual('Already in use');
        scope.$digest();
        expect(scope.$invalid).toBeTruthy();
      });
    });

    describe('No Spaces', function () {
      it('Spaces Validation', function () {
        directiveScope.noSpaces = 'true';
        expect(directiveScope.check('f oo')).toEqual('Spaces or tabs are not allowed');
        scope.$digest();
        expect(scope.$invalid).toBeTruthy();

        directiveScope.noSpaces = 'true';
        expect(directiveScope.check('Foo&Bar')).toEqual('Use valid characters: alphanumeric, "_", ".", or "-"');
        scope.$digest();
        expect(scope.$invalid).toBeTruthy();
      });
    });
  });
});
