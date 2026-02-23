/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import DataRetentionEditor from 'MainRoot/OrgsAndPolicies/dataRetentionEditor/DataRetentionEditor';
import { render, screen, fireEvent, axiosMockAdapter, within } from 'TestRoot/SpecUtil';
import {
  getOrganizationUrl,
  getOrganizationsUrl,
  getParentRetentionPoliciesUrl,
  getRetentionPoliciesUrl,
} from 'MainRoot/util/CLMLocation';

describe('Data Retention Editor component', () => {
  let axiosMock, renderComponent;

  const defaultPreloadedState = {
    router: {
      currentState: {
        name: 'management.view.organization',
        url: '/retention/org-id',
        data: {
          title: 'Organization Management',
          viewportSized: true,
        },
      },
      currentParams: {
        organizationId: 'org-id',
      },
    },
  };

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.onGet(getParentRetentionPoliciesUrl('org-id')).reply(200, {
      applicationReports: {
        stages: {
          develop: {
            inheritPolicy: false,
            enablePurging: true,
            maxCount: 100,
            maxAge: '10 years',
          },
          source: {
            inheritPolicy: false,
            enablePurging: false,
          },
          build: {
            inheritPolicy: false,
            enablePurging: false,
          },
          'stage-release': {
            inheritPolicy: false,
            enablePurging: false,
          },
          release: {
            inheritPolicy: false,
            enablePurging: false,
          },
          operate: {
            inheritPolicy: false,
            enablePurging: false,
          },
          'continuous-monitoring': {
            inheritPolicy: false,
            enablePurging: false,
          },
        },
      },
      successMetrics: {
        inheritPolicy: false,
        enablePurging: false,
      },
    });
    axiosMock.onGet(getRetentionPoliciesUrl('org-id')).reply(200, {
      applicationReports: {
        stages: {
          develop: {
            inheritPolicy: true,
            enablePurging: true,
            maxCount: 100,
            maxAge: '10 years',
          },
          source: {
            inheritPolicy: false,
            enablePurging: false,
          },
          build: {
            inheritPolicy: false,
            enablePurging: false,
          },
          'stage-release': {
            inheritPolicy: false,
            enablePurging: false,
          },
          release: {
            inheritPolicy: false,
            enablePurging: false,
          },
          operate: {
            inheritPolicy: false,
            enablePurging: false,
          },
          'continuous-monitoring': {
            inheritPolicy: false,
            enablePurging: false,
          },
        },
      },
      successMetrics: {
        inheritPolicy: false,
        enablePurging: false,
      },
    });
    axiosMock.onGet(getParentRetentionPoliciesUrl('org-id-2')).reply(200, {
      applicationReports: {
        stages: {
          develop: {
            inheritPolicy: false,
            enablePurging: true,
            maxCount: 100,
            maxAge: '10 years',
          },
          source: {
            inheritPolicy: false,
            enablePurging: false,
          },
          build: {
            inheritPolicy: false,
            enablePurging: false,
          },
          'stage-release': {
            inheritPolicy: false,
            enablePurging: false,
          },
          release: {
            inheritPolicy: false,
            enablePurging: false,
          },
          operate: {
            inheritPolicy: false,
            enablePurging: false,
          },
          'continuous-monitoring': {
            inheritPolicy: false,
            enablePurging: false,
          },
        },
      },
      successMetrics: {
        inheritPolicy: false,
        enablePurging: false,
      },
    });
    axiosMock.onGet(getRetentionPoliciesUrl('org-id-2')).reply(200, {
      applicationReports: {
        stages: {
          develop: {
            inheritPolicy: true,
            enablePurging: true,
            maxCount: 100,
            maxAge: '10 years',
          },
          source: {
            inheritPolicy: false,
            enablePurging: false,
          },
          build: {
            inheritPolicy: false,
            enablePurging: false,
          },
          'stage-release': {
            inheritPolicy: false,
            enablePurging: false,
          },
          release: {
            inheritPolicy: false,
            enablePurging: false,
          },
          operate: {
            inheritPolicy: false,
            enablePurging: false,
          },
          'continuous-monitoring': {
            inheritPolicy: false,
            enablePurging: false,
          },
        },
      },
      successMetrics: {
        inheritPolicy: false,
        enablePurging: false,
      },
    });
    axiosMock.onGet(getOrganizationsUrl()).reply(200, [
      {
        id: 'ROOT_ORGANIZATION_ID',
        name: 'ROOT_ORGANIZATION_NAME',
        parentOrganizationId: null,
      },
      {
        id: 'org-id',
        name: 'org-name',
        parentOrganizationId: 'someOtherId',
      },
      {
        id: 'org-id-2',
        name: 'org-name',
        parentOrganizationId: 'ROOT_ORGANIZATION_NAME',
      },
    ]);
    axiosMock.onGet(getOrganizationUrl('org-id')).reply(200, {
      id: 'org-id',
      name: 'org-name',
      parentOrganizationId: 'someOtherId',
    });
    renderComponent = (preloadedState) =>
      render(<DataRetentionEditor />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  describe('renders the following correctly upon loading: ', () => {
    it('a loading spinner', async () => {
      axiosMock.onGet(getParentRetentionPoliciesUrl('org-id')).reply(() => new Promise(() => {}));
      axiosMock.onGet(getRetentionPoliciesUrl('org-id')).reply(() => new Promise(() => {}));
      renderComponent();

      expect(await screen.findByText('Loading…')).toBeVisible();
    });

    it('the page title', async () => {
      renderComponent();

      expect(await screen.findByText('Application Reports')).toBeVisible();
    });

    it('stage titles', async () => {
      renderComponent();

      expect(await screen.findByRole('group', { name: 'Develop' })).toBeVisible();
      expect(await screen.findByRole('group', { name: 'Source' })).toBeVisible();
      expect(await screen.findByRole('group', { name: 'Build' })).toBeVisible();
      expect(await screen.findByRole('group', { name: 'Stage-Release' })).toBeVisible();
      expect(await screen.findByRole('group', { name: 'Release' })).toBeVisible();
      expect(await screen.findByRole('group', { name: 'Operate' })).toBeVisible();
      expect(await screen.findByRole('group', { name: 'Continuous-Monitoring' })).toBeVisible();
    });
  });

  it('shows an error message upon loading error', async () => {
    axiosMock.onGet(getParentRetentionPoliciesUrl('ROOT_ORGANIZATION_ID')).reply(500);
    axiosMock.onGet(getRetentionPoliciesUrl('org-id')).reply(500);
    renderComponent();
    const alert = await screen.findByRole('alert');

    expect(alert).toBeVisible();
    expect(alert).toHaveTextContent(/an error occurred loading data/i);
  });

  describe("'s radio buttons", () => {
    it('has form validation errors when the same radio button is clicked', async () => {
      renderComponent();
      const developFieldset = await screen.findByRole('group', { name: 'Develop' }),
        developSameRadio = await within(developFieldset).findByRole('radio', {
          name: /inherit \(keep at most 10 years, 100 reports\)/i,
        }),
        updateButton = await screen.findByRole('button');

      expect(developSameRadio).toBeVisible();
      expect(developSameRadio).toHaveAttribute('checked');

      fireEvent.click(developSameRadio);
      fireEvent.click(updateButton);

      const alert = await screen.findByRole('alert');

      expect(alert).toHaveTextContent('There were validation errors. There are no changes to save.');
    });
  });

  describe("'s custom radio button and input fields", () => {
    it('are filled in if custom purging is true', async () => {
      axiosMock.onGet(getRetentionPoliciesUrl('org-id')).reply(200, {
        applicationReports: {
          stages: {
            develop: {
              inheritPolicy: false,
              enablePurging: true,
              maxCount: 200,
              maxAge: '20 weeks',
            },
            source: {
              inheritPolicy: false,
              enablePurging: false,
            },
            build: {
              inheritPolicy: false,
              enablePurging: false,
            },
            'stage-release': {
              inheritPolicy: false,
              enablePurging: false,
            },
            release: {
              inheritPolicy: false,
              enablePurging: false,
            },
            operate: {
              inheritPolicy: false,
              enablePurging: false,
            },
            'continuous-monitoring': {
              inheritPolicy: false,
              enablePurging: false,
            },
          },
        },
        successMetrics: {
          inheritPolicy: false,
          enablePurging: false,
        },
      });
      renderComponent();
      const developFieldset = await screen.findByRole('group', { name: 'Develop' }),
        textInputs = await within(developFieldset).findAllByRole('textbox'),
        ageInput = textInputs[0],
        unitDropdown = await within(developFieldset).findByRole('combobox'),
        countInput = textInputs[1];

      expect(ageInput.value).toBe('20');
      expect(unitDropdown.value).toBe('weeks');
      expect(countInput.value).toBe('200');
    });

    it('are filled in if custom purging is true for multiple stages', async () => {
      axiosMock.onGet(getRetentionPoliciesUrl('org-id')).reply(200, {
        applicationReports: {
          stages: {
            develop: {
              inheritPolicy: false,
              enablePurging: true,
              maxCount: 200,
              maxAge: '20 weeks',
            },
            source: {
              inheritPolicy: false,
              enablePurging: true,
              maxCount: 300,
              maxAge: '30 months',
            },
            build: {
              inheritPolicy: false,
              enablePurging: false,
            },
            'stage-release': {
              inheritPolicy: false,
              enablePurging: false,
            },
            release: {
              inheritPolicy: false,
              enablePurging: false,
            },
            operate: {
              inheritPolicy: false,
              enablePurging: false,
            },
            'continuous-monitoring': {
              inheritPolicy: false,
              enablePurging: false,
            },
          },
        },
        successMetrics: {
          inheritPolicy: false,
          enablePurging: false,
        },
      });
      renderComponent();
      const developFieldset = await screen.findByRole('group', { name: 'Develop' }),
        developTextInputs = await within(developFieldset).findAllByRole('textbox'),
        developAgeInput = developTextInputs[0],
        developUnitDropdown = await within(developFieldset).findByRole('combobox'),
        developCountInput = developTextInputs[1],
        sourceFieldset = await screen.findByRole('group', { name: 'Source' }),
        sourceTextInputs = await within(sourceFieldset).findAllByRole('textbox'),
        sourceAgeInput = sourceTextInputs[0],
        sourceUnitDropdown = await within(sourceFieldset).findByRole('combobox'),
        sourceCountInput = sourceTextInputs[1];

      expect(developAgeInput.value).toBe('20');
      expect(developUnitDropdown.value).toBe('weeks');
      expect(developCountInput.value).toBe('200');
      expect(sourceAgeInput.value).toBe('30');
      expect(sourceUnitDropdown.value).toBe('months');
      expect(sourceCountInput.value).toBe('300');
    });

    describe('has form validation errors', () => {
      describe('when the custom radio button is clicked', () => {
        it('and both input fields are empty', async () => {
          renderComponent();
          const developFieldset = await screen.findByRole('group', { name: 'Develop' }),
            customRadio = await within(developFieldset).findByRole('radio', { name: /custom/i }),
            updateButton = await screen.findByRole('button');

          fireEvent.click(customRadio);

          const textInputs = await within(developFieldset).findAllByRole('textbox'),
            ageInput = textInputs[0],
            countInput = textInputs[1];

          expect(textInputs.length).toBe(2);
          expect(ageInput.value).toBe('');
          expect(countInput.value).toBe('');

          fireEvent.click(updateButton);

          const alert = await screen.findByRole('alert');

          expect(alert).toHaveTextContent('There were validation errors. There are no changes to save.');
        });

        it('and time unit dropdown is changed, but no age input value', async () => {
          renderComponent();
          const developFieldset = await screen.findByRole('group', { name: 'Develop' }),
            customRadio = await within(developFieldset).findByRole('radio', { name: /custom/i }),
            updateButton = await screen.findByRole('button');

          fireEvent.click(customRadio);

          const dropdown = await within(developFieldset).findByRole('combobox');

          fireEvent.change(dropdown, { target: { value: 'days' } });

          expect(dropdown.value).toBe('days');

          fireEvent.click(updateButton);

          const alert = await screen.findAllByRole('alert');

          expect(alert[1]).toHaveTextContent(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
        });

        it('then the user inputs a valid value then removes it', async () => {
          renderComponent();
          const developFieldset = await screen.findByRole('group', { name: 'Develop' }),
            customRadio = await within(developFieldset).findByRole('radio', { name: /custom/i }),
            updateButton = await screen.findByRole('button');

          fireEvent.click(customRadio);

          const textInputs = await within(developFieldset).findAllByRole('textbox'),
            ageInput = textInputs[0];

          fireEvent.change(ageInput, { target: { value: 20 } });

          expect(ageInput.value).toBe('20');

          fireEvent.change(ageInput, { target: { value: '' } });

          expect(ageInput.value).toBe('');

          fireEvent.click(updateButton);

          const alert = await screen.findAllByRole('alert');

          expect(alert[1]).toHaveTextContent(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
        });
      });

      it('when a stage with custom purging is changed then reverted back to the original value', async () => {
        axiosMock.onGet(getRetentionPoliciesUrl('org-id')).reply(200, {
          applicationReports: {
            stages: {
              develop: {
                inheritPolicy: false,
                enablePurging: true,
                maxAge: '20 weeks',
              },
              source: {
                inheritPolicy: false,
                enablePurging: false,
              },
              build: {
                inheritPolicy: false,
                enablePurging: false,
              },
              'stage-release': {
                inheritPolicy: false,
                enablePurging: false,
              },
              release: {
                inheritPolicy: false,
                enablePurging: false,
              },
              operate: {
                inheritPolicy: false,
                enablePurging: false,
              },
              'continuous-monitoring': {
                inheritPolicy: false,
                enablePurging: false,
              },
            },
          },
          successMetrics: {
            inheritPolicy: false,
            enablePurging: false,
          },
        });
        renderComponent();

        const developFieldset = await screen.findByRole('group', { name: 'Develop' }),
          textInputs = await within(developFieldset).findAllByRole('textbox'),
          ageInput = textInputs[0],
          updateButton = await screen.findByRole('button');

        fireEvent.change(ageInput, { target: { value: '2' } });

        expect(ageInput.value).toBe('2');

        fireEvent.change(ageInput, { target: { value: '20' } });

        expect(ageInput.value).toBe('20');

        fireEvent.click(updateButton);

        const alert = await screen.findByRole('alert');

        expect(alert).toHaveTextContent('There were validation errors. There are no changes to save.');
      });

      describe('when multiple stages have custom purging and', () => {
        beforeEach(() => {
          axiosMock.onGet(getRetentionPoliciesUrl('org-id')).reply(200, {
            applicationReports: {
              stages: {
                develop: {
                  inheritPolicy: false,
                  enablePurging: true,
                  maxAge: '20 weeks',
                },
                source: {
                  inheritPolicy: false,
                  enablePurging: true,
                  maxCount: 300,
                },
                build: {
                  inheritPolicy: false,
                  enablePurging: false,
                },
                'stage-release': {
                  inheritPolicy: false,
                  enablePurging: false,
                },
                release: {
                  inheritPolicy: false,
                  enablePurging: false,
                },
                operate: {
                  inheritPolicy: false,
                  enablePurging: false,
                },
                'continuous-monitoring': {
                  inheritPolicy: false,
                  enablePurging: false,
                },
              },
            },
            successMetrics: {
              inheritPolicy: false,
              enablePurging: false,
            },
          });
        });

        it('any of the stages have a validation error', async () => {
          renderComponent();

          const developFieldset = await screen.findByRole('group', { name: 'Develop' }),
            textInputs = await within(developFieldset).findAllByRole('textbox'),
            ageInput = textInputs[0],
            updateButton = await screen.findByRole('button');

          fireEvent.change(ageInput, { target: { value: '' } });

          expect(ageInput.value).toBe('');

          const errorMessage = await within(developFieldset).findByText(/must be non-empty/i);

          expect(errorMessage).toBeVisible();

          fireEvent.click(updateButton);

          const alert = await screen.findAllByRole('alert');

          expect(alert[1]).toHaveTextContent(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
        });

        it('any of the stages have a validation error, then a new stage uses custom purging', async () => {
          renderComponent();

          const developFieldset = await screen.findByRole('group', { name: 'Develop' }),
            developTextInputs = await within(developFieldset).findAllByRole('textbox'),
            developAgeInput = developTextInputs[0],
            buildFieldsetAll = await screen.findAllByRole('group', { name: 'Build' }),
            buildFieldset = buildFieldsetAll[0],
            buildCustomRadio = await within(buildFieldset).findByRole('radio', { name: /custom/i }),
            updateButton = await screen.findByRole('button');

          fireEvent.change(developAgeInput, { target: { value: '' } });

          expect(developAgeInput.value).toBe('');

          const errorMessage = await within(developFieldset).findByText(/must be non-empty/i);

          expect(errorMessage).toBeVisible();

          fireEvent.click(buildCustomRadio);

          const buildTextInputs = await within(buildFieldset).findAllByRole('textbox'),
            buildAgeInput = buildTextInputs[0];

          fireEvent.change(buildAgeInput, { target: { value: 40 } });

          expect(buildAgeInput.value).toBe('40');

          fireEvent.click(updateButton);

          const alert = await screen.findAllByRole('alert');

          expect(alert[1]).toHaveTextContent(
            'There were validation errors. Unable to save: fields with invalid or missing data'
          );
        });
      });
    });

    it('shows no validation errors on valid input value', async () => {
      renderComponent();
      const developFieldset = await screen.findByRole('group', { name: 'Develop' }),
        customRadio = await within(developFieldset).findByRole('radio', { name: /custom/i });

      fireEvent.click(customRadio);

      const textInputs = await within(developFieldset).findAllByRole('textbox'),
        ageInput = textInputs[0];

      fireEvent.change(ageInput, { target: { value: '10' } });

      expect(ageInput.value).toBe('10');
      expect(screen.queryByText(/this field only accepts numbers 0-9/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/minimum allowed value is/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/maximum allowed value is/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/must be non-empty/i)).not.toBeInTheDocument();
    });

    describe('shows the correct error validation message when', () => {
      it('there is a non-numerical value or space in the age input box', async () => {
        renderComponent();
        const developFieldset = await screen.findByRole('group', { name: 'Develop' }),
          customRadio = await within(developFieldset).findByRole('radio', { name: /custom/i });

        fireEvent.click(customRadio);

        const textInputs = await within(developFieldset).findAllByRole('textbox'),
          ageInput = textInputs[0];

        fireEvent.change(ageInput, { target: { value: '10a' } });

        expect(ageInput.value).toBe('10a');

        let errorMessage = await within(developFieldset).findByText(/this field only accepts numbers 0-9/i);

        expect(errorMessage).toBeVisible();

        // Test blank space, but reset to valid first
        fireEvent.change(ageInput, { target: { value: '10' } });

        expect(ageInput.value).toBe('10');
        expect(within(developFieldset).queryByText(/this field only accepts numbers 0-9/i)).not.toBeInTheDocument();

        fireEvent.change(ageInput, { target: { value: ' 10' } });

        expect(ageInput.value).toBe(' 10');

        errorMessage = await within(developFieldset).findByText(/this field only accepts numbers 0-9/i);
        expect(errorMessage).toBeVisible();
      });

      it("'0' is typed into the age input box", async () => {
        renderComponent();
        const developFieldset = await screen.findByRole('group', { name: 'Develop' }),
          customRadio = await within(developFieldset).findByRole('radio', { name: /custom/i });

        fireEvent.click(customRadio);

        const textInputs = await within(developFieldset).findAllByRole('textbox'),
          ageInput = textInputs[0];

        fireEvent.change(ageInput, { target: { value: '0' } });

        expect(ageInput.value).toBe('0');

        const errorMessage = await within(developFieldset).findByText(/minimum allowed value is 1/i);

        expect(errorMessage).toBeVisible();
      });

      it('The number in the age input box exceeds max value', async () => {
        renderComponent();
        const developFieldset = await screen.findByRole('group', { name: 'Develop' }),
          customRadio = await within(developFieldset).findByRole('radio', { name: /custom/i });

        fireEvent.click(customRadio);

        const textInputs = await within(developFieldset).findAllByRole('textbox'),
          ageInput = textInputs[0];

        fireEvent.change(ageInput, { target: { value: '100' } });

        expect(ageInput.value).toBe('100');

        const errorMessage = await within(developFieldset).findByText(/maximum allowed value is 49/i);

        expect(errorMessage).toBeVisible();
      });

      it('A valid input is used then replaced with an empty string', async () => {
        renderComponent();
        const developFieldset = await screen.findByRole('group', { name: 'Develop' }),
          customRadio = await within(developFieldset).findByRole('radio', { name: /custom/i });

        fireEvent.click(customRadio);

        const textInputs = await within(developFieldset).findAllByRole('textbox'),
          ageInput = textInputs[0];

        fireEvent.change(ageInput, { target: { value: '10' } });

        expect(ageInput.value).toBe('10');

        fireEvent.change(ageInput, { target: { value: '' } });

        expect(ageInput.value).toBe('');

        const errorMessage = await within(developFieldset).findByText(/must be non-empty/i);

        expect(errorMessage).toBeVisible();
      });

      it('invalid inputs are used in reports input box', async () => {
        renderComponent();
        const developFieldset = await screen.findByRole('group', { name: 'Develop' }),
          customRadio = await within(developFieldset).findByRole('radio', { name: /custom/i }),
          resetInput = () => {
            fireEvent.change(reportInput, { target: { value: '10' } });
            expect(reportInput.value).toBe('10');
          };

        fireEvent.click(customRadio);

        const textInputs = await within(developFieldset).findAllByRole('textbox'),
          reportInput = textInputs[1];

        fireEvent.change(reportInput, { target: { value: '10a' } });

        expect(reportInput.value).toBe('10a');

        let errorMessage = await within(developFieldset).findByText(/this field only accepts numbers 0-9/i);

        expect(errorMessage).toBeVisible();

        resetInput();
        fireEvent.change(reportInput, { target: { value: '0' } });

        expect(reportInput.value).toBe('0');

        errorMessage = await within(developFieldset).findByText(/minimum allowed value is 1/i);

        expect(errorMessage).toBeVisible();

        resetInput();
        fireEvent.change(reportInput, { target: { value: '10000' } });

        expect(reportInput.value).toBe('10000');

        errorMessage = await within(developFieldset).findByText(/maximum allowed value is 9999/i);

        expect(errorMessage).toBeVisible();
      });

      it('a valid number for one time unit is invalid for another', async () => {
        renderComponent();
        const developFieldset = await screen.findByRole('group', { name: 'Develop' }),
          customRadio = await within(developFieldset).findByRole('radio', { name: /custom/i });

        fireEvent.click(customRadio);

        const dropdown = await within(developFieldset).findByRole('combobox'),
          textInput = await within(developFieldset).findAllByRole('textbox'),
          ageInput = textInput[0];

        fireEvent.change(dropdown, { target: { value: 'days' } });
        fireEvent.change(ageInput, { target: { value: '100' } });

        expect(dropdown.value).toBe('days');
        expect(ageInput.value).toBe('100');

        fireEvent.change(dropdown, { target: { value: 'years' } });

        expect(dropdown.value).toBe('years');

        const errorMessage = await within(developFieldset).findByText(/maximum allowed value is 49/i);

        expect(errorMessage).toBeVisible();
      });
    });
  });

  describe('when it is rendered for the Root Organization', () => {
    const preloadedStateRoot = {
      router: {
        currentState: {
          name: 'management.view.organization',
          url: '/retention/ROOT_ORGANIZATION_ID',
          data: {
            title: 'Organization Management',
            viewportSized: true,
          },
        },
        currentParams: {
          organizationId: 'ROOT_ORGANIZATION_ID',
        },
      },
    };

    beforeEach(() => {
      axiosMock.onGet(getParentRetentionPoliciesUrl('ROOT_ORGANIZATION_ID')).reply(200, {
        applicationReports: {
          stages: {
            develop: {
              inheritPolicy: false,
              enablePurging: true,
              maxCount: 100,
              maxAge: '10 years',
            },
            source: {
              inheritPolicy: false,
              enablePurging: false,
            },
            build: {
              inheritPolicy: false,
              enablePurging: false,
            },
            'stage-release': {
              inheritPolicy: false,
              enablePurging: false,
            },
            release: {
              inheritPolicy: false,
              enablePurging: false,
            },
            operate: {
              inheritPolicy: false,
              enablePurging: false,
            },
            'continuous-monitoring': {
              inheritPolicy: false,
              enablePurging: false,
            },
          },
        },
        successMetrics: {
          inheritPolicy: false,
          enablePurging: false,
        },
      });
    });

    it('does not show the inherit radio button or titles', () => {
      renderComponent(preloadedStateRoot);

      expect(screen.queryByText(/inherit/i)).not.toBeInTheDocument();
    });
  });

  it('triggers update upon clicking Update button', async () => {
    axiosMock.onPut(getRetentionPoliciesUrl('org-id')).reply(200);
    renderComponent();
    const developFieldset = await screen.findByRole('group', { name: 'Develop' }),
      developDontPurgeRadio = await within(developFieldset).findByRole('radio', { name: /don't purge/i }),
      updateButton = await screen.findByRole('button');

    fireEvent.click(developDontPurgeRadio);

    expect(updateButton).toBeVisible();

    fireEvent.click(updateButton);

    expect(axiosMock.history.put.length).toBe(1);
  });
});
