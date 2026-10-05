create table users (
    id            bigserial primary key,
    email         varchar(255) not null unique,
    password_hash varchar(100) not null,
    name          varchar(120) not null,
    role          varchar(20)  not null,
    phone         varchar(30),
    area          varchar(100),
    verified      boolean      not null default true,
    created_at    timestamptz  not null default now()
);

create table listings (
    id               bigserial primary key,
    donor_id         bigint       not null references users (id),
    claimer_id       bigint references users (id),
    title            varchar(150) not null,
    description      text,
    category         varchar(20)  not null,
    storage          varchar(20)  not null,
    quantity         varchar(60)  not null,
    area             varchar(100) not null,
    address          varchar(255) not null,
    audience         varchar(10)  not null,
    status           varchar(20)  not null,
    pickup_start     timestamptz  not null,
    pickup_end       timestamptz  not null,
    expires_at       timestamptz  not null,
    claim_expires_at timestamptz,
    picked_up_at     timestamptz,
    created_at       timestamptz  not null default now(),
    check (pickup_start < pickup_end and pickup_end <= expires_at)
);

create index idx_listings_status_expires on listings (status, expires_at);
create index idx_listings_donor on listings (donor_id);
create index idx_listings_claimer on listings (claimer_id);
