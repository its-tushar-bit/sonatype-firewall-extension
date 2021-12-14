/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from '../../../enzymeUtils';
import { NxTableCell, NxLoadingSpinner, NxSmallThreatCounter } from '@sonatype/react-shared-components';
import { faTrophy, faExclamationTriangle } from '@fortawesome/pro-solid-svg-icons';
import { CompareVersions } from 'MainRoot/componentDetails/overview/riskRemediation/CompareVersions';
import {
  SECURITY,
  LICENSE,
  QUALITY,
  OTHER,
} from 'MainRoot/componentDetails/overview/riskRemediation/policyThreatCategory';

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
    const versionRow = getShallow({ loading: true }).find('#version');
    expect(versionRow.find(NxLoadingSpinner)).toExist();
  });

  describe('Version row', () => {
    describe('when version is undefined', () => {
      it('renders empty cell for current version', () => {
        const versionRow = getShallow().find('#version');
        expect(versionRow.childAt(1)).toContainReact(<NxTableCell />);
      });
      it('renders "--" for selected version', () => {
        const versionRow = getShallow().find('#version');
        expect(versionRow.childAt(2)).toContainReact(<NxTableCell>-</NxTableCell>);
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
      const highestPolicyThreatRow = getShallow().find('#highestPolicyThreat');
      expect(highestPolicyThreatRow.childAt(1)).toContainReact(<NxTableCell />);
      expect(highestPolicyThreatRow.childAt(2)).toContainReact(<NxTableCell />);
    });

    it('renders "None" text if highestPolicyThreat is None', () => {
      const highestPolicyThreatRow = getShallow({
        currentVersion: { highestPolicyThreat: 'None' },
        selectedVersion: { highestPolicyThreat: 'None' },
      }).find('#highestPolicyThreat');
      expect(highestPolicyThreatRow.childAt(1)).toContainReact(<NxTableCell>None</NxTableCell>);
      expect(highestPolicyThreatRow.childAt(2)).toContainReact(<NxTableCell>None</NxTableCell>);
    });

    it('renders threat-indicator styled as critical if policy threat is 8-10', () => {
      const highestPolicyThreatRow = getShallow({
        currentVersion: { highestPolicyThreat: 10 },
        selectedVersion: { highestPolicyThreat: 8 },
      }).find('#highestPolicyThreat');

      const currentThreat = highestPolicyThreatRow.find(NxSmallThreatCounter).at(0);
      const selectedThreat = highestPolicyThreatRow.find(NxSmallThreatCounter).at(1);
      expect(currentThreat).toHaveProp('criticalCount', 10);
      expect(selectedThreat).toHaveProp('criticalCount', 8);
    });

    it('renders threat-indicator styled as severe if policy threat is 4-7', () => {
      const highestPolicyThreatRow = getShallow({
        currentVersion: { highestPolicyThreat: 7 },
        selectedVersion: { highestPolicyThreat: 4 },
      }).find('#highestPolicyThreat');

      const currentThreat = highestPolicyThreatRow.find(NxSmallThreatCounter).at(0);
      const selectedThreat = highestPolicyThreatRow.find(NxSmallThreatCounter).at(1);
      expect(currentThreat).toHaveProp('severeCount', 7);
      expect(selectedThreat).toHaveProp('severeCount', 4);
    });

    it('renders threat-indicator styled as moderate if policy threat is 2-3', () => {
      const highestPolicyThreatRow = getShallow({
        currentVersion: { highestPolicyThreat: 3 },
        selectedVersion: { highestPolicyThreat: 2 },
      }).find('#highestPolicyThreat');

      const currentThreat = highestPolicyThreatRow.find(NxSmallThreatCounter).at(0);
      const selectedThreat = highestPolicyThreatRow.find(NxSmallThreatCounter).at(1);
      expect(currentThreat).toHaveProp('moderateCount', 3);
      expect(selectedThreat).toHaveProp('moderateCount', 2);
    });

    it('renders threat-indicator styled as low if policy threat is 1', () => {
      const highestPolicyThreatRow = getShallow({
        currentVersion: { highestPolicyThreat: 1 },
        selectedVersion: { highestPolicyThreat: 1 },
      }).find('#highestPolicyThreat');

      const currentThreat = highestPolicyThreatRow.find(NxSmallThreatCounter).at(0);
      const selectedThreat = highestPolicyThreatRow.find(NxSmallThreatCounter).at(1);
      expect(currentThreat).toHaveProp('lowCount', 1);
      expect(selectedThreat).toHaveProp('lowCount', 1);
    });

    it('renders threat-indicator styled as none if policy threat is 0', () => {
      const highestPolicyThreatRow = getShallow({
        currentVersion: { highestPolicyThreat: 0 },
        selectedVersion: { highestPolicyThreat: 0 },
      }).find('#highestPolicyThreat');

      const currentThreat = highestPolicyThreatRow.find(NxSmallThreatCounter).at(0);
      const selectedThreat = highestPolicyThreatRow.find(NxSmallThreatCounter).at(1);
      expect(currentThreat).toHaveProp('noneCount', 0);
      expect(selectedThreat).toHaveProp('noneCount', 0);
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
      expect(currentCell.childAt(1)).toIncludeText('within 2 policies');

      expect(selectedCell.children().length).toBe(2);
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

      expect(selectedCell.children().length).toBe(1);
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

      expect(selectedCell.children().length).toBe(1);
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

      expect(selectedCell.children().length).toBe(1);
    });
  });

  describe('violation threat per policy type', () => {
    it('renders empty cell if policyMaxThreatLevelsByCategory is undefined', () => {
      const row = getShallow().find('#highestSecurityThreat');
      expect(row.childAt(1)).toContainReact(<NxTableCell />);
      expect(row.childAt(2)).toContainReact(<NxTableCell />);
    });

    testThreatByCategoryRow(SECURITY, '#highestSecurityThreat');
    testThreatByCategoryRow(LICENSE, '#highestLicenseThreat');
    testThreatByCategoryRow(QUALITY, '#highestQualityThreat');
    testThreatByCategoryRow(OTHER, '#highestOtherThreat');

    function testThreatByCategoryRow(category, rowSelector) {
      describe(`${category} row`, () => {
        it('renders "None" if category is undefined in policyMaxThreatLevelsByCategory', () => {
          const row = getShallow({
            currentVersion: { policyMaxThreatLevelsByCategory: {} },
            selectedVersion: { policyMaxThreatLevelsByCategory: {} },
          }).find(rowSelector);
          expect(row.childAt(1)).toContainReact(<NxTableCell>None</NxTableCell>);
          expect(row.childAt(2)).toContainReact(<NxTableCell>None</NxTableCell>);
        });

        it('renders threat-indicator styled as critical if policy threat is 8-10', () => {
          const row = getShallow({
            currentVersion: { policyMaxThreatLevelsByCategory: { [category]: 10 } },
            selectedVersion: { policyMaxThreatLevelsByCategory: { [category]: 8 } },
          }).find(rowSelector);

          const currentThreat = row.find(NxSmallThreatCounter).at(0);
          const selectedThreat = row.find(NxSmallThreatCounter).at(1);
          expect(currentThreat).toHaveProp('criticalCount', 10);
          expect(selectedThreat).toHaveProp('criticalCount', 8);
        });

        it('renders threat-indicator styled as severe if policy threat is 4-7', () => {
          const row = getShallow({
            currentVersion: { policyMaxThreatLevelsByCategory: { [category]: 7 } },
            selectedVersion: { policyMaxThreatLevelsByCategory: { [category]: 4 } },
          }).find(rowSelector);

          const currentThreat = row.find(NxSmallThreatCounter).at(0);
          const selectedThreat = row.find(NxSmallThreatCounter).at(1);
          expect(currentThreat).toHaveProp('severeCount', 7);
          expect(selectedThreat).toHaveProp('severeCount', 4);
        });

        it('renders threat-indicator styled as moderate if policy threat is 2-3', () => {
          const row = getShallow({
            currentVersion: { policyMaxThreatLevelsByCategory: { [category]: 3 } },
            selectedVersion: { policyMaxThreatLevelsByCategory: { [category]: 2 } },
          }).find(rowSelector);

          const currentThreat = row.find(NxSmallThreatCounter).at(0);
          const selectedThreat = row.find(NxSmallThreatCounter).at(1);
          expect(currentThreat).toHaveProp('moderateCount', 3);
          expect(selectedThreat).toHaveProp('moderateCount', 2);
        });

        it('renders threat-indicator styled as low if policy threat is 1', () => {
          const row = getShallow({
            currentVersion: { policyMaxThreatLevelsByCategory: { [category]: 1 } },
            selectedVersion: { policyMaxThreatLevelsByCategory: { [category]: 1 } },
          }).find(rowSelector);

          const currentThreat = row.find(NxSmallThreatCounter).at(0);
          const selectedThreat = row.find(NxSmallThreatCounter).at(1);
          expect(currentThreat).toHaveProp('lowCount', 1);
          expect(selectedThreat).toHaveProp('lowCount', 1);
        });

        it('renders threat-indicator styled as none if policy threat is 0', () => {
          const row = getShallow({
            currentVersion: { policyMaxThreatLevelsByCategory: { [category]: 0 } },
            selectedVersion: { policyMaxThreatLevelsByCategory: { [category]: 0 } },
          }).find(rowSelector);

          const currentThreat = row.find(NxSmallThreatCounter).at(0);
          const selectedThreat = row.find(NxSmallThreatCounter).at(1);
          expect(currentThreat).toHaveProp('noneCount', 0);
          expect(selectedThreat).toHaveProp('noneCount', 0);
        });
      });
    }
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
      expect(hygieneRatingRow).not.toExist();
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

  describe('Cataloged row', () => {
    beforeAll(() => jasmine.clock().install());
    afterAll(() => jasmine.clock().uninstall());

    it('renders empty cell if catalogDate is null', () => {
      const catalogedRow = getShallow().find('#catalogDate');
      expect(catalogedRow.childAt(1)).toContainReact(<NxTableCell>-</NxTableCell>);
      expect(catalogedRow.childAt(2)).toContainReact(<NxTableCell />);
    });

    it('renders time passed since component version was added', () => {
      jasmine.clock().mockDate(new Date(1635245371294));
      const catalogRow = getShallow({
        currentVersion: { catalogDate: 1462894745000 },
        selectedVersion: { catalogDate: 1635159876000 },
      }).find('#catalogDate');

      expect(catalogRow.childAt(1)).toContainReact(<NxTableCell>5 years ago</NxTableCell>);
      expect(catalogRow.childAt(2)).toContainReact(<NxTableCell>Less than a day ago</NxTableCell>);
    });
  });

  describe('Integrity Rating row', () => {
    it('does not renders cell if integrityRating is undefined', () => {
      const integrityRatingRow = getShallow().find('#integrityRating');
      expect(integrityRatingRow).not.toExist();
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
