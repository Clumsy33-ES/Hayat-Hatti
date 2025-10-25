# migrations/env.py
import os
from logging.config import fileConfig
from alembic import context
from sqlalchemy import create_engine, pool
from app.db.models import Base

config = context.config
if config.config_file_name:
    fileConfig(config.config_file_name)

target_metadata = Base.metadata

def _sync_url():
    url = os.getenv("DATABASE_SYNC_URL") or os.getenv("DATABASE_URL")
    if url and "+asyncpg" in url:
        url = url.replace("+asyncpg", "")
    return url

def run_migrations_offline():
    url = _sync_url()
    context.configure(url=url, target_metadata=target_metadata,
                      literal_binds=True, compare_type=True)
    with context.begin_transaction():
        context.run_migrations()

def run_migrations_online():
    url = _sync_url()
    connectable = create_engine(url, poolclass=pool.NullPool)
    with connectable.connect() as connection:
        context.configure(connection=connection, target_metadata=target_metadata,
                          compare_type=True)
        with context.begin_transaction():
            context.run_migrations()

if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
