/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { createRef } from 'react';
import { render, screen, within } from 'TestRoot/SpecUtil';
import IqPopover, { IqPopoverFooter, IqPopoverHeader } from '../../../../main/frontend/react/IqPopover';

import userEvent from '@testing-library/user-event';

describe('IqPopover', function () {
  it('renders component', () => {
    renderComponent();

    const aside = screen.getByRole('complementary');
    expect(aside).toBeVisible();
    expect(aside.className).toContain('iq-popover');
  });

  it('forwards all extra props to the root .iq-popover element', () => {
    renderComponent({ id: 'thisone', tabIndex: 0 });

    const aside = screen.getByRole('complementary');
    expect(aside).toBeVisible();
    expect(aside.id).toEqual('thisone');
    expect(aside.tabIndex).toEqual(0);
  });

  it('merges the className attribute to the root element if className is passed as props', () => {
    renderComponent({ className: 'my-class' });

    const aside = screen.getByRole('complementary');
    expect(aside).toBeVisible();
    expect(aside.className).toEqual('iq-popover nx-viewport-sized iq-popover--small my-class');
  });

  it('if passed a ref it will forward that ref to the root .iq-popover element', () => {
    const ref = createRef();

    renderComponent({ ref });

    const aside = screen.getByRole('complementary');
    expect(aside).toEqual(ref.current);
  });

  describe('onClose', () => {
    it('calls onClose on clickaway event', async () => {
      const onClose = jest.fn();
      renderComponent({ onClose });

      const aside = screen.getByRole('complementary');
      expect(aside).toBeVisible();

      await userEvent.click(document.body);

      expect(onClose).toHaveBeenCalled();
    });

    it('calls onClose on escape key event', async () => {
      const onClose = jest.fn();
      renderComponent({ onClose });

      const aside = screen.getByRole('complementary');
      expect(aside).toBeVisible();

      await userEvent.keyboard('{Escape}');
      expect(onClose).toHaveBeenCalled();
    });
  });

  describe('IqPopover.Header & IqPopover.Footer', () => {
    describe('Header title', () => {
      it('renders the given title', () => {
        renderHeaderComponent();
        const heading = screen.getByRole('heading', { name: 'A title', level: 2 });
        expect(heading).toBeVisible();
        expect(heading.classList).toContain('nx-h2');
      });

      it('renders according to the specified size', () => {
        renderHeaderComponent({ headerSize: 'h3' });
        const heading = screen.getByRole('heading', { name: 'A title', level: 3 });
        expect(heading).toBeVisible();
        expect(heading.classList).toContain('nx-h3');
      });

      it('renders a button using passed props', async () => {
        const onClose = jest.fn();

        renderHeaderComponent({
          onClose,
          buttonId: 'some-button',
        });

        const button = screen.getByRole('button');
        expect(button).toBeVisible();

        expect(button.id).toEqual('some-button');

        const icon = within(button).getByRole('img', { hidden: true });
        expect(icon).toBeInTheDocument();
        expect(icon.getAttribute('data-icon')).toEqual('arrow-right-to-line');

        await userEvent.click(button);

        expect(onClose).toHaveBeenCalled();
      });

      function renderHeaderComponent(overrides = {}) {
        const props = {
          onClose: jest.fn(),
          buttonId: 'buttonId',
          headerTitle: 'A title',
          ...overrides,
        };

        return render(<IqPopover.Header {...props} />);
      }
    });

    it('will render all children correctly placing IqPopover.Header above and IqPopover.Footer below other children', () => {
      const { container } = render(
        <IqPopover>
          <p>I am some other content</p>
          <IqPopover.Footer>I am the Footer</IqPopover.Footer>

          <IqPopover.Header headerTitle="some title" onClose={jest.fn()}>
            I am the header
          </IqPopover.Header>

          <p>I go in a special content section</p>

          <p>Me too, I also go in content</p>
        </IqPopover>
      );

      const aside = screen.getByRole('complementary');
      expect(aside).toBeVisible();

      const expectedContent = 'I am some other contentI go in a special content sectionMe too, I also go in content';

      // content only contains our text content, not the header and footer
      const content = container.querySelector('.iq-popover__content');
      expect(content.textContent).toEqual(expectedContent);

      // header and footer are rendered
      const banner = screen.getByRole('banner');
      expect(banner).toBeVisible();
      const heading = within(banner).getByRole('heading', { name: 'some title', level: 2 });
      expect(heading).toBeVisible();

      const expectedBannerContent = 'some titleI am the header';

      // includes both the headerTitle and the child text passed to the IqPopover.Header
      expect(banner.textContent).toEqual(expectedBannerContent);

      const expectedFooterContent = 'I am the Footer';
      const footer = within(aside).getByRole('contentinfo');
      expect(footer).toBeVisible();
      expect(footer.textContent).toEqual(expectedFooterContent);

      // regardless of where it was passed children come between header and footer
      expect(aside.textContent).toEqual(expectedBannerContent + expectedContent + expectedFooterContent);
    });

    it('are equivalent to IqPopoverHeader and IqPopoverFooter', () => {
      expect(IqPopoverHeader).toBe(IqPopover.Header);
      expect(IqPopoverFooter).toBe(IqPopover.Footer);
    });
  });

  function renderComponent(props = {}) {
    return render(<IqPopover {...props} />);
  }
});
