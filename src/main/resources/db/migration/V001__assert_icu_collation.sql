-- Guards the one piece of the collation setup that lives outside this repository.
DO
$$
    DECLARE
        db_provider "char";
        db_locale   text;
        db_encoding text;
    BEGIN
        SELECT datlocprovider, datlocale, pg_encoding_to_char(encoding)
        INTO db_provider, db_locale, db_encoding
        FROM pg_database
        WHERE datname = current_database();

        IF db_provider <> 'i' OR db_locale IS DISTINCT FROM 'und' OR db_encoding <> 'UTF8' THEN
            RAISE EXCEPTION
                'Database % must use the ICU provider, the und locale and UTF8 encoding (found provider=%, locale=%, encoding=%). Recreate it with: CREATE DATABASE % ENCODING ''UTF8'' LOCALE_PROVIDER icu ICU_LOCALE ''und'' TEMPLATE template0;',
                current_database(), db_provider, coalesce(db_locale, '<none>'), db_encoding, current_database();
        END IF;
    END
$$;
