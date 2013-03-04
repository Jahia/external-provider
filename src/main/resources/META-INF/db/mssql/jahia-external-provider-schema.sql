
    drop table jahia_external_mapping;

    drop table jahia_external_provider_id;

    create table jahia_external_mapping (
        internalUuid nvarchar(36) not null,
        externalId ntext not null,
        externalIdHash int null,
        providerKey nvarchar(255) not null,
        primary key (internalUuid)
    );

    create table jahia_external_provider_id (
        id int identity not null,
        providerKey nvarchar(255) not null,
        primary key (id)
    );

    create index jahia_external_mapping_index1 on jahia_external_mapping (externalIdHash, providerKey);

    create index jahia_external_provider_id_index1 on jahia_external_provider_id (providerKey);
