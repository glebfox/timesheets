# Client screens

Source: `view/client/`, `component/avatarupload/`, `app/util/ComponentSupport.java`, `data/renderer/AvatarCellRenderer.java`

The client list, which is also the client picker, and the client editor. What a client is: [client](../data-model/client.md).

## Clients

`clients`, menu Project Management. Lists clients by name, with the name beside the logo, or beside an avatar with the name's initials when there is no logo; the email; the phone; the URL, a link that opens the site in a new browser tab; and the address, cut short with the full text in a popover. It offers a filter on any attribute, which opens on its "By name" («По названию») configuration, a single condition that keeps the clients whose name contains the text typed, and pagination, both kept when the browser page is reloaded; Create, Edit, and Remove. It is also the client picker of the [project editor](projects.md#project-editor). Empty, it shows the Clients icon and "No clients" («Нет клиентов»), with Refresh ([empty lists and tables](README.md#empty-lists-and-tables)).

## Client editor

`clients/:id`, with the name field focused. It opens as a page from the list, and as a 640px dialog from the list that the picker opens.

- **Image** — the client's logo, in the same round field as the user editor's avatar ([users](users.md#user-editor)), with the same person placeholder. The field is read-only whenever the editor is.
- **Name**.
- **Contact information** — a section under a divider heading: email, phone, URL, and the address across the form's width.

## Not covered

- The list: `ClientListViewUiTest` checks its columns and their order, the name column's avatar cell and the address column's popover, and the URL cell, a link to the site that opens in a new tab, or empty without a URL; `AvatarCellRendererTest` checks a client's cell without a logo, an avatar named after the client beside the name; `GridEmptyStatesUiTest` checks the empty state. The filter and its By name configuration, pagination, the order by name, and Create, Edit, and Remove: no test.
- The picker: `ClientListViewUiTest` checks that the project editor's client picker opens the client list in a dialog with Select and Cancel. Choosing a client there: no test.
- The editor: `ClientDetailViewUiTest` checks the Contact information heading and the address across the form's width; `AvatarUploadEditorsUiTest` checks that the image field stores an upload in the client. The editor as a dialog from the picker: no test.
- The image field in a read-only client editor: no test; read-only is tested only in the user editor. The field in a browser: as for the user editor's avatar ([users](users.md#not-covered)).
- How the screens look in a browser: the list's cells, the logos and avatars, the picker and the editor as dialogs, and the divider heading: only a live check shows them. Checked live on 2026-11-07, in English and Russian, light and dark: the logos and an initials avatar beside the names; the URL a link in the accent color, and an empty cell without one; the address on one line with an ellipsis and the full text in its popover; sorting by each contact column, with empty cells last in ascending order; the empty state under a filter that matches nothing; Remove asking for confirmation; the picker as a 1024px dialog whose Select stays disabled until a row is selected and sets the field, with Create and Edit opening the editor as a 640px dialog that Esc closes alone; the editor's 80px logo, the name field as wide as Email, the Contact information divider heading, the address across the form, and one column at a 375px window; and the logos loading after a switch to another tab and back. A long name ending with an ellipsis in a narrow Name column: not checked live.
