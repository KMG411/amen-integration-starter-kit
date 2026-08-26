from __future__ import annotations
from ..models import Webhook


class Webhooks:
    def __init__(self, client): self._c = client
    def list(self) -> list[Webhook]:
        return [Webhook.from_api(w) for w in self._c.request("GET", "/web-hooks/")]
    def create(self, url: str) -> Webhook:
        """The returned secret_key is shown ONLY now — store it in a secret manager immediately."""
        return Webhook.from_api(self._c.request("POST", "/web-hooks/", json={"url": url}))
    def delete(self, webhook_id: str) -> None:
        self._c.request("DELETE", f"/web-hooks/{webhook_id}")
