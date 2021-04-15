/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import routeStateUtilService from '../../../../main/frontend/utility/services/routeStateUtilService';

describe('routeStateUtilService', function () {
  let mockCurrentState,
    getService = () => routeStateUtilService({ current: mockCurrentState });

  beforeEach(function () {
    mockCurrentState = {};
  });

  describe('stateRequiresAuthentication', function () {
    describe('when not passed a parameter', function () {
      it('returns true when the current state has no data property', function () {
        expect(getService().stateRequiresAuthentication()).toBe(true);
      });

      it('returns true when the current state has a data property with no authenticationRequired property', function () {
        mockCurrentState = { data: {} };
        expect(getService().stateRequiresAuthentication()).toBe(true);
      });

      it('returns true when the current state has a data property with authenticationRequired set to true', function () {
        mockCurrentState = { data: { authenticationRequired: true } };
        expect(getService().stateRequiresAuthentication()).toBe(true);
      });

      it('returns false when the current state has a data property with authenticationRequired set to false', function () {
        mockCurrentState = { data: { authenticationRequired: false } };
        expect(getService().stateRequiresAuthentication()).toBe(false);
      });
    });

    describe('when passed a parameter', function () {
      it('returns true when the passed state has no data property', function () {
        expect(getService().stateRequiresAuthentication({})).toBe(true);

        // current state has no effect
        mockCurrentState = { data: { authenticationRequired: false } };
        expect(getService().stateRequiresAuthentication({})).toBe(true);
      });

      it('returns true when the passed state has a data property with no authenticationRequired property', function () {
        expect(getService().stateRequiresAuthentication({ data: {} })).toBe(true);

        // current state has no effect
        mockCurrentState = { data: { authenticationRequired: false } };
        expect(getService().stateRequiresAuthentication({ data: {} })).toBe(true);
      });

      it('returns true when the passed state has a data property with authenticationRequired set to true', function () {
        expect(
          getService().stateRequiresAuthentication({
            data: { authenticationRequired: true },
          })
        ).toBe(true);

        // current state has no effect
        mockCurrentState = { data: { authenticationRequired: false } };
        expect(
          getService().stateRequiresAuthentication({
            data: { authenticationRequired: true },
          })
        ).toBe(true);
      });

      it('returns false when the passed state has a data property with authenticationRequired set to false', function () {
        expect(
          getService().stateRequiresAuthentication({
            data: { authenticationRequired: false },
          })
        ).toBe(false);

        // current state has no effect
        mockCurrentState = { data: { authenticationRequired: true } };
        expect(
          getService().stateRequiresAuthentication({
            data: { authenticationRequired: false },
          })
        ).toBe(false);
      });
    });
  });
});
