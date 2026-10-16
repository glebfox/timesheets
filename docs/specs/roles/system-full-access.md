# Full access

Source: `security/FullAccessRole.java`, `security/DatabaseUserRepository.java`, `liquibase/changelog/010-init-user.xml`, `liquibase/changelog/init-data/010-init-user.xml`

Resource role `system-full-access`, named "Full Access".

## Grants

Every operation on every entity, changing every attribute, every view, every menu item, and every specific permission, signing in to the UI included.

## Holders

- The `admin` user, in every environment.
- The demo user `anderson`, in the `dev` demo data only.
- The system user, which code running without a signed-in user authenticates as.

## Not covered

- What the role grants: no test checks it.
