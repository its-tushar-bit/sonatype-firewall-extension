/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { axiosMockAdapter, render, waitFor } from 'TestRoot/SpecUtil';
import React from 'react';
import { screen } from '@testing-library/dom';

import { cleanup, fireEvent, getByText, queryByText } from '@testing-library/react';
import VexAnnotationDrawerPopover from 'MainRoot/sbomManager/features/componentDetails/vexAnnotationsDrawer/VexAnnotationDrawerPopover';
import { formatDate } from 'MainRoot/util/dateUtils';
import { saveSbomVulnerabilityAnnotationUrl } from 'MainRoot/util/CLMLocation';

describe('VexAnnotationDrawerPopover', () => {
  let renderDefaultComponent;
  const axiosMock = axiosMockAdapter();

  const responsesOptions = [
    {
      key: 'can_not_fix',
      value: 'Can not fix',
    },
    {
      key: 'rollback',
      value: 'Rollback',
    },
    {
      key: 'update',
      value: 'Update',
    },
    {
      key: 'will_not_fix',
      value: 'Will not fix',
    },
    {
      key: 'workaround_available',
      value: 'Workaround available',
    },
  ];

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

  const justificationsOptions = [
    {
      key: 'code_not_present',
      value: 'Code not present',
    },
    {
      key: 'code_not_reachable',
      value: 'Code not reachable',
    },
    {
      key: 'protected_at_perimeter',
      value: 'Protected at perimeter',
    },
    {
      key: 'protected_at_runtime',
      value: 'Protected at runtime',
    },
    {
      key: 'protected_by_compiler',
      value: 'Protected by compiler',
    },
    {
      key: 'protected_by_mitigating_control',
      value: 'Protected by mitigating control',
    },
    {
      key: 'requires_configuration',
      value: 'Requires configuration',
    },
    {
      key: 'requires_dependency',
      value: 'Requires dependency',
    },
    {
      key: 'requires_environment',
      value: 'Requires environment',
    },
  ];

  const vulnerabilityRowObject = {
    issue: 'CVE-123',
    description: 'short description',
    cvssScore: 4.7,
    verified: true,
    details: 'Lorem ipsum test',
    justification: 'protected_by_mitigating_control',
    analysisStatus: 'in-triage',
    response: 'test_response',
    // timestamp formatted 2024-05-22 21:30:19
    updatedAt: 1716427819000,
    lastUpdatedBy: 'testAuthor',
  };

  const defaultExpectedTimestamp = formatDate(vulnerabilityRowObject.updatedAt, 'YYYY-MM-DD HH:mm:ss');

  const mockVexAnnotationPopover = {
    showVexAnnotationFormPopover: true,
    componentPurl: 'pkg:a/b/c',
    componentHash: 'abc123',
    internalAppId: 'testInternalAppId',
    sbomVersion: '1234567890',

    responsesOptions,
    analysisStatusesOptions,
    justificationsOptions,

    isVulnerabilityReferenceDataLoading: false,
    errorLoadingAnalysisReferenceData: null,

    // reload functions. Definition required even if it's null
    loadVexReferenceData: () => null,
    reloadComponentDetails: () => null,
    onClose: () => cleanup(),
    vulnerabilityRowObject,
  };

  const renderComponentWithMockData = (mockData) => render(<VexAnnotationDrawerPopover {...mockData} />);
  const renderWithOverriddenData = (overriddenMockObject) =>
    renderComponentWithMockData({ ...mockVexAnnotationPopover, ...overriddenMockObject });

  beforeEach(() => {
    renderDefaultComponent = () => renderComponentWithMockData(mockVexAnnotationPopover);
  });

  const getHeaderSection = (parent) => parent.querySelector('header');
  const getSummaryContainer = (parentNode) => parentNode.querySelector('.vex-annotation-drawer__summary');
  const getDescriptionSection = (parentNode) =>
    parentNode.querySelector('.vex-annotation-drawer__vulnerability-description');
  const getFormContainer = (parentNode) => parentNode.querySelector('#vex-annotation-drawer__form');
  const getUserInfoContainer = (parentNode) => parentNode.querySelector('.vex-annotation-drawer__updated-info');

  const assertDropdownRenderedWithOptions = (arrOptions, nodeContainer) =>
    arrOptions.forEach((entry) => expect(getByText(nodeContainer, entry.value)).toBeInTheDocument());

  const assertFormDoesNotHaveErrors = (container) => {
    expect(queryByText(container, /There were validation errors./)).not.toBeInTheDocument();
  };

  describe('Render with default data', () => {
    it('renders sonatype verified data', () => {
      const { container } = renderDefaultComponent();

      // Header section
      const headerContainer = getHeaderSection(container);
      expect(getByText(headerContainer, mockVexAnnotationPopover.componentPurl)).toBeInTheDocument();
      expect(
        getByText(headerContainer, `Annotate ${mockVexAnnotationPopover.vulnerabilityRowObject.issue}`)
      ).toBeInTheDocument();
      const closeButton = headerContainer.querySelector('.iq-popover-header__close-btn');
      expect(closeButton).toBeInTheDocument();
      expect(closeButton.querySelector('svg').getAttribute('class')).toContain('fa-xmark');

      // Vulnerability summary section
      const summaryContainer = getSummaryContainer(container);
      expect(getByText(summaryContainer, 'CVSS Score')).toBeInTheDocument();
      expect(getByText(summaryContainer, 'Verification Status')).toBeInTheDocument();
      expect(
        getByText(summaryContainer, mockVexAnnotationPopover.vulnerabilityRowObject.cvssScore)
      ).toBeInTheDocument();
      expect(getByText(summaryContainer, 'Sonatype Verified')).toBeInTheDocument();
      expect(summaryContainer.querySelector('.nx-threat-indicator--severe')).toBeInTheDocument();
      expect(summaryContainer.querySelector('.fa-check-circle')).toBeInTheDocument();
      const descriptionContainer = getDescriptionSection(container);
      expect(queryByText(descriptionContainer, 'Description')).toBeInTheDocument();
      expect(
        queryByText(descriptionContainer, mockVexAnnotationPopover.vulnerabilityRowObject.description)
      ).toBeInTheDocument();

      // Form section
      const formContainer = getFormContainer(container);
      expect(getByText(formContainer, 'Analysis status')).toBeInTheDocument();
      assertDropdownRenderedWithOptions(analysisStatusesOptions, formContainer);

      expect(getByText(formContainer, 'Justification')).toBeInTheDocument();
      assertDropdownRenderedWithOptions(justificationsOptions, formContainer);

      expect(getByText(formContainer, 'Response')).toBeInTheDocument();
      assertDropdownRenderedWithOptions(responsesOptions, formContainer);

      expect(getByText(formContainer, 'Description')).toBeInTheDocument();

      const saveButton = screen.getByRole('button', { name: 'Save' });
      expect(saveButton).toBeInTheDocument();
    });

    it('saves form successfully', async () => {
      axiosMock
        .onPut(
          saveSbomVulnerabilityAnnotationUrl(
            mockVexAnnotationPopover.internalAppId,
            mockVexAnnotationPopover.sbomVersion,
            mockVexAnnotationPopover.vulnerabilityRowObject.issue
          )
        )
        .reply(200, {});

      const { container } = renderDefaultComponent();

      const vexAnnotationPopover = container.querySelector('aside');
      expect(screen.queryByRole('complementary')).toBeInTheDocument();
      expect(
        queryByText(vexAnnotationPopover, 'Annotate ' + mockVexAnnotationPopover.vulnerabilityRowObject.issue)
      ).toBeInTheDocument();

      // When vulnerability is not annotated at least the analysis dropdown value should be selected before saving
      const dropdown = container.querySelector('#vex-annotation-drawer__form__analysis-status-select');

      fireEvent.change(dropdown, {
        target: { value: analysisStatusesOptions[0].key },
      });
      assertFormDoesNotHaveErrors(container);

      const saveButton = screen.getByRole('button', { name: 'Save' });
      expect(saveButton).toBeInTheDocument();
      fireEvent.click(saveButton);
      await waitFor(() => expect(screen.getByText(/Success/)).toBeInTheDocument());
    });

    it('displays an error message when failing to save the form data', async () => {
      axiosMock
        .onPut(
          saveSbomVulnerabilityAnnotationUrl(
            mockVexAnnotationPopover.internalAppId,
            mockVexAnnotationPopover.sbomVersion,
            mockVexAnnotationPopover.vulnerabilityRowObject.issue
          )
        )
        .reply(404, {});

      const { container } = renderDefaultComponent();

      const vexAnnotationPopover = container.querySelector('aside');

      expect(screen.queryByRole('complementary')).toBeInTheDocument();
      expect(
        queryByText(vexAnnotationPopover, 'Annotate ' + mockVexAnnotationPopover.vulnerabilityRowObject.issue)
      ).toBeInTheDocument();

      // When vulnerability is not annotated at least the analysis dropdown value should be selected before saving
      const dropdown = container.querySelector('#vex-annotation-drawer__form__analysis-status-select');

      fireEvent.change(dropdown, {
        target: { value: analysisStatusesOptions[0].key },
      });
      assertFormDoesNotHaveErrors(container);
      const saveButton = screen.getByRole('button', { name: 'Save' });
      expect(saveButton).toBeInTheDocument();
      fireEvent.click(saveButton);
      await waitFor(() => expect(screen.queryByText(/Success/)).not.toBeInTheDocument());
      const retryButton = screen.getByRole('button', { name: 'Retry' });
      expect(retryButton).toBeInTheDocument();
    });
  });

  describe('Render with overridden data', () => {
    it('renders with isRowAnnotated true', () => {
      const { container } = renderWithOverriddenData({
        vulnerabilityRowObject: {
          ...vulnerabilityRowObject,
          isRowAnnotated: true,
        },
      });
      screen.getByRole('button', { name: 'Update' });

      const formContainer = getFormContainer(container);

      // Updated info section
      const userInfoContainer = getUserInfoContainer(formContainer);
      expect(getByText(userInfoContainer, 'Updated')).toBeInTheDocument();
      expect(getByText(userInfoContainer, defaultExpectedTimestamp)).toBeInTheDocument();
      expect(
        queryByText(userInfoContainer, 'By ' + mockVexAnnotationPopover.vulnerabilityRowObject.lastUpdatedBy)
      ).toBeInTheDocument();
    });

    it('updates data successfully', async () => {
      axiosMock
        .onPut(
          saveSbomVulnerabilityAnnotationUrl(
            mockVexAnnotationPopover.internalAppId,
            mockVexAnnotationPopover.sbomVersion,
            mockVexAnnotationPopover.vulnerabilityRowObject.issue
          )
        )
        .reply(200, {});

      const { container } = renderWithOverriddenData({
        vulnerabilityRowObject: {
          ...vulnerabilityRowObject,
          isRowAnnotated: true,
        },
      });

      const vexAnnotationPopover = container.querySelector('aside');
      expect(screen.queryByRole('complementary')).toBeInTheDocument();
      expect(
        queryByText(vexAnnotationPopover, 'Annotate ' + mockVexAnnotationPopover.vulnerabilityRowObject.issue)
      ).toBeInTheDocument();

      // When vulnerability is not annotated at least the analysis dropdown value should be selected before saving
      const dropdown = container.querySelector('#vex-annotation-drawer__form__analysis-status-select');

      fireEvent.change(dropdown, {
        target: { value: 'not_affected' },
      });
      assertFormDoesNotHaveErrors(container);

      const saveButton = screen.getByRole('button', { name: 'Update' });
      expect(saveButton).toBeInTheDocument();
      fireEvent.click(saveButton);
      await waitFor(() => expect(screen.getByText(/Success/)).toBeInTheDocument());
    });

    it('displays error an message when failing to update the form data', async () => {
      axiosMock
        .onPut(
          saveSbomVulnerabilityAnnotationUrl(
            mockVexAnnotationPopover.internalAppId,
            mockVexAnnotationPopover.sbomVersion,
            mockVexAnnotationPopover.vulnerabilityRowObject.issue
          )
        )
        .reply(404, {});

      const { container } = renderWithOverriddenData({
        vulnerabilityRowObject: {
          ...vulnerabilityRowObject,
          isRowAnnotated: true,
        },
      });

      const vexAnnotationPopover = container.querySelector('aside');

      expect(screen.queryByRole('complementary')).toBeInTheDocument();
      expect(
        queryByText(vexAnnotationPopover, 'Annotate ' + mockVexAnnotationPopover.vulnerabilityRowObject.issue)
      ).toBeInTheDocument();

      // When vulnerability is not annotated at least the analysis dropdown value should be selected before saving
      const dropdown = container.querySelector('#vex-annotation-drawer__form__analysis-status-select');

      fireEvent.change(dropdown, {
        target: { value: analysisStatusesOptions[0].key },
      });
      assertFormDoesNotHaveErrors(container);
      const saveButton = screen.getByRole('button', { name: 'Update' });
      expect(saveButton).toBeInTheDocument();
      fireEvent.click(saveButton);
      await waitFor(() => expect(screen.queryByText(/Success/)).not.toBeInTheDocument());
      const retryButton = screen.getByRole('button', { name: 'Retry' });
      expect(retryButton).toBeInTheDocument();
    });
  });
});
