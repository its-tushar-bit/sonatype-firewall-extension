/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { render } from 'TestRoot/SpecUtil';
import { screen } from '@testing-library/dom';
import React from 'react';
import { cleanup, fireEvent, getByText, queryByText } from '@testing-library/react';
import VexAnnotationDrawer from 'MainRoot/sbomManager/features/componentDetails/vexAnnotationsDrawer/VexAnnotationDrawer';
import { formatDate } from 'MainRoot/util/dateUtils';

describe('VexAnnotationDrawer', () => {
  let renderDefaultComponent;

  const longTestDescription =
    'Included in Log4j 1.2 is a SocketServer class that is ' +
    'vulnerable to deserialization of untrusted data which can be exploited to ' +
    'remotely execute arbitrary code when combined with a deserialization gadget ' +
    'when listening to untrusted network traffic for log data. ' +
    'This affects Log4j versions up to 1.2 up to 1.2.17.';

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

  const mockVexAnnotationDrawer = {
    issue: 'CVE-123',
    cvssScore: 4.7,
    verified: true,
    description: 'short description',
    details: 'Lorem ipsum test',
    justification: 'protected_by_mitigating_control',
    analysisStatus: 'in-triage',
    componentPurl: 'pkg:a/b/c',
    componentHash: 'abc123',
    internalAppkey: 'testInternalAppId',
    sbomVersion: '1234567890',
    response: 'test_response',
    updatedAt: 1716427819000,
    lastUpdatedBy: 'testAuthor',

    responsesOptions,
    analysisStatusesOptions,
    justificationsOptions,

    preSaveMaskActions: null,
    postSaveMaskActions: null,
  };
  const defaultExpectedTimestamp = formatDate(mockVexAnnotationDrawer.updatedAt, 'YYYY-MM-DD HH:mm:ss');

  const renderComponentWithMockData = (mockData) => render(<VexAnnotationDrawer {...mockData} />);
  const renderWithOverridenData = (overridenProps) => renderComponentWithMockData(overridenProps);

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
        expect(summaryContainer.querySelector('.fa-check-circle')).toBeInTheDocument();
      });

      it('renders unverified data', () => {
        const { container } = renderWithOverridenData({
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
        expect(summaryContainer.querySelector('.fa-exclamation-triangle')).toBeInTheDocument();
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
        const { container } = renderWithOverridenData({
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
        const { container } = renderWithOverridenData({
          ...mockVexAnnotationDrawer,
          description: null,
        });
        const descriptionContainer = getDescriptionSection(container);
        expect(descriptionContainer).toBeNull();
      });
    });

    describe('Annotation Vulnerability Form section', () => {
      it('renders Analysis Status dropdown', () => {
        const { container } = renderDefaultComponent();

        const formContainer = getFormContainer(container);

        expect(getByText(formContainer, 'Analysis status')).toBeInTheDocument();
        assertDropdownRenderedWithOptions(analysisStatusesOptions, formContainer);
        assertDropdownRenderedSelectOption(
          formContainer.querySelector('#vex-annotation-drawer__form__analysis-status-select')
        );
      });

      it('renders empty Analysis Status dropdown', () => {
        const { container } = renderWithOverridenData({ ...mockVexAnnotationDrawer, analysisStatusesOptions: [] });

        const formContainer = getFormContainer(container);

        expect(getByText(formContainer, 'Analysis status')).toBeInTheDocument();
        assertDropdownsOptionsNotRendered(analysisStatusesOptions, formContainer);
        assertDropdownRenderedSelectOption(
          formContainer.querySelector('#vex-annotation-drawer__form__analysis-status-select')
        );
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
        const { container } = renderWithOverridenData({ ...mockVexAnnotationDrawer, justificationsOptions: [] });

        const formContainer = getFormContainer(container);
        expect(getByText(formContainer, 'Justification')).toBeInTheDocument();
        assertDropdownsOptionsNotRendered(justificationsOptions, formContainer);
        assertDropdownRenderedSelectOption(
          formContainer.querySelector('#vex-annotation-drawer__form__justification-select')
        );
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
        const { container } = renderWithOverridenData({ ...mockVexAnnotationDrawer, responsesOptions: [] });

        const formContainer = getFormContainer(container);
        expect(getByText(formContainer, 'Response')).toBeInTheDocument();
        assertDropdownsOptionsNotRendered(responsesOptions, formContainer);
        assertDropdownRenderedSelectOption(
          formContainer.querySelector('#vex-annotation-drawer__form__response-select')
        );
      });

      it('renders Save button', () => {
        renderDefaultComponent();
        expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument();
      });
    });

    describe('Render form validation errors', () => {
      it('renders error message because no Analysis Status was selected in dropdown before saving', () => {
        const { container } = renderWithOverridenData({
          ...mockVexAnnotationDrawer,
          analysisStatus: undefined,
        });

        const footerContainer = container.querySelector('footer');
        const saveButton = screen.getByRole('button', { name: 'Save' });
        expect(saveButton).toBeInTheDocument();
        fireEvent.click(saveButton);

        expect(getByText(footerContainer, /There were validation errors./)).toBeInTheDocument();
        expect(
          getByText(footerContainer, /Analysis status field is required. Please select a value from the dropdown list/)
        ).toBeInTheDocument();
      });

      it('hides save button when required analysis status field error triggers and show back button when is valid', () => {
        const { container } = renderWithOverridenData({
          ...mockVexAnnotationDrawer,
          analysisStatus: 'SELECT',
        });

        const dropdown = container.querySelector('#vex-annotation-drawer__form__analysis-status-select');
        const validationErrorsMatcher = /There were validation errors./;
        const requiredValidationErrorMatcher = /Analysis status field is required. Please select a value from the dropdown list/;

        const footerContainer = container.querySelector('footer');
        const saveButton = screen.getByRole('button', { name: 'Save' });
        expect(saveButton).toBeInTheDocument();
        fireEvent.click(saveButton);

        expect(getByText(footerContainer, validationErrorsMatcher)).toBeInTheDocument();
        expect(getByText(footerContainer, requiredValidationErrorMatcher)).toBeInTheDocument();

        fireEvent.change(dropdown, {
          target: { value: analysisStatusesOptions[0].key },
        });

        expect(queryByText(footerContainer, validationErrorsMatcher)).not.toBeInTheDocument();
        expect(queryByText(footerContainer, requiredValidationErrorMatcher)).not.toBeInTheDocument();
      });
    });
  });

  describe('Form when isRowAnnotated prop is true', () => {
    const renderAnnotatedForm = (extraParams) =>
      renderWithOverridenData({
        ...mockVexAnnotationDrawer,
        isRowAnnotated: true,
        ...extraParams,
      });

    it('renders update button', () => {
      renderAnnotatedForm();
      expect(screen.getByRole('button', { name: 'Update' })).toBeInTheDocument();
    });

    it('will not render SELECT text in dropdown controls', () => {
      const { container } = renderAnnotatedForm();
      const formContainer = getFormContainer(container);

      expect(getByText(formContainer, 'Response')).toBeInTheDocument();
      assertDropdownDidNotRenderedSelectOption(
        formContainer.querySelector('#vex-annotation-drawer__form__analysis-status-select')
      );

      assertDropdownDidNotRenderedSelectOption(
        formContainer.querySelector('#vex-annotation-drawer__form__justification-select')
      );

      assertDropdownDidNotRenderedSelectOption(
        formContainer.querySelector('#vex-annotation-drawer__form__response-select')
      );
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
      const { container } = renderWithOverridenData({
        ...mockVexAnnotationDrawer,
        details: null,
      });

      const formContainer = getFormContainer(container);
      const descriptionTextArea = formContainer.querySelector('textarea');
      expect(getByText(formContainer, 'Description')).toBeInTheDocument();
      expect(queryByText(descriptionTextArea, mockVexAnnotationDrawer.details)).not.toBeInTheDocument();
      expect(descriptionTextArea.getAttribute('placeholder')).toBe('Entry');
    });
  });
});
