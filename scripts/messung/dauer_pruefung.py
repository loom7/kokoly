# SPDX-License-Identifier: GPL-3.0-or-later
"""
Segmentdauer-Prüfung einer Wortlautregel an der Windows-Referenz.

Ergänzt die K-Kennzahl (k_pruefung.py): K misst nur das Betonungsverhältnis —
ob das Modell ALLE Segmente wirklich spricht, zeigen erst die Phonemdauern aus
dem duration-Ausgang. Lehrstück Synthese (25.08.2026): die Regelfassung
zyntˈeːzə war zeichengenau in der Pipeline, klang aber verstümmelt, weil der
Dauerprädiktor einzelnen Tokens ~0 Rahmen zuwies.

Aufruf:
  "../TTS Test/.venv/Scripts/python.exe" scripts/messung/dauer_pruefung.py \
      "zˈyntəsə" "zyntˈeːzə" [weitere Kandidaten ...]
Gibt je Kandidat die Dauer jedes Wort-Phonems in Millisekunden aus und
schreibt pruefstein/dauer-<n>.wav für die Hörprobe.
"""

import sys
import warnings
from pathlib import Path

warnings.filterwarnings("ignore")
sys.stdout.reconfigure(encoding="utf-8")
WURZEL = Path(__file__).parent.parent.parent
REFERENZ = WURZEL.parent / "TTS Test"
sys.path.insert(0, str(REFERENZ))

import numpy as np
import soundfile as sf
from kokoro_onnx import Kokoro

RAHMEN_VOR = "diː "
RAHMEN_NACH = " lˈɔøft aʊf dˌiːzəm ɡərˈɛːt."

k = Kokoro(str(REFERENZ / "modell/kokoro-martin.onnx"),
           str(REFERENZ / "modell/voices-martin.npz"))

for n, wort in enumerate(sys.argv[1:], 1):
    satz = RAHMEN_VOR + wort + RAHMEN_NACH
    audio, sr, zeiten = k.create_timed(satz, voice="martin", lang="de",
                                       is_phonemes=True)
    strom = [t.phoneme for t in zeiten]
    folge = [z for z in wort if not z.isspace()]
    for i in range(len(strom) - len(folge) + 1):
        if strom[i:i + len(folge)] == folge:
            break
    else:
        print(f"{wort}: NICHT im Timing-Strom gefunden!")
        continue
    dauern = [(zeiten[i + j].phoneme,
               round((zeiten[i + j].end - zeiten[i + j].start) * 1000))
              for j in range(len(folge))]
    gesamt = sum(d for _, d in dauern)
    warnung = " ⚠ Segmente ohne Dauer!" if any(
        d < 15 for p, d in dauern if p not in "ˈˌː") else ""
    print(f"{n}. {wort}  ({gesamt} ms gesamt, Satz {len(audio)/sr:.2f} s){warnung}")
    print("   " + "  ".join(f"{p}:{d}" for p, d in dauern))
    sf.write(str(WURZEL / f"pruefstein/dauer-{n}.wav"), audio, sr)
