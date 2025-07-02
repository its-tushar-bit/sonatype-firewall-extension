/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { fireEvent, render, screen, within } from 'TestRoot/SpecUtil';

import VulnerabilitiesTile from 'MainRoot/sbomManager/features/componentDetails/VulnerabilitiesTile';
import {
  defaultSortConfiguration,
  SORT_BY_FIELDS,
  SORT_DIRECTION,
} from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSlice';

describe('Vulnerabilities Tile', () => {
  let renderPage;

  const analysisStatusesOptions = [
    {
      key: 'resolved',
      value: 'Resolved',
    },

    {
      key: 'resolved_with_pedigree',
      value: 'Resolved with pedigree',
    },

    {
      key: 'exploitable',
      value: 'Exploitable',
    },

    {
      key: 'in_triage',
      value: 'In triage',
    },

    {
      key: 'false_positive',
      value: 'False positive',
    },

    {
      key: 'not_affected',
      value: 'Not affected',
    },
  ];

  const disclosedVulnerabilities = [
    {
      cvssScore: 4,
      issue: 'CVE-2019-10247',
      analysisStatus: null,
      justification: null,
      details: null,
      verified: true,
      identificationSources: 'SBOM,Sonatype',
      latestPreviousAnnotation: {
        sbomVersion: '1.0',
        analysisStatus: 'exploitable',
        justification: 'requires_dependency',
        response: 'can_not_fix',
        detail: 'some details',
      },
    },
    {
      cvssScore: 2,
      issue: 'CVE-2019-11358',
      analysisStatus: 'resolved',
      justification: 'protected_by_mitigating_control',
      details:
        'Automated dataflow analysis and manual code review indicates that the vulnerable code was removed and replaced.',
      verified: false,
      identificationSources: 'SBOM',
    },
    {
      cvssScore: 6,
      issue: 'CVE-2022-38752',
      analysisStatus: 'resolved_with_pedigree',
      justification: 'requires_dependency',
      details: 'CVE-2022-38752 for zookeeper-sbom/pkg:maven/io.fabric8.jube/war@2.2.0?type=war',
      verified: true,
      identificationSources: 'SBOM,Sonatype',
    },
  ];

  const sonatypeIdentifiedVulnerabilities = [
    {
      cvssScore: 10,
      issue: 'CVE-2021-41182',
      analysisStatus: null,
      justification: null,
      details: null,
      verified: false,
      identificationSources: 'Sonatype',
    },
    {
      cvssScore: 8,
      issue: 'CVE-2021-41183',
      analysisStatus: null,
      justification: null,
      details: null,
      verified: false,
      identificationSources: 'Sonatype',
    },
  ];

  const toggleSortDirection = jest.fn();

  beforeEach(() => {
    renderPage = (isDisclosedVulnerabilities, vulnerabilities, sortConfiguration = { ...defaultSortConfiguration }) =>
      render(
        <VulnerabilitiesTile
          isDisclosedVulnerabilities={isDisclosedVulnerabilities}
          vulnerabilities={vulnerabilities}
          analysisStatusesOptions={analysisStatusesOptions}
          sortConfiguration={sortConfiguration}
          toggleSortDirection={toggleSortDirection}
        />
      );
  });

  it('Renders Disclosed Vulnerabilities table', async () => {
    renderPage(true, disclosedVulnerabilities);
    expect(await screen.findByText('Disclosed Vulnerabilities')).toBeVisible();
    expect(screen.getByText('Existing vulnerabilities disclosed by the originator of this SBOM.')).toBeVisible();

    const tableRows = await screen.findAllByRole('row');
    expect(tableRows.length).toBe(4); // Including the header

    const headersRow = tableRows[0];
    let rowCells = within(headersRow).getAllByRole('columnheader');
    expect(rowCells[0]).toHaveTextContent('CVSS Score');
    expect(rowCells[1]).toHaveTextContent('Issue');
    expect(rowCells[2]).toHaveTextContent('Verification');
    expect(rowCells[3]).toHaveTextContent('Data Enrichment');
    expect(rowCells[4]).toHaveTextContent('Analysis State');
    expect(rowCells[5]).toHaveTextContent('Justification');
    expect(rowCells[6]).toHaveTextContent('Action');

    const firstRow = tableRows[1];
    rowCells = within(firstRow).getAllByRole('cell');
    expect(rowCells[0]).toHaveTextContent('6');
    expect(rowCells[1]).toHaveTextContent('CVE-2022-38752');
    expect(rowCells[2]).toHaveTextContent('');
    expect(rowCells[3]).toHaveTextContent('');
    expect(rowCells[4]).toHaveTextContent('Resolved with Pedigree');
    expect(rowCells[5]).toHaveTextContent('Requires dependency');
    const dropdownFirstRow = within(firstRow).getByRole('button');
    fireEvent.click(dropdownFirstRow);
    expect(screen.getByRole('button', { name: 'Edit Annotation' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Delete Annotation' })).toBeVisible();
    fireEvent.click(dropdownFirstRow);

    const secondRow = tableRows[2];
    rowCells = within(secondRow).getAllByRole('cell');
    expect(rowCells[0]).toHaveTextContent('4');
    expect(rowCells[1]).toHaveTextContent('CVE-2019-10247');
    expect(rowCells[2]).toHaveTextContent('');
    expect(rowCells[3]).toHaveTextContent('');
    expect(rowCells[4]).toHaveTextContent('Unannotated');
    expect(rowCells[5]).toHaveTextContent('Requires dependency');
    const dropdownSecondRow = within(secondRow).getByRole('button');
    fireEvent.click(dropdownSecondRow);
    expect(screen.getByRole('button', { name: 'Add Annotation' })).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Delete Annotation' })).toBeNull();
    expect(screen.getByRole('button', { name: 'Copy Annotation' })).toBeVisible();
    fireEvent.click(dropdownSecondRow);

    const thirdRow = tableRows[3];
    rowCells = within(thirdRow).getAllByRole('cell');
    expect(rowCells[0]).toHaveTextContent('2');
    expect(rowCells[1]).toHaveTextContent('CVE-2019-11358');
    expect(rowCells[2]).toHaveTextContent('');
    expect(rowCells[3]).toHaveTextContent('');
    expect(rowCells[4]).toHaveTextContent('Resolved');
    expect(rowCells[5]).toHaveTextContent('Protected by mitigating control');
    const dropdownThirdRow = within(thirdRow).getByRole('button');
    fireEvent.click(dropdownThirdRow);
    expect(screen.getByRole('button', { name: 'Edit Annotation' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Delete Annotation' })).toBeVisible();
  });

  it('Renders Additional Sonatype Identified Vulnerabilities table', async () => {
    renderPage(false, sonatypeIdentifiedVulnerabilities);
    expect(await screen.findByText('Additional Sonatype Identified Vulnerabilities')).toBeVisible();
    expect(
      screen.getByText('Additional vulnerabilities in this SBOM, detected by Sonatype vulnerability detection system.')
    ).toBeVisible();

    const tableRows = await screen.findAllByRole('row');
    expect(tableRows.length).toBe(3); // Including the header

    const headersRow = tableRows[0];
    let rowCells = within(headersRow).getAllByRole('columnheader');
    expect(rowCells[0]).toHaveTextContent('CVSS Score');
    expect(rowCells[1]).toHaveTextContent('Issue');
    expect(rowCells[2]).toHaveTextContent('Data Enrichment');
    expect(rowCells[3]).toHaveTextContent('Analysis State');
    expect(rowCells[4]).toHaveTextContent('Justification');
    expect(rowCells[5]).toHaveTextContent('Action');

    const firstRow = tableRows[1];
    rowCells = within(firstRow).getAllByRole('cell');
    expect(rowCells[0]).toHaveTextContent('10');
    expect(rowCells[1]).toHaveTextContent('CVE-2021-41182');
    expect(rowCells[2]).toHaveTextContent('');
    expect(rowCells[3]).toHaveTextContent('Unannotated');
    expect(rowCells[4]).toHaveTextContent('');
    const dropdownFirstRow = within(firstRow).getByRole('button');
    fireEvent.click(dropdownFirstRow);
    expect(screen.getByRole('button', { name: 'Add Annotation' })).toBeVisible();
    fireEvent.click(dropdownFirstRow);

    const secondRow = tableRows[2];
    rowCells = within(secondRow).getAllByRole('cell');
    expect(rowCells[0]).toHaveTextContent('8');
    expect(rowCells[1]).toHaveTextContent('CVE-2021-41183');
    expect(rowCells[2]).toHaveTextContent('');
    expect(rowCells[3]).toHaveTextContent('Unannotated');
    expect(rowCells[4]).toHaveTextContent('');
    const dropdownSecondRow = within(secondRow).getByRole('button');
    fireEvent.click(dropdownSecondRow);
    expect(screen.getByRole('button', { name: 'Add Annotation' })).toBeVisible();
    fireEvent.click(dropdownSecondRow);
  });

  it('Renders Additional Sonatype Identified Vulnerabilities table empty with empty message displayed', async () => {
    renderPage(false, []);
    expect(await screen.findByText('Additional Sonatype Identified Vulnerabilities')).toBeVisible();
    expect(
      screen.getByText('Additional vulnerabilities in this SBOM, detected by Sonatype vulnerability detection system.')
    ).toBeVisible();

    const tableRows = await screen.findAllByRole('row');
    expect(tableRows.length).toBe(2); // Including the header

    const headersRow = tableRows[0];
    let rowCells = within(headersRow).getAllByRole('columnheader');
    expect(rowCells[0]).toHaveTextContent('CVSS Score');
    expect(rowCells[1]).toHaveTextContent('Issue');
    expect(rowCells[2]).toHaveTextContent('Data Enrichment');
    expect(rowCells[3]).toHaveTextContent('Analysis State');
    expect(rowCells[4]).toHaveTextContent('Justification');
    expect(rowCells[5]).toHaveTextContent('Action');

    const firstRow = tableRows[1];
    rowCells = within(firstRow).getAllByRole('cell');
    expect(rowCells[0]).toHaveTextContent('No vulnerabilities found');
  });

  describe('Sorting', () => {
    const unsortedVulnerabilities = [
      {
        cvssScore: 5,
        issue: 'CVE-0000-0001',
        analysisStatus: null,
        justification: null,
        details: null,
        verified: false,
      },
      {
        cvssScore: 10,
        issue: 'CVE-0000-0002',
        analysisStatus: 'resolved',
        justification: null,
        details: null,
        verified: false,
      },
      {
        cvssScore: 1,
        issue: 'CVE-0000-0003',
        analysisStatus: 'resolved_with_pedigree',
        justification: null,
        details: null,
        verified: false,
      },
    ];

    it('renders the table in the correct sorting order: cvssScore, descending', async () => {
      const sortConfiguration = {
        sortBy: SORT_BY_FIELDS.cvssScore,
        sortDirection: SORT_DIRECTION.DESC,
      };

      renderPage(false, unsortedVulnerabilities, sortConfiguration);

      expect(await screen.findByText('Additional Sonatype Identified Vulnerabilities')).toBeVisible();

      const tableRows = await screen.findAllByRole('row');

      const firstRow = tableRows[1];
      let rowCells = within(firstRow).getAllByRole('cell');
      expect(rowCells[0]).toHaveTextContent('10');
      expect(rowCells[1]).toHaveTextContent('CVE-0000-0002');
      expect(rowCells[2]).toHaveTextContent('');
      expect(rowCells[3]).toHaveTextContent('Resolved');

      const secondRow = tableRows[2];
      rowCells = within(secondRow).getAllByRole('cell');
      expect(rowCells[0]).toHaveTextContent('5');
      expect(rowCells[1]).toHaveTextContent('CVE-0000-0001');
      expect(rowCells[2]).toHaveTextContent('');
      expect(rowCells[3]).toHaveTextContent('Unannotated');

      const thirdRow = tableRows[3];
      rowCells = within(thirdRow).getAllByRole('cell');
      expect(rowCells[0]).toHaveTextContent('1');
      expect(rowCells[1]).toHaveTextContent('CVE-0000-0003');
      expect(rowCells[2]).toHaveTextContent('');
      expect(rowCells[3]).toHaveTextContent('Resolved with Pedigree');
    });

    it('renders the table in the correct sorting order: cvssScore, ascending', async () => {
      const sortConfiguration = {
        sortBy: SORT_BY_FIELDS.cvssScore,
        sortDirection: SORT_DIRECTION.ASC,
      };

      renderPage(false, unsortedVulnerabilities, sortConfiguration);

      expect(await screen.findByText('Additional Sonatype Identified Vulnerabilities')).toBeVisible();

      const tableRows = await screen.findAllByRole('row');

      const firstRow = tableRows[1];
      let rowCells = within(firstRow).getAllByRole('cell');
      expect(rowCells[0]).toHaveTextContent('1');
      expect(rowCells[1]).toHaveTextContent('CVE-0000-0003');
      expect(rowCells[2]).toHaveTextContent('');
      expect(rowCells[3]).toHaveTextContent('Resolved with Pedigree');

      const secondRow = tableRows[2];
      rowCells = within(secondRow).getAllByRole('cell');
      expect(rowCells[0]).toHaveTextContent('5');
      expect(rowCells[1]).toHaveTextContent('CVE-0000-0001');
      expect(rowCells[2]).toHaveTextContent('');
      expect(rowCells[3]).toHaveTextContent('Unannotated');

      const thirdRow = tableRows[3];
      rowCells = within(thirdRow).getAllByRole('cell');
      expect(rowCells[0]).toHaveTextContent('10');
      expect(rowCells[1]).toHaveTextContent('CVE-0000-0002');
      expect(rowCells[2]).toHaveTextContent('');
      expect(rowCells[3]).toHaveTextContent('Resolved');
    });

    it('renders the table in the correct sorting order: analysisStatus, ascending', async () => {
      const sortConfiguration = {
        sortBy: SORT_BY_FIELDS.analysisStatus,
        sortDirection: SORT_DIRECTION.ASC,
      };

      renderPage(false, unsortedVulnerabilities, sortConfiguration);

      expect(await screen.findByText('Additional Sonatype Identified Vulnerabilities')).toBeVisible();

      const tableRows = await screen.findAllByRole('row');

      const firstRow = tableRows[1];
      let rowCells = within(firstRow).getAllByRole('cell');
      rowCells = within(firstRow).getAllByRole('cell');
      expect(rowCells[0]).toHaveTextContent('10');
      expect(rowCells[1]).toHaveTextContent('CVE-0000-0002');
      expect(rowCells[2]).toHaveTextContent('');
      expect(rowCells[3]).toHaveTextContent('Resolved');

      const secondRow = tableRows[2];
      rowCells = within(secondRow).getAllByRole('cell');
      expect(rowCells[0]).toHaveTextContent('1');
      expect(rowCells[1]).toHaveTextContent('CVE-0000-0003');
      expect(rowCells[2]).toHaveTextContent('');
      expect(rowCells[3]).toHaveTextContent('Resolved with Pedigree');

      const thirdRow = tableRows[3];
      rowCells = within(thirdRow).getAllByRole('cell');
      expect(rowCells[0]).toHaveTextContent('5');
      expect(rowCells[1]).toHaveTextContent('CVE-0000-0001');
      expect(rowCells[2]).toHaveTextContent('');
      expect(rowCells[3]).toHaveTextContent('Unannotated');
    });

    it('renders the table in the correct sorting order: analysisStatus, descending', async () => {
      const sortConfiguration = {
        sortBy: SORT_BY_FIELDS.analysisStatus,
        sortDirection: SORT_DIRECTION.DESC,
      };

      renderPage(false, unsortedVulnerabilities, sortConfiguration);

      expect(await screen.findByText('Additional Sonatype Identified Vulnerabilities')).toBeVisible();

      const tableRows = await screen.findAllByRole('row');

      const firstRow = tableRows[1];
      let rowCells = within(firstRow).getAllByRole('cell');
      rowCells = within(firstRow).getAllByRole('cell');
      expect(rowCells[0]).toHaveTextContent('5');
      expect(rowCells[1]).toHaveTextContent('CVE-0000-0001');
      expect(rowCells[2]).toHaveTextContent('');
      expect(rowCells[3]).toHaveTextContent('Unannotated');

      const secondRow = tableRows[2];
      rowCells = within(secondRow).getAllByRole('cell');
      expect(rowCells[0]).toHaveTextContent('1');
      expect(rowCells[1]).toHaveTextContent('CVE-0000-0003');
      expect(rowCells[2]).toHaveTextContent('');
      expect(rowCells[3]).toHaveTextContent('Resolved with Pedigree');

      const thirdRow = tableRows[3];
      rowCells = within(thirdRow).getAllByRole('cell');
      expect(rowCells[0]).toHaveTextContent('10');
      expect(rowCells[1]).toHaveTextContent('CVE-0000-0002');
      expect(rowCells[2]).toHaveTextContent('');
      expect(rowCells[3]).toHaveTextContent('Resolved');
    });
  });
});
