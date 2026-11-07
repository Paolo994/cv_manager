-- Contatti del candidato: usati solo per impaginare i documenti esportati, mai inviati al modello
create table contact_info
(
    user_id    uuid primary key references app_user (id) on delete cascade,
    contact    jsonb       not null,
    updated_at timestamptz not null
);

-- Annunci di lavoro. In questa fase solo quelli incollati dall'utente (source = MANUAL, owner valorizzato);
-- dalla fase 4 anche quelli dei provider, condivisi tra utenti (owner nullo, source + external_id univoci)
create table job_posting
(
    id            uuid primary key,
    owner_user_id uuid references app_user (id) on delete cascade,
    source        varchar(20)  not null,
    external_id   varchar(200),
    url           varchar(1000),
    description   text         not null,
    analysis      jsonb,
    created_at    timestamptz  not null
);
create unique index ux_job_posting_source_external on job_posting (source, external_id) where external_id is not null;
create index ix_job_posting_owner on job_posting (owner_user_id, created_at desc);

-- Documenti generati per un annuncio: ogni modifica (dell'AI o dell'utente) è una nuova versione
create table generated_document
(
    id           uuid primary key,
    user_id      uuid        not null references app_user (id) on delete cascade,
    job_id       uuid        not null references job_posting (id) on delete cascade,
    type         varchar(20) not null,
    version      int         not null,
    origin       varchar(10) not null,
    language     varchar(5)  not null,
    run_id       uuid        references agent_run (id) on delete set null,
    content      jsonb       not null,
    review_notes jsonb,
    created_at   timestamptz not null,
    constraint uk_generated_document_version unique (user_id, job_id, type, version)
);
create index ix_generated_document_job on generated_document (user_id, job_id, type, version desc);

-- Tentativo del passo: 1 = prima stesura, 2+ = revisione richiesta dal Critic
alter table agent_step add column attempt int not null default 1;
