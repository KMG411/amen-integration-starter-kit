"""Environment-based configuration. Nothing here is hard-coded per deployment."""
from __future__ import annotations
import os
from dataclasses import dataclass
from pathlib import Path

BASE_URLS = {"sandbox": "https://sandbox-api.amnn.sa", "live": "https://api.amnn.sa"}
API_PREFIX = "/api/v1"


def _load_dotenv() -> None:
    try:
        from dotenv import load_dotenv
    except ImportError:  # pragma: no cover
        return
    # Walk up from the current directory so examples/ and tests/ find the stack or kit-root .env
    here = Path.cwd().resolve()
    for d in [here, *here.parents][:4]:
        if (d / ".env").exists():
            load_dotenv(d / ".env", override=False)


@dataclass(frozen=True)
class Config:
    env: str
    api_key: str
    base_url: str
    timeout_s: float = 20.0
    webhook_secret: str | None = None
    max_retries: int = 3

    @classmethod
    def from_env(cls) -> "Config":
        _load_dotenv()
        env = os.getenv("AMN_ENV", "sandbox").lower()
        if env not in BASE_URLS:
            raise ValueError(f"AMN_ENV must be 'sandbox' or 'live', got {env!r}")
        key = os.getenv("AMN_API_KEY")
        if not key:
            raise ValueError("AMN_API_KEY is not set (see .env.example)")
        return cls(env=env, api_key=key, base_url=os.getenv("AMN_BASE_URL", BASE_URLS[env]),
                   timeout_s=int(os.getenv("AMN_TIMEOUT_MS", "20000")) / 1000,
                   webhook_secret=os.getenv("AMN_WEBHOOK_SECRET") or None)
