/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import TestConfigurationResults from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/TestConfigurationResults';
import TestConfigurationButton from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/TestConfigurationButton';
import { render, screen, axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getValidateScmConfigButtonUrl } from 'MainRoot/util/CLMLocation';
import { fireEvent } from '@testing-library/react';
import { mergeDeepRight } from 'ramda';

import 'TestRoot/SpecUtil';

const ownerType = 'application';
const ownerId = '0006b1bf904e45999ee1b4eb05d898fd';

const unconfiguredTestResponse = {
  configurationComplete: {
    valid: false,
    message: 'Some required values are missing or unsaved',
  },
  repoPrivate: null,
  repoPublic: null,
  tokenPermissions: null,
  sshConfiguration: null,
};

const configuredTestResponse = {
  configurationComplete: {
    valid: true,
    message: null,
  },
  repoPrivate: {
    valid: true,
    message: null,
  },
  repoPublic: {
    valid: false,
    message: null,
  },
  tokenPermissions: {
    valid: true,
    message: null,
  },
  sshConfiguration: {
    valid: true,
    message: null,
  },
};

const configurationTestErrorMessages = {
  configurationComplete: {
    valid: true,
    message: null,
  },
  repoPrivate: {
    valid: false,
    message: 'Unable to connect to repo: http://my.awesomerepo.com',
  },
  repoPublic: {
    valid: false,
    message: null,
  },
  tokenPermissions: {
    valid: false,
    message: 'Unable to test permissions: my-token',
  },
  sshConfiguration: {
    valid: false,
    message: 'Unable to determine the SSH URL.',
  },
};

const setSshEnabledState = (sshEnabled = { value: null, parentValue: null, isInherited: false }) => ({
  orgsAndPolicies: { sourceControlConfiguration: { sourceControl: { sshEnabled: sshEnabled } } },
});

describe('testConfiguration', () => {
  let renderComponent, axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  let defaultPreloadedState;
  beforeEach(() => {
    defaultPreloadedState = {
      router: {
        currentState: {
          name: 'management.edit.application.edit-source-control',
        },
        currentParams: {
          applicationPublicId: 'vulnerable-java-app',
        },
      },
      orgsAndPolicies: {
        root: {
          selectedOwner: {
            id: '0006b1bf904e45999ee1b4eb05d898fd',
            publicId: 'vulnerable-java-app',
            name: 'Vulnerable java app',
          },
        },
        sourceControlConfiguration: {
          scmConfigValidation: {
            result: undefined,
            error: null,
            loading: false,
          },
          owner: {
            id: '0006b1bf904e45999ee1b4eb05d898fd',
            publicId: 'vulnerable-java-app',
            name: 'Vulnerable java app',
          },
          isDirty: false,
        },
      },
    };

    axiosMock.onGet(getValidateScmConfigButtonUrl(ownerType, ownerId)).reply(200, unconfiguredTestResponse);
  });

  renderComponent = (preloadedState = {}) =>
    render(
      <>
        <TestConfigurationResults />
        <TestConfigurationButton isDisabled={false} />
      </>,
      { preloadedState: mergeDeepRight(defaultPreloadedState, preloadedState) }
    );

  function checkResults(parentList, expectedListItems) {
    expectedListItems.forEach(({ expectedIconClass, expectedText, expectedMessage }, i) => {
      const [icon, text, message] = parentList.children[i].children;
      expect(icon).toHaveClass(expectedIconClass);
      expect(text).toHaveTextContent(expectedText);
      if (expectedMessage) {
        expect(message).toHaveTextContent(expectedMessage);
      }
    });
  }

  describe('initial test configuration', () => {
    it('test configuration button should be rendered', () => {
      renderComponent();

      expect(screen.getByRole('button', { name: 'Test Configuration' })).toBeVisible();
    });

    it('test configuration results should not be rendered', () => {
      renderComponent();

      const findConfigurationTestResults = () => screen.getByText('Configuration Test Results');

      expect(findConfigurationTestResults).toThrowError(
        /Unable to find an element with the text: Configuration Test Results/
      );
    });

    it('test configuration results should be rendered when button pressed', async () => {
      renderComponent();

      const testConfigurationButton = screen.getByRole('button', { name: 'Test Configuration' });
      fireEvent.click(testConfigurationButton);

      expect(await screen.findByText('Configuration Test Results')).toBeVisible();

      const results = screen.getByRole('list');
      expect(results.children.length).toBe(3);
      checkResults(results, [
        {
          expectedIconClass: 'iq-source-control-check-error',
          expectedText: 'Configuration complete',
          expectedMessage: 'Some required values are missing or unsaved',
        },
        {
          expectedIconClass: 'iq-source-control-check-nok',
          expectedText: 'Private repository',
        },
        {
          expectedIconClass: 'iq-source-control-check-nok',
          expectedText: 'Sufficient token permissions',
        },
      ]);
    });
  });

  describe('success for private repository', () => {
    beforeEach(() => {
      axiosMock.onGet(getValidateScmConfigButtonUrl(ownerType, ownerId)).reply(200, configuredTestResponse);
    });

    it('test configuration results should be ok when backend is configured', async () => {
      renderComponent(setSshEnabledState({ value: true }));

      const testConfigurationButton = screen.getByRole('button', { name: 'Test Configuration' });
      fireEvent.click(testConfigurationButton);
      expect(await screen.findByText('Configuration Test Results')).toBeVisible();

      const results = screen.getByRole('list');
      expect(results.children.length).toBe(4);
      checkResults(results, [
        {
          expectedIconClass: 'iq-source-control-check-ok',
          expectedText: 'Configuration complete',
        },
        {
          expectedIconClass: 'iq-source-control-check-ok',
          expectedText: 'Private repository',
        },
        {
          expectedIconClass: 'iq-source-control-check-ok',
          expectedText: 'Sufficient token permissions',
        },
        {
          expectedIconClass: 'iq-source-control-check-ok',
          expectedText: 'SSH configuration complete',
        },
      ]);
    });

    it('test configuration results should be ok when backend is configured with ssh inherited', async () => {
      renderComponent(setSshEnabledState({ value: null, parentValue: true, isInherited: true }));

      const testConfigurationButton = screen.getByRole('button', { name: 'Test Configuration' });
      fireEvent.click(testConfigurationButton);
      expect(await screen.findByText('Configuration Test Results')).toBeVisible();

      const results = screen.getByRole('list');
      expect(results.children.length).toBe(4);
      checkResults(results, [
        {
          expectedIconClass: 'iq-source-control-check-ok',
          expectedText: 'Configuration complete',
        },
        {
          expectedIconClass: 'iq-source-control-check-ok',
          expectedText: 'Private repository',
        },
        {
          expectedIconClass: 'iq-source-control-check-ok',
          expectedText: 'Sufficient token permissions',
        },
        {
          expectedIconClass: 'iq-source-control-check-ok',
          expectedText: 'SSH configuration complete',
        },
      ]);
    });

    it('test configuration results should be ok when backend is configured without ssh', async () => {
      renderComponent();

      const testConfigurationButton = screen.getByRole('button', { name: 'Test Configuration' });
      fireEvent.click(testConfigurationButton);
      expect(await screen.findByText('Configuration Test Results')).toBeVisible();

      const results = screen.getByRole('list');
      expect(results.children.length).toBe(3);
      checkResults(results, [
        {
          expectedIconClass: 'iq-source-control-check-ok',
          expectedText: 'Configuration complete',
        },
        {
          expectedIconClass: 'iq-source-control-check-ok',
          expectedText: 'Private repository',
        },
        {
          expectedIconClass: 'iq-source-control-check-ok',
          expectedText: 'Sufficient token permissions',
        },
      ]);
    });
  });

  describe('success for public repository', () => {
    beforeEach(() => {
      axiosMock.onGet(getValidateScmConfigButtonUrl(ownerType, ownerId)).reply(200, {
        ...configuredTestResponse,
        repoPublic: {
          valid: true,
          message: null,
        },
        repoPrivate: {
          valid: false,
          message: null,
        },
      });
    });

    it('test configuration results should be ok when backend is configured', async () => {
      renderComponent(setSshEnabledState({ value: true }));

      const testConfigurationButton = screen.getByRole('button', { name: 'Test Configuration' });
      fireEvent.click(testConfigurationButton);
      expect(await screen.findByText('Configuration Test Results')).toBeVisible();

      const results = screen.getByRole('list');
      expect(results.children.length).toBe(3);
      checkResults(results, [
        {
          expectedIconClass: 'iq-source-control-check-ok',
          expectedText: 'Configuration complete',
        },
        {
          expectedIconClass: 'iq-source-control-check-ok',
          expectedText: 'Sufficient token permissions',
        },
        {
          expectedIconClass: 'iq-source-control-check-ok',
          expectedText: 'SSH configuration complete',
        },
      ]);
    });
  });

  describe('failure', () => {
    it('test configuration results should be rendered with error messages when button pressed', async () => {
      axiosMock.onGet(getValidateScmConfigButtonUrl(ownerType, ownerId)).reply(200, configurationTestErrorMessages);

      renderComponent(setSshEnabledState({ value: true }));

      const testConfigurationButton = screen.getByRole('button', { name: 'Test Configuration' });
      fireEvent.click(testConfigurationButton);
      expect(await screen.findByText('Configuration Test Results')).toBeVisible();

      const results = screen.getByRole('list');
      expect(results.children.length).toBe(4);
      checkResults(results, [
        {
          expectedIconClass: 'iq-source-control-check-ok',
          expectedText: 'Configuration complete',
        },
        {
          expectedIconClass: 'iq-source-control-check-error',
          expectedText: 'Private repository',
          expectedMessage: 'Unable to connect to repo: http://my.awesomerepo.com',
        },
        {
          expectedIconClass: 'iq-source-control-check-error',
          expectedText: 'Sufficient token permissions',
          expectedMessage: 'Unable to test permissions: my-token',
        },
        {
          expectedIconClass: 'iq-source-control-check-error',
          expectedText: 'SSH configuration complete',
          expectedMessage: 'Unable to determine the SSH URL.',
        },
      ]);
    });
  });

  describe('error', () => {
    it('error message should be rendered instead of results', async () => {
      axiosMock.onGet(getValidateScmConfigButtonUrl(ownerType, ownerId)).reply(404);

      renderComponent(setSshEnabledState({ value: true }));

      const testConfigurationButton = screen.getByRole('button', { name: 'Test Configuration' });
      fireEvent.click(testConfigurationButton);
      expect(await screen.findByText('Configuration Test Results')).toBeVisible();

      const errors = screen.getByRole('list');
      expect(errors).toHaveTextContent('An error occurred loading data. Error');
    });
  });
});
