/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useEffect, useRef } from 'react';

const stack = [];

const EVENT = 'keydown';

const upHandler = ({ key }) => {
  if (key === 'Escape' || key === 'Esc') {
    if (stack.length > 0) {
      const callback = stack[stack.length - 1].current;
      callback();
    }
  }
};

function subscribeToEscape(ref) {
  if (stack.length === 0) {
    document.addEventListener(EVENT, upHandler);
  }
  stack.push(ref);

  return () => {
    if (stack.includes(ref)) {
      stack.splice(stack.indexOf(ref), 1);
    }
    if (stack.length === 0) {
      document.removeEventListener(EVENT, upHandler);
    }
  };
}

export default function useEscapeKeyStack(isListening, callback) {
  const componentSpecificRef = useRef();
  componentSpecificRef.current = callback;

  useEffect(() => {
    if (isListening) {
      return subscribeToEscape(componentSpecificRef);
    }
  }, [isListening, componentSpecificRef]);
}
