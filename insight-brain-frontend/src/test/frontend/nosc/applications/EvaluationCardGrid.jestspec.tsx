/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, within } from '@testing-library/react';
import { Theme } from '@radix-ui/themes';
import { render } from 'TestRoot/SpecUtil';
import EvaluationCardGrid, {
  resolveStageColumns,
} from 'MainRoot/nosc/applications/EvaluationCardGrid';
import type { ApplicationRiskScore } from 'MainRoot/nosc/applications/applicationListTypes';

const EMPTY_RISK = {
  totalRisk: 0,
  criticalRisk: 0,
  severeRisk: 0,
  moderateRisk: 0,
  lowRisk: 0,
};

function app(partial: Partial<ApplicationRiskScore> & Pick<ApplicationRiskScore, 'stageRisks'>): ApplicationRiskScore {
  return {
    organizationName: 'Java-team',
    organizationId: 'org-1',
    applicationName: 'Apple - Java',
    applicationId: 'apple-java',
    totalApplicationRisk: { ...EMPTY_RISK, totalRisk: 12, criticalRisk: 2 },
    lastEvaluationDate: '2026-06-22T12:00:00.000Z',
    ...partial,
  };
}

describe('resolveStageColumns', () => {
  it('returns only stages with a scanId, in canonical order', () => {
    const columns = resolveStageColumns(
      app({
        stageRisks: [
          {
            stageTypeId: 'release',
            stageTypeName: 'Release',
            scanId: 'scan-r',
            evaluationDate: '2026-06-20T00:00:00.000Z',
            risk: EMPTY_RISK,
          },
          {
            stageTypeId: 'build',
            stageTypeName: '',
            scanId: 'scan-b',
            evaluationDate: '2026-06-22T00:00:00.000Z',
            risk: EMPTY_RISK,
          },
          {
            stageTypeId: 'source',
            stageTypeName: 'Source',
            scanId: '',
            evaluationDate: '2026-06-01T00:00:00.000Z',
            risk: EMPTY_RISK,
          },
        ],
      }),
    );

    expect(columns.map((column) => column.id)).toEqual(['build', 'release']);
    expect(columns[0].label).toBe('Build');
    expect(columns[1].label).toBe('Release');
  });

  it('returns an empty list when no stages are evaluated', () => {
    expect(resolveStageColumns(app({ stageRisks: [] }))).toEqual([]);
  });
});

describe('EvaluationCardGrid', () => {
  it('shows absolute Last Evaluation and stage dates, and falls back empty-stage label', () => {
    render(
      <Theme>
        <EvaluationCardGrid
          applications={[
            app({
              stageRisks: [
                {
                  stageTypeId: 'build',
                  stageTypeName: '',
                  scanId: 'scan-b',
                  evaluationDate: '2026-06-22T00:00:00.000Z',
                  risk: EMPTY_RISK,
                },
              ],
            }),
          ]}
        />
      </Theme>,
    );

    expect(screen.getByTestId('evaluation-card-last-evaluation')).toHaveTextContent(/Jun 2[12], 2026/);
    const tile = screen.getByTestId('evaluation-card-stage-tile');
    expect(within(tile).getByText('Build')).toBeInTheDocument();
    expect(within(tile).getByTestId('evaluation-card-stage-date')).toHaveTextContent(/Jun 2[12], 2026/);
  });

  it('shows the no-stages empty state when nothing is evaluated', () => {
    render(
      <Theme>
        <EvaluationCardGrid applications={[app({ stageRisks: [], lastEvaluationDate: undefined })]} />
      </Theme>,
    );

    expect(screen.getByTestId('evaluation-card-no-stages')).toBeInTheDocument();
  });
});
