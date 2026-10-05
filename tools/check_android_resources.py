#!/usr/bin/env python3
"""Validação estática de XML e referências locais. Não substitui aapt/lint ou um build Android."""
from pathlib import Path
from collections import defaultdict
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
resources = defaultdict(set)
xml_files = sorted((root / "app/src").rglob("*.xml"))
for path in xml_files:
    tree = ET.parse(path)
    if "/res/" not in str(path):
        continue
    folder = path.parent.name.split("-")[0]
    if folder != "values":
        resources[folder].add(path.stem)
    for node in tree.iter():
        if folder == "values" and node.get("name"):
            kind = node.get("type") if node.tag == "item" else node.tag
            resources[kind].add(node.get("name").replace(".", "_"))
        for value in node.attrib.values():
            for name in re.findall(r"@\+id/(\w+)", value):
                resources["id"].add(name)

errors = []
for path in xml_files:
    # O parser XML descarta comentários. Estilos das bibliotecas serão resolvidos pelo aapt.
    content = " ".join(" ".join(node.attrib.values()) + " " + (node.text or "") for node in ET.parse(path).iter())
    for kind, name in re.findall(r"@(?:\+)?(\w+)/(\w[\w.]*)", content):
        if kind == "style" and name.startswith(("Widget.MaterialComponents.", "Widget.Material3.", "Theme.Material3.")):
            continue
        if name.replace(".", "_") not in resources[kind]:
            errors.append(f"{path.relative_to(root)}: recurso ausente @{kind}/{name}")
for path in (root / "app/src").rglob("*.java"):
    for kind, name in re.findall(r"(?<![.\w])R\.(\w+)\.(\w+)", path.read_text()):
        if name not in resources[kind]:
            errors.append(f"{path.relative_to(root)}: recurso ausente R.{kind}.{name}")

android = "{http://schemas.android.com/apk/res/android}"
tools = "{http://schemas.android.com/tools}"
local = ET.parse(root / "app/src/local/AndroidManifest.xml").getroot()
assert any(n.get(android + "name") == "android.permission.INTERNET" and n.get(tools + "node") == "remove" for n in local.findall("uses-permission"))
assert any(n.get(android + "name") == "com.google.firebase.provider.FirebaseInitProvider" and n.get(tools + "node") == "remove" for n in local.findall("application/provider"))
main = ET.parse(root / "app/src/main/AndroidManifest.xml").getroot()
launchers = [n.get(android + "name") for n in main.findall("application/activity") if n.find("intent-filter") is not None]
assert launchers == [".LoginActivity"], launchers
if errors:
    raise SystemExit("\n".join(errors))
print(f"PASS: {len(xml_files)} XMLs, referências locais e isolamento declarado da variante local. Build/merge Android não executado.")
