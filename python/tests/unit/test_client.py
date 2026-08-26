import httpx, respx, pytest
from amen import AmenClient, AmenApiError, AmenLifecycleError
from amen.models import ts


@respx.mock
def test_auth_header_and_base_url(offline_config):
    route = respx.get("https://sandbox-api.amnn.sa/api/v1/account").mock(return_value=httpx.Response(200, json={"id": "a1"}))
    AmenClient(offline_config).account.get()
    assert route.calls[0].request.headers["X-API-Token"] == "test-token"


@respx.mock
def test_error_codes_are_parsed(offline_config):
    respx.post("https://sandbox-api.amnn.sa/api/v1/customers/").mock(
        return_value=httpx.Response(400, json={"error": ["first_name__required", "phone_code__required"]}))
    with pytest.raises(AmenApiError) as e:
        AmenClient(offline_config).customers.create(first_name="", last_name="x", phone_code="", phone_number="5")
    assert e.value.status == 400 and e.value.has("first_name__required") and not e.value.retryable


@respx.mock
def test_429_is_retried_then_succeeds(offline_config, monkeypatch):
    monkeypatch.setattr("amen.client.time.sleep", lambda s: None)
    respx.get("https://sandbox-api.amnn.sa/api/v1/cities").mock(side_effect=[
        httpx.Response(429, json={"error": ["rate_limit__exceeded"]}), httpx.Response(200, json=[{"id": 1}])])
    assert AmenClient(offline_config).lookups.cities() == [{"id": 1}]


@respx.mock
def test_lifecycle_guard_blocks_invalid_action(offline_config):
    respx.get("https://sandbox-api.amnn.sa/api/v1/deals/DL-1").mock(return_value=httpx.Response(200, json={"number": "DL-1", "status": "draft"}))
    with pytest.raises(AmenLifecycleError):
        AmenClient(offline_config).deals.actions.approve("DL-1")


@respx.mock
def test_mutating_requests_send_origin(offline_config):
    route = respx.post("https://sandbox-api.amnn.sa/api/v1/web-hooks/").mock(return_value=httpx.Response(201, json={"id": "w", "url": "u", "secret_key": "s"}))
    wh = AmenClient(offline_config).webhooks.create("https://example.com/hook")
    assert wh.secret_key == "s" and route.calls[0].request.headers["Origin"] == "https://sandbox-api.amnn.sa"


def test_timestamps_are_epoch_milliseconds():
    assert ts(1679568486000).year == 2023
