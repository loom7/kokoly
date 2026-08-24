# Beleg-Dossier: Lizenzkette des Martin-Fine-Tunes

**Status: IN ARBEIT — Gate vor M2b.** Bis dieses Dossier vollständig ist, bleibt
jedes Release mit Martin-Dateien Draft/privat (Nutzer-Entscheid F1: selbst
verteilen, sobald belegt).

## Zu belegende Kette

| Glied | Quelle | Behauptete Lizenz | Beleg (Revision-Hash + Zitat) |
|---|---|---|---|
| kokoro-martin.onnx | huggingface.co/Godelaune/Kokoro-82M-ONNX-German-Martin | Apache-2.0 | **OFFEN** |
| Basis: kikiri-german-martin | huggingface.co/kikiri-tts/kikiri-german-martin | Apache-2.0 | **OFFEN** |
| Basis: Kokoro-82M | huggingface.co/hexgrad/Kokoro-82M | Apache-2.0 | **OFFEN** (Modellkarte sichern) |
| Stimmdaten des Fine-Tunes | LibriVox/HUI-Korpus? | CC0/Public Domain? | **OFFEN** |

## Bekannte Restunschärfen

1. Die Apache-Kette wurde bisher nur im Arbeitsverlauf festgestellt, nicht mit
   Revision-Hashes gesichert — genau das leistet dieses Dossier.
2. Persönlichkeitsrechte des Sprechers der Trainingsdaten sind von Modelllizenzen
   nicht abgedeckt; Herkunft der Stimme dokumentieren.

## Arbeitsschritte

- [ ] Modellkarten + LICENSE-Dateien aller vier Glieder mit Revision-Hash sichern
      (Kopien hierher, `docs/lizenz/belege/`).
- [ ] Prüfdatum + Prüfer eintragen.
- [ ] THIRD_PARTY_NOTICES.md-Zeile von „in Prüfung" auf belegt heben.
