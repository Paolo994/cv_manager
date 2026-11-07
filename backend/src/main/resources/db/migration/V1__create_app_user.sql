create table app_user
(
    id            uuid primary key,
    email         varchar(254) not null,
    password_hash varchar(100) not null,
    full_name     varchar(120) not null,
    role          varchar(20)  not null,
    created_at    timestamptz  not null,
    constraint uk_app_user_email unique (email)
);
