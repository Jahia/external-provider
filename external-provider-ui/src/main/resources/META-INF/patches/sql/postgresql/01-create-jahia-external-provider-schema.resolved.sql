
    create table jahia_external_mapping (
        internalUuid varchar(36) not null,
        externalId text not null,
        externalIdHash int4,
        providerKey varchar(255) not null,
        primary key (internalUuid)
    );

    create table jahia_external_provider_id (
        id int4 not null,
        providerKey varchar(255) not null,
        primary key (id)
    );

    create index jahia_external_mapping_index1 on jahia_external_mapping (externalIdHash, providerKey);

    create index jahia_external_provider_id_ix1 on jahia_external_provider_id (providerKey);

    create sequence jahia_external_provider_id_seq;
