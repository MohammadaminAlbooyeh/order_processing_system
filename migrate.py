#!/usr/bin/env python
"""
Database migration runner using Alembic.
Run this before starting services to apply pending migrations.

Usage:
    python migrate.py upgrade    # Apply all pending migrations
    python migrate.py downgrade  # Rollback to previous revision
    python migrate.py current    # Show current revision
    python migrate.py history    # Show migration history
"""
import sys
from alembic.config import Config
from alembic import command

def main():
    alembic_cfg = Config("alembic.ini")

    if len(sys.argv) == 1:
        command.upgrade(alembic_cfg, "head")
        print("✓ Applied all pending migrations")
    else:
        cmd = sys.argv[1]
        if cmd == "upgrade":
            command.upgrade(alembic_cfg, "head")
            print("✓ Applied all pending migrations")
        elif cmd == "downgrade":
            if len(sys.argv) < 3:
                print("Usage: python migrate.py downgrade <revision>")
                return
            command.downgrade(alembic_cfg, sys.argv[2])
            print(f"✓ Downgraded to {sys.argv[2]}")
        elif cmd == "current":
            command.current(alembic_cfg)
        elif cmd == "history":
            command.history(alembic_cfg)
        else:
            print(f"Unknown command: {cmd}")

if __name__ == "__main__":
    main()
