-- CV caricati: il file originale è nello storage, qui il testo estratto
create table cv_document
(
    id                uuid primary key,
    user_id           uuid         not null references app_user (id) on delete cascade,
    original_filename varchar(255) not null,
    content_type      varchar(100) not null,
    size_bytes        bigint       not null,
    storage_key       varchar(500) not null,
    extracted_text    text         not null,
    created_at        timestamptz  not null
);
create index ix_cv_document_user on cv_document (user_id, created_at desc);

-- Profilo strutturato del candidato (uno per utente): prodotto dal CV Parser, rivisto dall'utente
create table candidate_profile
(
    user_id      uuid primary key references app_user (id) on delete cascade,
    source_cv_id uuid        references cv_document (id) on delete set null,
    profile      jsonb       not null,
    status       varchar(20) not null,
    updated_at   timestamptz not null,
    confirmed_at timestamptz
);

-- Preferenze di ricerca lavoro (una riga per utente)
create table job_preferences
(
    user_id     uuid primary key references app_user (id) on delete cascade,
    preferences jsonb       not null,
    updated_at  timestamptz not null
);

-- Run dell'orchestratore e passi dei singoli agenti
create table agent_run
(
    id            uuid primary key,
    user_id       uuid        not null references app_user (id) on delete cascade,
    workflow      varchar(50) not null,
    status        varchar(20) not null,
    input         jsonb,
    output        jsonb,
    error         text,
    input_tokens  int         not null default 0,
    output_tokens int         not null default 0,
    created_at    timestamptz not null,
    finished_at   timestamptz
);
create index ix_agent_run_user on agent_run (user_id, created_at desc);
create index ix_agent_run_status on agent_run (status);

create table agent_step
(
    id            uuid primary key,
    run_id        uuid        not null references agent_run (id) on delete cascade,
    agent         varchar(50) not null,
    model         varchar(100),
    status        varchar(20) not null,
    output        jsonb,
    error         text,
    input_tokens  int         not null default 0,
    output_tokens int         not null default 0,
    started_at    timestamptz not null,
    finished_at   timestamptz
);
create index ix_agent_step_run on agent_step (run_id, started_at);

-- Valutazioni dei CV prodotte dal CV Reviewer
create table cv_review
(
    id              uuid primary key,
    user_id         uuid        not null references app_user (id) on delete cascade,
    cv_id           uuid        not null references cv_document (id) on delete cascade,
    run_id          uuid        references agent_run (id) on delete set null,
    job_description text,
    review          jsonb       not null,
    created_at      timestamptz not null
);
create index ix_cv_review_cv on cv_review (cv_id, created_at desc);
