/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { submitTelemetryData } from '../../../main/frontend/util/telemetryUtils';
import { getTelemetryUrl } from '../../../main/frontend/util/CLMLocation';

describe('telemetryUtils', function () {
  describe('submitData', function () {
    it('submits proper json asynchronously by default', function () {
      jest.spyOn(XMLHttpRequest.prototype, 'open');
      jest.spyOn(XMLHttpRequest.prototype, 'setRequestHeader');
      jest.spyOn(XMLHttpRequest.prototype, 'send');
      document.cookie = 'CLM-CSRF-TOKEN=csrfToken';

      submitTelemetryData('test_purpose', {
        testAttribute1: 'testAttr1',
        testAttribute2: 'testAttr2',
      });

      expect(XMLHttpRequest.prototype.open).toHaveBeenCalledWith('POST', getTelemetryUrl(), true);
      expect(XMLHttpRequest.prototype.setRequestHeader).toHaveBeenCalledWith('Content-Type', 'application/json');
      expect(XMLHttpRequest.prototype.setRequestHeader).toHaveBeenCalledWith('X-CSRF-TOKEN', 'csrfToken');

      const telemetryData = JSON.parse(XMLHttpRequest.prototype.send.mock.calls[0][0]);
      expect(telemetryData).toEqual(
        expect.objectContaining({
          purpose: 'test_purpose',
          timestamp: expect.any(Number),
          attributes: {
            testAttribute1: 'testAttr1',
            testAttribute2: 'testAttr2',
          },
        })
      );
    });

    it('submits data synchronously when provided sync flag is true', function () {
      jest.spyOn(XMLHttpRequest.prototype, 'open');
      jest.spyOn(XMLHttpRequest.prototype, 'send').mockImplementation(() => {});

      submitTelemetryData('test_purpose', null, true);

      expect(XMLHttpRequest.prototype.open).toHaveBeenCalledWith('POST', getTelemetryUrl(), false);
    });

    it('submits data asynchronously when provided sync flag is not true but truthy', function () {
      jest.spyOn(XMLHttpRequest.prototype, 'open');
      jest.spyOn(XMLHttpRequest.prototype, 'send');

      submitTelemetryData('test_purpose', null, 1);

      expect(XMLHttpRequest.prototype.open).toHaveBeenCalledWith('POST', getTelemetryUrl(), true);
    });
  });
});
