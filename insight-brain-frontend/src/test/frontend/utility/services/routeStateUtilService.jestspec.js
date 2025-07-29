/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { initialize, stateRequiresAuthentication } from 'MainRoot/utility/services/routeStateUtilService';

describe('routeStateUtilService', function () {
  let mockReduxStore, mockRouteState;

  beforeEach(function () {
    mockRouteState = { current: {} };
    mockReduxStore = {
      getState() {
        return {
          userLogin: {
            loginModalState: {
              isUnauthenticatedPagesEnabled: true,
              isQuarantinedComponentViewAnonymousAccessEnabled: true,
            },
          },
        };
      },
    };
    mockReduxStore.dispatch = jest
      .fn()
      .mockName('dispatch')
      .mockReturnValue(Promise.resolve({ payload: true }));

    // Initialize the ES6 module with mock dependencies
    initialize(mockRouteState, mockReduxStore);
  });

  describe('stateRequiresAuthentication', function () {
    describe('when state.data is backend-configurable', function () {
      it('returns false when isUnauthenticatedPagesEnabled is true', function () {
        return stateRequiresAuthentication({ data: { authenticationRequired: 'backend-configurable' } }).then(
          (stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(false);
          }
        );
      });

      it('returns true when isUnauthenticatedPagesEnabled is false', function () {
        let customMockReduxStore = {
          ...mockReduxStore,
          getState() {
            return {
              userLogin: {
                loginModalState: {
                  isUnauthenticatedPagesEnabled: false,
                },
              },
            };
          },
        };
        customMockReduxStore.dispatch = jest
          .fn()
          .mockName('dispatch')
          .mockReturnValue(Promise.resolve({ payload: false }));

        // Re-initialize with custom mock
        initialize({ current: {} }, customMockReduxStore);

        return stateRequiresAuthentication({ data: { authenticationRequired: 'backend-configurable' } }).then(
          (stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(true);
          }
        );
      });
    });

    describe('when not passed a parameter', function () {
      it('returns true when the current state has no data property', function () {
        return stateRequiresAuthentication().then((stateRequiresAuthentication) => {
          expect(stateRequiresAuthentication).toBe(true);
        });
      });

      it('returns true when the current state has a data property with no authenticationRequired property', function () {
        mockRouteState = { current: { data: {} } };
        initialize(mockRouteState, mockReduxStore);
        return stateRequiresAuthentication().then((stateRequiresAuthentication) => {
          expect(stateRequiresAuthentication).toBe(true);
        });
      });

      it('returns true when the current state has a data property with authenticationRequired set to true', function () {
        mockRouteState = { current: { data: { authenticationRequired: true } } };
        initialize(mockRouteState, mockReduxStore);
        return stateRequiresAuthentication().then((stateRequiresAuthentication) => {
          expect(stateRequiresAuthentication).toBe(true);
        });
      });

      it('returns false when the current state has a data property with authenticationRequired set to false', function () {
        mockRouteState = { current: { data: { authenticationRequired: false } } };
        initialize(mockRouteState, mockReduxStore);
        return stateRequiresAuthentication().then((stateRequiresAuthentication) => {
          expect(stateRequiresAuthentication).toBe(false);
        });
      });
    });

    describe('when passed a parameter', function () {
      it('returns true when the passed state has no data property', function () {
        return stateRequiresAuthentication({}).then((stateRequiresAuthentication) => {
          expect(stateRequiresAuthentication).toBe(true);
        });
      });

      it('returns true when the passed state has property true and current state has no effect', function () {
        // current state has no effect
        mockRouteState = { current: { data: { authenticationRequired: false } } };
        initialize(mockRouteState, mockReduxStore);
        return stateRequiresAuthentication({ data: { authenticationRequired: true } }).then(
          (stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(true);
          }
        );
      });

      it('returns true when the passed state has a data property with no authenticationRequired property', function () {
        return stateRequiresAuthentication({}).then((stateRequiresAuthentication) => {
          expect(stateRequiresAuthentication).toBe(true);
        });
      });

      it('returns true when the passed state has a data property true and current state has no effect', function () {
        // current state has no effect
        mockRouteState = { current: { data: { authenticationRequired: false } } };
        initialize(mockRouteState, mockReduxStore);
        return stateRequiresAuthentication({ data: { authenticationRequired: true } }).then(
          (stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(true);
          }
        );
      });

      it('returns true when the passed state has a data property with authenticationRequired set to true', function () {
        return stateRequiresAuthentication({
          data: { authenticationRequired: true },
        }).then((stateRequiresAuthentication) => {
          expect(stateRequiresAuthentication).toBe(true);
        });
      });

      it('returns false when the passed state has a data property with authenticationRequired set to false', function () {
        // current state has no effect
        mockRouteState = { current: { data: { authenticationRequired: true } } };
        initialize(mockRouteState, mockReduxStore);
        return stateRequiresAuthentication({
          data: { authenticationRequired: false },
        }).then((stateRequiresAuthentication) => {
          expect(stateRequiresAuthentication).toBe(false);
        });
      });

      it('returns false when the passed state has a data property with authenticationRequired set to false', function () {
        return stateRequiresAuthentication({
          data: { authenticationRequired: false },
        }).then((stateRequiresAuthentication) => {
          expect(stateRequiresAuthentication).toBe(false);
        });
      });

      it('returns false when the passed state has a data property with authenticationRequired set to false and current state has no effect', function () {
        // current state has no effect
        mockRouteState = { current: { data: { authenticationRequired: true } } };
        initialize(mockRouteState, mockReduxStore);
        return stateRequiresAuthentication({
          data: { authenticationRequired: false },
        }).then((stateRequiresAuthentication) => {
          expect(stateRequiresAuthentication).toBe(false);
        });
      });
    });
  });
});
