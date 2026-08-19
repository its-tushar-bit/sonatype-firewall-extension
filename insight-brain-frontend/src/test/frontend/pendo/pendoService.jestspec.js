/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import PendoService from 'MainRoot/pendo/PendoService';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';

describe('PendoService', function () {
  let axiosMock, sanitizeUrlService, pendoService;

  beforeAll(function () {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(function () {
    // pendo expects there to be at least one script element in the document
    document.body.appendChild(document.createElement('script'));

    window.pendo = { initialize: jest.fn() };
    sanitizeUrlService = { sanitize: jest.fn() };
    pendoService = new PendoService(sanitizeUrlService);
  });

  it('initializes pendo when start is called', async function () {
    axiosMock.onGet('/rest/user-telemetry/config').reply(200, { visitors: {}, account: {} });

    pendoService.start();

    // wait for the async axios call to complete
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(window.pendo.initialize).toHaveBeenCalledWith({
      account: {},
      visitors: {},
      excludeAllText: true,
      excludeTitle: true,
      guides: {
        disabled: true,
      },
      contentHost: '/rest/user-telemetry/events',
      dataHost: '/rest/user-telemetry/events',
      sanitizeUrl: expect.any(Function),
    });
  });

  describe('flush', function () {
    it('calls pendo.flushNow if it is defined', async function () {
      let resolveRetval;
      const flushRetval = new Promise((resolve) => {
        resolveRetval = resolve;
      });

      window.pendo.flushNow = jest.fn().mockName('flushNow').mockReturnValue(flushRetval);

      const returnedPromise = pendoService.flush();

      expect(window.pendo.flushNow).toHaveBeenCalled();

      resolveRetval('result');
      const retval = await returnedPromise;
      expect(retval).toBe('result');
    });

    it('returns a resolved promise if pendo.flushNow is not defined', async function () {
      await pendoService.flush();
      // if we get here the test was successful
    });
  });
});
