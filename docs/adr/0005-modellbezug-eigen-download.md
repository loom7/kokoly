# ADR-0005: Modellbezug: Eigen-Download von GitHub-Release, kein LFS, keine Gewichte im Git/APK

**Status:** angenommen (24.08.2026)
**Kontext-Dokument:** [PROJEKTPLAN.md](../../PROJEKTPLAN.md), Abschnitt 3

## Entscheidung

Repo enthält nur Prüfsummen + Manifest (`models.json`, RHVoice-Muster, von Anfang an mehrmodell- und mehrvariantenfähig: kokoro-martin, kokoro-v1.0, voices). App lädt beim ersten Start vom eigenen GitHub-Release (SHA-256, HTTP-Range-Resume, atomares Umbenennen nach `noBackupFilesDir`, standardmäßig nur ungetaktetes Netz, Mobilfunk-Opt-in), wandelt einmalig on-device nach .ort und löscht die .onnx nach verifizierter .ort (SHA-256 der .ort ins lokale Manifest; Re-Download ist der dokumentierte Wiederherstellungsweg). Die Wandlung ist im UI ein ausgewiesener einmaliger Einrichtungsschritt, bevorzugt am Ladegerät/im Leerlauf. Die voices-Datei wird per versioniertem Skript auf die Stufe-1-Stimmen zugeschnitten (~21 MiB), liegt `noCompress` im APK — aber nicht im Git (Build bezieht sie über `models.json`). Solange F1 offen ist, bleibt jedes Release mit Martin-Dateien Draft/privat

## Begründung

GitHub blockt >100 MB; LFS-Bandbreite wäre nach ~30 Klonen erschöpft; Releases sind kostenlos (≤2 GiB/Datei). .ort-on-device löst die ORT-Versionsbindung des Formats. 27-MiB-Binärblobs im Git widersprächen der eigenen Regel und wüchsen mit jedem Update in der Historie

## Verworfene Alternativen

Git LFS (Kostenfalle); Modelle im APK (~680 MiB); Play Asset Delivery on-demand (Play-Core proprietär, GPL-unverträglich); voices-v1.0.bin komplett bündeln (6–7 MiB toter ja/zh-Ballast)
