/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import routeStateUtilService from 'MainRoot/utility/services/routeStateUtilService';

describe('routeStateUtilService', function () {
  let mockCurrentState,
    mockNgRedux,
    getService = () => routeStateUtilService({ current: mockCurrentState }, mockNgRedux);

  beforeEach(function () {
    mockCurrentState = {};
    mockNgRedux = {
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
    mockNgRedux.dispatch = jasmine.createSpy('dispatch').and.callFake(() => Promise.resolve({ payload: true }));
  });

  describe('stateRequiresAuthentication', function () {
    describe('when state.data is backend-configurable', function () {
      it('returns false when isUnauthenticatedPagesEnabled is true', function () {
        getService()
          .stateRequiresAuthentication({ data: { authenticationRequired: 'backend-configurable' } })
          .then((stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(false);
          });
      });

      it('returns true when isUnauthenticatedPagesEnabled is false', function () {
        let customMockNgRedux = {
          ...mockNgRedux,
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
        mockNgRedux.dispatch = jasmine.createSpy('dispatch').and.callFake(() => Promise.resolve({ payload: false }));

        routeStateUtilService({ current: mockCurrentState }, customMockNgRedux)
          .stateRequiresAuthentication({ data: { authenticationRequired: 'backend-configurable' } })
          .then((stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(true);
          });
      });
    });

    describe('when not passed a parameter', function () {
      it('returns true when the current state has no data property', function () {
        getService()
          .stateRequiresAuthentication()
          .then((stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(true);
          });
      });

      it('returns true when the current state has a data property with no authenticationRequired property', function () {
        mockCurrentState = { data: {} };
        getService()
          .stateRequiresAuthentication()
          .then((stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(true);
          });
      });

      it('returns true when the current state has a data property with authenticationRequired set to true', function () {
        mockCurrentState = { data: { authenticationRequired: true } };
        getService()
          .stateRequiresAuthentication()
          .then((stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(true);
          });
      });

      it('returns false when the current state has a data property with authenticationRequired set to false', function () {
        mockCurrentState = { data: { authenticationRequired: false } };
        getService()
          .stateRequiresAuthentication()
          .then((stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(false);
          });
      });
    });

    describe('when passed a parameter', function () {
      it('returns true when the passed state has no data property', function () {
        getService()
          .stateRequiresAuthentication({})
          .then((stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(true);
          });
      });

      it('returns true when the passed state has property true and current state has no effect', function () {
        // current state has no effect
        mockCurrentState = { data: { authenticationRequired: false } };
        getService()
          .stateRequiresAuthentication({ data: { authenticationRequired: true } })
          .then((stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(true);
          });
      });

      it('returns true when the passed state has a data property with no authenticationRequired property', function () {
        getService()
          .stateRequiresAuthentication({})
          .then((stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(true);
          });
      });

      it('returns true when the passed state has a data property true and current state has no effect', function () {
        // current state has no effect
        mockCurrentState = { data: { authenticationRequired: false } };
        getService()
          .stateRequiresAuthentication({ data: { authenticationRequired: true } })
          .then((stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(true);
          });
      });

      it('returns true when the passed state has a data property with authenticationRequired set to true', function () {
        getService()
          .stateRequiresAuthentication({
            data: { authenticationRequired: true },
          })
          .then((stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(true);
          });
      });

      it('returns false when the passed state has a data property with authenticationRequired set to false', function () {
        // current state has no effect
        mockCurrentState = { data: { authenticationRequired: true } };
        getService()
          .stateRequiresAuthentication({
            data: { authenticationRequired: false },
          })
          .then((stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(false);
          });
      });

      it('returns false when the passed state has a data property with authenticationRequired set to false', function () {
        getService()
          .stateRequiresAuthentication({
            data: { authenticationRequired: false },
          })
          .then((stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(false);
          });
      });

      it('returns false when the passed state has a data property with authenticationRequired set to false and current state has no effect', function () {
        // current state has no effect
        mockCurrentState = { data: { authenticationRequired: true } };
        getService()
          .stateRequiresAuthentication({
            data: { authenticationRequired: false },
          })
          .then((stateRequiresAuthentication) => {
            expect(stateRequiresAuthentication).toBe(false);
          });
      });
    });
  });
});
