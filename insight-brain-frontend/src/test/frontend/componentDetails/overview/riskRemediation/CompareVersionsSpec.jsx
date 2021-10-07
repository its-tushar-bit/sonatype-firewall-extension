/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from '../../../enzymeUtils';
import { NxTableCell, NxTableBody } from '@sonatype/react-shared-components';
import { faTrophy, faExclamationTriangle } from '@fortawesome/pro-solid-svg-icons';
import { CompareVersions } from 'MainRoot/componentDetails/overview/riskRemediation/CompareVersions';

describe('CompareVersions', () => {
  let getShallow;

  beforeEach(function () {
    const minimalProps = {
      currentVersion: {},
      selectedVersion: {},
      loading: false,
    };
    getShallow = enzymeUtils.getShallowComponent(CompareVersions, minimalProps);
  });

  it('shows loading spinner', () => {
    const body = getShallow({ loading: true }).find(NxTableBody);
    expect(body).toHaveProp('isLoading', true);
  });

  describe('Version row', () => {
    describe('when version is undefined', () => {
      it('renders empty cell for current version', () => {
        const versionRow = getShallow().find('#version');
        expect(versionRow.childAt(1)).toContainReact(<NxTableCell />);
      });
      it('renders "--" for selected version', () => {
        const versionRow = getShallow().find('#version');
        expect(versionRow.childAt(2)).toContainReact(<NxTableCell>--</NxTableCell>);
      });
    });

    describe('when version is defined', () => {
      it('renders version for current and selected version', () => {
        const versionRow = getShallow({
          currentVersion: { version: 'current version' },
          selectedVersion: { version: 'selected version' },
        }).find('#version');
        expect(versionRow.childAt(1).childAt(0)).toHaveText('current version');
        expect(versionRow.childAt(2).childAt(0)).toHaveText('selected version');
      });
    });
  });

  describe('Highest Policy Threat row', () => {
    it('renders empty cell if highestPolicyThreat is undefined', () => {
      const versionRow = getShallow().find('#highestPolicyThreat');
      expect(versionRow.childAt(1)).toContainReact(<NxTableCell />);
      expect(versionRow.childAt(2)).toContainReact(<NxTableCell />);
    });

    it('renders div with "None" text if highestPolicyThreat is None', () => {
      const highestPolicyThreatRow = getShallow({
        currentVersion: { highestPolicyThreat: 'None' },
        selectedVersion: { highestPolicyThreat: 'None' },
      }).find('#highestPolicyThreat');
      expect(highestPolicyThreatRow.childAt(1).childAt(0)).toHaveText('None');
      expect(highestPolicyThreatRow.childAt(2).childAt(0)).toHaveText('None');
    });

    it('renders threat-indicator styled as critical if policy threat is 8-10', () => {
      const highestPolicyThreatRow = getShallow({
        currentVersion: { highestPolicyThreat: 10 },
        selectedVersion: { highestPolicyThreat: 8 },
      }).find('#highestPolicyThreat');

      const currentThreat = highestPolicyThreatRow
        .childAt(1)
        .childAt(0)
        .find('.iq-compare-versions__policy-threat-indicator');
      const selectedThreat = highestPolicyThreatRow
        .childAt(2)
        .childAt(0)
        .find('.iq-compare-versions__policy-threat-indicator');
      expect(currentThreat).toHaveClassName('critical');
      expect(currentThreat.childAt(0)).toHaveText('10');
      expect(selectedThreat).toHaveClassName('critical');
      expect(selectedThreat.childAt(0)).toHaveText('8');
    });

    it('renders threat-indicator styled as severe if policy threat is 4-7', () => {
      const highestPolicyThreatRow = getShallow({
        currentVersion: { highestPolicyThreat: 7 },
        selectedVersion: { highestPolicyThreat: 4 },
      }).find('#highestPolicyThreat');

      const currentThreat = highestPolicyThreatRow
        .childAt(1)
        .childAt(0)
        .find('.iq-compare-versions__policy-threat-indicator');
      const selectedThreat = highestPolicyThreatRow
        .childAt(2)
        .childAt(0)
        .find('.iq-compare-versions__policy-threat-indicator');
      expect(currentThreat).toHaveClassName('severe');
      expect(currentThreat.childAt(0)).toHaveText('7');
      expect(selectedThreat).toHaveClassName('severe');
      expect(selectedThreat.childAt(0)).toHaveText('4');
    });

    it('renders threat-indicator styled as moderate if policy threat is 2-3', () => {
      const highestPolicyThreatRow = getShallow({
        currentVersion: { highestPolicyThreat: 3 },
        selectedVersion: { highestPolicyThreat: 2 },
      }).find('#highestPolicyThreat');

      const currentThreat = highestPolicyThreatRow
        .childAt(1)
        .childAt(0)
        .find('.iq-compare-versions__policy-threat-indicator');
      const selectedThreat = highestPolicyThreatRow
        .childAt(2)
        .childAt(0)
        .find('.iq-compare-versions__policy-threat-indicator');
      expect(currentThreat).toHaveClassName('moderate');
      expect(currentThreat.childAt(0)).toHaveText('3');
      expect(selectedThreat).toHaveClassName('moderate');
      expect(selectedThreat.childAt(0)).toHaveText('2');
    });

    it('renders threat-indicator styled as low if policy threat is 1', () => {
      const highestPolicyThreatRow = getShallow({
        currentVersion: { highestPolicyThreat: 1 },
        selectedVersion: { highestPolicyThreat: 1 },
      }).find('#highestPolicyThreat');

      const currentThreat = highestPolicyThreatRow
        .childAt(1)
        .childAt(0)
        .find('.iq-compare-versions__policy-threat-indicator');
      const selectedThreat = highestPolicyThreatRow
        .childAt(2)
        .childAt(0)
        .find('.iq-compare-versions__policy-threat-indicator');
      expect(currentThreat).toHaveClassName('low');
      expect(currentThreat.childAt(0)).toHaveText('1');
      expect(selectedThreat).toHaveClassName('low');
      expect(selectedThreat.childAt(0)).toHaveText('1');
    });

    it('renders threat-indicator styled as none if policy threat is 0', () => {
      const highestPolicyThreatRow = getShallow({
        currentVersion: { highestPolicyThreat: 0 },
        selectedVersion: { highestPolicyThreat: 0 },
      }).find('#highestPolicyThreat');

      const currentThreat = highestPolicyThreatRow
        .childAt(1)
        .childAt(0)
        .find('.iq-compare-versions__policy-threat-indicator');
      const selectedThreat = highestPolicyThreatRow
        .childAt(2)
        .childAt(0)
        .find('.iq-compare-versions__policy-threat-indicator');

      expect(currentThreat).toHaveClassName('none');
      expect(currentThreat.childAt(0)).toHaveText('0');
      expect(selectedThreat).toHaveClassName('none');
      expect(selectedThreat.childAt(0)).toHaveText('0');
    });

    it('renders number of violated policies if numberOfViolatedPolicies > 1', () => {
      const highestPolicyThreatRow = getShallow({
        currentVersion: {
          highestPolicyThreat: 1,
          numberOfViolatedPolicies: 2,
        },
        selectedVersion: {
          highestPolicyThreat: 1,
          numberOfViolatedPolicies: 10,
        },
      }).find('#highestPolicyThreat');

      const currentCell = highestPolicyThreatRow.childAt(1);
      const selectedCell = highestPolicyThreatRow.childAt(2);

      expect(currentCell.children().length).toBe(2);
      expect(currentCell.childAt(0)).toHaveClassName('iq-compare-versions__policy-threat-indicator');
      expect(currentCell.childAt(1)).toIncludeText('within 2 policies');

      expect(selectedCell.children().length).toBe(2);
      expect(selectedCell.childAt(0)).toHaveClassName('iq-compare-versions__policy-threat-indicator');
      expect(selectedCell.childAt(1)).toIncludeText('within 10 policies');
    });

    it('does not render number of violated policies if numberOfViolatedPolicies is 1', () => {
      const highestPolicyThreatRow = getShallow({
        currentVersion: {
          highestPolicyThreat: 1,
          numberOfViolatedPolicies: 1,
        },
        selectedVersion: {
          highestPolicyThreat: 1,
          numberOfViolatedPolicies: 1,
        },
      }).find('#highestPolicyThreat');

      const currentCell = highestPolicyThreatRow.childAt(1);
      const selectedCell = highestPolicyThreatRow.childAt(2);

      expect(currentCell.children().length).toBe(1);
      expect(currentCell.childAt(0)).toHaveClassName('iq-compare-versions__policy-threat-indicator');

      expect(selectedCell.children().length).toBe(1);
      expect(selectedCell.childAt(0)).toHaveClassName('iq-compare-versions__policy-threat-indicator');
    });

    it('does not render number of violated policies if numberOfViolatedPolicies is 0', () => {
      const highestPolicyThreatRow = getShallow({
        currentVersion: {
          highestPolicyThreat: 1,
          numberOfViolatedPolicies: 0,
        },
        selectedVersion: {
          highestPolicyThreat: 1,
          numberOfViolatedPolicies: 0,
        },
      }).find('#highestPolicyThreat');

      const currentCell = highestPolicyThreatRow.childAt(1);
      const selectedCell = highestPolicyThreatRow.childAt(2);

      expect(currentCell.children().length).toBe(1);
      expect(currentCell.childAt(0)).toHaveClassName('iq-compare-versions__policy-threat-indicator');

      expect(selectedCell.children().length).toBe(1);
      expect(selectedCell.childAt(0)).toHaveClassName('iq-compare-versions__policy-threat-indicator');
    });

    it('does not render number of violated policies if numberOfViolatedPolicies is undefined', () => {
      const highestPolicyThreatRow = getShallow({
        currentVersion: {
          highestPolicyThreat: 1,
        },
        selectedVersion: {
          highestPolicyThreat: 1,
        },
      }).find('#highestPolicyThreat');

      const currentCell = highestPolicyThreatRow.childAt(1);
      const selectedCell = highestPolicyThreatRow.childAt(2);

      expect(currentCell.children().length).toBe(1);
      expect(currentCell.childAt(0)).toHaveClassName('iq-compare-versions__policy-threat-indicator');

      expect(selectedCell.children().length).toBe(1);
      expect(selectedCell.childAt(0)).toHaveClassName('iq-compare-versions__policy-threat-indicator');
    });
  });

  describe('Highest CVSS Score row', () => {
    it('renders empty cell if highestCVSSScore is not undefined', () => {
      const highestCvssScoreRow = getShallow().find('#highestCvssScore');
      expect(highestCvssScoreRow.childAt(1)).toContainReact(<NxTableCell />);
      expect(highestCvssScoreRow.childAt(2)).toContainReact(<NxTableCell />);
    });

    it('renders provided highestCVSSScore', () => {
      const highestCvssScoreRow = getShallow({
        currentVersion: { highestCVSSScore: 5.7 },
        selectedVersion: { highestCVSSScore: 0.1 },
      }).find('#highestCvssScore');
      const currentScore = highestCvssScoreRow.childAt(1).childAt(0);
      const selectedScore = highestCvssScoreRow.childAt(2).childAt(0);
      expect(currentScore).toHaveText('5.7');
      expect(selectedScore).toHaveText('0.1');
    });
  });

  describe('Effective License row', () => {
    it('renders empty cell if effectiveLicenses is undefined', () => {
      const effectiveLicensesRow = getShallow().find('#effectiveLicense');
      expect(effectiveLicensesRow.childAt(1)).toContainReact(<NxTableCell />);
      expect(effectiveLicensesRow.childAt(2)).toContainReact(<NxTableCell />);
    });

    it('renders effectiveLicenses value if provided', () => {
      const effectiveLicensesRow = getShallow({
        currentVersion: { effectiveLicenses: 'foo, bar' },
        selectedVersion: { effectiveLicenses: 'foo, baz' },
      }).find('#effectiveLicense');
      expect(effectiveLicensesRow.childAt(1).childAt(0)).toHaveText('foo, bar');
      expect(effectiveLicensesRow.childAt(2).childAt(0)).toHaveText('foo, baz');
    });

    it('renders effectiveLicenseStatus as purple tag if status is Overridden', () => {
      const effectiveLicensesRow = getShallow({
        currentVersion: { effectiveLicenses: 'foo, bar', effectiveLicenseStatus: 'Overridden' },
        selectedVersion: { effectiveLicenses: 'foo, baz', effectiveLicenseStatus: 'Overridden' },
      }).find('#effectiveLicense');

      const currentStatus = effectiveLicensesRow.childAt(1).childAt(1);
      const selectedStatus = effectiveLicensesRow.childAt(2).childAt(1);

      expect(currentStatus).toHaveProp('color', 'purple');
      expect(currentStatus.childAt(0)).toHaveText('Overridden');
      expect(selectedStatus).toHaveProp('color', 'purple');
      expect(selectedStatus.childAt(0)).toHaveText('Overridden');
    });

    it('renders effectiveLicenseStatus as indigo tag if status is Not Overridden', () => {
      const effectiveLicensesRow = getShallow({
        currentVersion: { effectiveLicenses: 'foo, bar', effectiveLicenseStatus: 'Selected' },
        selectedVersion: { effectiveLicenses: 'foo, baz', effectiveLicenseStatus: 'Selected' },
      }).find('#effectiveLicense');

      const currentStatus = effectiveLicensesRow.childAt(1).childAt(1);
      const selectedStatus = effectiveLicensesRow.childAt(2).childAt(1);

      expect(currentStatus).toHaveProp('color', 'indigo');
      expect(currentStatus.childAt(0)).toHaveText('Selected');
      expect(selectedStatus).toHaveProp('color', 'indigo');
      expect(selectedStatus.childAt(0)).toHaveText('Selected');
    });

    it('does not render effectiveLicenseStatus if it is undefined', () => {
      const effectiveLicensesRow = getShallow({
        currentVersion: { effectiveLicenses: 'foo, bar' },
        selectedVersion: { effectiveLicenses: 'foo, baz' },
      }).find('#effectiveLicense');

      const currentCell = effectiveLicensesRow.childAt(1);
      const selectedCell = effectiveLicensesRow.childAt(2);

      expect(currentCell.find('.iq-compare-versions__license-status')).not.toExist();
      expect(selectedCell.find('.iq-compare-versions__license-status')).not.toExist();
    });
  });

  describe('Hygiene Rating row', () => {
    it('renders empty cell if hygieneRating is undefined', () => {
      const hygieneRatingRow = getShallow().find('#hygieneRating');
      expect(hygieneRatingRow.childAt(1)).toContainReact(<NxTableCell />);
      expect(hygieneRatingRow.childAt(2)).toContainReact(<NxTableCell />);
    });

    it('renders rating label and faTrophy icon with .iq-hygiene-rating-exemplar class if rating id is 1', () => {
      const hygieneRatingRow = getShallow({
        currentVersion: { hygieneRating: { id: 1, label: 'foo' } },
        selectedVersion: { hygieneRating: { id: 1, label: 'bar' } },
      }).find('#hygieneRating');

      const currentCell = hygieneRatingRow.childAt(1);
      const selectedCell = hygieneRatingRow.childAt(2);

      expect(currentCell.childAt(0)).toHaveClassName('iq-hygiene-rating-exemplar');
      expect(currentCell.childAt(0)).toHaveProp('icon', faTrophy);
      expect(currentCell.childAt(1)).toHaveText('foo');
      expect(selectedCell.childAt(0)).toHaveClassName('iq-hygiene-rating-exemplar');
      expect(selectedCell.childAt(0)).toHaveProp('icon', faTrophy);
      expect(selectedCell.childAt(1)).toHaveText('bar');
    });

    it('renders rating label and faExclamationTriangle icon with .iq-hygiene-rating-laggard class if rating id is 4', () => {
      const hygieneRatingRow = getShallow({
        currentVersion: { hygieneRating: { id: 4, label: 'foo' } },
        selectedVersion: { hygieneRating: { id: 4, label: 'bar' } },
      }).find('#hygieneRating');

      const currentCell = hygieneRatingRow.childAt(1);
      const selectedCell = hygieneRatingRow.childAt(2);

      expect(currentCell.childAt(0)).toHaveClassName('iq-hygiene-rating-laggard');
      expect(currentCell.childAt(0)).toHaveProp('icon', faExclamationTriangle);
      expect(currentCell.childAt(1)).toHaveText('foo');
      expect(selectedCell.childAt(0)).toHaveClassName('iq-hygiene-rating-laggard');
      expect(selectedCell.childAt(0)).toHaveProp('icon', faExclamationTriangle);
      expect(selectedCell.childAt(1)).toHaveText('bar');
    });

    it('does not render an icon if rating id is not 4 or 1', () => {
      const hygieneRatingRow = getShallow({
        currentVersion: { hygieneRating: { id: 2, label: 'foo' } },
        selectedVersion: { hygieneRating: { id: 3, label: 'bar' } },
      }).find('#hygieneRating');

      const currentCell = hygieneRatingRow.childAt(1);
      const selectedCell = hygieneRatingRow.childAt(2);

      expect(currentCell.children().length).toBe(1);
      expect(currentCell.childAt(0)).toHaveText('foo');

      expect(selectedCell.children().length).toBe(1);
      expect(selectedCell.childAt(0)).toHaveText('bar');
    });
  });

  describe('Integrity Rating row', () => {
    it('renders empty cell if integrityRating is undefined', () => {
      const integrityRatingRow = getShallow().find('#integrityRating');
      expect(integrityRatingRow.childAt(1)).toContainReact(<NxTableCell />);
      expect(integrityRatingRow.childAt(2)).toContainReact(<NxTableCell />);
    });

    it('renders rating label with .iq-integrity-rating-suspicious class if rating id is 1', () => {
      const integrityRatingRow = getShallow({
        currentVersion: { integrityRating: { id: 1, label: 'foo' } },
        selectedVersion: { integrityRating: { id: 1, label: 'bar' } },
      }).find('#integrityRating');

      const currentCell = integrityRatingRow.childAt(1);
      const selectedCell = integrityRatingRow.childAt(2);

      expect(currentCell.find('.iq-integrity-rating-suspicious')).toHaveText('foo');
      expect(selectedCell.find('.iq-integrity-rating-suspicious')).toHaveText('bar');
    });

    it('renders rating label without .iq-integrity-rating-suspicious class if rating id is not 1', () => {
      const integrityRatingRow = getShallow({
        currentVersion: { integrityRating: { id: 2, label: 'foo' } },
        selectedVersion: { integrityRating: { id: 3, label: 'bar' } },
      }).find('#integrityRating');

      const currentCell = integrityRatingRow.childAt(1);
      const selectedCell = integrityRatingRow.childAt(2);

      expect(currentCell.find('.iq-integrity-rating-suspicious')).not.toExist();
      expect(currentCell.childAt(0)).toHaveText('foo');

      expect(selectedCell.find('.iq-integrity-rating-suspicious')).not.toExist();
      expect(selectedCell.childAt(0)).toHaveText('bar');
    });
  });
});
