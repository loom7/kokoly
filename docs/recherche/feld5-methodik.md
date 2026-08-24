# Recherchefeld 5: Professionelle Projektplanung und -dokumentation

> Rechercheablage vom 24.08.2026, erhoben für den PROJEKTPLAN. Inhalt vor Schönheit.

## Kernaussagen

- Architekturdoku als arc42-Light mit ~7 der 12 Abschnitte in EINER Datei docs/architektur.md; die Abschnitte 2, 7, 10, 12 werden eingedampft oder verschmolzen — arc42 selbst rät ausdrücklich zum Weglassen.
- Blockdiagramm nach C4: Ebene 1 (Kontext) + Ebene 2 (Container) als gepflegte Mermaid-Diagramme im Markdown; Ebene 3 (Komponenten) genau einmal für den Pipeline-Kern; Ebene 4 (Code) nie. GitHub rendert Mermaid seit Februar 2022 nativ — bestätigt.
- ADRs nach Nygard (Titel/Status/Kontext/Entscheidung/Konsequenzen) in docs/adr/NNNN-*.md; ADR fällig bei teuer umkehrbaren Entscheidungen. ADR-0001 liegt schon vor: 'Eigene vermessene Pipeline statt sherpa-onnx-Frontend' (die ich-Laut-Geschichte).
- Modelldateien NICHT per Git-LFS (10-GiB-Kontingent, getaktete Abrechnung, Klon-Traffic geht zulasten des Repo-Inhabers), sondern Download-Skript mit SHA-256-Prüfung aus Upstream-Quellen; eigene Artefakte (Martin-Stimme, nach Lizenzklärung) als GitHub-Release-Assets — bis 2 GiB je Datei, Bandbreite unbegrenzt und kostenlos.
- Planung: Walking Skeleton zuerst (TextToSpeechService + festes Phonem-Beispiel + ONNX + AudioTrack, eine Sprache, keine Regeln), danach Meilensteine streng nach Restrisiko geordnet; riskanteste Annahme ist die Phonemgleichheit Android == Windows (espeak-ng im NDK), geprüft per Golden-File-Vergleich mit der Windows-Referenz.
- Definition of Done je Meilenstein immer vierteilig: baut + Tests grün + auf dem Gerät gemessen + Doku nachgezogen (Diagramm, CHANGELOG, ggf. ADR). Die Repo-Doku ist zugleich das Gedächtnis der KI-Sitzungen — Erkenntnisse gehören ins Repo, nicht in den Chatverlauf.
- Sprache: keine gespiegelte Zweisprachigkeit (verdoppelt Pflege und driftet), sondern Rollenteilung: englisches README + Code/Commits englisch; Tiefendoku (Architektur, Erkenntnisse, Regelwerk) deutsch, weil sie deutsche Aussprachephänomene behandelt; Sprachpolitik explizit in CONTRIBUTING festhalten.
- Repo-Hygiene: Keep a Changelog 1.1.0 mit Unreleased-Sektion, Conventional Commits von Hand (ohne Tooling), SemVer mit 0.x bis zum stabilen Gerätelauf und explizit definierter 'öffentlicher API' (TTS-Verhalten + Regeldateiformate), Android-.gitignore aus der offiziellen GitHub-Vorlage plus models/ und Signatur-Dateien.

## Bericht

# Recherchefeld 5: Projektplanung und Dokumentation

Empfehlung für ein Ein-Personen-OpenSource-Projekt dieser Größe (eine Android-App, ein Modell, wachsendes Regelwerk), mit KI-Unterstützung und baldiger Veröffentlichung. Grundsatz vorweg, der alles Folgende ordnet: **Die Doku ist in diesem Projekt nicht Beiwerk, sondern das Gedächtnis.** KI-Sitzungen verlieren Kontext; die hart erarbeiteten Erkenntnisse (stille Phonemfilterung, Sprachwechsel-Flags, Stapelnaht …) haben nur dann Bestand, wenn sie im Repo stehen. Die bestehende VERBESSERUNGEN.md-Kultur („alles gemessen und belegt") ist genau richtig und wird fortgeführt.

## 1. Architekturdokumentation

### arc42: als Checkliste nutzen, nicht als Formular

arc42 hat 12 Abschnitte und sagt selbst: so viel wie nötig, so wenig wie möglich — Weglassen ist ausdrücklich vorgesehen. Für diese Projektgröße lohnt **eine einzige Datei** `docs/architektur.md` mit etwa sieben Abschnitten:

| arc42-Nr. | Abschnitt | Empfehlung |
|---|---|---|
| 1 | Ziele & Qualitätsziele | **Ja, zuerst.** 3–5 messbare Qualitätsziele: RAM/Energie-Budget, Phonemverluste bleiben sichtbar, Ausspracheregeln leicht erweiterbar, Phonemgleichheit mit Windows-Referenz. Randbedingungen (Abschnitt 2) hier als Absatz einarbeiten. |
| 3 | Kontext | **Ja.** Fremde App → `android.speech.tts`-API → dieser Dienst → Audio. Das ist zugleich das C4-Kontextdiagramm. |
| 4 | Lösungsstrategie | **Ja, halbe Seite.** „Vermessene Windows-Pipeline 1:1 portieren, nicht neu erfinden" — mit Verweis auf ADR-0001. |
| 5 | Bausteinsicht | **Ja.** Das gepflegte Blockdiagramm (siehe C4 unten). |
| 6 | Laufzeitsicht | **Ja, ein Diagramm.** Der Weg eines `synthesizeText`-Aufrufs durch die Pipeline bis zum Audio-Callback. |
| 8 | Querschnittskonzepte | **Ja.** Genau die Erkenntnisse, die der Port nicht verlieren darf: Verlustmeldung, remove-flags, Betonungsposition, continuous-Schwelle, Pausenwerte. Hier lebt das Sitzungswissen weiter. |
| 9 | Entscheidungen | **Ausgelagert** in `docs/adr/` (unten), im Hauptdokument nur die Liste mit Links. |
| 11 | Risiken & Schulden | **Ja, kurz.** Lizenzlage (espeak-GPL, Martin-Fine-Tune), misaki-Lücke (ja/zh), Nicht-Bitstabilität des Vokoders. |
| 2, 7, 10, 12 | Randbedingungen, Verteilung, Qualitätsszenarien, Glossar | **Eindampfen:** 2 und 10 in Abschnitt 1 verschmelzen; 7 ist bei einer App ein Absatz (APK, Modell-Download beim ersten Start); 12 nur als kleine Begriffstafel deutsch↔englisch, falls der Code englisch benannt wird (empfohlen, siehe Abschnitt 4). |

### C4 für das Blockdiagramm: Kontext + Container gepflegt, Komponenten einmalig

Die Frage „reichen Kontext + Container?" beantwortet sich fast von selbst: **Ja für die dauerhaft gepflegten Diagramme, plus genau ein Komponentendiagramm für den Pipeline-Kern** (Textregeln → Phonemisierer → Phonemregeln → ONNX → PSOLA), denn genau dort steckt der Wert des Projekts und dort werden Ausspracheregeln künftig eingepflegt. Ebene 4 (Code) nie zeichnen — sie veraltet mit jedem Commit. Faustregel für die Pflege: Kontext- und Containerdiagramm werden bei jeder Architekturänderung im selben Commit nachgezogen (Teil der Definition of Done), das Komponentendiagramm nur bei Pipeline-Umbauten.

### ADRs nach Nygard: kurz, nummeriert, unveränderlich

Format (Nygard 2011): **Titel, Status (vorgeschlagen/akzeptiert/ersetzt durch NNNN), Kontext, Entscheidung, Konsequenzen** — eine Seite, nicht mehr. Ablage `docs/adr/0001-….md`. Ein ADR ist fällig, wenn eine Entscheidung (a) teuer umkehrbar ist oder (b) ein späterer Leser (auch die nächste KI-Sitzung!) fragen würde „warum eigentlich so?". Alte ADRs werden nie editiert, nur durch neue ersetzt — das konserviert die Begründungsgeschichte.

Die ersten ADRs liegen inhaltlich schon vor, sie müssen nur geschrieben werden:
- **0001** Eigene vermessene Pipeline statt sherpa-onnx-Kokoro-Frontend (die ich-Laut-Geschichte — das wertvollste ADR des Projekts, weil es einen bereits einmal begangenen Irrweg versperrt)
- **0002** espeak-ng als Phonemisierer trotz GPL-3.0 (Ergebnis aus dem Lizenz-Recherchefeld einarbeiten)
- **0003** ja/zh zurückstellen, weil misaki reines Python ist
- **0004** Modellvariante (fp32/fp16/int8) — erst nach Messung auf dem Gerät
- **0005** Modelle per Download-Skript statt Git-LFS (Begründung unten)

### Mermaid: bestätigt und empfohlen

GitHub rendert Mermaid seit Februar 2022 nativ in Markdown-Dateien, Issues, PRs und Wikis — Codeblock mit ` ```mermaid ` genügt, keine Plugins ([GitHub-Roadmap-Issue](https://github.com/github/roadmap/issues/372), [Plattformübersicht](https://devpane.tools/mermaid/mermaid-in-markdown)). Zusätzlich rendern GitLab, Obsidian, VS-Code-Vorschau u. a. dieselben Blöcke. Entscheidender Vorteil gegenüber Bilddateien: Diagramme sind **Text, diffbar und von der KI direkt editierbar** — bei diesem Arbeitsmodell fast Pflicht. Praxishinweis: die einfache `flowchart`-Syntax verwenden, nicht Mermaids experimentelle C4-Syntax (`C4Context`) — die ist layoutschwach; ein C4-Container-Diagramm lässt sich als gewöhnliches Flowchart mit Subgraphs sauberer zeichnen. Bei mehr als ~15 Knoten lieber zwei Diagramme als ein unlesbares.

## 2. Repo-Hygiene

### Struktur

```
README.md            (englisch, kurz — siehe Abschnitt 4)
LICENSE              (Hauptlizenz der eigenen Beiträge)
THIRD_PARTY_NOTICES.md  (espeak-ng, Kokoro, ONNX Runtime, …)
CHANGELOG.md         (Keep-a-Changelog-Format)
CONTRIBUTING.md      (inkl. Sprachpolitik und „wie pflege ich eine Ausspracheregel ein")
CLAUDE.md            (Arbeitsanweisungen für KI-Sitzungen: Erkenntnisliste, Testkultur, Nicht-Verhandelbares)
docs/
  architektur.md     (arc42-light, deutsch)
  erkenntnisse.md    (Nachfolger von VERBESSERUNGEN.md: Messungen mit Belegen)
  regelwerk.md       (deutsch: wie textregeln/betonung/wortlaute erweitert werden)
  adr/0001-….md
scripts/
  download-models.(ps1|sh)  (oder Gradle-Task)
app/ …               (Standard-Android-Gradle-Layout)
```

Besonderheit dieses Projekts: **CONTRIBUTING braucht einen Abschnitt „Ausspracheregel melden/einpflegen"** — das ist die wahrscheinlichste Form künftiger Beiträge und zugleich die Nutzeranforderung „leicht einpflegbar". Wer eine falsche Aussprache meldet, soll per Issue-Vorlage nur Wort, Ist-Klang und Soll-Klang angeben müssen.

### Commits, Versionen, Changelog

- **Conventional Commits 1.0.0** ([Spezifikation](https://www.conventionalcommits.org/)): ja — kostet eine Person nichts und macht die Historie maschinen- und KI-lesbar (`feat(regeln): Uhrzeit mit Kasus`, `fix(engine): remove-flags beim Sprachwechsel`). Aber **ohne Tooling**: kein commitlint, kein semantic-release — bei dieser Größe reine Konvention.
- **SemVer 2.0.0** ([semver.org](https://semver.org/)): `0.x` bis der System-TTS-Ersatz auf dem Zielgerät stabil läuft; 1.0.0 ist der erste Alltagseinsatz. Wichtig und oft übersehen: SemVer verlangt eine deklarierte öffentliche API. Für eine App im README definieren: **öffentliche API = Verhalten gegenüber der Android-TTS-API + Formate der Regeldateien + Einstellungsschema.** MAJOR-Sprung, wenn eines davon bricht.
- **CHANGELOG nach [Keep a Changelog 1.1.0](https://keepachangelog.com/)**: `[Unreleased]`-Sektion oben, Kategorien Added/Changed/Fixed/Removed, je Release datiert. Für Menschen geschrieben, nicht aus Commits generiert — gerade weil die Zielgruppe (Nutzer, die eine Stimme installieren) keine Entwickler sind.

### .gitignore und große Dateien

Basis ist die offizielle [Android-Vorlage von GitHub](https://github.com/github/gitignore/blob/main/Android.gitignore) (`.gradle/`, `build/`, `local.properties`, `.cxx/`, `captures/`, `*.keystore`/`*.jks`), ergänzt um `models/` bzw. das Asset-Zielverzeichnis der heruntergeladenen Modelle und um `.idea/`-Anteile außer den teilbaren.

**Modelldateien: Download-Skript, nicht LFS.** Begründung statt Optionsliste:
1. Git-LFS auf GitHub ist seit der Umstellung getaktet abgerechnet; Free/Pro-Konten haben 10 GiB Speicher/Bandbreite frei ([GitHub-Doku](https://docs.github.com/billing/managing-billing-for-git-large-file-storage/about-billing-for-git-large-file-storage), [FAQ](https://github.com/orgs/community/discussions/61362)). Ein 311-MiB-Modell verbraucht das Bandbreitenkontingent nach ~30 Klonen — und der Traffic öffentlicher Klone geht zulasten des Repo-Inhabers. Für ein Projekt, das Verbreitung *will*, ist LFS eine Kostenfalle.
2. Die Kokoro-Modelle und `voices-v1.0.bin` liegen bereits bei Dritten (kokoro-onnx-Releases, Hugging Face). Ein **Download-Skript mit fest verdrahteten URLs und SHA-256-Prüfsummen** (plus Gradle-Task, damit der Build es automatisch zieht) vermeidet zusätzlich die Weiterverbreitung fremder Gewichte aus dem eigenen Repo — was angesichts der ungeklärten Martin-Lizenz auch juristisch die vorsichtigere Haltung ist.
3. Eigene Artefakte (die Martin-Stimme nach Lizenzklärung, fertige APKs) gehören in **GitHub-Release-Assets**: bis 2 GiB je Datei, unbegrenzte, kostenlose Download-Bandbreite ([GitHub-Doku](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases)).
4. Konsequenz für die App selbst: das Modell steckt nicht im APK (Grenznähe zu Play-Limits, F-Droid-Tauglichkeit), sondern wird beim ersten Start in den App-Speicher geladen — dasselbe Prüfsummen-Manifest wie im Build-Skript wiederverwenden.

## 3. Planungsmethodik: Skelett zuerst, dann nach Risiko

### Walking Skeleton als Meilenstein 1

Die dünnste durchgehende Kette, bewusst ohne jeden Ausbau: eine App, die sich als `TextToSpeechService` registriert, einen fest verdrahteten deutschen Satz über espeak-ng-Phonemisierung und das ONNX-Modell synthetisiert und über den TTS-Callback ausgibt — eine Sprache, eine Stimme, keine Regeln, keine Einstellungen. Warum zuerst: dieses Skelett berührt **alle vier großen Unbekannten gleichzeitig** (TTS-Dienstvertrag, espeak im NDK, ORT-Session auf dem Gerät, Audio-Callback-Pfad) und liefert das erste abnahmefähige Ergebnis: „eine fremde App liest hörbar vor."

### Reihenfolge nach Risiko — die riskanteste Annahme zuerst

Die Risiken dieses Projekts sind ungewöhnlich gut bekannt, weil die Geschichte schon einen Fehlschlag enthält. Rangfolge:

1. **R1 — Phonemgleichheit Android == Windows.** Der sherpa-Fehlschlag („itsch") entstand im Integrationsweg, nicht im Modell. Die Annahme „unsere Pipeline erzeugt auf Android dieselben Phoneme wie auf Windows" ist die riskanteste und zugleich billig prüfbare: die Windows-Pipeline erzeugt **Golden Files** (Testsatz → erwartete Phonemfolge), die Android-Seite vergleicht sie in JVM-Tests ohne Emulator — passend zur erprobten Testkultur aus CodeTest. Diese Prüfung gehört in Meilenstein 1, nicht ans Ende.
2. **R2 — espeak-ng als NDK-Bibliothek** liefert mit `remove-flags`-Verhalten und `language_switch` dieselben Ergebnisse (hängt an R1, eigenes Teilrisiko wegen der 17-MB-Daten und der GPL-Prozessfrage).
3. **R3 — RAM/Energie des Modells** auf dem Gerät: fp32 gegen fp16 gegen int8 messen (Ladezeit, Speicher, Hörprobe), erst dann ADR-0004.
4. **R4 — API-Vertrag**: `setLanguage`/`setVoice`/`onIsLanguageAvailable` verhalten sich so, dass reale fremde Apps (Vorlese-Apps, TalkBack) die Stimmen finden.
5. **R5 — misaki (ja/zh)**: kein Android-Weg ohne Python — bewusst zurückstellen (ADR-0003), nicht nebenbei mitschleppen.

Daraus ergibt sich eine natürliche Meilensteinkette: **M1** Walking Skeleton + Golden-File-Gleichheit (erledigt R1/R2) → **M2** Modellvariante gemessen und gewählt (R3) → **M3** volles deutsches Regelwerk portiert, Golden Files erweitert → **M4** Mehrsprachigkeit + Stimmenauswahl über die TTS-API, Einstellungen mit Sprach-An/Abwahl (R4) → **M5** Energie-/Langtextverhalten (continuous-Schwelle, Pausen) auf dem Gerät verifiziert → **M6** Veröffentlichungsreife (Lizenzen geklärt, Doku vollständig, Release-Pipeline). Jeder Meilenstein endet mit etwas Vorführbarem — nie zwei angefangene Baustellen gleichzeitig.

### Definition of Done je Meilenstein

Einheitlich vierteilig, im CONTRIBUTING festgeschrieben: (1) baut reproduzierbar, (2) Tests grün, neue Erkenntnis = neuer Test (die Zählproben-Idee aus betonung.py verallgemeinern), (3) **auf dem Gerät gemessen**, wo eine Behauptung über Klang, RAM oder Energie gemacht wird — Messwert in docs/erkenntnisse.md mit Beleg, (4) Doku nachgezogen: Diagramm, CHANGELOG-`[Unreleased]`, ggf. ADR. Punkt 4 ist bei KI-Unterstützung nicht Kosmetik, sondern die Bedingung dafür, dass die nächste Sitzung auf dem Stand der letzten aufsetzt.

## 4. Deutsch gegen Englisch

Übliche Praxis: Englisch ist die Verkehrssprache von OpenSource; ein rein deutsches Repo halbiert nicht die Reichweite, es dezimiert sie. Zugleich ist die Zielgruppe dieses Projekts (Nutzer einer deutschen Stimme, Melder deutscher Aussprachefehler) überwiegend deutschsprachig, und die Tiefendoku behandelt zwangsläufig deutsche Aussprachephänomene, die sich auf Englisch nur verlustreich erklären lassen.

Volle Zweisprachigkeit (gespiegelte Dokumente) ist die schlechteste Option: Sie verdoppelt die Pflege lebender Dokumente, und Spiegel driften — der Leser weiß nie, welche Fassung stimmt. Davon ist abzuraten.

**Empfehlung: Rollenteilung statt Spiegelung, jedes Dokument hat genau eine Sprache:**
- **Englisch:** README (kurz — was es ist, Screenshot, Installation, Lizenzlage, ein Absatz „German-first project" mit Link auf die deutsche Doku), Code-Bezeichner, Commit-Messages, LICENSE/NOTICES, Issue-Titelvorlagen.
- **Deutsch:** `docs/architektur.md`, `docs/erkenntnisse.md`, `docs/regelwerk.md`, ADR-Texte, CHANGELOG-Einträge (die Nutzer sind deutsch), Issue-Inhalte willkommen in beiden Sprachen.
- Die Sprachpolitik in CONTRIBUTING in zwei Sätzen ausweisen („Code and commits in English; in-depth docs in German because they discuss German pronunciation; issues welcome in both"). Das ist gängige, akzeptierte Praxis bei sprachgebundenen Projekten (Rechtschreibprüfungen, Mundart-Werkzeuge) und kostet fast nichts an Pflege, weil nichts doppelt existiert.

Einziger Wermutstropfen: internationale Entwickler können die Architekturdoku nicht flüssig lesen. Der Preis ist akzeptabel — wer an einer deutschen TTS-Stimme mitarbeiten will, liest Deutsch; und maschinelle Übersetzung von Markdown ist heute für Leser trivial, für den Autor als Pflegepflicht aber teuer. Sollte das Projekt wider Erwarten internationale Mitwirkung am Engine-Kern anziehen, lässt sich gezielt eine englische `docs/architecture-overview.md` (statisches Übersichtsdokument, kein Spiegel der lebenden Doku) nachrüsten — diese Entscheidung dann als ADR.

## Quellen

- [GitHub Roadmap: Mermaid in Markdown (ausgeliefert Feb 2022)](https://github.com/github/roadmap/issues/372) · [Mermaid-Plattformübersicht](https://devpane.tools/mermaid/mermaid-in-markdown)
- [GitHub-Doku: Git-LFS-Abrechnung](https://docs.github.com/billing/managing-billing-for-git-large-file-storage/about-billing-for-git-large-file-storage) · [LFS Metered Billing FAQ](https://github.com/orgs/community/discussions/61362)
- [GitHub-Doku: About releases (2-GiB-Grenze je Asset, Bandbreite frei)](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases)
- [arc42](https://arc42.org/) · [C4-Modell](https://c4model.com/) · [Nygard: Documenting Architecture Decisions](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
- [Keep a Changelog 1.1.0](https://keepachangelog.com/) · [Conventional Commits 1.0.0](https://www.conventionalcommits.org/) · [SemVer 2.0.0](https://semver.org/) · [Android-.gitignore-Vorlage](https://github.com/github/gitignore/blob/main/Android.gitignore)