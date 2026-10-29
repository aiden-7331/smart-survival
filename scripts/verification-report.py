"""Expose generated QA evidence through read-only Actions logs and artifact reports."""
from pathlib import Path
import base64
import xml.etree.ElementTree as ET

for path in sorted(Path("build/test-results/test").glob("TEST-*.xml")):
    root = ET.parse(path).getroot()
    print("UNIT_TEST_RESULT", root.attrib, flush=True)

wanted = {"dashboard-home.png", "dashboard-small.png", "hud-editor-small.png", "survival-hud.png"}
for folder in (Path("build/run"), Path("run")):
    if not folder.exists():
        continue
    for path in sorted(folder.rglob("*.png")):
        if not any(path.name.endswith(name) for name in wanted):
            continue
        data = path.read_bytes()
        if len(data) > 2_000_000:
            print("SCREENSHOT_TOO_LARGE", path)
            continue
        # Only generated test screenshots, never world files, configuration or user data.
        print("SCREENSHOT_BEGIN " + path.name, flush=True)
        encoded = base64.b64encode(data).decode("ascii")
        for offset in range(0, len(encoded), 2048):
            print("SCREENSHOT_DATA " + encoded[offset:offset+2048], flush=True)
        print("SCREENSHOT_END " + path.name, flush=True)
