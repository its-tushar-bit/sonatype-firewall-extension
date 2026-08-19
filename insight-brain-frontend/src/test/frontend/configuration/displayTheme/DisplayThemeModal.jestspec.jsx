/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, within, configureStore, render, fireEvent } from 'TestRoot/SpecUtil';
import userEvent from '@testing-library/user-event';
import { Provider } from 'react-redux';
import DisplayThemeModal from 'MainRoot/configuration/displayTheme/DisplayThemeModal';
import reducers from 'MainRoot/reduxConfig/reducers';
import { actions } from 'MainRoot/configuration/displayTheme/displayThemeSlice';

describe('DisplayThemeModal', () => {
  let renderComponent,
    onCloseSpy = jest.fn(),
    mockStore;

  const minimalProps = {
    onClose: onCloseSpy,
  };

  const defaultPreloadedState = {
    productFeatures: {
      productFeatures: {
        'dark-mode': true,
      },
    },
  };

  const createMockStore = (preloadedState) => configureStore({ reducer: reducers, preloadedState });

  beforeEach(() => {
    localStorage.clear();

    renderComponent = (preloadedState) => {
      mockStore = createMockStore(preloadedState || defaultPreloadedState);
      mockStore.dispatch(actions.initialize());

      return render(
        <Provider store={mockStore}>
          <DisplayThemeModal {...minimalProps} />
        </Provider>
      );
    };
  });

  afterAll(() => {
    localStorage.clear();
  });

  it('should have correct contents', () => {
    renderComponent();

    const dialog = screen.getByRole('dialog', { name: 'Edit Display Theme' });
    expect(dialog).toBeInTheDocument();

    expect(within(dialog).getByRole('heading', { name: 'Edit Display Theme' })).toBeInTheDocument();

    const radioButtons = within(dialog).getAllByRole('radio');
    expect(radioButtons).toHaveLength(3);

    const systemSettingRadio = within(dialog).getByRole('radio', { name: 'System Setting' });
    expect(systemSettingRadio).toBeChecked();

    const darkModeRadio = within(dialog).getByRole('radio', { name: 'Dark Mode' });
    expect(darkModeRadio).not.toBeChecked();

    const lightModeRadio = within(dialog).getByRole('radio', { name: 'Light Mode' });
    expect(lightModeRadio).not.toBeChecked();

    expect(within(dialog).getByRole('button', { name: 'Close' })).toBeInTheDocument();
  });

  it('should call close handler when clicked', async () => {
    const user = userEvent.setup();
    renderComponent();

    const dialog = screen.getByRole('dialog', { name: 'Edit Display Theme' });
    expect(dialog).toBeInTheDocument();

    const closeButton = within(dialog).getByRole('button', { name: 'Close' });
    await user.click(closeButton);

    expect(onCloseSpy).toHaveBeenCalled();
  });

  it('when system setting is selected, it should set the theme to system setting', async () => {
    const user = userEvent.setup();
    renderComponent();

    const dialog = screen.getByRole('dialog', { name: 'Edit Display Theme' });
    expect(dialog).toBeInTheDocument();

    // Since system is selected by default, we need to set it to dark mode first
    const radioButtons = within(dialog).getAllByRole('radio');
    await user.click(radioButtons[1]);

    // Then set to system setting
    await user.click(radioButtons[0]);

    expect(localStorage.getItem('displayTheme')).toBe('system');
    expect(within(dialog).getByRole('radio', { name: 'System Setting' })).toBeChecked();
    expect(within(dialog).getByRole('radio', { name: 'Dark Mode' })).not.toBeChecked();
    expect(within(dialog).getByRole('radio', { name: 'Light Mode' })).not.toBeChecked();
  });

  it('when dark mode is selected, it should set the theme to dark mode', async () => {
    const user = userEvent.setup();
    renderComponent();

    const dialog = screen.getByRole('dialog', { name: 'Edit Display Theme' });
    expect(dialog).toBeInTheDocument();

    const radioButtons = within(dialog).getAllByRole('radio');
    await user.click(radioButtons[1]);

    expect(localStorage.getItem('displayTheme')).toBe('dark');
    expect(within(dialog).getByRole('radio', { name: 'System Setting' })).not.toBeChecked();
    expect(within(dialog).getByRole('radio', { name: 'Dark Mode' })).toBeChecked();
    expect(within(dialog).getByRole('radio', { name: 'Light Mode' })).not.toBeChecked();
  });

  it('when light mode is selected, it should set the theme to light mode', async () => {
    const user = userEvent.setup();
    renderComponent();

    const dialog = screen.getByRole('dialog', { name: 'Edit Display Theme' });
    expect(dialog).toBeInTheDocument();

    const radioButtons = within(dialog).getAllByRole('radio');
    await user.click(radioButtons[2]);

    expect(localStorage.getItem('displayTheme')).toBe('light');
    expect(within(dialog).getByRole('radio', { name: 'System Setting' })).not.toBeChecked();
    expect(within(dialog).getByRole('radio', { name: 'Dark Mode' })).not.toBeChecked();
    expect(within(dialog).getByRole('radio', { name: 'Light Mode' })).toBeChecked();
  });

  describe('should check the correct radio button when the modal is opened', () => {
    it('when display theme is set to system', () => {
      localStorage.setItem('displayTheme', 'system');
      renderComponent();

      const dialog = screen.getByRole('dialog', { name: 'Edit Display Theme' });
      expect(dialog).toBeInTheDocument();

      expect(within(dialog).getByRole('radio', { name: 'System Setting' })).toBeChecked();
      expect(within(dialog).getByRole('radio', { name: 'Dark Mode' })).not.toBeChecked();
      expect(within(dialog).getByRole('radio', { name: 'Light Mode' })).not.toBeChecked();
    });

    it('when display theme is set to dark', () => {
      localStorage.setItem('displayTheme', 'dark');
      renderComponent();

      const dialog = screen.getByRole('dialog', { name: 'Edit Display Theme' });
      expect(dialog).toBeInTheDocument();

      expect(within(dialog).getByRole('radio', { name: 'System Setting' })).not.toBeChecked();
      expect(within(dialog).getByRole('radio', { name: 'Dark Mode' })).toBeChecked();
      expect(within(dialog).getByRole('radio', { name: 'Light Mode' })).not.toBeChecked();
    });

    it('when display theme is set to light', () => {
      localStorage.setItem('displayTheme', 'light');
      renderComponent();

      const dialog = screen.getByRole('dialog', { name: 'Edit Display Theme' });
      expect(dialog).toBeInTheDocument();

      expect(within(dialog).getByRole('radio', { name: 'System Setting' })).not.toBeChecked();
      expect(within(dialog).getByRole('radio', { name: 'Dark Mode' })).not.toBeChecked();
      expect(within(dialog).getByRole('radio', { name: 'Light Mode' })).toBeChecked();
    });

    describe('storage event handling', () => {
      it('correctly applies theme based on event', () => {
        localStorage.setItem('displayTheme', 'dark');
        renderComponent();

        // Simulate a storage event to change the theme to light
        fireEvent(
          window,
          new StorageEvent('storage', {
            key: 'displayTheme',
            newValue: 'light',
            oldValue: 'dark',
          })
        );

        const dialog = screen.getByRole('dialog', { name: 'Edit Display Theme' });
        expect(dialog).toBeInTheDocument();

        expect(within(dialog).getByRole('radio', { name: 'System Setting' })).not.toBeChecked();
        expect(within(dialog).getByRole('radio', { name: 'Dark Mode' })).not.toBeChecked();
        expect(within(dialog).getByRole('radio', { name: 'Light Mode' })).toBeChecked();
      });

      it('does not apply theme if value is unchanged', () => {
        localStorage.setItem('displayTheme', 'system');
        renderComponent();

        // Simulate a storage event to change the theme to light
        fireEvent(
          window,
          new StorageEvent('storage', {
            key: 'displayTheme',
            newValue: 'system',
            oldValue: 'system',
          })
        );

        const dialog = screen.getByRole('dialog', { name: 'Edit Display Theme' });
        expect(dialog).toBeInTheDocument();

        expect(within(dialog).getByRole('radio', { name: 'System Setting' })).toBeChecked();
        expect(within(dialog).getByRole('radio', { name: 'Dark Mode' })).not.toBeChecked();
        expect(within(dialog).getByRole('radio', { name: 'Light Mode' })).not.toBeChecked();
      });
    });
  });

  describe('correctly saves and applies saved theme', () => {
    it('when stored theme is set to "system"', async () => {
      const user = userEvent.setup();
      const { rerender } = renderComponent();

      let dialog = screen.getByRole('dialog', { name: 'Edit Display Theme' });
      expect(dialog).toBeInTheDocument();

      const radioButtons = within(dialog).getAllByRole('radio');
      expect(radioButtons).toHaveLength(3);

      let systemSettingRadio = within(dialog).getByRole('radio', { name: 'System Setting' });
      let darkModeRadio = within(dialog).getByRole('radio', { name: 'Dark Mode' });
      let lightModeRadio = within(dialog).getByRole('radio', { name: 'Light Mode' });

      // Since theme is set to light initially, set theme to dark first
      await user.click(darkModeRadio);

      // Set theme back to light
      await user.click(systemSettingRadio);

      expect(systemSettingRadio).toBeChecked();
      expect(darkModeRadio).not.toBeChecked();
      expect(lightModeRadio).not.toBeChecked();

      await user.click(screen.getByRole('button', { name: 'Close' }));
      expect(onCloseSpy).toHaveBeenCalled();

      rerender(
        <Provider store={mockStore}>
          <DisplayThemeModal {...minimalProps} />
        </Provider>
      );

      dialog = screen.getByRole('dialog', { name: 'Edit Display Theme' });
      expect(dialog).toBeInTheDocument();

      expect(localStorage.getItem('displayTheme')).toBe('system');

      systemSettingRadio = within(dialog).getByRole('radio', { name: 'System Setting' });
      darkModeRadio = within(dialog).getByRole('radio', { name: 'Dark Mode' });
      lightModeRadio = within(dialog).getByRole('radio', { name: 'Light Mode' });

      expect(systemSettingRadio).toBeChecked();
      expect(darkModeRadio).not.toBeChecked();
      expect(lightModeRadio).not.toBeChecked();
    });

    it('when stored theme is set to "dark"', async () => {
      const user = userEvent.setup();
      const { rerender } = renderComponent();

      let dialog = screen.getByRole('dialog', { name: 'Edit Display Theme' });
      expect(dialog).toBeInTheDocument();

      const radioButtons = within(dialog).getAllByRole('radio');
      expect(radioButtons).toHaveLength(3);

      let systemSettingRadio = within(dialog).getByRole('radio', { name: 'System Setting' });
      let darkModeRadio = within(dialog).getByRole('radio', { name: 'Dark Mode' });
      let lightModeRadio = within(dialog).getByRole('radio', { name: 'Light Mode' });

      await user.click(darkModeRadio);

      expect(systemSettingRadio).not.toBeChecked();
      expect(darkModeRadio).toBeChecked();
      expect(lightModeRadio).not.toBeChecked();

      await user.click(screen.getByRole('button', { name: 'Close' }));
      expect(onCloseSpy).toHaveBeenCalled();

      rerender(
        <Provider store={mockStore}>
          <DisplayThemeModal {...minimalProps} />
        </Provider>
      );

      dialog = screen.getByRole('dialog', { name: 'Edit Display Theme' });
      expect(dialog).toBeInTheDocument();

      expect(localStorage.getItem('displayTheme')).toBe('dark');

      systemSettingRadio = within(dialog).getByRole('radio', { name: 'System Setting' });
      darkModeRadio = within(dialog).getByRole('radio', { name: 'Dark Mode' });
      lightModeRadio = within(dialog).getByRole('radio', { name: 'Light Mode' });

      expect(systemSettingRadio).not.toBeChecked();
      expect(darkModeRadio).toBeChecked();
      expect(lightModeRadio).not.toBeChecked();
    });

    it('when stored theme is set to "light"', async () => {
      const user = userEvent.setup();
      const { rerender } = renderComponent();

      let dialog = screen.getByRole('dialog', { name: 'Edit Display Theme' });
      expect(dialog).toBeInTheDocument();

      const radioButtons = within(dialog).getAllByRole('radio');
      expect(radioButtons).toHaveLength(3);

      let systemSettingRadio = within(dialog).getByRole('radio', { name: 'System Setting' });
      let darkModeRadio = within(dialog).getByRole('radio', { name: 'Dark Mode' });
      let lightModeRadio = within(dialog).getByRole('radio', { name: 'Light Mode' });

      await user.click(lightModeRadio);

      expect(systemSettingRadio).not.toBeChecked();
      expect(darkModeRadio).not.toBeChecked();
      expect(lightModeRadio).toBeChecked();

      await user.click(screen.getByRole('button', { name: 'Close' }));
      expect(onCloseSpy).toHaveBeenCalled();

      rerender(
        <Provider store={mockStore}>
          <DisplayThemeModal {...minimalProps} />
        </Provider>
      );

      dialog = screen.getByRole('dialog', { name: 'Edit Display Theme' });
      expect(dialog).toBeInTheDocument();

      expect(localStorage.getItem('displayTheme')).toBe('light');

      systemSettingRadio = within(dialog).getByRole('radio', { name: 'System Setting' });
      darkModeRadio = within(dialog).getByRole('radio', { name: 'Dark Mode' });
      lightModeRadio = within(dialog).getByRole('radio', { name: 'Light Mode' });

      expect(systemSettingRadio).not.toBeChecked();
      expect(darkModeRadio).not.toBeChecked();
      expect(lightModeRadio).toBeChecked();
    });
  });
});
