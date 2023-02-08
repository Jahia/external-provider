CREATE EXTENSION IF NOT EXISTS lo;

ALTER TABLE jahia_external_mapping ADD COLUMN new_column oid;
UPDATE jahia_external_mapping SET new_column = cast(externalid as oid);
ALTER TABLE jahia_external_mapping DROP COLUMN externalid;
ALTER TABLE jahia_external_mapping RENAME COLUMN new_column TO externalid;

DROP TRIGGER IF EXISTS t_oid_jahia_external_mapping_externalid ON jahia_external_mapping;
CREATE TRIGGER t_oid_jahia_external_mapping_externalid BEFORE DELETE OR UPDATE ON jahia_external_mapping FOR EACH ROW EXECUTE FUNCTION lo_manage('externalid');
