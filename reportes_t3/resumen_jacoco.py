import sys
import xml.etree.ElementTree as ET
from pathlib import Path

local_xml = Path("target/site/jacoco/jacoco.xml")
xml_path = local_xml if local_xml.exists() else Path(
    r"C:\Users\ale\Downloads\XIMASAK_T3_Listo\ximasak_full\backend\target\site\jacoco\jacoco.xml"
)
tree = ET.parse(xml_path)
root = tree.getroot()
compact = "--compact" in sys.argv

print("=== GLOBAL COVERAGE ===")
for c in root.findall("counter"):
    ctype = c.attrib["type"]
    if compact and ctype not in ("INSTRUCTION", "BRANCH", "LINE", "METHOD"):
        continue
    cov, mis = int(c.attrib["covered"]), int(c.attrib["missed"])
    tot = cov + mis
    pct = (cov * 100 / tot) if tot else 0.0
    print(f"  {ctype:12s}: {cov}/{tot} ({pct:.2f}%)")

print(("" if compact else "\n") + "=== BY PACKAGE ===")
for pkg in root.findall("package"):
    pname = pkg.attrib["name"].split("/")[-1] or "(root)"
    stats = []
    for c in pkg.findall("counter"):
        if c.attrib["type"] in ("INSTRUCTION", "LINE", "BRANCH"):
            cov, mis = int(c.attrib["covered"]), int(c.attrib["missed"])
            tot = cov + mis
            pct = (cov * 100 / tot) if tot else 0.0
            stats.append(f"{c.attrib['type']}={cov}/{tot} ({pct:.1f}%)")
    if compact and not stats:
        continue
    if compact and pname in ("backend", "security", "entity"):
        continue
    print(f"  {pname:12s}: " + " | ".join(stats))
    if pname == "service" and not compact:
        for cls in pkg.findall("class"):
            cname = cls.attrib["name"].split("/")[-1]
            for c in cls.findall("counter"):
                if c.attrib["type"] == "LINE":
                    cov, mis = int(c.attrib["covered"]), int(c.attrib["missed"])
                    tot = cov + mis
                    pct = (cov * 100 / tot) if tot else 0.0
                    print(f"    - {cname:25s}: {cov}/{tot} lines ({pct:.1f}%)")
    elif pname == "service" and compact:
        parts = []
        for cls in pkg.findall("class"):
            cname = cls.attrib["name"].split("/")[-1]
            for c in cls.findall("counter"):
                if c.attrib["type"] == "LINE":
                    cov, mis = int(c.attrib["covered"]), int(c.attrib["missed"])
                    tot = cov + mis
                    pct = (cov * 100 / tot) if tot else 0.0
                    if cov > 0:
                        parts.append(f"{cname.replace('Service','')}: {pct:.1f}%")
        print("    services(1) : " + " | ".join(parts[:4]))
        if parts[4:]:
            print("    services(2) : " + " | ".join(parts[4:]))
