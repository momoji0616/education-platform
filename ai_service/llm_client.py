import json
import os
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any

BASE_DIR = Path(__file__).resolve().parent


def _read_env_file_value(names: tuple[str, ...]) -> str:
    env_path = BASE_DIR / ".env"
    if not env_path.exists():
        return ""
    try:
        with env_path.open("r", encoding="utf-8", errors="ignore") as file:
            for line in file:
                line = line.strip()
                if not line or line.startswith("#") or "=" not in line:
                    continue
                key, value = line.split("=", 1)
                if key.strip() in names:
                    value = value.strip().strip('"').strip("'")
                    if value:
                        return value
    except Exception:
        return ""
    return ""


def load_deepseek_api_key() -> str:
    value = os.getenv("DEEPSEEK_API_KEY", "").strip()
    if value:
        return value
    value = _read_env_file_value(("DEEPSEEK_API_KEY",))
    if value:
        os.environ["DEEPSEEK_API_KEY"] = value
    return value


def _load_deepseek_setting(name: str, default: str) -> str:
    value = os.getenv(name, "").strip()
    if value:
        return value
    return _read_env_file_value((name,)) or default


def load_deepseek_base_url() -> str:
    base_url = _load_deepseek_setting("DEEPSEEK_BASE_URL", "https://api.deepseek.com").rstrip("/")
    suffix = "/chat/completions"
    if base_url.endswith(suffix):
        return base_url[: -len(suffix)].rstrip("/")
    return base_url


def load_deepseek_model() -> str:
    return _load_deepseek_setting("DEEPSEEK_MODEL", "deepseek-v4-flash").strip() or "deepseek-v4-flash"


def is_deepseek_available() -> bool:
    return bool(load_deepseek_api_key())


def call_deepseek_chat(messages: list[dict[str, Any]], timeout: int = 15) -> str:
    api_key = load_deepseek_api_key()
    if not api_key:
        return ""

    payload = {
        "model": load_deepseek_model(),
        "messages": messages,
        "stream": False,
        "thinking": {"type": "disabled"},
    }
    request = urllib.request.Request(
        f"{load_deepseek_base_url()}/chat/completions",
        data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            body = response.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="ignore")
        raise RuntimeError(f"DeepSeek API request failed: HTTP {exc.code} {detail}") from exc

    data = json.loads(body)
    choices = data.get("choices") or []
    if not choices:
        return ""
    message = choices[0].get("message") or {}
    return str(message.get("content") or "").strip()
