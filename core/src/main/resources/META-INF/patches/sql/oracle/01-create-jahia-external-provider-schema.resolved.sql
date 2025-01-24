
    create table jahia_external_mapping (
        internalUuid varchar2(36 char) not null,
        externalId clob not null,
        externalIdHash number(10,0),
        providerKey varchar2(255 char) not null,
        primary key (internalUuid)
    );

    create table jahia_external_provider_id (
        id number(10,0) not null,
        providerKey varchar2(255 char) not null,
        primary key (id),
        unique (providerKey)
    );

    create index jahia_external_mapping_index1 on jahia_external_mapping (externalIdHash, providerKey);

    create index jahia_external_provider_id_ix1 on jahia_external_provider_id (providerKey);

    create sequence jahia_external_provider_id_seq;
