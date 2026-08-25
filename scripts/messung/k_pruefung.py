# SPDX-License-Identifier: GPL-3.0-or-later
"""
Schnellprüfung einer Betonungs-/Wortlautregel an der Windows-Referenz.

Methode wie in der Referenzuntersuchung (TTS Test, feld2): Satzrahmen (nie
isoliert, nie satzfinal!), Silbengrenzen aus dem duration-Ausgang des Modells
(1 Rahmen = 600 Samples = 25 ms), Tonhöhe über Praat. Kennzahl: F0-Mittel der
Silben NACH dem Erstsilbenende geteilt durch die Erstsilbe — Jamben liegen
über 1,2, Trochäen um 0,95 (Eichung der Referenz).

Aufruf:
  "../TTS Test/.venv/Scripts/python.exe" scripts/messung/k_pruefung.py \
      "zˈyntəsə" "zyntˈeːzə" 3
Argumente: Phoneme alt, Phoneme neu, Tokenzahl der ersten Silbe.
"""

import sys
import warnings
from pathlib import Path

warnings.filterwarnings("ignore")
sys.stdout.reconfigure(encoding="utf-8")
REFERENZ = Path(__file__).parent.parent.parent.parent / "TTS Test"
sys.path.insert(0, str(REFERENZ))

import numpy as np
import parselmouth
from kokoro_onnx import Kokoro

RAHMEN_VOR = "diː "
RAHMEN_NACH = " vɑːɾ vˈɪɾklɪç zˈeːɾ ʃˈøːn."
SAMPLES_JE_RAHMEN = 600

alt, neu, erste_silbe_token = sys.argv[1], sys.argv[2], int(sys.argv[3])

k = Kokoro(str(REFERENZ / "modell/kokoro-martin.onnx"),
           str(REFERENZ / "modell/voices-martin.npz"))


def kennzahl(wort_phoneme: str) -> float:
    satz = RAHMEN_VOR + wort_phoneme + RAHMEN_NACH
    audio, sr, zeiten = k.create_timed(satz, voice="martin", lang="de", is_phonemes=True)
    # zeiten: je gesprochenem Phonem (start, end) in Sekunden — Wortanfang ist
    # das erste Zeichen des Wortes, Silbengrenze der Token-Versatz darin.
    # Nicht stimmtragende Zeichen (ˈ, Leerzeichen) tauchen mit auf; der Versatz
    # zählt deshalb über die Phonemliste der Timings, nicht über den String.
    start_token = len(RAHMEN_VOR.strip()) + 0
    # Position des Wortes in den Timings: erstes Timing nach den Rahmen-Phonemen.
    # Robust: suche die Folge der Wortphoneme (ohne Leerzeichen) im Timing-Strom.
    strom = [t.phoneme for t in zeiten]
    wort = [z for z in wort_phoneme if not z.isspace()]
    for i in range(len(strom) - len(wort) + 1):
        if strom[i:i + len(wort)] == wort:
            break
    else:
        raise SystemExit(f"Wortphoneme nicht im Timing-Strom: {wort_phoneme}")

    def sekunde(versatz):
        j = min(i + versatz, len(zeiten) - 1)
        return zeiten[j].start

    ton = parselmouth.Sound(np.asarray(audio, dtype="float64"),
                            sampling_frequency=sr).to_pitch(time_step=0.005)

    def f0_mittel(t0, t1):
        werte = [ton.get_value_at_time(t) for t in np.arange(t0, t1, 0.005)]
        werte = [w for w in werte if w and not np.isnan(w)]
        return float(np.mean(werte)) if werte else float("nan")

    s1 = f0_mittel(sekunde(0), sekunde(erste_silbe_token))
    rest = f0_mittel(sekunde(erste_silbe_token), zeiten[min(i + len(wort) - 1, len(zeiten) - 1)].end)
    return rest / s1


print(f"alt  {alt}: K = {kennzahl(alt):.3f}")
print(f"neu  {neu}: K = {kennzahl(neu):.3f}")
print("Eichung der Referenz: Trochäen ~0,95 · Jamben 1,2–1,4")
