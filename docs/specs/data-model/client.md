# Client

Source: `entity/client/Client.java`, `entity/misc/ContactInformation.java`

An organization that projects are done for.

## Attributes

- **Name** — required; unique among non-deleted clients (`docs/guidelines/database.md`).
- **Image** — optional logo.
- **Contact information** — email, phone, URL, and address, all optional; the email must be a valid address. Every client has this block, even when all of it is empty.

## Deletion

Deleting a client leaves its projects in place, still referring to it.
