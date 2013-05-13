
    drop table jahia_external_mapping;

    drop table jahia_external_provider_id;

    drop table hibernate_unique_key;

    create table jahia_external_mapping (
        internalUuid varchar(36) not null,
        externalId clob(255) not null,
        externalIdHash integer,
        providerKey varchar(255) not null,
        primary key (internalUuid)
    );

    create table jahia_external_provider_id (
        id integer not null,
        providerKey varchar(255) not null,
        primary key (id)
    );

    create index jahia_external_mapping_index1 on jahia_external_mapping (externalIdHash, providerKey);

    create index jahia_external_provider_id_index1 on jahia_external_provider_id (providerKey);

    create table hibernate_unique_key (
         next_hi integer 
    );

    insert into hibernate_unique_key values ( 0 );
