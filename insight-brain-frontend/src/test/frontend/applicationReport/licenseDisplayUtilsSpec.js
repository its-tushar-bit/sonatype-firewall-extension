import * as licenseDisplayUtils from '../../../main/frontend/applicationReport/licenseDisplayUtils';

describe('licenseDisplayUtils', function() {
  describe('getDeclaredLicensesDisplay', () => {
    it('returns Not Declared for empty declaredLicenses parameter', () => {
      const licenseObj = {
        declaredLicenses: []
      };
      expect(licenseDisplayUtils.getDeclaredLicensesDisplay(licenseObj)).toBe('Not Declared');
    });

    it('returns declared licenses if they are passed in on the object', () => {
      const licenseObj = {
        declaredLicenses: ['foo', 'bar', 'baz']
      };
      expect(licenseDisplayUtils.getDeclaredLicensesDisplay(licenseObj)).toBe('foo, bar, baz');
    });

    it('it includes licenses that are in both declared and observed licenses in declared licenses', () => {
      const licenseObj = {
        declaredLicenses: ['bar'],
        observedLicenses: ['foo', 'bar', 'baz']
      };
      expect(licenseDisplayUtils.getDeclaredLicensesDisplay(licenseObj)).toBe('bar');
    });
  });

  describe('getObservedLicensesDisplay', () => {
    it('returns observed licenses if they are passed in on the object', () => {
      const licenseObj = {
        declaredLicenses: [],
        observedLicenses: ['foo', 'bar', 'baz']
      };
      expect(licenseDisplayUtils.getObservedLicensesDisplay(licenseObj)).toBe('foo, bar, baz');
    });

    it('dedups to not include licenses that are already in declaredLicenses', () => {
      const licenseObj = {
        declaredLicenses: ['bar'],
        observedLicenses: ['foo', 'bar', 'baz']
      };
      expect(licenseDisplayUtils.getObservedLicensesDisplay(licenseObj)).toBe('foo, baz');
    });

    it('does not dedupe "Not Provided" value', () => {
      const licenseObj = {
        declaredLicenses: ['Not Provided'],
        observedLicenses: ['Not Provided']
      };

      expect(licenseDisplayUtils.getObservedLicensesDisplay(licenseObj)).toBe('Not Provided');
    });
  });
});
