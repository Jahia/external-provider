
    drop table jahia_external_mapping;

    drop table jahia_external_provider_id;

    create table jahia_external_mapping (
        internalUuid varchar(36) not null,
        externalId varchar(MAX) not null,
        externalIdHash int,
        providerKey varchar(255) not null,
        primary key (internalUuid)
    );

    create table jahia_external_provider_id (
        id int identity not null,
        providerKey varchar(255) not null,
        primary key (id)
    );

    create index jahia_external_mapping_index1 on jahia_external_mapping (externalIdHash, providerKey);

    create index jahia_external_provider_id_ix1 on jahia_external_provider_id (providerKey);
