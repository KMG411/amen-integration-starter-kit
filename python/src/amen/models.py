"""Typed views over API payloads. Unknown fields are kept in `.raw` so additive API changes never break."""
from __future__ import annotations
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Any


def ts(ms: int | None) -> datetime | None:
    """API timestamps are epoch **milliseconds**."""
    return datetime.fromtimestamp(ms / 1000, tz=timezone.utc) if ms else None


@dataclass
class Model:
    raw: dict[str, Any] = field(default_factory=dict, repr=False)

    @classmethod
    def from_api(cls, d: dict[str, Any]):
        return cls(raw=d, **{k: d.get(k) for k in cls.__dataclass_fields__ if k != "raw" and not k.endswith("_at")},
                   **{k: ts(d.get(k)) for k in cls.__dataclass_fields__ if k.endswith("_at")})


@dataclass
class Customer(Model):
    id: str | None = None
    number: str | None = None
    first_name: str | None = None
    last_name: str | None = None
    status: str | None = None
    type: str | None = None
    created_at: datetime | None = None


@dataclass
class Deal(Model):
    id: str | None = None
    number: str | None = None
    status: str | None = None
    price: str | None = None
    offer: dict | None = None
    buyer: dict | None = None
    seller: dict | None = None
    created_at: datetime | None = None
    updated_at: datetime | None = None


@dataclass
class Checkout(Model):
    id: int | None = None
    provider: str | None = None
    hyperpay: dict | None = None
    amount: str | None = None
    created_at: datetime | None = None


@dataclass
class Withdrawal(Model):
    id: str | None = None
    number: str | None = None
    status: str | None = None
    amount: str | None = None
    created_at: datetime | None = None


@dataclass
class Webhook(Model):
    id: str | None = None
    url: str | None = None
    secret_key: str | None = None


@dataclass
class Account(Model):
    id: str | None = None
    name: str | None = None
    wallet: dict | None = None


@dataclass
class Page(Model):
    items: list[dict] = field(default_factory=list)
    page: int = 0
    pages: int = 1
    total: int = 0

    @classmethod
    def from_api(cls, d: dict[str, Any], items_key: str):
        p = d.get("page", d) if isinstance(d.get("page"), dict) else d
        return cls(raw=d, items=d.get(items_key) or d.get("results") or d.get("items") or [],
                   page=p.get("page", 0), pages=p.get("pages", 1), total=p.get("total", 0))
