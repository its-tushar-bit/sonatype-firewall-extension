/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { screen, within } from 'TestRoot/SpecUtil';
import { isEmpty } from 'ramda';

export const testSourceControlContainers = async (assertionResults) => {
  const sectionContainers = await screen.findAllByRole('group');
  expect(sectionContainers.length).toBe(assertionResults.length);
  sectionContainers.forEach((sectionContainer, index) => {
    const section = assertionResults[index];
    if (!section || isEmpty(section)) return;
    applyAssertion({ ...section.visibility, element: sectionContainer });
    applyAssertion({ ...section.isDisabled, element: sectionContainer });
    if (section.type === 'visibility-only') {
      return;
    }
    if (section.type === 'combobox') {
      const [inherit, override] = within(sectionContainer).getAllByRole('radio');
      const combobox = within(sectionContainer).getByRole('combobox');
      section.assertions.forEach((assertion) => {
        applyAssertion({ ...assertion, element: combobox });
      });
      expect(combobox).toHaveValue(section.value);
      expect(inherit.checked).toBe(section.inherit);
      expect(override.checked).toBe(section.override);
    } else if (section.type === 'credentials') {
      if (section.isRadioVisible) {
        const [inherit, override] = within(sectionContainer).getAllByRole('radio');
        expect(inherit.checked).toBe(section.inherit);
        expect(override.checked).toBe(section.override);
      } else {
        const radio = within(sectionContainer).queryAllByRole('radio');
        expect(radio.length).toBe(0);
      }
      const tokenInputWrapper = within(sectionContainer)[section.tokenQuery]('token-input');
      const usernameInputWrapper = within(sectionContainer)[section.usernameQuery]('username-input');

      // Token input is type="password", not textbox role, so use querySelector
      const tokenInput = tokenInputWrapper ? tokenInputWrapper.querySelector('input') : null;
      const usernameInput = usernameInputWrapper ? usernameInputWrapper.querySelector('input') : null;

      section.tokenAssertions.forEach((assertion) => {
        applyAssertion({ ...assertion, element: tokenInput });
      });
      section.usernameAssertions.forEach((assertion) => {
        applyAssertion({ ...assertion, element: usernameInput });
      });
      if (tokenInput) expect(tokenInput).toHaveValue(section.tokenValue);
      if (usernameInput) expect(usernameInput).toHaveValue(section.usernameValue);
    } else if (section.type === 'textbox') {
      const [inherit, override] = within(sectionContainer).getAllByRole('radio');
      const textbox = within(sectionContainer).getByRole('textbox');
      section.assertions.forEach((assertion) => {
        applyAssertion({ ...assertion, element: textbox });
      });
      expect(textbox).toHaveValue(section.value);
      expect(inherit.checked).toBe(section.inherit);
      expect(override.checked).toBe(section.override);
    } else if (section.type === 'radio') {
      const [inherit, enabled, disabled] = within(sectionContainer).getAllByRole('radio');
      expect(inherit.checked).toBe(section.inherit);
      expect(enabled.checked).toBe(section.enabled);
      expect(disabled.checked).toBe(section.disabled);
    }
  });
};

const applyAssertion = (assertion) => {
  if (assertion.isNegative) {
    expect(assertion.element).not[assertion.assertion](...(assertion.args ? assertion.args : []));
  } else {
    expect(assertion.element)[assertion.assertion](...(assertion.args ? assertion.args : []));
  }
};
