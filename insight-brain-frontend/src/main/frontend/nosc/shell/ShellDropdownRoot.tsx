/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import { DropdownMenu } from '@radix-ui/themes';

/**
 * `DropdownMenu.Root` preset shared by the Nexus One TopNav menus
 * (solution switcher, system preferences, user menu).
 *
 * Owns the controlled `open` state and forces `modal={false}` so the rest of
 * the page stays interactive while a menu is open (Radix's default
 * `modal={true}` sets `pointer-events: none` on <body>, which locks the
 * shell).
 *
 * The open state is driven ENTIRELY by Radix's Trigger via `onOpenChange`.
 * Do NOT add an onClick toggle on the trigger: Radix's Trigger already
 * toggles `open` on pointerdown, so a second onClick toggle fires on the same
 * click and immediately re-closes the menu (the original "menus do nothing"
 * bug). Children are the usual `<DropdownMenu.Trigger>` + `<DropdownMenu.Content>`.
 *
 * Open menus also rely on the `.rt-PopperContent` / `.rt-TooltipContent`
 * z-index rules in nexus-one.css to paint above the shell content region.
 */
export default function ShellDropdownRoot({
  children,
}: {
  readonly children: React.ReactNode;
}): JSX.Element {
  const [open, setOpen] = useState(false);

  return (
    <DropdownMenu.Root open={open} onOpenChange={setOpen} modal={false}>
      {children}
    </DropdownMenu.Root>
  );
}
