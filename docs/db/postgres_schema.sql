-- Hayat Hattı - PostgreSQL Schema (Neon)

drop table if exists public.signals cascade;
drop table if exists public.users cascade;

create table public.users (
  id bigint generated always as identity primary key,
  first_name varchar(50) not null,
  last_name  varchar(50) not null,
  email      varchar(100) not null unique,
  password   varchar(200) not null,
  phone      varchar(20),
  created_at timestamptz not null default now()
);

create table public.signals (
  id bigint generated always as identity primary key,
  device_signal_id text not null unique,
  user_id bigint not null references public.users(id) on delete restrict,
  notes text default 'Yardımınıza ihtiyacım var.',
  attachments text,
  lon double precision not null,
  lat double precision not null,
  accuracy_m real,
  type varchar(32) not null,
  "timestamp" timestamptz not null,
  created_at timestamptz not null default now()
);
