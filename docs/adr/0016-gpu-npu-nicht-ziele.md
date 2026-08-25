# ADR-0016: GPU und NPU sind geprüfte Nicht-Ziele

**Status:** angenommen (25.08.2026, Nutzer-Entscheid)
**Kontext:** Nutzerfrage nach weiteren RTF-Senkungen und GPU-Ausführung,
nach Abschluss der M2a-Matrix.

## Entscheidung

Kokoly rechnet auf der **CPU**. GPU- und NPU-Backends werden für Version 1.x
nicht verfolgt — als bewusste, begründete Entscheidung, nicht als Lücke.
Ein WebGPU-Messexperiment bleibt als möglicher Stufe-2-Punkt notiert.

## Begründung

**1. Der Graph ist für mobile Beschleuniger-Backends strukturell ungeeignet.**
Kokoro hat dynamische Sequenzlängen, einen Loop-Teilgraphen mit
Sequence-Operationen (STFT-Rahmenschleife) und seedlose Zufallsknoten.
Mobile GPU-/NPU-Backends beherrschen statische Faltungsnetze; bei
Kontrollfluss und dynamischen Formen zerfällt der Graph in Partitionen, und
jeder Übergang ist ein Host⇄Beschleuniger-Transfer. Die Rechenblöcke dazwischen
sind zu kurz, um die Transferkosten hereinzuholen.

**2. Die CPU-Messlage nimmt jedem Beschleuniger das Motiv.** fp16 über den
CPU-EP liefert am Zielgerät RTF 0,35–0,41 (M2a, 25.08.2026) — dreifache
Echtzeit. Ein Beschleuniger müsste dies schlagen UND den zweiten Codepfad
rechtfertigen, denn der CPU-Fallback für alle anderen Geräte bliebe ohnehin
bestehen: zwei Inferenzwege, zwei Fehlerbilder, doppelte Wartung.

**3. Die konkreten Backends scheitern einzeln:**
- **NNAPI:** von Google abgekündigt; schwach bei dynamischen Formen.
- **QNN (Hexagon-NPU/GPU):** bindet an Snapdragon — bricht das
  Kompatibilitätsziel per Definition; bringt proprietäre Qualcomm-Bibliotheken
  mit, die sich mit der GPL-3.0-Lizenzierung dieser App beißen; im
  Schwesterprojekt für die dortige TTS bereits mit denselben Argumenten
  abgewählt (CodeTest, Stufe-6-Entscheid 19.08.2026).
- **WebGPU-EP:** experimentell, auf Android kaum erprobt — als
  Stufe-2-Messexperiment denkbar, nicht als 1.x-Fundament.

**4. Energie:** Für kurze Sprechstöße (der TTS-Lastfall) sind
Beschleuniger-Anläufe samt Transfers erfahrungsgemäß teurer als der
CPU-Burst; die M5-mAh-Messung bewertet ausschließlich CPU-Konfigurationen.

## Verworfene Alternativen

GPU/NPU als „später nachrüstbar" offenzuhalten ohne ADR — dann würde die
Begründung verloren gehen und die Frage in jeder Planungsrunde neu aufreißen.

## Wiedervorlage

Nur wenn sich eine Voraussetzung ändert: ein stabiler, Loop-fähiger
Android-GPU-EP in ORT, ein Kokoro-Export ohne Kontrollfluss, oder ein
Zielgerät, dessen CPU die Echtzeit nicht mehr hält.
