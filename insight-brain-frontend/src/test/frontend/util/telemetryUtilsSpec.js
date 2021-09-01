/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { submitTelemetryData } from '../../../main/frontend/util/telemetryUtils';
import { getTelemetryUrl } from '../../../main/frontend/util/CLMLocation';

describe('telemetryUtils', function () {
  let cookies, xsrfHeaderName;
  beforeEach(() => {
    cookies = jasmine.createSpyObj('cookies', ['get']);
    xsrfHeaderName = 'X-CSRF-TOKEN';
  });

  describe('submitData', function () {
    it('submits proper json asynchronously by default', function () {
      spyOn(XMLHttpRequest.prototype, 'open');
      spyOn(XMLHttpRequest.prototype, 'setRequestHeader');
      spyOn(XMLHttpRequest.prototype, 'send');
      cookies.get.and.returnValue('xsrfCookieTestValue');

      submitTelemetryData('test_purpose', {
        testAttribute1: 'testAttr1',
        testAttribute2: 'testAttr2',
      });

      expect(XMLHttpRequest.prototype.open).toHaveBeenCalledWith('POST', getTelemetryUrl(), true);
      expect(XMLHttpRequest.prototype.setRequestHeader).toHaveBeenCalledWith('Content-Type', 'application/json');
      expect(XMLHttpRequest.prototype.setRequestHeader).toHaveBeenCalledWith(xsrfHeaderName, 'csrfToken');

      const telemetryData = JSON.parse(XMLHttpRequest.prototype.send.calls.argsFor(0));
      expect(telemetryData).toEqual(
        jasmine.objectContaining({
          purpose: 'test_purpose',
          timestamp: jasmine.any(Number),
          attributes: {
            testAttribute1: 'testAttr1',
            testAttribute2: 'testAttr2',
          },
        })
      );
    });

    it('submits data synchronously when provided sync flag is true', function () {
      spyOn(XMLHttpRequest.prototype, 'open').and.callThrough();
      spyOn(XMLHttpRequest.prototype, 'send');
      submitTelemetryData('test_purpose', null, true);
      expect(XMLHttpRequest.prototype.open).toHaveBeenCalledWith('POST', getTelemetryUrl(), false);
    });

    it('submits data asynchronously when provided sync flag is not true but truthy', function () {
      spyOn(XMLHttpRequest.prototype, 'open').and.callThrough();
      spyOn(XMLHttpRequest.prototype, 'send');
      submitTelemetryData('test_purpose', null, 1);
      expect(XMLHttpRequest.prototype.open).toHaveBeenCalledWith('POST', getTelemetryUrl(), true);
    });
  });
});
