/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { faAd } from '@fortawesome/pro-regular-svg-icons';

import { render, screen, within, fireEvent } from 'TestRoot/SpecUtil';
import { IqAssociationEditor, FieldType } from 'MainRoot/react/IqAssociationEditor';
import { angularToRscColorMap } from 'MainRoot/OrgsAndPolicies/utility/util';

import 'TestRoot/SpecUtil';

describe('IqAssociationEditor', () => {
  let minimalProps;
  let renderComponent;
  let onChangeSpy;

  beforeEach(() => {
    onChangeSpy = jest.fn().mockName('onChange');
    minimalProps = {
      items: [
        { name: 'item1', color: 'light-blue', isApplied: false },
        { name: 'item2', color: 'dark-blue', isApplied: true },
      ],
      label: 'label',
      onChange: onChangeSpy,
      fieldType: FieldType.CheckBox,
      icon: 'hexagon',
    };
    renderComponent = (additionalProps) => render(<IqAssociationEditor {...minimalProps} {...additionalProps} />);
  });

  it('renders label', () => {
    renderComponent();

    expect(screen.getByText(minimalProps.label)).toBeVisible();
  });

  it('renders error alert with default text', () => {
    renderComponent({ items: null });

    const fieldSet = screen.getByRole('group', {
      name: /label/i,
    });

    expect(screen.getByText('There are no items configured')).toBeVisible();
    expect(fieldSet).toHaveClass('iq-association-editor--full-width');
  });

  it('renders error alert with custom text', () => {
    const emptyItemsText = 'custom text';
    renderComponent({ items: null, emptyItemsText });

    const fieldSet = screen.getByRole('group', {
      name: /label/i,
    });

    expect(screen.getByText(emptyItemsText)).toBeVisible();
    expect(fieldSet).toHaveClass('iq-association-editor--full-width');
  });

  it('renders 2 column for 10 or more items', () => {
    const items = new Array(10).fill(0);

    renderComponent({ items });
    const fieldSet = screen.getByRole('group', {
      name: /label/i,
    });

    expect(fieldSet).toHaveClass('iq-association-editor--multi-column');
  });

  it('renders item"s description', () => {
    const description = 'name';

    renderComponent({ description });
    const fieldSet = screen.getByRole('group', {
      name: /label/i,
    });

    const item1 = within(fieldSet).getByText(minimalProps.items[0].name);
    expect(item1).toBeVisible();
    const item2 = within(fieldSet).getByText(minimalProps.items[1].name);
    expect(item2).toBeVisible();
  });

  it('renders a hexagon icon', () => {
    renderComponent();

    const fieldSet = screen.getByRole('group', {
      name: /label/i,
    });
    const icons = within(fieldSet).getAllByRole('presentation');

    expect(icons[0]).toBeVisible();
    expect(icons[0]).toHaveClass(`nx-selectable-color--${angularToRscColorMap[minimalProps.items[0].color]}`);
    expect(icons[1]).toBeVisible();
    expect(icons[1]).toHaveClass(`nx-selectable-color--${angularToRscColorMap[minimalProps.items[1].color]}`);
  });

  it('renders a Fontawesome icon', () => {
    renderComponent({ icon: faAd });

    const fieldSet = screen.getByRole('group', {
      name: /label/i,
    });
    const icons = within(fieldSet).getAllByRole('img', { hidden: true });

    expect(icons[0]).toBeVisible();
    expect(icons[0]).toHaveClass(`nx-selectable-color--${angularToRscColorMap[minimalProps.items[0].color]}`);
    expect(icons[1]).toBeVisible();
    expect(icons[1]).toHaveClass(`nx-selectable-color--${angularToRscColorMap[minimalProps.items[1].color]}`);
  });

  it('renders items with no color', () => {
    minimalProps.items[0].color = null;
    minimalProps.items[1].color = null;

    renderComponent();

    const fieldSet = screen.getByRole('group', {
      name: /label/i,
    });
    const icons = within(fieldSet).getAllByRole('presentation');

    expect(icons[0]).toBeVisible();
    expect(icons[0].getAttribute('class').includes('nx-selectable-color--')).toBe(false);
    expect(icons[1]).toBeVisible();
    expect(icons[1].getAttribute('class').includes('nx-selectable-color--')).toBe(false);
  });

  it('calls onChange after toggling item"s selectedParam', () => {
    renderComponent({ selectedParam: 'isApplied' });
    const fieldSet = screen.getByRole('group', {
      name: /label/i,
    });
    const icons = within(fieldSet).getAllByRole('presentation');
    fireEvent.click(icons[1]);

    expect(onChangeSpy).toHaveBeenCalledTimes(1);
    expect(onChangeSpy).toHaveBeenCalledWith({ ...minimalProps.items[1], isApplied: false });
  });
});
