/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  submitData,
  DEPARTED_ACTION,
} from '../../../../main/frontend/configuration/gettingStarted/gettingStartedTelemetryServiceHelper';
import * as telemetryUtils from '../../../../main/frontend/util/telemetryUtils';

describe('gettingStartedTelemetryServiceHelper', function () {
  beforeEach(() => {
    jest.spyOn(telemetryUtils, 'submitTelemetryData');
  });

  describe('submitData', function () {
    it('calls submitTelemetryData with expected attributes', function () {
      submitData(
        DEPARTED_ACTION,
        {
          foo: 'fooz',
          bar: 'barz',
        },
        'somePrevState'
      );

      expect(telemetryUtils.submitTelemetryData).toHaveBeenCalledWith('GETTING_STARTED_USAGE', {
        action: 'DEPARTED',
        pageNavigatedFrom: 'systemMenu',
        foo: 'fooz',
        bar: 'barz',
      });
    });

    it('sets pageNavigatedFrom attribute to empty string if prevState is empty', function () {
      submitData(
        DEPARTED_ACTION,
        {
          foo: 'bar',
        },
        undefined
      );

      expect(telemetryUtils.submitTelemetryData).toHaveBeenCalledWith('GETTING_STARTED_USAGE', {
        action: 'DEPARTED',
        pageNavigatedFrom: '',
        foo: 'bar',
      });
    });

    it('send action as undefined if action is empty', function () {
      submitData(
        undefined,
        {
          href: '/',
          pageNavigatedFrom: '',
        },
        undefined
      );

      expect(telemetryUtils.submitTelemetryData).toHaveBeenCalledWith('GETTING_STARTED_USAGE', {
        action: undefined,
        pageNavigatedFrom: '',
        href: '/',
      });
    });

    it('send only action and pageNavigatedFrom if attrs is empty', function () {
      submitData(DEPARTED_ACTION, undefined, 'somePrevState');

      expect(telemetryUtils.submitTelemetryData).toHaveBeenCalledWith('GETTING_STARTED_USAGE', {
        action: 'DEPARTED',
        pageNavigatedFrom: 'systemMenu',
      });
    });
  });
});
