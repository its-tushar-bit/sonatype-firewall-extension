/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import PrioritiesPageHeader from 'MainRoot/development/prioritiesPage/PrioritiesPageHeader';
import moment from 'moment';

describe('PrioritiesPageHeader', () => {
  let renderComponent;

  const metadata = {
    reportTime: 1702041439230,
    reportTitle: 'Build Report',
    application: {
      name: 'TestApp',
      nameLowercaseNoWhitespace: 'testapp',
      id: 'a03a6722af3f47fc8b7de86c78176de5',
      publicId: 'TestApp',
      publicIdLowercase: 'testapp',
    },
    stageId: 'build',
    commitHash: null,
    initiator: 'admin',
    scanTriggerType: 'Continuous Integration',
    totalRisk: 138,
    forMonitoring: false,
    reevaluation: false,
  };

  const defaultPreloadedState = {
    applicationReport: {
      metadata,
    },
  };

  beforeEach(() => {
    renderComponent = (preloadedState) =>
      render(<PrioritiesPageHeader />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  it('renders a header with the app name', () => {
    renderComponent();
    expect(screen.getByRole('heading', { name: 'TestApp - Priorities' })).toBeInTheDocument();
  });

  describe('description section', () => {
    it('renders correct time and date', () => {
      const reportTime = metadata.reportTime;
      const expectedFormattedDate = moment(reportTime).format('YYYY-MM-DD HH:mm:ss');
      renderComponent();
      expect(screen.getByText(expectedFormattedDate)).toBeInTheDocument();
    });

    describe('triggered by section', () => {
      it('renders a "Triggered by" text based on scanTriggerType', () => {
        renderComponent();
        expect(screen.getByText('Triggered by')).toBeInTheDocument();
        expect(screen.getByText(metadata.scanTriggerType)).toBeInTheDocument();
      });

      it('renders text (Continuous Monitoring) if forMonitoring is true', () => {
        renderComponent({
          applicationReport: {
            metadata: {
              ...metadata,
              forMonitoring: true,
            },
          },
        });
        expect(screen.getByText(/(continuous monitoring)/i)).toBeInTheDocument();
      });

      it('renders text (Re-evaluation) if reevaluation is true', () => {
        renderComponent({
          applicationReport: {
            metadata: {
              ...metadata,
              reevaluation: true,
            },
          },
        });
        expect(screen.getByText(/(re-evaluation)/i)).toBeInTheDocument();
      });
    });

    describe('commit hash section', () => {
      const originalNavigator = window.navigator;
      const writeText = jest.fn();

      Object.assign(navigator, {
        clipboard: {
          writeText,
        },
      });

      afterAll(() => {
        Object.assign(navigator, originalNavigator);
      });

      it('does not render if commit hash is null', () => {
        renderComponent();
        expect(screen.queryByText('Commit')).not.toBeInTheDocument();
      });

      it('renders if commit hash is present', () => {
        const commitHash = '473a9adb0824525dd69d375f067de0290deb2183';
        const expectedCommitHash = commitHash.substring(0, 7);
        renderComponent({
          applicationReport: {
            metadata: {
              ...defaultPreloadedState.applicationReport.metadata,
              commitHash,
            },
          },
        });
        expect(screen.getByText('Commit')).toBeInTheDocument();
        expect(screen.getByText(expectedCommitHash)).toBeInTheDocument();
      });

      it('renders with a copy icon that copies the commit hash if commit hash is present', async () => {
        const commitHash = '473a9adb0824525dd69d375f067de0290deb2183';
        renderComponent({
          applicationReport: {
            metadata: {
              ...defaultPreloadedState.applicationReport.metadata,
              commitHash,
            },
          },
        });

        const copyIcon = screen.getByRole('img', { hidden: true });
        fireEvent.click(copyIcon);
        expect(navigator.clipboard.writeText).toHaveBeenCalledWith(commitHash);
      });
    });

    describe('stage section', () => {
      it('does not render if stageId is null', () => {
        renderComponent({
          applicationReport: {
            metadata: {
              ...defaultPreloadedState.applicationReport.metadata,
              stageId: null,
            },
          },
        });
        expect(screen.queryByText('Stage: ')).not.toBeInTheDocument();
      });

      it('renders if stageId is present', () => {
        renderComponent();
        expect(screen.getByText('Stage')).toBeInTheDocument();
        expect(screen.getByText('Build')).toBeInTheDocument();
      });
    });
  });
});
