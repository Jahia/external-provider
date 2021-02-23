
    create table jahia_external_mapping (
        internalUuid varchar(36) not null,
        externalId longtext not null,
        externalIdHash integer,
        providerKey varchar(255) not null,
        primary key (internalUuid)
    ) ENGINE=InnoDB;

    create table jahia_external_provider_id (
        id integer not null auto_increment,
        providerKey varchar(255) not null,
        primary key (id)
    ) ENGINE=InnoDB;

    create index jahia_external_mapping_index1 on jahia_external_mapping (externalIdHash, providerKey);

    create index jahia_external_provider_id_ix1 on jahia_external_provider_id (providerKey);
