import os, sys, pytest
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "src"))
from amen.config import Config
from amen import AmenClient


@pytest.fixture
def offline_config():
    return Config(env="sandbox", api_key="test-token", base_url="https://sandbox-api.amnn.sa", max_retries=1)


@pytest.fixture
def sandbox():
    """Real client; the whole integration suite is skipped without credentials."""
    try:
        cfg = Config.from_env()
    except ValueError:
        pytest.skip("AMN_API_KEY not set — integration tests need sandbox credentials")
    if cfg.env != "sandbox":
        pytest.skip("integration tests only run against the sandbox")
    with AmenClient(cfg) as c:
        yield c
