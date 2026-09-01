# Beleg-Dossier: Lizenzkette des Martin-Fine-Tunes

**Status: BELEGT mit benannten Restunschärfen (Stand 31.08.2026).** Die
Modell-Lizenzkette ist an jedem öffentlich erreichbaren Glied revisionsgepinnt
Apache-2.0; die Trainingsdaten-/Stimmprovenienz ist es NICHT vollständig —
siehe Restunschärfen. Die Freigabe der Selbstverteilung (Nutzer-Entscheid F1)
setzt die Kenntnisnahme dieser Restunschärfen voraus. Alle Belege wurden am
31.08.2026 abgerufen und liegen als Kopien unter [belege/](belege/).

## Die belegte Kette

| Glied | Revision (HF-API »sha«) | Lizenz | Beleg |
|---|---|---|---|
| [Godelaune/Kokoro-82M-ONNX-German-Martin](https://huggingface.co/Godelaune/Kokoro-82M-ONNX-German-Martin) (unsere Bezugsquelle: kokoro-martin.onnx + voices-martin.npz) | `a1cba7fbf0e72fbae38f0a3a48ce0dc8e6077804` (22.05.2026) | **Apache-2.0** — HF-Metadatenfeld, README-Frontmatter (`license: apache-2.0`) UND Kartentext („**License:** Apache 2.0") | [belege/godelaune-api.json](belege/godelaune-api.json), [belege/godelaune-martin-README.md](belege/godelaune-martin-README.md) |
| [kikiri-tts/kikiri-german-martin](https://huggingface.co/kikiri-tts/kikiri-german-martin) (PyTorch-Original des Fine-Tunes; `base_model` laut Godelaune-Karte) | `1e9dcd16ed48fda0a7a1f62e5e37130a5fdf10d9` (22.04.2026) | **Apache-2.0** (Metadatenfeld + Frontmatter) | [belege/kikiri-martin-api.json](belege/kikiri-martin-api.json), [belege/kikiri-martin-README.md](belege/kikiri-martin-README.md) |
| [kikiri-tts/kikiri-german-base-51speakers-synthetic](https://huggingface.co/kikiri-tts/kikiri-german-base-51speakers-synthetic) (Stage-1-Basis des Fine-Tunes) | `0d15d026c94423b00bbdfc6644b0910c8daa29a9` | **Apache-2.0** (Metadatenfeld) | [belege/kikiri-base-README.md](belege/kikiri-base-README.md) |
| [hexgrad/Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M) (Ursprungsmodell/Architektur) | `f3ff3571791e39611d31c381e3a41a3af07b4987` (10.04.2025) | **Apache-2.0** (Metadatenfeld + zweimal im Kartentext; kommerzielle Nutzung ausdrücklich begrüßt) | [belege/hexgrad-kokoro-README.md](belege/hexgrad-kokoro-README.md) |

Sicherung im Manifest: [models.json](../../models.json) pinnt die
Godelaune-Dateien per Revision-URL UND SHA-256 — ein späteres Umlizenzieren
des HF-Repos kann unsere bezogenen Artefakte nicht rückwirkend ändern
(Apache-2.0 ist unwiderruflich für die erhaltene Kopie), und der Pin macht
den Bezugsstand beweisbar.

## Restunschärfen (ehrlich, entscheidungsrelevant für F1)

1. **Keine LICENSE-Dateien in den HF-Repos.** Alle vier Glieder deklarieren
   Apache-2.0 ausschließlich über das maschinenlesbare HF-Lizenzfeld bzw. den
   Kartentext — das ist der übliche und allgemein als wirksam behandelte
   HF-Lizenzmechanismus, aber keine klassische beigelegte Lizenzdatei
   (Dateilisten in den API-Belegen).
2. **Verschwundene Vorstufe.** Das GitHub-Trainingsrezept
   ([belege/semidark-kokoro-deutsch-README.md](belege/semidark-kokoro-deutsch-README.md))
   nennt als Vorstufe `dida-80b/kokoro-deutsch-hui-base` (Angabe dort:
   CC0-1.0, ~51 h Audio) und das Dataset `dida-80b/hui-german-51speakers` —
   beide HF-Repos liefern heute HTTP 401 (privat/entfernt). Die
   CC0-Behauptung dieser Stufe hängt damit an einer Sekundärquelle.
3. **Stimmprovenienz „Martin Harbecke".** Die kikiri-Karte führt den Sprecher
   als „Martin Harbecke (synthetic)" (627 Samples); die Stage-1-Basis nennt
   sich „entirely synthetic (TTS-generated)", verschweigt aber das erzeugende
   TTS-System — dessen Nutzungsbedingungen sind damit unprüfbar. Ob hinter
   der Stimme ein realer Sprecher (Einwilligung?), ein HUI-/LibriVox-Sprecher
   oder eine Kunststimme steht, ist öffentlich nirgends dokumentiert.
   Persönlichkeitsrechte sind von Modelllizenzen ohnehin nicht abgedeckt.
4. **Doppeltes Export-Repo.** Es existiert ein namensgleiches Repo
   `huggingFresse/Kokoro-82M-ONNX-German-Martin` (die Godelaune-Karte nennt
   huggingFresse als ONNX-Konverter); das Verhältnis der beiden Repos wurde
   nicht abschließend geprüft. Unsere Prüfsummen binden uns an den
   Godelaune-Stand.

## Einordnung

Für die VERTEILUNG der Dateien ist die Lizenz der unmittelbaren Quelle
maßgeblich, und die ist vierfach konsistent und revisionsgepinnt Apache-2.0
belegt — einschließlich der von allen Karten bestätigten Herkunft aus dem
Apache-2.0-Ursprungsmodell. Die Restunschärfen betreffen die
Trainingsdaten-Ebene UNTER der Kette; sie sind bei praktisch allen
TTS-Gewichten dieser Familie gleichartig (auch die hexgrad-Karte belegt ihre
Datenherkunft nur als Selbstauskunft). Empfehlung: Selbstverteilung auf
dieser Basis vertretbar; die Restunschärfen bleiben im Dossier dokumentiert,
und Punkt 3 wird in den Release-Notes nicht verschwiegen.

## Prüfprotokoll

- Prüfdatum: 31.08.2026 · Geprüft durch: KI-Recherche (16 Agenten, Web-Abruf)
  mit adversarialer Gegenprüfung jeder Lizenz-Kernaussage (alle Urteile:
  haltbar); Belege eigenhändig per HF-API/raw-URL gezogen.
- [x] Modellkarten + Metadaten aller vier erreichbaren Glieder mit
      Revision-Hash gesichert (belege/)
- [x] models.json auf Revision gepinnt (URL + SHA-256)
- [x] THIRD_PARTY_NOTICES.md-Zeile von „in Prüfung" auf „belegt
      (Restunschärfen im Dossier)" gehoben
- [x] F1 ENTSCHIEDEN (31.08.2026): **Mittelweg.** Verteilung bleibt bei der
      revisionsgepinnten HF-Quelle (nur Verlinken — konservativste
      Rechtsposition); der Betreiber hält zwei prüfsummenverifizierte
      Sicherungskopien beider Dateien außerhalb dieses Repos. Fällt die
      Quelle aus, greift der Umschwenkplan unten.

## Umschwenkplan (Quelle fällt aus → Selbstverteilung binnen eines Tages)

1. Sicherungskopien gegen die SHA-256 aus models.json prüfen (beide Werte
   stehen dort und in diesem Dossier unveränderlich im Git-Verlauf).
2. GitHub-Release `modelle-martin` im Projekt-Repo anlegen, beide Dateien
   anhängen (die Kette dieses Dossiers deckt die Weiterverbreitung als
   Apache-2.0; Restunschärfen oben gelten unverändert und gehören in die
   Release-Notes).
3. In models.json die beiden URLs auf das eigene Release umstellen —
   Prüfsummen bleiben identisch, Bestandsnutzer sind nicht betroffen
   (Modelle liegen lokal).
4. App-Release mit dem neuen Manifest veröffentlichen (models.json ist ins
   APK gebündelt).
