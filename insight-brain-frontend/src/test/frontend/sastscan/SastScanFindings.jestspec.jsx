/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, fireEvent, within } from 'TestRoot/SpecUtil';
import SastScanFindings from 'MainRoot/sastScan/SastScanFindings';
import { faker } from '@faker-js/faker';

describe('SastScanFindings', () => {
  it('should render filter dropdown & View PR button', () => {
    const highFinding = generateFinding('HIGH');
    const lowFinding = generateFinding('LOW');
    renderComponent([highFinding, lowFinding], 'http://myurl.com');

    const filter = screen.getByRole('button', { name: 'Filter' });
    expect(filter).toBeInTheDocument();
    fireEvent.click(filter);
    const highOption = screen.getByRole('checkbox', { name: 'HIGH' });
    expect(highOption).toBeInTheDocument();
    const lowOption = screen.getByRole('checkbox', { name: 'LOW' });
    expect(lowOption).toBeInTheDocument();

    const viewPRButton = screen.getByRole('link', { name: 'View PR' });
    expect(viewPRButton).toBeInTheDocument();
  });

  it('should render findings and its default sorting is descending', () => {
    const highFinding = generateFinding('HIGH');
    const lowFinding = generateFinding('LOW');
    const mediumFinding = generateFinding('MEDIUM');
    renderComponent([highFinding, mediumFinding, lowFinding]);
    const totalDataRows = 3;
    const rows = screen.getAllByRole('row');
    expect(rows.length).toEqual(totalDataRows + 1); // +1 for header row
    const threatHeader = screen.getByRole('columnheader', { name: 'THREATS' });
    expect(threatHeader).toBeInTheDocument();

    const highRow = within(rows[1]);
    expect(highRow.getAllByText(highFinding.ruleName)[0]).toBeInTheDocument();
    const mediumRow = within(rows[2]);
    expect(mediumRow.getAllByText(mediumFinding.ruleName)[0]).toBeInTheDocument();
    const lowRow = within(rows[3]);
    expect(lowRow.getAllByText(lowFinding.ruleName)[0]).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'View PR' })).not.toBeInTheDocument();
  });

  it('should only show the selected severity finding when filter is applied', () => {
    const highFinding = generateFinding('HIGH');
    const criticalFinding = generateFinding('CRITICAL');
    renderComponent([highFinding, criticalFinding]);

    const filter = screen.getByRole('button', { name: 'Filter' });
    expect(filter).toBeInTheDocument();
    fireEvent.click(filter);
    const highOption = screen.getByRole('checkbox', { name: 'HIGH' });
    expect(highOption).toBeInTheDocument();
    fireEvent.click(highOption);
    expect(highOption.checked).toEqual(true);
    expect(screen.queryByRole('link', { name: 'View PR' })).not.toBeInTheDocument();

    expect(screen.getAllByRole('row').length).toEqual(2); // +1 for header row

    //reset
    const resetButton = screen.getByRole('button', { name: 'Reset' });
    expect(resetButton).toBeInTheDocument();
    fireEvent.click(resetButton);
    expect(highOption.checked).toEqual(false);
    expect(screen.getAllByRole('row').length).toEqual(3);

    //filter by critical
    const criticalOption = screen.getByRole('checkbox', { name: 'CRITICAL' });
    expect(criticalOption).toBeInTheDocument();
    fireEvent.click(criticalOption);
    expect(criticalOption.checked).toEqual(true);
    expect(screen.getAllByRole('row').length).toEqual(2);
  });

  it('sorts by threat when threat header is clicked', () => {
    const highFinding = generateFinding('HIGH');
    const mediumFinding = generateFinding('MEDIUM');
    renderComponent([highFinding, mediumFinding]);

    const threatsColumnHeader = screen.getByRole('columnheader', { name: /threats/i });
    expect(screen.getByRole('columnheader', { name: /threats/i })).toHaveAttribute('aria-sort', 'descending');

    expect(screen.getAllByRole('row')[1]).toHaveTextContent(highFinding.ruleName);
    expect(screen.getAllByRole('row')[2]).toHaveTextContent(mediumFinding.ruleName);

    fireEvent.click(threatsColumnHeader);
    expect(screen.getByRole('columnheader', { name: /threats/i })).toHaveAttribute('aria-sort', 'ascending');

    expect(screen.getAllByRole('row')[1]).toHaveTextContent(mediumFinding.ruleName);
    expect(screen.getAllByRole('row')[2]).toHaveTextContent(highFinding.ruleName);

    fireEvent.click(threatsColumnHeader);
    expect(screen.getByRole('columnheader', { name: /threats/i })).toHaveAttribute('aria-sort', 'descending');

    expect(screen.getAllByRole('row')[1]).toHaveTextContent(highFinding.ruleName);
    expect(screen.getAllByRole('row')[2]).toHaveTextContent(mediumFinding.ruleName);
  });

  function renderComponent(findings, sastPullRequestURL) {
    return render(<SastScanFindings findings={findings} sastPullRequestURL={sastPullRequestURL} />);
  }

  function generateFinding(severity) {
    return {
      sastFindingId: faker.datatype.uuid(),
      coordinate: {
        namespace: faker.lorem.word(5),
        name: faker.lorem.word(1),
        methodName: faker.lorem.word(4),
      },
      lineNumber: faker.random.numeric(),
      cwe: `CWE-${faker.random.numeric()}`,
      severity: severity,
      confidence: 'HIGH',
      ruleName: `${faker.random.word()} + ${severity}`,
      description: faker.lorem.paragraph(),
      remediations: [],
    };
  }
});
