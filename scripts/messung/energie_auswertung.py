# SPDX-License-Identifier: GPL-3.0-or-later
"""
M5-Auswertung: energie-*.json (adb pull aus dem externen App-Verzeichnis)
zu einer Vergleichstafel, roh und basislinien-bereinigt.

Aufruf: python scripts/messung/energie_auswertung.py <basislinie_mA> messungen/energie-*.json
Die Basislinie (Geräte-Grundverbrauch, Bildschirm aus, drahtloses adb) kommt
aus der Leerlaufmessung desselben Laufs — sie wird zeitanteilig abgezogen,
damit die Zahl den ENGINE-Anteil zeigt. Beide Werte werden berichtet.
"""

import json
import sys
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8")
basis_mA = float(sys.argv[1])

zeilen = []
for pfad in sys.argv[2:]:
    d = json.loads(Path(pfad).read_text(encoding="utf-8"))
    stunden = d["rechenSekunden"] / 3600.0
    basis_mAh = basis_mA * stunden
    netto = d["mAh"] - basis_mAh
    zeilen.append((d, basis_mAh, netto))

zeilen.sort(key=lambda z: z[2] * 1000.0 / z[0]["zeichen"])
print(f"{'Konfiguration':14} {'RTF':>6} {'Wand':>6} {'roh mAh':>8} "
      f"{'Basis':>6} {'netto':>6} {'je 1000 Z roh':>14} {'netto':>6} {'Temp':>10}")
for d, basis_mAh, netto in zeilen:
    print(f"{d['name']:14} {d['rtf']:6.3f} {d['rechenSekunden']:5.0f}s "
          f"{d['mAh']:8.1f} {basis_mAh:6.1f} {netto:6.1f} "
          f"{d['mAh'] * 1000.0 / d['zeichen']:14.2f} "
          f"{netto * 1000.0 / d['zeichen']:6.2f} "
          f"{d['tempStartZehntelC'] / 10:.1f}→{d['tempEndeZehntelC'] / 10:.1f}")
print("\nQuantisierung des Ladungszählers am SM-F971B: ~4,2-mAh-Schritte — "
      "Unsicherheit je Messung etwa ±4 mAh.")
