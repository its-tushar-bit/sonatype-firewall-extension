/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Messages } from '../../../main/frontend/util/CommonServices';

describe('CommonServices', () => {
  describe('Messages.getHttpErrorMessage', function () {
    describe('when provided argument is a string', () => {
      it('uses provided string', () => {
        expect(Messages.getHttpErrorMessage('Provided message')).toEqual('Provided message');
      });
    });

    describe('when provided argument is a response object', () => {
      it('uses generic message if response status is less than 0', () => {
        expect(Messages.getHttpErrorMessage({ data: 'Bogus String', status: -1 })).toEqual(
          'Unable to reach Sonatype IQ Server'
        );
      });

      it('uses generic message if response status is 0', () => {
        expect(Messages.getHttpErrorMessage({ data: 'Bogus String', status: 0 })).toEqual(
          'Unable to reach Sonatype IQ Server'
        );
      });

      it('uses generic message if response status is 1000', () => {
        expect(Messages.getHttpErrorMessage({ data: 'Bogus String', status: 1000 })).toEqual(
          'Unable to reach Sonatype IQ Server'
        );
      });

      it('uses generic message if response status is greater than 1000', () => {
        expect(Messages.getHttpErrorMessage({ data: 'Bogus String', status: 1001 })).toEqual(
          'Unable to reach Sonatype IQ Server'
        );
      });

      it('uses data property if content-type is not text/html', () => {
        expect(Messages.getHttpErrorMessage({ data: 'Internal Error', status: 999 })).toEqual('Internal Error');
      });

      it('uses canned status messages if content-type is text/html', () => {
        const data = '<html>Error</html>';
        const headers = () => ({ 'content-type': 'text/html' });

        expect(Messages.getHttpErrorMessage({ data, status: 502, headers })).toEqual('Bad Gateway');
        expect(Messages.getHttpErrorMessage({ data, status: 503, headers })).toEqual('Service Unavailable');
        expect(Messages.getHttpErrorMessage({ data, status: 504, headers })).toEqual('Gateway Timeout');
        expect(Messages.getHttpErrorMessage({ data, status: 999, headers })).toEqual('Error 999');
      });

      it('uses canned status messages if data property is empty', () => {
        expect(Messages.getHttpErrorMessage({ data: '', status: 502 })).toEqual('Bad Gateway');
        expect(Messages.getHttpErrorMessage({ data: '', status: 503 })).toEqual('Service Unavailable');
        expect(Messages.getHttpErrorMessage({ data: '', status: 504 })).toEqual('Gateway Timeout');
        expect(Messages.getHttpErrorMessage({ data: '', status: 999 })).toEqual('Error 999');
      });

      it('handles axios headers property', () => {
        expect(
          Messages.getHttpErrorMessage({
            headers: { 'content-type': 'text/html' },
            status: 503,
          })
        ).toEqual('Service Unavailable');
      });
    });

    describe('when provided argument is an object with a response property', () => {
      it('uses generic message if response status is less than 0', () => {
        expect(
          Messages.getHttpErrorMessage({
            response: { data: 'Bogus String', status: -1 },
          })
        ).toEqual('Unable to reach Sonatype IQ Server');
      });

      it('uses generic message if response status is 0', () => {
        expect(
          Messages.getHttpErrorMessage({
            response: { data: 'Bogus String', status: 0 },
          })
        ).toEqual('Unable to reach Sonatype IQ Server');
      });

      it('uses generic message if response status is 1000', () => {
        expect(
          Messages.getHttpErrorMessage({
            response: { data: 'Bogus String', status: 1000 },
          })
        ).toEqual('Unable to reach Sonatype IQ Server');
      });

      it('uses generic message if response status is greater than 1000', () => {
        expect(
          Messages.getHttpErrorMessage({
            response: { data: 'Bogus String', status: 1001 },
          })
        ).toEqual('Unable to reach Sonatype IQ Server');
      });

      it('uses data property if content-type is not text/html', () => {
        expect(
          Messages.getHttpErrorMessage({
            response: { data: 'Internal Error', status: 999 },
          })
        ).toEqual('Internal Error');
      });

      it('uses property.message if data property is an object', () => {
        expect(
          Messages.getHttpErrorMessage({
            response: { data: { server: 'jersey', message: 'Invalid Request', status: 400 }, status: 400 },
          })
        ).toEqual('Invalid Request');
      });

      it('uses generic message if data property does not contain message', () => {
        expect(
          Messages.getHttpErrorMessage({
            response: { data: { server: 'jersey', info: 'Some Error' }, status: 999 },
          })
        ).toEqual('Error');
      });

      it('uses canned status messages if content-type is text/html', () => {
        const data = '<html>Error</html>';
        const headers = () => ({ 'content-type': 'text/html' });

        expect(
          Messages.getHttpErrorMessage({
            response: { data, status: 502, headers },
          })
        ).toEqual('Bad Gateway');
        expect(
          Messages.getHttpErrorMessage({
            response: { data, status: 503, headers },
          })
        ).toEqual('Service Unavailable');
        expect(
          Messages.getHttpErrorMessage({
            response: { data, status: 504, headers },
          })
        ).toEqual('Gateway Timeout');
        expect(
          Messages.getHttpErrorMessage({
            response: { data, status: 999, headers },
          })
        ).toEqual('Error 999');
      });

      it('uses canned status messages if data property is empty', () => {
        expect(Messages.getHttpErrorMessage({ response: { data: '', status: 502 } })).toEqual('Bad Gateway');
        expect(Messages.getHttpErrorMessage({ response: { data: '', status: 503 } })).toEqual('Service Unavailable');
        expect(Messages.getHttpErrorMessage({ response: { data: '', status: 504 } })).toEqual('Gateway Timeout');
        expect(Messages.getHttpErrorMessage({ response: { data: '', status: 999 } })).toEqual('Error 999');
      });

      it('handles axios headers property', () => {
        expect(
          Messages.getHttpErrorMessage({
            response: {
              headers: { 'content-type': 'text/html' },
              status: 503,
            },
          })
        ).toEqual('Service Unavailable');
      });
    });

    describe('when provided argument is an array', () => {
      it('uses generic message if provided status is less than 0', () => {
        expect(Messages.getHttpErrorMessage(['Bogus String', -1, null, null])).toEqual(
          'Unable to reach Sonatype IQ Server'
        );
      });

      it('uses generic message if provided status is 0', () => {
        expect(Messages.getHttpErrorMessage(['Bogus String', 0, null, null])).toEqual(
          'Unable to reach Sonatype IQ Server'
        );
      });

      it('uses generic message if provided status is 1000', () => {
        expect(Messages.getHttpErrorMessage(['Bogus String', 1000, null, null])).toEqual(
          'Unable to reach Sonatype IQ Server'
        );
      });

      it('uses generic message if provided status is greater than 1000', () => {
        expect(Messages.getHttpErrorMessage(['Bogus String', 1001, null, null])).toEqual(
          'Unable to reach Sonatype IQ Server'
        );
      });

      it('uses provided message if content-type is not text/html', () => {
        expect(Messages.getHttpErrorMessage(['Internal Error', 999, null, null])).toEqual('Internal Error');
      });

      it('uses canned status messages if content-type is text/html', () => {
        const data = '<html>Error</html>';
        const headers = () => ({ 'content-type': 'text/html' });

        expect(Messages.getHttpErrorMessage([data, 502, headers])).toEqual('Bad Gateway');
        expect(Messages.getHttpErrorMessage([data, 503, headers])).toEqual('Service Unavailable');
        expect(Messages.getHttpErrorMessage([data, 504, headers])).toEqual('Gateway Timeout');
        expect(Messages.getHttpErrorMessage([data, 999, headers])).toEqual('Error 999');
      });

      it('uses canned status messages if data property is empty', () => {
        expect(Messages.getHttpErrorMessage(['', 502, null, null])).toEqual('Bad Gateway');
        expect(Messages.getHttpErrorMessage(['', 503, null, null])).toEqual('Service Unavailable');
        expect(Messages.getHttpErrorMessage(['', 504, null, null])).toEqual('Gateway Timeout');
        expect(Messages.getHttpErrorMessage(['', 999, null, null])).toEqual('Error 999');
      });
    });
  });
});
