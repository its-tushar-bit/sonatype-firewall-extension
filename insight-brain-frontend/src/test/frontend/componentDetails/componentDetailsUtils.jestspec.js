/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  parseOccurrencePathname,
  getComponentVersionComparisonInfo,
  createTabConfiguration,
} from 'MainRoot/componentDetails/componentDetailsUtils';

describe('componentDetailsUtils', function () {
  describe('parseOccurrencePathname', function () {
    it('separates the basename and dirname of a path that includes one slash', function () {
      expect(parseOccurrencePathname('foo/bar.js')).toEqual({
        isDependency: false,
        dirname: 'foo',
        basename: 'bar.js',
      });
    });

    it('separates the basename with backslash and dirname of a path with previous and next folder', function () {
      expect(parseOccurrencePathname('dependency:/bar/go.sum/site\\baz\\foo\\foo@v1.0.1')).toEqual({
        isDependency: true,
        dirname: 'bar/go.sum',
        basename: 'site/baz/foo/foo@v1.0.1',
      });
    });

    it('separates the basename and dirname of a path that include multiple slashes', function () {
      expect(parseOccurrencePathname('foo/bar/baz.js')).toEqual({
        isDependency: false,
        dirname: 'foo/bar',
        basename: 'baz.js',
      });
    });

    it('passes through the value as the basename when there is no slash', function () {
      expect(parseOccurrencePathname('baz.js')).toEqual({
        isDependency: false,
        dirname: undefined,
        basename: 'baz.js',
      });
    });

    it('separates the basename with backslash and dirname of a path that includes no previous folder', function () {
      expect(parseOccurrencePathname('dependency:/go.sum/site\\foo\\foo@v1.0.1')).toEqual({
        isDependency: true,
        dirname: 'go.sum',
        basename: 'site/foo/foo@v1.0.1',
      });
    });

    describe('when the pathname starts with "dependency:/"', function () {
      it('separates the basename and dirname of a path that includes one slash', function () {
        expect(parseOccurrencePathname('dependency:/foo/bar.js')).toEqual({
          isDependency: true,
          dirname: 'foo',
          basename: 'bar.js',
        });
      });

      it('separates the basename and dirname of a path that include multiple slashes', function () {
        expect(parseOccurrencePathname('dependency:/foo/bar/baz.js')).toEqual({
          isDependency: true,
          dirname: 'foo/bar',
          basename: 'baz.js',
        });
      });

      it('passes through the value as the basename when there is no slash', function () {
        expect(parseOccurrencePathname('dependency:/baz.js')).toEqual({
          isDependency: true,
          dirname: undefined,
          basename: 'baz.js',
        });
      });
    });
  });

  describe('getComponentVersionComparisonInfo', () => {
    let componentDetails;
    beforeEach(() => {
      componentDetails = {
        policyAlerts: [],
        securityVulnerabilities: [],
        componentIdentifier: {
          coordinates: {},
        },
      };
    });

    it('returns empty object if componentDetails is null', () => {
      expect(getComponentVersionComparisonInfo(null)).toEqual({});
    });

    it('returns empty object if componentDetails is undefined', () => {
      expect(getComponentVersionComparisonInfo(undefined)).toEqual({});
    });

    describe('version', () => {
      it('is set from componentIdentifier object', () => {
        componentDetails.componentIdentifier.coordinates.version = 'version-123-componentIdentifier';
        componentDetails.version = 'version-123-fallback';
        expect(getComponentVersionComparisonInfo(componentDetails).version).toBe('version-123-componentIdentifier');
      });

      it('is set from version property if componentIdentifier object is unavailable', () => {
        componentDetails.componentIdentifier.coordinates.version = null;
        componentDetails.version = 'version-123-from-version-prop';
        expect(getComponentVersionComparisonInfo(componentDetails).version).toBe('version-123-from-version-prop');
      });
    });

    describe('highestPolicyThreat', () => {
      it('is set to None if there are no policy alerts', () => {
        expect(getComponentVersionComparisonInfo(componentDetails).highestPolicyThreat).toBe('None');
      });

      it('is set to the threat of most severe policy alert', () => {
        componentDetails.policyAlerts = [
          { trigger: { threatLevel: 3 } },
          { trigger: { threatLevel: 6 } },
          { trigger: { threatLevel: 0 } },
        ];
        expect(getComponentVersionComparisonInfo(componentDetails).highestPolicyThreat).toBe(6);
      });
    });

    describe('numberOfViolatedPolicies', () => {
      it('is set to 0 if there are no policy alerts', () => {
        expect(getComponentVersionComparisonInfo(componentDetails).numberOfViolatedPolicies).toBe(0);
      });

      it('is set to the number of policy alerts', () => {
        componentDetails.policyAlerts = [
          { trigger: { threatLevel: 3 } },
          { trigger: { threatLevel: 6 } },
          { trigger: { threatLevel: 0 } },
        ];
        expect(getComponentVersionComparisonInfo(componentDetails).numberOfViolatedPolicies).toBe(3);
      });
    });

    describe('highestCVSSScore', () => {
      it('is set to None if there are no security vulnerabilities', () => {
        expect(getComponentVersionComparisonInfo(componentDetails).highestCVSSScore).toBe('None');
      });

      it('is set to Unscored if all vulnerabilities have null severity', () => {
        componentDetails.securityVulnerabilities = [{ severity: null }, { severity: null }];
        expect(getComponentVersionComparisonInfo(componentDetails).highestCVSSScore).toBe('Unscored');
      });

      it('is set to the highest severity', () => {
        componentDetails.securityVulnerabilities = [
          { severity: null },
          { severity: 5.7 },
          { severity: 5.8 },
          { severity: 5.6 },
        ];
        expect(getComponentVersionComparisonInfo(componentDetails).highestCVSSScore).toBe(5.8);
      });

      it('properly handles component with Manual identification source', () => {
        componentDetails.identificationSource = 'Manual';
        componentDetails.securityVulnerabilities = [{ severity: 5.6 }];
        expect(getComponentVersionComparisonInfo(componentDetails).highestCVSSScore).toBe(
          'Unavailable, Claimed Component'
        );
      });
    });

    it('sets effectiveLicenseStatus', () => {
      componentDetails.effectiveLicenseStatus = 'Selected';
      expect(getComponentVersionComparisonInfo(componentDetails).effectiveLicenseStatus).toBe('Selected');
    });

    describe('effectiveLicenses', () => {
      it('is set to null if effectiveLicenses list is empty', () => {
        componentDetails.effectiveLicenses = [];
        expect(getComponentVersionComparisonInfo(componentDetails).effectiveLicenses).toBe(null);
      });

      it('is set to null if effectiveLicenses list is null', () => {
        componentDetails.effectiveLicenses = null;
        expect(getComponentVersionComparisonInfo(componentDetails).effectiveLicenses).toBe(null);
      });

      it('is set to the effective license name if there is only one effective license', () => {
        componentDetails.effectiveLicenses = [{ licenseName: 'foo' }];
        expect(getComponentVersionComparisonInfo(componentDetails).effectiveLicenses).toBe('foo');
      });

      it('is set to the comma separated list of effective license names', () => {
        componentDetails.effectiveLicenses = [{ licenseName: 'foo' }, { licenseName: 'bar' }, { licenseName: 'baz' }];
        expect(getComponentVersionComparisonInfo(componentDetails).effectiveLicenses).toBe('foo, bar, baz');
      });
    });

    it('sets integrityRating object', () => {
      componentDetails.integrityRating = {};
      expect(getComponentVersionComparisonInfo(componentDetails).integrityRating).toBe(
        componentDetails.integrityRating
      );
    });

    it('sets hygieneRating object', () => {
      componentDetails.hygieneRating = {};
      expect(getComponentVersionComparisonInfo(componentDetails).hygieneRating).toBe(componentDetails.hygieneRating);
    });

    it('sets catalogDate value', () => {
      componentDetails.catalogDate = 1635245371294;
      expect(getComponentVersionComparisonInfo(componentDetails).catalogDate).toBe(componentDetails.catalogDate);
    });

    it('sets policyMaxThreatLevelsByCategory', () => {
      componentDetails.policyMaxThreatLevelsByCategory = {};
      expect(getComponentVersionComparisonInfo(componentDetails).policyMaxThreatLevelsByCategory).toBe(
        componentDetails.policyMaxThreatLevelsByCategory
      );
    });

    it('creates a tab configuration object', () => {
      const component = {};
      const tabId = 'tabId';
      const title = 'Tab Name';
      expect(createTabConfiguration(tabId, title, component)).toEqual({ tabId, title, component });
    });
  });
});
