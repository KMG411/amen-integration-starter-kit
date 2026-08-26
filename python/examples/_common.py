"""Shared helpers for the examples: unique phone numbers and a status printer."""
import time, logging
logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")


def unique_phone(prefix: str) -> str:
    """9-digit Saudi mobile number without country code, unique per run (sandbox only)."""
    return f"{prefix}{str(int(time.time()))[-7:]}"[:9]


def step(label: str, deal=None) -> None:
    status = f" → status={deal.status}" if deal is not None else ""
    print(f"✔ {label}{status}")
