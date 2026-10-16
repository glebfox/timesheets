# Client screens

Source: `view/client/`, `component/imageupload/`

The client list, the client editor, and the client picker. What a client is: [client](../data-model/client.md).

## Clients

`clients`, menu Project Management. Shows clients as cards: the logo (or an avatar with initials), the name, and a contact information block listing the email, phone, address, and URL that are filled in; the URL is a link that opens in a new browser tab. When the list is empty, including when the filter matches nothing, a "There aren't any clients" placeholder replaces the cards. A name filter and pagination are kept in the URL.

- **Create** — the primary button, disabled for users who may not create clients; it navigates to the client editor.
- Each card's **Edit** button navigates to the editor; for users who may not edit clients it shows an eye icon and opens the editor read-only. Its **Remove** button is shown only to users who may delete clients and asks for confirmation.

## Client editor

`clients/:id`, opened as a page, with the name field focused.

- **Logo** — the image, or a placeholder when there is none, with upload and clear controls. The controls are hidden only when security forbids changing the logo attribute; an editor that is read-only for any other reason still shows them.
- **Name**.
- **Contact information** — email, phone, URL, and address.

## Client picker

"Select a client", the picker of the client field in the project editor: a name filter and pagination over the client names.

## Not covered

- All of these screens: no UI test exists for them.
- How the cards, logo, and placeholders look: only a live check shows it.
