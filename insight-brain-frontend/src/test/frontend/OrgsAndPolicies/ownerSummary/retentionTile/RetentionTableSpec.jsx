/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, within } from 'TestRoot/SpecUtil';
import { keys } from 'ramda';
import RetentionTable, { timeShortener } from 'MainRoot/OrgsAndPolicies/ownerSummary/retentionTile/RetentionTable';
import { NOT_ENABLED, NOT_APPLICABLE } from 'MainRoot/OrgsAndPolicies/retentionSlice';

describe('RetentionTable', () => {
  let stages;

  stages = {
    develop: {
      inheritPolicy: true,
      enablePurging: true,
      maxAge: '3 months',
    },
    source: {
      inheritPolicy: true,
      enablePurging: true,
      maxCount: '30',
    },
    build: {
      inheritPolicy: true,
      enablePurging: true,
      maxCount: '40',
    },
    'stage-release': {
      inheritPolicy: true,
      enablePurging: true,
      maxAge: '2 months',
    },
    release: {
      inheritPolicy: true,
      enablePurging: true,
    },
    operate: {
      inheritPolicy: true,
      enablePurging: false,
      maxAge: '10 years',
    },
    'continuous-monitoring': {
      inheritPolicy: true,
      enablePurging: true,
      maxAge: '2 weeks',
    },
  };
  const renderComponent = (props) => render(<RetentionTable stages={props.stages} />);

  const testNumberOfStages = async (stages, numberOfColumns) => {
    renderComponent({ stages });

    const table = await screen.findByRole('table');
    const groups = within(table).getAllByRole('rowgroup');
    expect(groups.length).toBe(2);

    const stageNames = keys(stages);

    const headers = within(groups[0]).getAllByRole('columnheader');
    expect(headers.length).toBe(numberOfColumns);

    const tbodyRows = within(groups[1]).getAllByRole('row');
    expect(tbodyRows.length).toBe(2);

    tbodyRows.forEach((row, idx) => {
      const cells = within(row).getAllByRole('cell');
      expect(cells.length).toBe(numberOfColumns);
      expect(cells[0].textContent).toEqual(idx === 0 ? 'Age' : 'Reports');

      cells.forEach((cell, cellIdx) => {
        if (cellIdx !== 0) {
          const stage = stages[stageNames[cellIdx - 1]];
          const property = idx === 0 ? timeShortener(stage.maxAge) : stage.maxCount;
          const text = stage.enablePurging ? property || NOT_APPLICABLE : NOT_ENABLED;

          expect(cell.textContent).toEqual(text);
        }
      });
    });
  };

  it('renders table with default number of stages', async () => {
    const numberOfColumns = 7 + 1; // 7 stages and value column
    await testNumberOfStages(stages, numberOfColumns);
  });

  it('renders table with 1 stage', async () => {
    const numberOfColumns = 1 + 1; // 1 stage and value column
    stages = {
      develop: {
        inheritPolicy: true,
        enablePurging: true,
        maxAge: '3 months',
      },
    };
    await testNumberOfStages(stages, numberOfColumns);
  });

  it('renders table with 2 stages', async () => {
    const numberOfColumns = 2 + 1; // 2 stages and value column
    stages = {
      develop: {
        inheritPolicy: true,
        enablePurging: true,
        maxAge: '3 months',
      },
      source: {
        inheritPolicy: true,
        enablePurging: true,
        maxCount: '30',
      },
    };
    await testNumberOfStages(stages, numberOfColumns);
  });

  it('renders table with 3 stages', async () => {
    const numberOfColumns = 3 + 1; // 3 stages and value column
    stages = {
      develop: {
        inheritPolicy: true,
        enablePurging: true,
        maxAge: '3 months',
      },
      source: {
        inheritPolicy: true,
        enablePurging: true,
        maxCount: '30',
      },
      build: {
        inheritPolicy: true,
        enablePurging: true,
        maxCount: '40',
      },
    };
    await testNumberOfStages(stages, numberOfColumns);
  });

  it('renders table with 4 stages', async () => {
    const numberOfColumns = 4 + 1; // 4 stages and value column
    stages = {
      develop: {
        inheritPolicy: true,
        enablePurging: true,
        maxAge: '3 months',
      },
      source: {
        inheritPolicy: true,
        enablePurging: true,
        maxCount: '30',
      },
      build: {
        inheritPolicy: true,
        enablePurging: true,
        maxCount: '40',
      },
      'stage-release': {
        inheritPolicy: true,
        enablePurging: true,
        maxAge: '2 months',
      },
    };
    await testNumberOfStages(stages, numberOfColumns);
  });

  it('renders table with 5 stages', async () => {
    const numberOfColumns = 5 + 1; // 5 stages and value column
    stages = {
      develop: {
        inheritPolicy: true,
        enablePurging: true,
        maxAge: '3 months',
      },
      source: {
        inheritPolicy: true,
        enablePurging: true,
        maxCount: '30',
      },
      build: {
        inheritPolicy: true,
        enablePurging: true,
        maxCount: '40',
      },
      'stage-release': {
        inheritPolicy: true,
        enablePurging: true,
        maxAge: '2 months',
      },
      release: {
        inheritPolicy: true,
        enablePurging: true,
      },
    };
    await testNumberOfStages(stages, numberOfColumns);
  });

  it('renders table with 6 stages', async () => {
    const numberOfColumns = 6 + 1; // 6 stages and value column
    stages = {
      develop: {
        inheritPolicy: true,
        enablePurging: true,
        maxAge: '3 months',
      },
      source: {
        inheritPolicy: true,
        enablePurging: true,
        maxCount: '30',
      },
      build: {
        inheritPolicy: true,
        enablePurging: true,
        maxCount: '40',
      },
      'stage-release': {
        inheritPolicy: true,
        enablePurging: true,
        maxAge: '2 months',
      },
      release: {
        inheritPolicy: true,
        enablePurging: true,
      },
      operate: {
        inheritPolicy: true,
        enablePurging: false,
        maxAge: '10 years',
      },
    };
    await testNumberOfStages(stages, numberOfColumns);
  });
});
