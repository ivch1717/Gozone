-- Hibernate schema update does not reliably replace existing enum CHECKs.
-- Run before Hibernate: on a fresh database these statements are skipped and
-- Hibernate creates the table with the current InboxStatus values itself.
BEGIN;

ALTER TABLE IF EXISTS inbox
    DROP CONSTRAINT IF EXISTS inbox_status_check;

ALTER TABLE IF EXISTS inbox
    ADD CONSTRAINT inbox_status_check
    CHECK (status IN ('NEW', 'PROCESSED', 'FAILED'));

COMMIT;
