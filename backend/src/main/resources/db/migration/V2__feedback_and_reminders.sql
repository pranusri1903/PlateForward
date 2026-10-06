-- Flags so each reminder / warning email is sent only once per listing
alter table listings
    add column reminder_sent        boolean not null default false,
    add column expiry_warning_sent  boolean not null default false;

-- Reviews (after a pickup) and reports (no-shows, issues) share one table
create table feedback (
    id          bigserial primary key,
    listing_id  bigint        not null references listings (id) on delete cascade,
    author_id   bigint        not null references users (id),
    kind        varchar(20)   not null,
    rating      integer check (rating between 1 and 5),
    text        varchar(2000) not null,
    resolution  varchar(2000),
    resolved_at timestamptz,
    created_at  timestamptz   not null default now(),
    check ((kind = 'REVIEW') = (rating is not null))
);

create unique index uq_feedback_one_review on feedback (listing_id, author_id) where kind = 'REVIEW';
create index idx_feedback_listing on feedback (listing_id);
create index idx_feedback_open on feedback (created_at) where kind <> 'REVIEW' and resolution is null;
