/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import resourceModule from '../../../main/frontend/Resource';
import clmContextLocationModule from '../../../main/frontend/util/CLMContextLocation';

describe('HierarchyStoreFactory', function () {
  var store,
    storeUrl = 'http://localhost:8234/rest/policy',
    storeResult = {
      policiesByOwner: [
        {
          ownerId: 'foo',
          ownerName: 'Foo',
          ownerType: 'organization',
          policies: [{ id: '1', name: 'First Policy', ownerId: 'foo' }],
        },
        {
          ownerId: 'bar',
          ownerName: 'Bar',
          ownerType: 'application',
          policies: [{ id: '2', name: 'Second Policy', ownerId: 'bar' }],
        },
      ],
    };

  function getUnexpectedErrorHandler() {
    return function (e) {
      expect(e).toBeNull();
    };
  }

  beforeEach(
    angular.mock.module(resourceModule.name, clmContextLocationModule.name)
  );

  beforeEach(inject(function (HierarchyStoreFactory) {
    store = HierarchyStoreFactory.getStore({
      field: 'policiesByOwner',
      url: storeUrl,
      template: { id: null },
      type: 'policy',
      storeField: 'policies',
    });
  }));

  afterEach(inject(function ($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  it('get() results have stores', inject(function ($httpBackend) {
    var result = null;

    store.get().then(function () {
      expect(arguments.length).toEqual(1);
      result = arguments[0];
    }, getUnexpectedErrorHandler());
    $httpBackend.expectGET(storeUrl).respond(storeResult);
    $httpBackend.flush();

    expect(result[0].ownerId).toEqual('foo');
    // check the resource PUTs to the right URL
    $httpBackend.expectPUT(storeUrl).respond(200);
    result[0].policies[0]
      .$save()
      .then(angular.noop(), getUnexpectedErrorHandler());
    $httpBackend.flush();
    // check store refresh() updates the resource
    $httpBackend
      .expectGET(storeUrl)
      .respond([{ name: 'First Policy - Updated' }]);
    result[0].store.refresh().then(angular.noop(), getUnexpectedErrorHandler());
    $httpBackend.flush();
    expect(result[0].policies[0].name).toBe('First Policy - Updated');

    expect(result[1].ownerId).toEqual('bar');
    // check the resource PUTs to the right URL
    $httpBackend.expectPUT(storeUrl).respond(200);
    result[1].policies[0]
      .$save()
      .then(angular.noop(), getUnexpectedErrorHandler());
    $httpBackend.flush();
    // check store refresh() updates the resource
    $httpBackend
      .expectGET(storeUrl)
      .respond([{ name: 'Second Policy - Updated' }]);
    result[1].store.refresh().then(angular.noop(), getUnexpectedErrorHandler());
    $httpBackend.flush();
    expect(result[1].policies[0].name).toBe('Second Policy - Updated');
  }));

  it('get() reloads in error state', inject(function ($httpBackend) {
    // first, initialise the store
    store.get().then(angular.noop(), getUnexpectedErrorHandler());
    $httpBackend.expectGET(storeUrl).respond(storeResult);
    $httpBackend.flush();

    // then, throw it into error state
    store.refresh().then(angular.noop(), function (error) {
      expect(error).toBeDefined();
    });
    $httpBackend.expectGET(storeUrl).respond(500);
    $httpBackend.flush();

    // calling get() in error state causes a reload
    store.get().then(function (data) {
      expect(data).toBeDefined();
    }, getUnexpectedErrorHandler());
    $httpBackend.expectGET(storeUrl).respond(storeResult);
    $httpBackend.flush();

    // calling get() in ok state doesn't cause a reload
    store.get().then(function (data) {
      expect(data).toBeDefined();
    }, getUnexpectedErrorHandler());
    $httpBackend.verifyNoOutstandingRequest();
  }));

  it('refresh() reloads whether in error state or not', inject(function (
    $httpBackend
  ) {
    // first, fail the request so we're in error state
    store.get().then(angular.noop(), function (error) {
      expect(error).toBeDefined();
    });
    $httpBackend.expectGET(storeUrl).respond(500);
    $httpBackend.flush();

    // calling refresh() causes a reload
    store.refresh().then(function (data) {
      expect(data).toBeDefined();
    }, getUnexpectedErrorHandler());
    $httpBackend.expectGET(storeUrl).respond(storeResult);
    $httpBackend.flush();

    // if get() doesn't cause a reload, we're not in error state
    store.get().then(function (data) {
      expect(data).toBeDefined();
    }, getUnexpectedErrorHandler());
    $httpBackend.verifyNoOutstandingRequest();

    // calling refresh() causes a reload
    store.refresh().then(function (data) {
      expect(data).toBeDefined();
    }, getUnexpectedErrorHandler());
    $httpBackend.expectGET(storeUrl).respond(storeResult);
    $httpBackend.flush();
  }));

  describe('getById', function () {
    beforeEach(inject(function ($httpBackend) {
      $httpBackend.expectGET(storeUrl).respond(storeResult);
    }));

    describe('Store already loaded', function () {
      beforeEach(inject(function ($httpBackend) {
        store.get();
        $httpBackend.flush();
      }));

      it('existing entity is found', inject(function ($httpBackend, $timeout) {
        var result = null;

        store.getById('1').then(function (entity) {
          result = entity;
        });
        $timeout.flush();
        expect(result.id).toEqual('1');
      }));

      describe('missing entity', function () {
        it('is found after reload', inject(function ($httpBackend) {
          var result = null;

          store.getById('xxx').then(function (entity) {
            result = entity;
          });
          $httpBackend.expectGET(storeUrl).respond({
            policiesByOwner: [
              {
                ownerId: 'foo',
                ownerName: 'Foo',
                ownerType: 'organization',
                policies: [{ id: 'xxx', name: 'First Policy', ownerId: 'foo' }],
              },
            ],
          });
          $httpBackend.flush();

          expect(result.id).toEqual('xxx');
        }));

        it('is still missing after reload and results in error', inject(function (
          $httpBackend
        ) {
          var result = null;

          store.getById('xxx').then(angular.noop, function (error) {
            result = error;
          });
          $httpBackend.expectGET(storeUrl).respond(storeResult);
          $httpBackend.flush();

          expect(result).toEqual('Could not find an policy with ID xxx.');
        }));
      });
    });

    function nonReloadingTest() {
      it('exists', inject(function ($httpBackend, $timeout) {
        var result;
        store.getById('1').then(function (entity) {
          result = entity;
        });
        $httpBackend.flush();
        $timeout.flush();
        expect(result.id).toEqual('1');
      }));

      it('missing', inject(function ($httpBackend, $timeout) {
        var result;
        store.getById('xxx').then(angular.noop, function (error) {
          result = error;
        });
        $httpBackend.flush();
        $timeout.flush();

        expect(result).toEqual('Could not find an policy with ID xxx.');
      }));
    }

    describe('Store not loaded', nonReloadingTest);

    describe('Store load in progress', function () {
      beforeEach(function () {
        store.get();
      });

      nonReloadingTest();
    });
  });
});
