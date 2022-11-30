/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import DataRetentionEditor from 'MainRoot/OrgsAndPolicies/dataRetentionEditor/DataRetentionEditor';
import { render, screen, fireEvent, axiosMockAdapter, within } from 'TestRoot/SpecUtil';
import { getRetentionPoliciesUrl } from 'MainRoot/util/CLMLocation';

describe('Data Retention Editor component', () => {
  let axiosMock;
  let renderComponent;

  beforeAll(() => {
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
    axiosMock = axiosMockAdapter();
    renderComponent = (preloadedState) =>
      render(<DataRetentionEditor />, { preloadedState: preloadedState || defaultPreloadedState });
  });

  beforeEach(() => {
    axiosMock.onGet(getRetentionPoliciesUrl('ROOT_ORGANIZATION_ID')).reply(200, {
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
            enablePurging: true,
          },
          build: {
            inheritPolicy: false,
            enablePurging: true,
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

  describe('Network request: ', () => {
    it('makes the correct GET request', () => {
      renderComponent();
      expect(axiosMock.history.get.length).toBe(2);
      expect(axiosMock.history.get[0].url).toBe(getRetentionPoliciesUrl('ROOT_ORGANIZATION_ID'));
      expect(axiosMock.history.get[1].url).toBe(getRetentionPoliciesUrl('org-id'));
    });
  });

  describe('renders the following correctly upon loading: ', () => {
    it('a loading spinner', () => {
      renderComponent();
      expect(screen.getByText('Loading…')).toBeVisible();
    });

    it('the page title', async () => {
      renderComponent();
      expect(await screen.findByText('Application Reports')).toBeVisible();
    });

    it('initial disabled Update button', async () => {
      renderComponent();
      const updateButton = await screen.findByRole('button', {
        name: /submit disabled: there are no changes to save/i,
      });

      expect(updateButton).toBeVisible();
      expect(updateButton).toHaveClassName('disabled');
      fireEvent.click(updateButton);
      expect(axiosMock.history.put.length).toBe(0);
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
    axiosMock.onGet(getRetentionPoliciesUrl('ROOT_ORGANIZATION_ID')).reply(500);
    axiosMock.onGet(getRetentionPoliciesUrl('org-id')).reply(500);
    renderComponent();

    const alert = await screen.findByRole('alert');
    expect(alert).toBeVisible();
    expect(alert).toHaveTextContent(/an error occurred loading data/i);
  });

  describe("'s radio buttons", async () => {
    it('enable the update button when selecting a different radio button', async () => {
      renderComponent();
      const developFieldset = await screen.findByRole('group', { name: 'Develop' });
      const developDontPurgeRadio = await within(developFieldset).findByRole('radio', { name: /don't purge/i });
      expect(developDontPurgeRadio).toBeVisible();

      let updateButton = await screen.findByRole('button', { name: 'Submit disabled: There are no changes to save' });
      expect(updateButton).toBeVisible();
      expect(updateButton).toHaveClassName('disabled');

      fireEvent.click(developDontPurgeRadio);

      updateButton = await screen.findByRole('button', { name: 'Update' });
      expect(updateButton).toBeVisible();
      expect(updateButton).not.toHaveClassName('disabled');
    });

    it('disables the update button when selecting a different radio button, then clicking to the original button', async () => {
      renderComponent();
      const developFieldset = await screen.findByRole('group', { name: 'Develop' });
      const developDontPurgeRadio = await within(developFieldset).findByRole('radio', { name: /don't purge/i });
      const developSameRadio = await within(developFieldset).findByRole('radio', {
        name: /inherit \(keep at most 10 years, 100 reports\)/i,
      });

      expect(developDontPurgeRadio).toBeVisible();

      let updateButton = await screen.findByRole('button', { name: 'Submit disabled: There are no changes to save' });
      expect(updateButton).toBeVisible();
      expect(updateButton).toHaveClassName('disabled');

      fireEvent.click(developDontPurgeRadio);

      updateButton = await screen.findByRole('button', { name: 'Update' });
      expect(updateButton).toBeVisible();
      expect(updateButton).not.toHaveClassName('disabled');

      fireEvent.click(developSameRadio);
      updateButton = await screen.findByRole('button', { name: 'Submit disabled: There are no changes to save' });
      expect(updateButton).toHaveClassName('disabled');
    });

    it('changes nothing when the same radio button is clicked', async () => {
      renderComponent();
      const developFieldset = await screen.findByRole('group', { name: 'Develop' });
      const developSameRadio = await within(developFieldset).getByRole('radio', {
        name: /inherit \(keep at most 10 years, 100 reports\)/i,
      });
      expect(developSameRadio).toBeVisible();
      expect(developSameRadio).toHaveAttribute('checked');

      let updateButton = await screen.findByRole('button', { name: 'Submit disabled: There are no changes to save' });
      expect(updateButton).toBeVisible();
      expect(updateButton).toHaveClassName('disabled');

      fireEvent.click(developSameRadio);

      expect(updateButton).toBeVisible();
      expect(updateButton).toHaveClassName('disabled');
    });
  });

  describe("'s custom radio button and input fields", async () => {
    it('are filled in if custom purging is true', async () => {
      await axiosMock.onGet(getRetentionPoliciesUrl('org-id')).reply(200, {
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
            },
            build: {
              inheritPolicy: false,
              enablePurging: true,
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
      await renderComponent();
      const developFieldset = await screen.findByRole('group', { name: 'Develop' });
      const textInputs = await within(developFieldset).findAllByRole('textbox');
      const ageInput = textInputs[0];
      const unitDropdown = await within(developFieldset).findByRole('combobox');
      const countInput = textInputs[1];
      expect(ageInput.value).toBe('20');
      expect(unitDropdown.value).toBe('weeks');
      expect(countInput.value).toBe('200');
    });

    it('are filled in if custom purging is true for multiple stages', async () => {
      await axiosMock.onGet(getRetentionPoliciesUrl('org-id')).reply(200, {
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
              enablePurging: true,
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
      await renderComponent();

      const developFieldset = await screen.findByRole('group', { name: 'Develop' });
      const developTextInputs = await within(developFieldset).findAllByRole('textbox');
      const developAgeInput = developTextInputs[0];
      const developUnitDropdown = await within(developFieldset).findByRole('combobox');
      const developCountInput = developTextInputs[1];
      expect(developAgeInput.value).toBe('20');
      expect(developUnitDropdown.value).toBe('weeks');
      expect(developCountInput.value).toBe('200');

      const sourceFieldset = await screen.findByRole('group', { name: 'Source' });
      const sourceTextInputs = await within(sourceFieldset).findAllByRole('textbox');
      const sourceAgeInput = sourceTextInputs[0];
      const sourceUnitDropdown = await within(sourceFieldset).findByRole('combobox');
      const sourceCountInput = sourceTextInputs[1];
      expect(sourceAgeInput.value).toBe('30');
      expect(sourceUnitDropdown.value).toBe('months');
      expect(sourceCountInput.value).toBe('300');
    });

    describe('has the update button disabled', async () => {
      describe('when the custom radio button is clicked', () => {
        it('and both input fields are empty', async () => {
          renderComponent();
          const developFieldset = await screen.findByRole('group', { name: 'Develop' });
          const customRadio = await within(developFieldset).findByRole('radio', {
            name: /custom/i,
          });
          const updateButton = await screen.findByRole('button', {
            name: 'Submit disabled: There are no changes to save',
          });
          fireEvent.click(customRadio);
          const textInputs = await within(developFieldset).findAllByRole('textbox');
          expect(textInputs.length).toBe(2);
          const ageInput = textInputs[0];
          const countInput = textInputs[1];
          expect(ageInput.value).toBe('');
          expect(countInput.value).toBe('');
          expect(updateButton).toHaveClassName('disabled');
        });

        it('and time unit dropdown is changed, but no age input value', async () => {
          renderComponent();
          const developFieldset = await screen.findByRole('group', { name: 'Develop' });
          const customRadio = await within(developFieldset).findByRole('radio', {
            name: /custom/i,
          });
          fireEvent.click(customRadio);
          const dropdown = await within(developFieldset).findByRole('combobox');
          fireEvent.change(dropdown, { target: { value: 'days' } });
          expect(dropdown.value).toBe('days');
          expect(await screen.queryByText(/must be non-empty/i)).toBeInTheDocument();
        });

        it('then the user inputs a valid value then removes it', async () => {
          renderComponent();
          const developFieldset = await screen.findByRole('group', { name: 'Develop' });
          const customRadio = await within(developFieldset).findByRole('radio', {
            name: /custom/i,
          });
          fireEvent.click(customRadio);
          const textInputs = await within(developFieldset).findAllByRole('textbox');
          const ageInput = textInputs[0];
          fireEvent.change(ageInput, { target: { value: 20 } });
          expect(ageInput.value).toBe('20');
          let updateButton = await screen.findByRole('button', { name: 'Update' });
          expect(updateButton).not.toHaveClassName('disabled');
          fireEvent.change(ageInput, { target: { value: '' } });
          expect(ageInput.value).toBe('');
          updateButton = await screen.findByRole('button', {
            name: /submit disabled: unable to save: fields with invalid or missing data/i,
          });
          expect(updateButton).toHaveClassName('disabled');
        });
      });

      it('when a stage with custom purging is changed then reverted back to the original value', async () => {
        await axiosMock.onGet(getRetentionPoliciesUrl('org-id')).reply(200, {
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
              },
              build: {
                inheritPolicy: false,
                enablePurging: true,
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
        await renderComponent();

        const developFieldset = await screen.findByRole('group', { name: 'Develop' });
        const textInputs = await within(developFieldset).findAllByRole('textbox');
        const ageInput = textInputs[0];
        fireEvent.change(ageInput, { target: { value: '2' } });
        expect(ageInput.value).toBe('2');
        fireEvent.change(ageInput, { target: { value: '20' } });
        expect(ageInput.value).toBe('20');
        const updateButton = await screen.findByRole('button', {
          name: 'Submit disabled: There are no changes to save',
        });
        expect(updateButton).toHaveClassName('disabled');
      });

      describe('when multiple stages have custom purging and', () => {
        beforeEach(async () => {
          await axiosMock.onGet(getRetentionPoliciesUrl('org-id')).reply(200, {
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
                  enablePurging: true,
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
          await renderComponent();
        });

        it('any of the stages have a validation error', async () => {
          renderComponent();
          const developFieldset = await screen.findByRole('group', { name: 'Develop' });
          const textInputs = await within(developFieldset).findAllByRole('textbox');
          const ageInput = textInputs[0];
          fireEvent.change(ageInput, { target: { value: '' } });
          expect(ageInput.value).toBe('');
          const errorMessage = await within(developFieldset).findByText(/must be non-empty/i);
          expect(errorMessage).toBeVisible();
          const updateButton = await screen.findByRole('button', {
            name: /submit disabled: unable to save: fields with invalid or missing data/i,
          });
          expect(updateButton).toHaveClassName('disabled');
        });

        it('any of the stages have a validation error, then a new stage uses custom purging', async () => {
          renderComponent();
          const developFieldset = await screen.findByRole('group', { name: 'Develop' });
          const developTextInputs = await within(developFieldset).findAllByRole('textbox');
          const developAgeInput = developTextInputs[0];
          fireEvent.change(developAgeInput, { target: { value: '' } });
          expect(developAgeInput.value).toBe('');
          const errorMessage = await within(developFieldset).findByText(/must be non-empty/i);
          expect(errorMessage).toBeVisible();
          const updateButton = await screen.findByRole('button', {
            name: /submit disabled: unable to save: fields with invalid or missing data/i,
          });
          expect(updateButton).toHaveClassName('disabled');

          const buildFieldsetAll = await screen.findAllByRole('group', { name: 'Build' });
          const buildFieldset = buildFieldsetAll[0];
          const buildCustomRadio = await within(buildFieldset).findByRole('radio', {
            name: /custom/i,
          });
          fireEvent.click(buildCustomRadio);
          const buildTextInputs = await within(buildFieldset).findAllByRole('textbox');
          const buildAgeInput = buildTextInputs[0];
          fireEvent.change(buildAgeInput, { target: { value: 40 } });
          expect(buildAgeInput.value).toBe('40');
          expect(updateButton).toHaveClassName('disabled');
        });
      });
    });

    describe('enables the update button', async () => {
      describe("when a stage's custom radio button is clicked", () => {
        it('and only the age input is filled in', async () => {
          renderComponent();
          const developFieldset = await screen.findByRole('group', { name: 'Develop' });
          const customRadio = await within(developFieldset).findByRole('radio', {
            name: /custom/i,
          });
          fireEvent.click(customRadio);
          const textInputs = await within(developFieldset).findAllByRole('textbox');
          const ageInput = textInputs[0];
          const countInput = textInputs[1];
          fireEvent.change(ageInput, { target: { value: 20 } });
          expect(ageInput.value).toBe('20');
          expect(countInput.value).toBe('');
          let updateButton = await screen.findByRole('button', { name: 'Update' });
          expect(updateButton).not.toHaveClassName('disabled');
        });

        it('and only the reports input is filled in', async () => {
          renderComponent();
          const developFieldset = await screen.findByRole('group', { name: 'Develop' });
          const customRadio = await within(developFieldset).findByRole('radio', {
            name: /custom/i,
          });
          fireEvent.click(customRadio);
          const textInputs = await within(developFieldset).findAllByRole('textbox');
          const ageInput = textInputs[0];
          const countInput = textInputs[1];
          fireEvent.change(countInput, { target: { value: 200 } });
          fireEvent.change(ageInput, { target: { value: '' } });
          expect(countInput.value).toBe('200');
          expect(ageInput.value).toBe('');
          let updateButton = await screen.findByRole('button', { name: 'Update' });
          expect(updateButton).not.toHaveClassName('disabled');
        });

        it('and both inputs are filled in', async () => {
          renderComponent();
          const developFieldset = await screen.findByRole('group', { name: 'Develop' });
          const customRadio = await within(developFieldset).findByRole('radio', {
            name: /custom/i,
          });
          fireEvent.click(customRadio);
          const textInputs = await within(developFieldset).findAllByRole('textbox');
          const ageInput = textInputs[0];
          const countInput = textInputs[1];
          fireEvent.change(ageInput, { target: { value: 20 } });
          fireEvent.change(countInput, { target: { value: 200 } });
          expect(ageInput.value).toBe('20');
          expect(countInput.value).toBe('200');
          let updateButton = await screen.findByRole('button', { name: 'Update' });
          expect(updateButton).not.toHaveClassName('disabled');
        });
      });
      it("when multiple stages have custom purging and a stage's input is changed", async () => {
        await axiosMock.onGet(getRetentionPoliciesUrl('org-id')).reply(200, {
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
                enablePurging: true,
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
        await renderComponent();
        const buildFieldset = await screen.findByRole('group', { name: 'Build' });
        const customRadio = await within(buildFieldset).findByRole('radio', {
          name: /custom/i,
        });
        fireEvent.click(customRadio);
        const textInputs = await within(buildFieldset).findAllByRole('textbox');
        const ageInput = textInputs[0];
        fireEvent.change(ageInput, { target: { value: 40 } });
        expect(ageInput.value).toBe('40');
        let updateButton = await screen.findByRole('button', { name: 'Update' });
        expect(updateButton).not.toHaveClassName('disabled');
      });
    });

    it('shows no validation errors on valid input value', async () => {
      renderComponent();
      const developFieldset = await screen.findByRole('group', { name: 'Develop' });
      const customRadio = await within(developFieldset).findByRole('radio', {
        name: /custom/i,
      });
      fireEvent.click(customRadio);
      const textInputs = await within(developFieldset).findAllByRole('textbox');
      const ageInput = textInputs[0];
      fireEvent.change(ageInput, { target: { value: '10' } });
      expect(ageInput.value).toBe('10');

      expect(await screen.queryByText(/this field only accepts numbers 0-9/i)).not.toBeInTheDocument();
      expect(await screen.queryByText(/minimum allowed value is/i)).not.toBeInTheDocument();
      expect(await screen.queryByText(/maximum allowed value is/i)).not.toBeInTheDocument();
      expect(await screen.queryByText(/must be non-empty/i)).not.toBeInTheDocument();
    });

    describe('shows the correct error validation message when', async () => {
      it('there is a non-numerical value or space in the age input box', async () => {
        renderComponent();
        const developFieldset = await screen.findByRole('group', { name: 'Develop' });
        const customRadio = await within(developFieldset).findByRole('radio', {
          name: /custom/i,
        });
        fireEvent.click(customRadio);
        const textInputs = await within(developFieldset).findAllByRole('textbox');
        const ageInput = textInputs[0];
        fireEvent.change(ageInput, { target: { value: '10a' } });
        expect(ageInput.value).toBe('10a');
        const errorMessage = await within(developFieldset).getByText(/this field only accepts numbers 0-9/i);
        expect(errorMessage).toBeVisible();
        // Test blank space, but reset to valid first
        fireEvent.change(ageInput, { target: { value: '10' } });
        expect(ageInput.value).toBe('10');
        expect(errorMessage).not.toBeInTheDocument;
        fireEvent.change(ageInput, { target: { value: ' 10' } });
        expect(ageInput.value).toBe(' 10');
        expect(errorMessage).toBeVisible();
      });

      it("'0' is typed into the age input box", async () => {
        renderComponent();
        const developFieldset = await screen.findByRole('group', { name: 'Develop' });
        const customRadio = await within(developFieldset).findByRole('radio', {
          name: /custom/i,
        });
        fireEvent.click(customRadio);
        const textInputs = await within(developFieldset).findAllByRole('textbox');
        const ageInput = textInputs[0];
        fireEvent.change(ageInput, { target: { value: '0' } });
        expect(ageInput.value).toBe('0');
        const errorMessage = await within(developFieldset).findByText(/minimum allowed value is 1/i);
        expect(errorMessage).toBeVisible();
      });

      it('The number in the age input box exceeds max value', async () => {
        renderComponent();
        const developFieldset = await screen.findByRole('group', { name: 'Develop' });
        const customRadio = await within(developFieldset).findByRole('radio', {
          name: /custom/i,
        });
        fireEvent.click(customRadio);
        const textInputs = await within(developFieldset).findAllByRole('textbox');
        const ageInput = textInputs[0];
        fireEvent.change(ageInput, { target: { value: '100' } });
        expect(ageInput.value).toBe('100');
        const errorMessage = await within(developFieldset).findByText(/maximum allowed value is 49/i);
        expect(errorMessage).toBeVisible();
      });

      it('A valid input is used then replaced with an empty string', async () => {
        renderComponent();
        const developFieldset = await screen.findByRole('group', { name: 'Develop' });
        const customRadio = await within(developFieldset).findByRole('radio', {
          name: /custom/i,
        });
        fireEvent.click(customRadio);
        const textInputs = await within(developFieldset).findAllByRole('textbox');
        const ageInput = textInputs[0];
        fireEvent.change(ageInput, { target: { value: '10' } });
        expect(ageInput.value).toBe('10');
        fireEvent.change(ageInput, { target: { value: '' } });
        expect(ageInput.value).toBe('');
        const errorMessage = await within(developFieldset).findByText(/must be non-empty/i);
        expect(errorMessage).toBeVisible();
      });

      it('invalid inputs are used in reports input box', async () => {
        const resetInput = () => {
          fireEvent.change(reportInput, { target: { value: '10' } });
          expect(reportInput.value).toBe('10');
        };
        renderComponent();
        const developFieldset = await screen.findByRole('group', { name: 'Develop' });
        const customRadio = await within(developFieldset).findByRole('radio', {
          name: /custom/i,
        });
        fireEvent.click(customRadio);
        const textInputs = await within(developFieldset).findAllByRole('textbox');
        const reportInput = textInputs[1];
        fireEvent.change(reportInput, { target: { value: '10a' } });
        expect(reportInput.value).toBe('10a');
        errorMessage = await within(developFieldset).getByText(/this field only accepts numbers 0-9/i);
        expect(errorMessage).toBeVisible();
        resetInput();
        fireEvent.change(reportInput, { target: { value: '0' } });
        expect(reportInput.value).toBe('0');
        errorMessage = await within(developFieldset).getByText(/minimum allowed value is 1/i);
        expect(errorMessage).toBeVisible();
        resetInput();
        fireEvent.change(reportInput, { target: { value: '10000' } });
        expect(reportInput.value).toBe('10000');
        let errorMessage = await within(developFieldset).findByText(/maximum allowed value is 9999/i);
        expect(errorMessage).toBeVisible();
      });

      it('a valid number for one time unit is invalid for another', async () => {
        renderComponent();
        const developFieldset = await screen.findByRole('group', { name: 'Develop' });
        const customRadio = await within(developFieldset).findByRole('radio', {
          name: /custom/i,
        });
        fireEvent.click(customRadio);
        const dropdown = await within(developFieldset).findByRole('combobox');
        const textInput = await within(developFieldset).findAllByRole('textbox');
        const ageInput = textInput[0];
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

  describe('when it is rendered for the Root Organization', async () => {
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
      axiosMock.onGet(getRetentionPoliciesUrl('ROOT_ORGANIZATION_ID')).reply(200, {
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

    it('does not show the inherit radio button or titles', async () => {
      renderComponent(preloadedStateRoot);
      expect(await screen.queryByText(/inherit/i)).not.toBeInTheDocument();
    });
  });

  it('triggers update upon clicking Update button', async () => {
    axiosMock.onPut(getRetentionPoliciesUrl('org-id')).reply(200);
    renderComponent();
    const developFieldset = await screen.findByRole('group', { name: 'Develop' });
    const developDontPurgeRadio = await within(developFieldset).findByRole('radio', { name: /don't purge/i });
    fireEvent.click(developDontPurgeRadio);

    const updateButton = await screen.findByRole('button', { name: /update/i });
    expect(updateButton).toBeVisible();
    expect(updateButton).not.toHaveClassName('disabled');

    fireEvent.click(updateButton);
    expect(axiosMock.history.put.length).toBe(1);
  });
});
