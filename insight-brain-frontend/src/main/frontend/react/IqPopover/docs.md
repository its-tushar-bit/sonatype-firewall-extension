<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# `<IqPopover />`

This component is used to render a "popover" sliding out from the right hand side of the page, with any number of different uses for the content of that popover.

The popover will attempt to close when the user clicks anywhere outside of the popover on the page, or if the user hits the escape key.

Currently the popover doesn't have an "open" or closed state, if it is rendered it will appear.

## Props Table

| prop       | default value | required |
| ---------- | ------------- | -------- |
| `children` | `undefined`   | no       |
| `onClose`  | `() => {}`    | no       |
| `size`     | `"small"`     | no       |

...remaining props will be spread on the root element. The `className` prop will be merged with the `className` props already passed to the root element.

---

### There are 2 helper components to aid in using the `<IqPopover />`

`<IqPopoverHeader />` or `<IqPopover.Header />` - which adds a sticky container to the **top** of the popover with a thin dividing line (`.popover__divider`).

`<IqPopoverFooter />` or `<IqPopover.Footer />` - which adds a sticky container to the **bottom** of the popover with a thin dividing line (`.popover__divider`).

If the `<IqPopover.Header />` or `<IqPopover.Footer />`s omitted the `<IqPopover />` will continue to work as normal, it's just about adding some specific styling for consistency and efficency.

Both the `<IqPopover.Header />` and the `<IqPopover.Footer />` will pass any props down to their root element, which are a `header` and a `footer` respectively.

## Example

```jsx
<IqPopover id="id-added-to-root-popver-element" onClose={() => setToggleOpen(false)}>
  <IqPopover.Header>
    <h1>I am the Header</h1>
  </IqPopover.Header>
  <p>I love content!</p>
  <IqPopover.Footer>
    <b>This Footer needs work</b>
  </IqPopover.Footer>
</IqPopover>
```
