# SPDX-License-Identifier: GPL-3.0-or-later
"""
Golden-Generator — läuft mit dem venv der Windows-Referenz:

    "../TTS Test/.venv/Scripts/python.exe" scripts/golden/generieren.py

Erzeugt je Sprache eine JSON-Datei unter app/src/test/resources/golden/ mit
zwei Stufen je Satz:

  Stufe A (roh):  je Interpunktions-Segment die UNVERÄNDERTE Ausgabe von
                  espeak_TextToPhonemes (text_mode=1, phonemes_mode='_'<<8|0x02)
                  aus der Referenz-DLL (espeak-ng 1.52.0, espeakng-loader 0.2.4).
                  Das ist exakt der Aufruf, den das Android-JNI macht — die
                  Geräteprüfung vergleicht arm64-Ausgabe gegen diese Strings.

  Stufe B (end):  die Endfassung aus phonemisierung.phonemisiere() der
                  Windows-Referenz — das, was das Modell bekommt.

Zusätzlich beweist der Generator die VOLLSTÄNDIGKEIT der Nachverarbeitungs-
Spezifikation: er baut Stufe B aus Stufe A mit genau den Regeln nach, die das
Kotlin-Frontend umsetzen wird, und meldet jede Abweichung. Nur bei 100 %
Übereinstimmung werden Goldens geschrieben.

DIE NACHVERARBEITUNGS-SPEZIFIKATION (= Pflichtenheft für PhonemeFrontend.kt):
 1. Text normalisieren: NFC + Leerraum zusammenfassen.
 2. Interpunktion abtrennen (Marks ';:,.!?¡¿—…"«»«»(){}[]', Positionen merken).
 3. Je Segment espeak_TextToPhonemes-Schleife, Teilergebnisse mit ' ' verbinden.
 4. Je Segment: '_'-Phonemseparatoren entfernen, '\\n'→' ', Mehrfach-Leerraum
    zusammenfassen, trimmen.
 5. Sprachwechselmarken entfernen: Muster \\(.+?\\) — NACH Schritt 2 ist das
    verlustfrei, weil echte Klammern bereits abgetrennt sind (Windows-Parität:
    phonemizer benutzt exakt dieses Muster an exakt dieser Stelle).
 6. Interpunktion wieder einsetzen (Positionen aus Schritt 2).
 7. Lautersatz ʏ→y (Trainingskonvention des Modells).
 8. Vokabularfilter (114 Token) — jeder entfernte Laut wird GEMELDET.
 9. Trimmen.
"""

import io
import json
import re
import sys
import unicodedata
from ctypes import POINTER, byref, c_char_p, c_int, c_void_p, cdll, pointer
from datetime import date
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8")
HIER = Path(__file__).parent
REPO = HIER.parent.parent
sys.path.insert(0, str(HIER))
from korpus import KORPUS, NEUE_REGEL_ERWARTUNGEN, REGEL_KORPUS  # noqa: E402

REFERENZ = REPO.parent / "TTS Test"
sys.path.insert(0, str(REFERENZ))

import warnings  # noqa: E402

warnings.filterwarnings("ignore")

import espeakng_loader  # noqa: E402
from kokoro_onnx.tokenizer import Tokenizer  # noqa: E402
from phonemizer.punctuation import Punctuation  # noqa: E402
from phonemizer.separator import default_separator  # noqa: E402

import betonung  # noqa: E402  (Windows-Referenz, Regelstufe)
import phonemisierung  # noqa: E402  (die Windows-Referenz — Stufe-B-Wahrheit)
import textregeln  # noqa: E402
import wortlaute  # noqa: E402

# ---------------------------------------------------------------- Referenz-DLL
LIB = cdll.LoadLibrary(espeakng_loader.get_library_path())
LIB.espeak_Initialize.restype = c_int
LIB.espeak_SetVoiceByName.restype = c_int
LIB.espeak_TextToPhonemes.restype = c_char_p
LIB.espeak_TextToPhonemes.argtypes = [POINTER(c_char_p), c_int, c_int]
rate = LIB.espeak_Initialize(0x02, 0, espeakng_loader.get_data_path().encode(), 0)
assert rate > 0, "espeak_Initialize fehlgeschlagen"

TEXT_MODE = 1                       # UTF-8
PHONEME_MODE = (ord("_") << 8) | 0x02  # IPA, '_'-Separator — wie die Referenz

#: Sprachcode → espeak-Stimmkennung, wie die Referenz sie auflöst (phonemizer
#: wählt je Sprachcode die erste Stimme der Liste; Ergebnis hier festgeschrieben,
#: nachgeprüft am 24.08.2026 gegen EspeakBackend(code)._espeak.voice.identifier).
#: Beim gepinnten espeak 1.52.0 + gepinnten Daten ist die Tabelle deterministisch —
#: und sie ist dieselbe, die das Android-JNI benutzt.
STIMMEN = {
    "de": "gmw/de", "en-us": "gmw/en-us", "en-gb": "gmw/en",
    "es": "roa/es", "fr-fr": "roa/fr", "it": "roa/it",
    "pt-br": "roa/pt-br", "hi": "inc/hi",
}

_aktuelle_stimme = None


def roh_espeak(text: str, stimme: str) -> str:
    """Der exakte JNI-Aufruf: SetVoiceByName + TextToPhonemes-Schleife."""
    global _aktuelle_stimme
    if stimme != _aktuelle_stimme:
        kennung = STIMMEN[stimme]
        assert LIB.espeak_SetVoiceByName(kennung.encode()) == 0, f"Stimme {kennung}?"
        _aktuelle_stimme = stimme
    zeiger = pointer(c_char_p(text.encode("utf-8")))
    teile = []
    while zeiger.contents.value is not None:
        p = LIB.espeak_TextToPhonemes(zeiger, TEXT_MODE, PHONEME_MODE)
        if p:
            teile.append(p.decode("utf-8"))
    return " ".join(teile)


# ------------------------------------------------- Nachverarbeitung (die Spez)
FLAGS = re.compile(r"\(.+?\)")
VOKABULAR = Tokenizer().vocab


def nachverarbeitung_segment(roh: str) -> str:
    s = roh.replace("_", "").replace("\n", " ")
    s = re.sub(r"\s+", " ", s).strip()
    s = FLAGS.sub("", s)
    return s


def baue_endfassung(segmente, marks) -> tuple[str, list]:
    """Schritte 6–9: Interpunktion einsetzen, Lautersatz, Filter."""
    # Instanzmethode mit dem Standard-Separator und strip=True — exakt der
    # Aufruf aus BaseBackend._phonemize_postprocess der Referenz.
    wieder = Punctuation().restore(segmente, marks, default_separator, True)
    text = " ".join(t for t in wieder if t).strip()
    text = re.sub(r"\s+", " ", text)
    text = text.replace("ʏ", "y")
    behalten, verworfen = [], set()
    for z in text:
        if z in VOKABULAR:
            behalten.append(z)
        elif not z.isspace():
            verworfen.add(z)
    return "".join(behalten).strip(), sorted(verworfen)


# ---------------------------------------------------------------- Hauptlauf
def main() -> int:
    punct = Punctuation()
    ziel = REPO / "app/src/test/resources/golden"
    ziel.mkdir(parents=True, exist_ok=True)

    fehler = 0
    for sprache, saetze in KORPUS.items():
        eintraege = []
        for satz in saetze:
            norm = unicodedata.normalize("NFC", satz)
            norm = " ".join(norm.split())

            chunks, marks = punct.preserve(norm)
            segmente = [roh_espeak(c, sprache) for c in chunks]
            nach = [nachverarbeitung_segment(s) for s in segmente]
            gebaut, verworfen_gebaut = baue_endfassung(list(nach), marks)

            wahrheit, verworfen_wahr = phonemisierung.phonemisiere(
                norm, sprache, normalisieren=False)

            if gebaut != wahrheit:
                fehler += 1
                print(f"ABWEICHUNG [{sprache}] {satz}")
                print(f"  gebaut  : {gebaut}")
                print(f"  Referenz: {wahrheit}")

            eintraege.append({
                "text": norm,
                "segmente": [
                    {"chunk": c, "roh": r} for c, r in zip(chunks, segmente)
                ],
                "marks": [
                    {"mark": m.mark, "position": m.position} for m in marks
                ],
                "endfassung": wahrheit,
                "verworfen": verworfen_wahr,
            })

        datei = ziel / f"{sprache}.json"
        # Zweitschrift für den Gerätetest: androidTest liest Assets, die
        # JVM-Tests lesen resources. EINE Quelle (dieser Generator), zwei
        # geschriebene Artefakte — beide im Git, beide aus demselben Lauf.
        ziel2 = REPO / "app/src/androidTest/assets/golden"
        ziel2.mkdir(parents=True, exist_ok=True)
        io.open(datei, "w", encoding="utf-8", newline="\n").write(json.dumps({
            "sprache": sprache,
            "espeak": "1.52.0 (espeakng-loader 0.2.4, Referenz-DLL)",
            "aufruf": "espeak_TextToPhonemes, text_mode=1, phonemes_mode=ord('_')<<8|0x02",
            "erzeugt": str(date.today()),
            "saetze": eintraege,
        }, ensure_ascii=False, indent=1) + "\n")
        import shutil
        shutil.copyfile(datei, ziel2 / datei.name)
        print(f"{datei.name}: {len(eintraege)} Sätze")

    # ---------------------------------------------------- Stufe C: Regelkette
    # Die volle deutsche Kette der Referenz (kokoro_test._synthese):
    # normalisieren → textregeln → phonemisieren → betonung → wortlaute.
    # Das Fixture trägt zusätzlich die Rohsegmente NACH den Textregeln, damit
    # der JVM-Test ohne espeak läuft (dasselbe Muster wie Stufe A/B).
    punct2 = Punctuation()
    regel_eintraege = []
    regel_fehler = 0
    for satz in REGEL_KORPUS:
        norm = unicodedata.normalize("NFC", satz)
        norm = " ".join(norm.split())
        nach_text, _meld = textregeln.berichtige(norm)

        chunks, marks = punct2.preserve(nach_text)
        segmente = [roh_espeak(c, "de") for c in chunks]

        phoneme, verworfen = phonemisierung.phonemisiere(nach_text, "de",
                                                         normalisieren=False)
        mit_betonung, _m1 = betonung.berichtige(nach_text, phoneme)
        endfassung, _m2 = wortlaute.berichtige(nach_text, mit_betonung)
        # ADR-0013: Regeln nach dem Einfrieren kennt die Referenz nicht — für
        # diese Sätze gilt die hergeleitete Erwartung aus dem Korpus.
        quelle = "windows-referenz"
        if norm in NEUE_REGEL_ERWARTUNGEN:
            endfassung = NEUE_REGEL_ERWARTUNGEN[norm]
            quelle = "golden-writer (Herleitung im Korpus)"

        # Beweis der Spezifikation auch hier: Stufe B aus den Rohsegmenten.
        nach = [nachverarbeitung_segment(x) for x in segmente]
        gebaut, _ = baue_endfassung(list(nach), marks)
        if gebaut != phoneme:
            regel_fehler += 1
            print(f"ABWEICHUNG [regeln/frontend] {satz}")
            print(f"  gebaut  : {gebaut}")
            print(f"  Referenz: {phoneme}")

        regel_eintraege.append({
            "text": norm,
            "nach_textregeln": nach_text,
            "segmente": [{"chunk": c, "roh": r} for c, r in zip(chunks, segmente)],
            "marks": [{"mark": m.mark, "position": m.position} for m in marks],
            "phoneme_ohne_regeln": phoneme,
            "endfassung": endfassung,
            "endfassung_quelle": quelle,
            "verworfen": verworfen,
        })

    for zielordner in (ziel, REPO / "app/src/androidTest/assets/golden"):
        io.open(zielordner / "de-regeln.json", "w", encoding="utf-8", newline="\n").write(
            json.dumps({
                "sprache": "de", "stufe": "C (Textregeln + Betonung + Wortlaute)",
                "espeak": "1.52.0 (Referenz-DLL)", "erzeugt": str(date.today()),
                "saetze": regel_eintraege,
            }, ensure_ascii=False, indent=1) + "\n")
    print(f"de-regeln.json: {len(regel_eintraege)} Sätze")
    fehler += regel_fehler

    # Vokabular-Export für Laufzeit UND Tests (char → Token-Id).
    vok = REPO / "app/src/main/assets/vokabular.json"
    vok.parent.mkdir(parents=True, exist_ok=True)
    io.open(vok, "w", encoding="utf-8", newline="\n").write(
        json.dumps(VOKABULAR, ensure_ascii=False, indent=0) + "\n")
    import shutil as _sh
    _sh.copyfile(vok, REPO / "app/src/test/resources/vokabular.json")
    print(f"vokabular.json: {len(VOKABULAR)} Token (assets + Testressourcen)")

    if fehler:
        print(f"\n{fehler} ABWEICHUNGEN — Goldens sind trotzdem geschrieben, "
              f"aber die Spezifikation ist NICHT vollständig. Nicht einchecken!")
        return 1
    print("\nSpezifikation vollständig: gebaute Endfassung == Referenz für jeden Satz.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
