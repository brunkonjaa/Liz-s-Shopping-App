"""Verify the actual packaged manifest, including dependency contributions."""
import sys
import xml.etree.ElementTree as ET

root = ET.parse(sys.argv[1]).getroot()
a = "{http://schemas.android.com/apk/res/android}"
package = root.attrib["package"]
permissions = [p.attrib[a + "name"] for p in root.findall("uses-permission")]
allowed = {package + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"}
assert set(permissions) <= allowed, f"Unexpected packaged permissions: {permissions}"
exported = []
for component in root.find("application"):
    if component.tag in {"activity", "activity-alias", "service", "receiver", "provider"}:
        if component.attrib.get(a + "exported") == "true":
            exported.append(component.attrib[a + "name"])
assert exported in [[package + ".MainActivity"], [".MainActivity"]], f"Unexpected exported components: {exported}"
print("Manifest verified: no network or runtime permissions; launcher is the only exported component.")
print("Compatibility signature permission:", permissions)
