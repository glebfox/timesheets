# Client screens

Source: `view/client/`, `component/avatarupload/`

The client list, the client editor, and the client picker. What a client is: [client](../data-model/client.md).

## Clients

`clients`, menu Project Management. Shows clients as cards: the logo (or an avatar with initials), the name, and a contact information block listing the email, phone, address, and URL that are filled in; the URL is a link that opens in a new browser tab. When the list is empty, including when the filter matches nothing, a "There aren't any clients" placeholder replaces the cards. A name filter and pagination are kept when the browser page is reloaded.

- **Create** — the primary button, disabled for users who may not create clients; it navigates to the client editor.
- Each card's **Edit** button navigates to the editor; for users who may not edit clients it shows an eye icon and opens the editor read-only. Its **Remove** button is shown only to users who may delete clients and asks for confirmation.

## Client editor

`clients/:id`, opened as a page, with the name field focused.

- **Image** — the client's logo, in the same round field as the user editor's avatar ([users](users.md#user-editor)), with the same person placeholder. The field is read-only whenever the editor is.
- **Name**.
- **Contact information** — email, phone, URL, and address.

## Client picker

"Select a client", the picker of the client field in the project editor: a name filter and pagination over the client names.

## Not covered

- The client list, the client picker, and the client editor beyond its image field storing an upload in the client: no UI test exists for them.
- The image field in a read-only client editor: no test; read-only is tested only in the user editor. The field in a browser: as for the user editor's avatar ([users](users.md#not-covered)).
- How the cards, logo, and placeholders look: only a live check shows it.
