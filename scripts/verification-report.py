"""Expose generated QA evidence through read-only Actions logs and artifact reports."""
from pathlib import Path
import base64
import xml.etree.ElementTree as ET

for path in sorted(Path("build/test-results/test").glob("TEST-*.xml")):
    root = ET.parse(path).getroot()
    print("UNIT_TEST_RESULT", root.attrib, flush=True)

wanted = {"dashboard-home.png", "dashboard-small.png", "hud-editor-small.png", "survival-hud.png", "quantity-editor-small.png", "acacia-resource-count.png", "next-resource-goals.png"}
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

# Verify the artifact users install, including metadata and absence of test code.
import hashlib
import json
import zipfile

jars = [p for p in Path("build/libs").glob("smart-survival-*.jar") if not p.name.endswith("-sources.jar")]
if len(jars) != 1:
    raise SystemExit("Expected exactly one installable mod JAR")
jar = jars[0]
with zipfile.ZipFile(jar) as archive:
    names = archive.namelist()
    metadata = json.loads(archive.read("fabric.mod.json"))
    assert metadata["environment"] == "client"
    assert metadata["id"] == "smart-survival"
    assert "dev/smartsurvival/client/SmartSurvivalClient.class" in names
    assert not any("/test/" in n or n.endswith("Test.class") for n in names)
    assert "smart-survival.client.mixins.json" in names
print("RELEASE_JAR", jar.name, "version", metadata["version"], "bytes", jar.stat().st_size, flush=True)
print("RELEASE_SHA256", hashlib.sha256(jar.read_bytes()).hexdigest(), flush=True)
