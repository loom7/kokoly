# ADR-0008: minSdk 26 · targetSdk aktuell · ORT 1.23.2 gepinnt

**Status:** angenommen (24.08.2026)
**Kontext-Dokument:** [PROJEKTPLAN.md](../../PROJEKTPLAN.md), Abschnitt 3

## Entscheidung

minSdk 26 · targetSdk aktuell · ORT 1.23.2 gepinnt

## Begründung

minSdk 26: Voice-API + `rangeStart` ohne Verzweigungen, ORT verlangt ≥24. onnxruntime-android 1.23.2: erste 16-KB-Page-kompatible Version und mit 19,3 MiB (arm64-.so) die kleinste kompatible — 1.29.0 wäre 32,1 MiB (WebGPU-Ballast). Upgrade nur bei konkretem Fix

## Verworfene Alternativen

1.22.0 (keine 16-KB-Pages); jeweils Neueste (Ballast); ORT-Custom-Build (3,3 statt 19 MiB möglich, aber Wartungslast — als spätere Option notiert; für F-Droid ggf. wieder relevant, s. Stufe 2)
