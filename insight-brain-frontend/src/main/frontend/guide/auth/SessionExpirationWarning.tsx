/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { AlertDialog, Button, Flex } from '@radix-ui/themes';

interface SessionExpirationWarningProps {
  open: boolean;
  onStayLoggedIn: () => void;
  onLogOut: () => void;
}

export function SessionExpirationWarning({
  open,
  onStayLoggedIn,
  onLogOut,
}: SessionExpirationWarningProps) {
  return (
    <AlertDialog.Root open={open}>
      <AlertDialog.Content>
        <AlertDialog.Title>Session Expiring</AlertDialog.Title>
        <AlertDialog.Description>
          Your session is about to expire due to inactivity.
        </AlertDialog.Description>
        <Flex gap="3" mt="4" justify="end">
          <AlertDialog.Cancel>
            <Button onClick={onStayLoggedIn}>Stay Logged In</Button>
          </AlertDialog.Cancel>
          <AlertDialog.Action>
            <Button variant="soft" color="gray" onClick={onLogOut}>
              Log Out
            </Button>
          </AlertDialog.Action>
        </Flex>
      </AlertDialog.Content>
    </AlertDialog.Root>
  );
}
