/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { axiosMockAdapter, render, waitFor } from 'TestRoot/SpecUtil';
import { screen } from '@testing-library/dom';
import React from 'react';
import { cleanup, fireEvent, getByText, queryByText } from '@testing-library/react';
import VexAnnotationDrawer from 'MainRoot/sbomManager/features/componentDetails/vexAnnotationsDrawer/VexAnnotationDrawer';
import { formatDate } from 'MainRoot/util/dateUtils';
import { getSbomVulnerabilityAnnotationUrl } from 'MainRoot/util/CLMLocation';

// Will be refactored in https://sonatype.atlassian.net/browse/CLM-35530
xdescribe('VexAnnotationDrawer', () => {
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

  const longTestDescription =
    'Included in Log4j 1.2 is a SocketServer class that is ' +
    'vulnerable to deserialization of untrusted data which can be exploited to ' +
    'remotely execute arbitrary code when combined with a deserialization gadget ' +
    'when listening to untrusted network traffic for log data. ' +
    'This affects Log4j versions up to 1.2 up to 1.2.17.';

  const mockVexAnnotationDrawer = {
    isDrawerOpen: true,
    issue: 'CVE-123',
    cvssScore: 4.7,
    verified: true,
    description: 'short description',
    details: 'Lorem ipsum test',
    justification: 'protected_by_mitigating_control',
    analysisStatus: 'in_triage',
    componentPurl: 'pkg:a/b/c',
    componentHash: 'abc123',
    internalAppkey: 'testInternalAppId',
    sbomVersion: '1234567890',
    response: 'test_response',
    updatedAt: 1716427819000,
    lastUpdatedBy: 'testAuthor',

    isRowAnnotated: false,
    isJustificationSet: false,
    isResponseSet: false,

    responsesOptions,
    analysisStatusesOptions,
    justificationsOptions,

    preSaveMaskActions: null,
    postSaveMaskActions: null,
    onClose: () => cleanup(),
    loadVexReferenceData: () => null,
  };
  const defaultExpectedTimestamp = formatDate(mockVexAnnotationDrawer.updatedAt, 'YYYY-MM-DD HH:mm:ss');

  const renderComponentWithMockData = (mockData) => render(<VexAnnotationDrawer {...mockData} />);
  const renderWithOverriddenData = (overriddenProps) => renderComponentWithMockData(overriddenProps);

  beforeEach(() => {
    renderDefaultComponent = () => renderComponentWithMockData(mockVexAnnotationDrawer);
  });

  const getSummaryContainer = (parentNode) => parentNode.querySelector('.vex-annotation-drawer__summary');
  const getDescriptionSection = (parentNode) =>
    parentNode.querySelector('.vex-annotation-drawer__vulnerability-description');
  const getFormContainer = (parentNode) => parentNode.querySelector('#vex-annotation-drawer__form');
  const getUserInfoContainer = (parentNode) => parentNode.querySelector('.vex-annotation-drawer__updated-info');

  const assertDropdownRenderedWithOptions = (arrOptions, nodeContainer) =>
    arrOptions.forEach((entry) => expect(getByText(nodeContainer, entry.value)).toBeInTheDocument());

  const assertDropdownsOptionsNotRendered = (arrOptions, nodeContainer) =>
    arrOptions.forEach((entry) => expect(queryByText(nodeContainer, entry.value)).not.toBeInTheDocument());

  const assertDropdownRenderedSelectOption = (nodeContainer) =>
    expect(queryByText(nodeContainer, 'SELECT')).toBeInTheDocument();

  const assertDropdownDidNotRenderedSelectOption = (nodeContainer) =>
    expect(queryByText(nodeContainer, 'SELECT')).not.toBeInTheDocument();

  const assertErrorLoadingDropdownsData = (container) => {
    expect(queryByText(container, /An error occurred loading data./)).toBeInTheDocument();
    expect(queryByText(container, /Please retry./)).toBeInTheDocument();
    expect(queryByText(container, /Retry/)).toBeInTheDocument();
  };

  const assertFormDoesNotHaveErrors = (container) => {
    expect(queryByText(container, /There were validation errors./)).not.toBeInTheDocument();
  };

  const getSubmitButton = (container) => container.querySelector('.vex-annotation-drawer__form__submit-button');

  const getButtonBar = (container) => container.querySelector('.vex-annotation-popover__footer-button-bar');

  it('renders nothing when isDrawerOpen is empty/null/false', async () => {
    const { container } = renderWithOverriddenData({
      ...mockVexAnnotationDrawer,
      isDrawerOpen: undefined,
    });
    const popover = container.querySelector('#vex-annotation-popover');
    expect(popover).not.toBeInTheDocument();
  });

  it('clicks close button and triggers function specified by onClose prop', async () => {
    const { container } = renderDefaultComponent();
    const closeButton = container.querySelector('header .nx-icon--close');
    fireEvent.click(closeButton);
    expect(closeButton).not.toBeInTheDocument();
  });

  describe('Form when isRowAnnotated prop is false/null/undef', () => {
    describe('Summary section', () => {
      it('renders sonatype verified data', () => {
        const { container } = renderDefaultComponent();

        const summaryContainer = getSummaryContainer(container);

        expect(getByText(summaryContainer, 'CVSS Score')).toBeInTheDocument();
        expect(getByText(summaryContainer, 'Verification Status')).toBeInTheDocument();
        expect(getByText(summaryContainer, mockVexAnnotationDrawer.cvssScore)).toBeInTheDocument();
        expect(getByText(summaryContainer, 'Sonatype Verified')).toBeInTheDocument();
        expect(summaryContainer.querySelector('.nx-threat-indicator--severe')).toBeInTheDocument();
        expect(summaryContainer.querySelector('.fa-circle-check')).toBeInTheDocument();
      });

      it('renders unverified data', () => {
        const { container } = renderWithOverriddenData({
          ...mockVexAnnotationDrawer,
          verified: false,
          cvssScore: 1.5,
        });

        const summaryContainer = getSummaryContainer(container);

        expect(getByText(summaryContainer, 'CVSS Score')).toBeInTheDocument();
        expect(getByText(summaryContainer, 'Verification Status')).toBeInTheDocument();
        expect(getByText(summaryContainer, 1.5)).toBeInTheDocument();
        expect(getByText(summaryContainer, 'Unverified')).toBeInTheDocument();
        expect(summaryContainer.querySelector('.nx-threat-indicator--low')).toBeInTheDocument();
        expect(summaryContainer.querySelector('.fa-triangle-exclamation')).toBeInTheDocument();
      });
    });

    describe('Description section', () => {
      beforeEach(() => cleanup());

      it('renders correct data (short description)', () => {
        const { container } = renderDefaultComponent();
        const descriptionContainer = getDescriptionSection(container);
        expect(queryByText(descriptionContainer, 'Description')).toBeInTheDocument();
        expect(queryByText(descriptionContainer, mockVexAnnotationDrawer.description)).toBeInTheDocument();
      });

      it('renders correct data (long description)', () => {
        const { container } = renderWithOverriddenData({
          ...mockVexAnnotationDrawer,
          description: longTestDescription,
        });
        const descriptionContainer = getDescriptionSection(container);
        expect(queryByText(descriptionContainer, 'Description')).toBeInTheDocument();
        expect(queryByText(descriptionContainer, new RegExp(longTestDescription.substring(0, 30)))).toBeInTheDocument();
        expect(queryByText(descriptionContainer, /\.\.\./)).toBeInTheDocument();
        expect(queryByText(descriptionContainer.querySelector('a'), 'Learn more')).toBeInTheDocument();
      });

      it('skips rendering description section when description prop is null', () => {
        const { container } = renderWithOverriddenData({
          ...mockVexAnnotationDrawer,
          description: null,
        });
        const descriptionContainer = getDescriptionSection(container);
        expect(descriptionContainer).toBeNull();
      });
    });

    describe('Annotation Vulnerability Form section', () => {
      it('renders Analysis State dropdown', () => {
        const { container } = renderDefaultComponent();

        const formContainer = getFormContainer(container);

        expect(getByText(formContainer, 'Analysis State')).toBeInTheDocument();
        assertDropdownRenderedWithOptions(analysisStatusesOptions, formContainer);
        assertDropdownRenderedSelectOption(
          formContainer.querySelector('#vex-annotation-drawer__form__analysis-status-select')
        );
      });

      it('renders empty Analysis State dropdown', () => {
        const { container } = renderWithOverriddenData({ ...mockVexAnnotationDrawer, analysisStatusesOptions: [] });

        const formContainer = getFormContainer(container);
        expect(formContainer).not.toBeInTheDocument();
        const alertElement = container.querySelector('.nx-alert__content-wrap');
        assertDropdownsOptionsNotRendered(justificationsOptions, container);
        assertErrorLoadingDropdownsData(alertElement);
      });

      it('renders Justification dropdown', () => {
        const { container } = renderDefaultComponent();

        const formContainer = getFormContainer(container);

        expect(getByText(formContainer, 'Justification')).toBeInTheDocument();
        assertDropdownRenderedWithOptions(justificationsOptions, formContainer);
        assertDropdownRenderedSelectOption(
          formContainer.querySelector('#vex-annotation-drawer__form__justification-select')
        );
      });

      it('renders empty Justification dropdown', () => {
        const { container } = renderWithOverriddenData({ ...mockVexAnnotationDrawer, justificationsOptions: [] });

        const formContainer = getFormContainer(container);
        expect(formContainer).not.toBeInTheDocument();
        const alertElement = container.querySelector('.nx-alert__content-wrap');
        assertDropdownsOptionsNotRendered(justificationsOptions, container);
        assertErrorLoadingDropdownsData(alertElement);
      });

      it('renders Response dropdown', () => {
        const { container } = renderDefaultComponent();

        const formContainer = getFormContainer(container);

        expect(getByText(formContainer, 'Response')).toBeInTheDocument();
        assertDropdownRenderedWithOptions(responsesOptions, formContainer);
        assertDropdownRenderedSelectOption(
          formContainer.querySelector('#vex-annotation-drawer__form__response-select')
        );
      });

      it('renders empty Response dropdown', () => {
        const { container } = renderWithOverriddenData({ ...mockVexAnnotationDrawer, responsesOptions: [] });

        const formContainer = getFormContainer(container);
        expect(formContainer).not.toBeInTheDocument();
        const alertElement = container.querySelector('.nx-alert__content-wrap');
        assertDropdownsOptionsNotRendered(responsesOptions, container);
        assertErrorLoadingDropdownsData(alertElement);
      });

      it('renders Save button', () => {
        const { container } = renderWithOverriddenData({
          ...mockVexAnnotationDrawer,
        });

        const dropdown = container.querySelector('#vex-annotation-drawer__form__analysis-status-select');

        fireEvent.change(dropdown, {
          target: { value: analysisStatusesOptions[0].key },
        });

        const buttonBar = container.querySelector('.vex-annotation-popover__footer-button-bar');
        const saveButton = buttonBar.querySelector('.vex-annotation-drawer__form__submit-button');
        expect(saveButton).toBeInTheDocument();
        expect(getByText(saveButton, 'Save')).toBeInTheDocument();
      });
    });

    describe('Render form validation errors', () => {
      it('renders error message because no Analysis State was selected in dropdown before saving', () => {
        const { container } = renderWithOverriddenData({
          ...mockVexAnnotationDrawer,
          analysisStatus: undefined,
        });

        const footerContainer = container.querySelector('.vex-annotation-popover__footer-nx-drawer');
        const saveButton = container.querySelector('.vex-annotation-drawer__form__submit-button');
        expect(saveButton).toBeInTheDocument();
        expect(saveButton.getAttribute('class')).not.toContain('vex-annotation-popover__footer-hidden');

        fireEvent.click(saveButton);
        expect(saveButton.getAttribute('class')).toContain('vex-annotation-popover__footer-hidden');

        expect(
          getByText(footerContainer, /Analysis State field is required. Please select a value from the dropdown list/)
        ).toBeInTheDocument();
      });

      it('hides save button when required analysis state field error triggers and show back button when is valid', () => {
        const { container } = renderWithOverriddenData({
          ...mockVexAnnotationDrawer,
          analysisStatus: 'SELECT',
        });

        const dropdown = container.querySelector('#vex-annotation-drawer__form__analysis-status-select');
        const requiredValidationErrorMatcher = /Analysis State field is required./;
        const requiredValidationErrorMatcher2ndLine = /Please select a value from the dropdown list/;

        const footerContainer = container.querySelector('.vex-annotation-popover__footer-nx-drawer');
        const saveButton = container.querySelector('.vex-annotation-drawer__form__submit-button');
        expect(saveButton).toBeInTheDocument();

        expect(saveButton.getAttribute('class')).not.toContain('vex-annotation-popover__footer-hidden');

        fireEvent.click(saveButton);
        expect(saveButton.getAttribute('class')).toContain('vex-annotation-popover__footer-hidden');

        expect(getByText(footerContainer, requiredValidationErrorMatcher)).toBeInTheDocument();
        expect(getByText(footerContainer, requiredValidationErrorMatcher2ndLine)).toBeInTheDocument();

        fireEvent.change(dropdown, {
          target: { value: analysisStatusesOptions[0].key },
        });

        expect(queryByText(footerContainer, requiredValidationErrorMatcher)).not.toBeInTheDocument();

        const saveButtonAfterErrorsCleared = container.querySelector('.vex-annotation-drawer__form__submit-button');
        expect(saveButtonAfterErrorsCleared).toBeInTheDocument();
        expect(saveButtonAfterErrorsCleared.getAttribute('class')).not.toContain(
          'vex-annotation-popover__footer-hidden'
        );
      });
    });
  });

  describe('Form when isRowAnnotated prop is true', () => {
    const renderAnnotatedForm = (extraParams) =>
      renderWithOverriddenData({
        ...mockVexAnnotationDrawer,
        isRowAnnotated: true,
        ...extraParams,
      });

    it('renders update button', () => {
      const { container } = renderAnnotatedForm();
      const buttonBar = container.querySelector('.vex-annotation-popover__footer-button-bar');
      const updateButton = buttonBar.querySelector('.vex-annotation-drawer__form__submit-button');
      expect(updateButton).toBeInTheDocument();
      expect(getByText(updateButton, 'Update')).toBeInTheDocument();
    });

    it('will not render SELECT text in analysis dropdown but will render SELECT text in the other dropdowns', () => {
      const { container } = renderAnnotatedForm();
      const formContainer = getFormContainer(container);

      expect(getByText(formContainer, 'Response')).toBeInTheDocument();
      assertDropdownDidNotRenderedSelectOption(
        formContainer.querySelector('#vex-annotation-drawer__form__analysis-status-select')
      );

      assertDropdownRenderedSelectOption(
        formContainer.querySelector('#vex-annotation-drawer__form__justification-select')
      );

      assertDropdownRenderedSelectOption(formContainer.querySelector('#vex-annotation-drawer__form__response-select'));
    });

    it('renders annotation description with preloaded details', () => {
      const { container } = renderAnnotatedForm();
      const formContainer = getFormContainer(container);

      expect(getByText(formContainer, 'Description')).toBeInTheDocument();
      expect(getByText(formContainer.querySelector('textarea'), mockVexAnnotationDrawer.details)).toBeInTheDocument();
    });

    it('renders Updated section with correct full data', () => {
      const { container } = renderAnnotatedForm();

      const formContainer = getFormContainer(container);
      const userInfoContainer = getUserInfoContainer(formContainer);
      expect(getByText(userInfoContainer, 'Updated')).toBeInTheDocument();
      expect(getByText(userInfoContainer, defaultExpectedTimestamp)).toBeInTheDocument();
      expect(getByText(userInfoContainer, 'By ' + mockVexAnnotationDrawer.lastUpdatedBy)).toBeInTheDocument();
    });

    it('renders Updated section only with date', () => {
      const { container } = renderAnnotatedForm({ lastUpdatedBy: null });

      const formContainer = getFormContainer(container);
      const userInfoContainer = getUserInfoContainer(formContainer);
      expect(getByText(userInfoContainer, 'Updated')).toBeInTheDocument();
      expect(getByText(userInfoContainer, defaultExpectedTimestamp)).toBeInTheDocument();
      expect(queryByText(userInfoContainer, 'By ' + mockVexAnnotationDrawer.lastUpdatedBy)).not.toBeInTheDocument();
    });

    it('renders Updated section only with author', () => {
      const { container } = renderAnnotatedForm({ updatedAt: null });

      const formContainer = getFormContainer(container);
      const userInfoContainer = getUserInfoContainer(formContainer);
      expect(getByText(userInfoContainer, 'Updated')).toBeInTheDocument();
      expect(queryByText(userInfoContainer, defaultExpectedTimestamp)).not.toBeInTheDocument();
      expect(getByText(userInfoContainer, 'By ' + mockVexAnnotationDrawer.lastUpdatedBy)).toBeInTheDocument();
    });

    it('renders empty annotation description with placeholder when details prop is empty', () => {
      const { container } = renderAnnotatedForm({
        details: undefined,
      });

      const formContainer = getFormContainer(container);
      const descriptionTextArea = formContainer.querySelector('textarea');
      expect(getByText(formContainer, 'Description')).toBeInTheDocument();
      expect(queryByText(descriptionTextArea, mockVexAnnotationDrawer.details)).not.toBeInTheDocument();
      expect(descriptionTextArea.getAttribute('placeholder')).toBe('Entry');
    });
  });

  describe('Save form', () => {
    it('saves form successfully', async () => {
      axiosMock
        .onPut(
          getSbomVulnerabilityAnnotationUrl(
            mockVexAnnotationDrawer.internalAppId,
            mockVexAnnotationDrawer.sbomVersion,
            mockVexAnnotationDrawer.issue
          )
        )
        .reply(200, {});

      const { container } = renderDefaultComponent();

      expect(queryByText(container, 'Annotate ' + mockVexAnnotationDrawer.issue)).toBeInTheDocument();

      // When vulnerability is not annotated at least the analysis dropdown value should be selected before saving
      const dropdown = container.querySelector('#vex-annotation-drawer__form__analysis-status-select');

      fireEvent.change(dropdown, {
        target: { value: analysisStatusesOptions[0].key },
      });
      assertFormDoesNotHaveErrors(container);

      const saveButton = container.querySelector('.vex-annotation-drawer__form__submit-button');
      expect(saveButton).toBeInTheDocument();
      fireEvent.click(saveButton);
      await waitFor(() => expect(screen.getByText(/Success/)).toBeInTheDocument());
    });

    it('displays an error message when failing to save the form data', async () => {
      axiosMock
        .onPut(
          getSbomVulnerabilityAnnotationUrl(
            mockVexAnnotationDrawer.internalAppId,
            mockVexAnnotationDrawer.sbomVersion,
            mockVexAnnotationDrawer.issue
          )
        )
        .reply(404, {});

      const { container } = renderDefaultComponent();
      expect(queryByText(container, 'Annotate ' + mockVexAnnotationDrawer.issue)).toBeInTheDocument();

      // When vulnerability is not annotated at least the analysis dropdown value should be selected before saving
      const dropdown = container.querySelector('#vex-annotation-drawer__form__analysis-status-select');

      fireEvent.change(dropdown, {
        target: { value: analysisStatusesOptions[0].key },
      });
      assertFormDoesNotHaveErrors(container);
      const saveButton = getSubmitButton(container);
      expect(saveButton).toBeInTheDocument();
      fireEvent.click(saveButton);
      await waitFor(() => expect(screen.queryByText(/Success/)).not.toBeInTheDocument());
      const retryButton = queryByText(container, 'Retry');
      expect(retryButton).toBeInTheDocument();
    });
  });

  describe('Update form', () => {
    it('updates data successfully', async () => {
      axiosMock
        .onPut(
          getSbomVulnerabilityAnnotationUrl(
            mockVexAnnotationDrawer.internalAppId,
            mockVexAnnotationDrawer.sbomVersion,
            mockVexAnnotationDrawer.issue
          )
        )
        .reply(200, {});

      const { container } = renderWithOverriddenData({
        ...mockVexAnnotationDrawer,
        isRowAnnotated: true,
      });

      expect(queryByText(container, 'Annotate ' + mockVexAnnotationDrawer.issue)).toBeInTheDocument();

      // When vulnerability is not annotated at least the analysis dropdown value should be selected before saving
      const dropdown = container.querySelector('#vex-annotation-drawer__form__analysis-status-select');

      fireEvent.change(dropdown, {
        target: { value: 'not_affected' },
      });
      assertFormDoesNotHaveErrors(container);

      const buttonBar = getButtonBar(container);
      expect(getByText(buttonBar, 'Update'));
      const updateButton = getSubmitButton(buttonBar);
      expect(updateButton).toBeInTheDocument();
      fireEvent.click(updateButton);
      await waitFor(() => expect(screen.getByText(/Success/)).toBeInTheDocument());
    });

    it('displays error an message when failing to update the form data', async () => {
      axiosMock
        .onPut(
          getSbomVulnerabilityAnnotationUrl(
            mockVexAnnotationDrawer.internalAppId,
            mockVexAnnotationDrawer.sbomVersion,
            mockVexAnnotationDrawer.issue
          )
        )
        .reply(404, {});

      const { container } = renderWithOverriddenData({
        ...mockVexAnnotationDrawer,
        isRowAnnotated: true,
      });

      expect(queryByText(container, 'Annotate ' + mockVexAnnotationDrawer.issue)).toBeInTheDocument();

      // When vulnerability is not annotated at least the analysis dropdown value should be selected before saving
      const analysisDropdown = container.querySelector('#vex-annotation-drawer__form__analysis-status-select');

      fireEvent.change(analysisDropdown, {
        target: { value: analysisStatusesOptions[0].key },
      });

      assertFormDoesNotHaveErrors(container);
      const saveButton = getSubmitButton(container);
      expect(saveButton).toBeInTheDocument();
      fireEvent.click(saveButton);
      await waitFor(() => expect(screen.queryByText(/Success/)).not.toBeInTheDocument());
      const retryButton = getByText(container, 'Retry');
      expect(retryButton).toBeInTheDocument();
    });
  });

  describe('Unsaved Changes Warning', () => {
    // TODO (CLM-35530): Refactor to use accessible queries (screen.getByRole) instead of
    // container.querySelector for better test maintainability and resilience to DOM changes
    it('shows unsaved changes modal when closing drawer with modified details field', async () => {
      const mockOnClose = jest.fn();
      const { container } = renderWithOverriddenData({
        ...mockVexAnnotationDrawer,
        isRowAnnotated: false,
        onClose: mockOnClose,
      });

      // Modify the details textarea
      const detailsTextarea = container.querySelector('textarea');
      fireEvent.change(detailsTextarea, { target: { value: 'New details text' } });

      // Try to close the drawer by calling the close handler
      const portalDrawer = container.querySelector('#vex-annotation-popover');
      expect(portalDrawer).toBeInTheDocument();

      // Simulate clicking outside to trigger close
      const closeButton = container.querySelector('.nx-btn-bar__btn'); // Assuming close button exists
      if (closeButton) {
        fireEvent.click(closeButton);
      }

      // Verify unsaved changes modal appears
      await waitFor(() => {
        expect(screen.queryByText('Unsaved Changes')).toBeInTheDocument();
        expect(
          screen.queryByText('The page may contain unsaved changes; continuing will discard them.')
        ).toBeInTheDocument();
      });

      // Verify drawer close handler was NOT called yet
      expect(mockOnClose).not.toHaveBeenCalled();
    });

    it('shows unsaved changes modal when closing drawer with modified analysis status', async () => {
      const mockOnClose = jest.fn();
      const { container } = renderWithOverriddenData({
        ...mockVexAnnotationDrawer,
        isRowAnnotated: false,
        onClose: mockOnClose,
      });

      // Change analysis status dropdown
      const analysisDropdown = container.querySelector('#vex-annotation-drawer__form__analysis-status-select');
      fireEvent.change(analysisDropdown, {
        target: { value: analysisStatusesOptions[0].key },
      });

      // Trigger close - modal should appear
      const closeButton = container.querySelector('.nx-btn-bar__btn');
      if (closeButton) {
        fireEvent.click(closeButton);
      }

      await waitFor(() => {
        expect(screen.queryByText('Unsaved Changes')).toBeInTheDocument();
      });

      expect(mockOnClose).not.toHaveBeenCalled();
    });

    it('shows unsaved changes modal when closing drawer with modified justification', async () => {
      const mockOnClose = jest.fn();
      const { container } = renderWithOverriddenData({
        ...mockVexAnnotationDrawer,
        isRowAnnotated: false,
        onClose: mockOnClose,
      });

      // Change justification dropdown
      const justificationDropdown = container.querySelector('#vex-annotation-drawer__form__justification-select');
      fireEvent.change(justificationDropdown, {
        target: { value: justificationsOptions[0].key },
      });

      // Trigger close
      const closeButton = container.querySelector('.nx-btn-bar__btn');
      if (closeButton) {
        fireEvent.click(closeButton);
      }

      await waitFor(() => {
        expect(screen.queryByText('Unsaved Changes')).toBeInTheDocument();
      });

      expect(mockOnClose).not.toHaveBeenCalled();
    });

    it('shows unsaved changes modal when closing drawer with modified response', async () => {
      const mockOnClose = jest.fn();
      const { container } = renderWithOverriddenData({
        ...mockVexAnnotationDrawer,
        isRowAnnotated: false,
        onClose: mockOnClose,
      });

      // Change response dropdown
      const responseDropdown = container.querySelector('#vex-annotation-drawer__form__response-select');
      fireEvent.change(responseDropdown, {
        target: { value: responsesOptions[0].key },
      });

      // Trigger close
      const closeButton = container.querySelector('.nx-btn-bar__btn');
      if (closeButton) {
        fireEvent.click(closeButton);
      }

      await waitFor(() => {
        expect(screen.queryByText('Unsaved Changes')).toBeInTheDocument();
      });

      expect(mockOnClose).not.toHaveBeenCalled();
    });

    it('does not show modal when closing drawer without any changes', async () => {
      const mockOnClose = jest.fn();
      const { container } = renderWithOverriddenData({
        ...mockVexAnnotationDrawer,
        isRowAnnotated: false,
        onClose: mockOnClose,
      });

      // Don't make any changes, just try to close
      const closeButton = container.querySelector('.nx-btn-bar__btn');
      if (closeButton) {
        fireEvent.click(closeButton);
      }

      // Verify modal does NOT appear
      await waitFor(() => {
        expect(screen.queryByText('Unsaved Changes')).not.toBeInTheDocument();
      });

      // Verify drawer was closed
      expect(mockOnClose).toHaveBeenCalled();
    });

    it('closes modal and keeps drawer open when Cancel button is clicked', async () => {
      const mockOnClose = jest.fn();
      const { container } = renderWithOverriddenData({
        ...mockVexAnnotationDrawer,
        isRowAnnotated: false,
        onClose: mockOnClose,
      });

      // Make a change
      const detailsTextarea = container.querySelector('textarea');
      fireEvent.change(detailsTextarea, { target: { value: 'Modified' } });

      // Trigger close to show modal
      const closeButton = container.querySelector('.nx-btn-bar__btn');
      if (closeButton) {
        fireEvent.click(closeButton);
      }

      // Wait for modal to appear
      await waitFor(() => {
        expect(screen.queryByText('Unsaved Changes')).toBeInTheDocument();
      });

      // Click Cancel button in the modal
      const cancelButton = screen.getByRole('button', { name: /Cancel/i });
      fireEvent.click(cancelButton);

      // Verify modal is closed
      await waitFor(() => {
        expect(screen.queryByText('Unsaved Changes')).not.toBeInTheDocument();
      });

      // Verify drawer was NOT closed
      expect(mockOnClose).not.toHaveBeenCalled();

      // Verify drawer is still open
      expect(container.querySelector('#vex-annotation-popover')).toBeInTheDocument();
    });

    it('closes both modal and drawer when Continue button is clicked', async () => {
      const mockOnClose = jest.fn();
      const { container } = renderWithOverriddenData({
        ...mockVexAnnotationDrawer,
        isRowAnnotated: false,
        onClose: mockOnClose,
      });

      // Make a change
      const detailsTextarea = container.querySelector('textarea');
      fireEvent.change(detailsTextarea, { target: { value: 'Modified' } });

      // Trigger close to show modal
      const closeButton = container.querySelector('.nx-btn-bar__btn');
      if (closeButton) {
        fireEvent.click(closeButton);
      }

      // Wait for modal to appear
      await waitFor(() => {
        expect(screen.queryByText('Unsaved Changes')).toBeInTheDocument();
      });

      // Click Continue button in the modal
      const continueButton = screen.getByRole('button', { name: /Continue/i });
      fireEvent.click(continueButton);

      // Verify modal is closed
      await waitFor(() => {
        expect(screen.queryByText('Unsaved Changes')).not.toBeInTheDocument();
      });

      // Verify drawer close handler was called
      expect(mockOnClose).toHaveBeenCalled();
    });

    it('does not show modal when closing annotated row without changes', async () => {
      const mockOnClose = jest.fn();
      const { container } = renderWithOverriddenData({
        ...mockVexAnnotationDrawer,
        isRowAnnotated: true,
        details: 'Existing details',
        analysisStatus: 'in_triage',
      });

      // Don't make any changes
      const closeButton = container.querySelector('.nx-btn-bar__btn');
      if (closeButton) {
        fireEvent.click(closeButton);
      }

      // Verify modal does NOT appear
      await waitFor(() => {
        expect(screen.queryByText('Unsaved Changes')).not.toBeInTheDocument();
      });

      // Verify drawer was closed
      expect(mockOnClose).toHaveBeenCalled();
    });
  });
}); // End test file
