/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import stores from '../../../main/frontend/util/Stores';

describe('Stores', function () {
  var getUrlSpy, cachedStore;

  beforeEach(
    angular.mock.module(stores.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  describe('CachedStore', function () {
    beforeEach(inject(function (CachedStore) {
      getUrlSpy = jasmine.createSpy('getUrl').and.returnValue('http://foo.bar');
      var template = {
        getUrl: getUrlSpy,
        template: { id: null },
      };
      cachedStore = CachedStore.get(template);
    }));

    afterEach(inject(function ($httpBackend) {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    }));

    it('Creates new store', inject(function ($httpBackend) {
      var data;
      $httpBackend.expectGET('http://foo.bar').respond([{ id: 'qux' }]);
      cachedStore.get().then(function (response) {
        data = response;
      });
      $httpBackend.flush();

      expect(getUrlSpy).toHaveBeenCalled();
      expect(data).not.toBeUndefined();
      expect(data.length).toBe(1);
      expect(data[0].id).toBe('qux');
    }));

    it('Returns cached store', inject(function ($httpBackend, $rootScope) {
      $httpBackend.expectGET('http://foo.bar').respond([{ id: 'qux' }]);
      cachedStore.get();
      $httpBackend.flush();

      expect(getUrlSpy.calls.count()).toBe(1);

      var data;
      cachedStore.get().then(function (response) {
        data = response;
      });

      $rootScope.$digest();
      expect(getUrlSpy.calls.count()).toBe(1);
      expect(data).not.toBeUndefined();
      expect(data.length).toBe(1);
      expect(data[0].id).toBe('qux');
    }));
  });
});
