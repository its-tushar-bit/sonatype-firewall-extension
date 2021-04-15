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

  describe('Stage stores', function () {
    let StageTypeStore, $ngRedux, $rootScope;

    beforeEach(inject(function (_StageTypeStore_, _$ngRedux_, _$rootScope_) {
      StageTypeStore = _StageTypeStore_;
      $ngRedux = _$ngRedux_;
      $rootScope = _$rootScope_;
    }));

    // see CLM-6352
    function createTests(storeMethod, purpose) {
      describe(storeMethod, function () {
        describe('when the stages are already loaded', function () {
          it('returns a resolved promise with the already-loaded info', function (done) {
            const stageInfo = [
              { stageTypeId: 'build', stageName: 'Build' },
              { stageTypeId: 'stage-release', stageName: 'Stage Release' },
            ];

            $ngRedux.dispatch = jasmine.createSpy('dispatch');
            $ngRedux.getState = jasmine.createSpy('getState').and.returnValue({
              stages: {
                [purpose]: {
                  stageTypes: stageInfo,
                },
              },
            });

            StageTypeStore[storeMethod]().then(function (result) {
              expect(result.length).toBe(2);
              expect(result[0].stageTypeId).toBe('build');
              expect(result[0].stageName).toBe('Build');
              expect(result[1].stageTypeId).toBe('stage-release');
              expect(result[1].stageName).toBe('Stage Release');

              done();
            });

            $rootScope.$digest();
          });

          it('returns a fresh deep copy of the objects each time it is called', function (done) {
            const stageInfo = [
              { stageTypeId: 'build', stageName: 'Build' },
              { stageTypeId: 'stage-release', stageName: 'Stage Release' },
            ];

            $ngRedux.dispatch = jasmine.createSpy('dispatch');
            $ngRedux.getState = jasmine.createSpy('getState').and.returnValue({
              stages: {
                [purpose]: {
                  stageTypes: stageInfo,
                },
              },
            });

            StageTypeStore[storeMethod]().then(function (result) {
              expect(result).not.toBe(stageInfo);
              expect(result[0]).not.toBe(stageInfo[0]);
              expect(result[1]).not.toBe(stageInfo[1]);

              done();
            });

            $rootScope.$digest();
          });
        });

        describe('when the stages are not already loaded', function () {
          it('tries to fetch them and resolves the promise if successful', function (done) {
            const stageInfo = [
                { stageTypeId: 'build', stageName: 'Build' },
                { stageTypeId: 'stage-release', stageName: 'Stage Release' },
              ],
              unsubscribeSpy = jasmine.createSpy('unsubscribe');

            $ngRedux.dispatch = jasmine.createSpy('dispatch');

            // empty at first, but has data after the dispatch
            $ngRedux.getState = jasmine.createSpy('getState').and.returnValues(
              {
                stages: {
                  [purpose]: {
                    stageTypes: null,
                  },
                },
              },
              {
                stages: {
                  [purpose]: {
                    stageTypes: stageInfo,
                  },
                },
              }
            );

            let subscription;
            $ngRedux.subscribe = jasmine.createSpy('subscribe').and.callFake(function (fn) {
              subscription = fn;
              return unsubscribeSpy;
            });

            StageTypeStore[storeMethod]().then(function (result) {
              expect(unsubscribeSpy).toHaveBeenCalled();

              expect(result.length).toBe(2);
              expect(result[0].stageTypeId).toBe('build');
              expect(result[0].stageName).toBe('Build');
              expect(result[1].stageTypeId).toBe('stage-release');
              expect(result[1].stageName).toBe('Stage Release');

              done();
            });

            // first immediate call, which does not resolve the promise
            expect($ngRedux.getState).toHaveBeenCalledTimes(1);
            expect($ngRedux.subscribe).toHaveBeenCalled();
            expect(unsubscribeSpy).not.toHaveBeenCalled();
            expect($ngRedux.dispatch).toHaveBeenCalled();

            // mock the update to the store
            subscription();

            expect($ngRedux.getState).toHaveBeenCalledTimes(2);

            // at this point the promise should resolve, executing the `then` callback above and done()
            $rootScope.$digest();
          });

          it('returns a fresh deep copy of the objects each time it is called', function (done) {
            const stageInfo = [
                { stageTypeId: 'build', stageName: 'Build' },
                { stageTypeId: 'stage-release', stageName: 'Stage Release' },
              ],
              unsubscribeSpy = jasmine.createSpy('unsubscribe');

            $ngRedux.dispatch = jasmine.createSpy('dispatch');

            // empty at first, but has data after the dispatch
            $ngRedux.getState = jasmine.createSpy('getState').and.returnValues(
              {
                stages: {
                  [purpose]: {
                    stageTypes: null,
                  },
                },
              },
              {
                stages: {
                  [purpose]: {
                    stageTypes: stageInfo,
                  },
                },
              }
            );

            let subscription;
            $ngRedux.subscribe = jasmine.createSpy('subscribe').and.callFake(function (fn) {
              subscription = fn;
              return unsubscribeSpy;
            });

            StageTypeStore[storeMethod]().then(function (result) {
              expect(result).not.toBe(stageInfo);
              expect(result[0]).not.toBe(stageInfo[0]);
              expect(result[1]).not.toBe(stageInfo[1]);

              done();
            });

            // mock the update to the store
            subscription();

            // at this point the promise should resolve, executing the `then` callback above and done()
            $rootScope.$digest();
          });

          it('rejects the promise if the fetch fails', function (done) {
            const error = 'errrrrrr',
              unsubscribeSpy = jasmine.createSpy('unsubscribe');

            $ngRedux.dispatch = jasmine.createSpy('dispatch');

            // empty at first, but has data after the dispatch
            $ngRedux.getState = jasmine.createSpy('getState').and.returnValues(
              {
                stages: {
                  [purpose]: {
                    stageTypes: null,
                  },
                },
              },
              {
                stages: {
                  [purpose]: {
                    stageTypes: null,
                    error,
                  },
                },
              }
            );

            let subscription;
            $ngRedux.subscribe = jasmine.createSpy('subscribe').and.callFake(function (fn) {
              subscription = fn;
              return unsubscribeSpy;
            });

            StageTypeStore[storeMethod]().then(
              () => {},
              function (err) {
                expect(unsubscribeSpy).toHaveBeenCalled();

                expect(err).toBe(error);

                done();
              }
            );

            // first immediate call, which does not resolve the promise
            expect($ngRedux.getState).toHaveBeenCalledTimes(1);
            expect($ngRedux.subscribe).toHaveBeenCalled();
            expect(unsubscribeSpy).not.toHaveBeenCalled();
            expect($ngRedux.dispatch).toHaveBeenCalled();

            // mock the update to the store
            subscription();

            expect($ngRedux.getState).toHaveBeenCalledTimes(2);

            // at this point the promise should resolve, executing the `then` callback above and done()
            $rootScope.$digest();
          });
        });
      });
    }

    createTests('get', 'cli');
    createTests('getActionStages', 'action');
    createTests('getDashboardStages', 'dashboard');
  });
});
