# Third-Party Notices

Skeleton — completed per milestone; final completeness check in M6.
Details and verbatim license research: `docs/recherche/feld4-lizenzen.md`.

| Component | Version/Pin | License | Role | Source |
|---|---|---|---|---|
| espeak-ng (code **and** data) | commit pinned in `native/espeak-ng/` (task 0.2) | GPL-3.0-or-later | grapheme-to-phoneme for 8 languages | https://github.com/espeak-ng/espeak-ng |
| Kokoro-82M weights | v1.0 | Apache-2.0 | TTS model (see model card for CC-BY dataset attributions: Koniwa, SIWIS) | https://huggingface.co/hexgrad/Kokoro-82M |
| kokoro-onnx model files (kokoro-v1.0.onnx, voices-v1.0.bin) | model-files-v1.1 | Apache-2.0 | ONNX export + voice vectors | https://github.com/thewh1teagle/kokoro-onnx |
| Kokoro-82M German fine-tune "Martin" | pinned revision, see models.json | Apache-2.0 (chain per `docs/lizenz/martin-kette.md`, dossier in progress) | German voice | https://huggingface.co/Godelaune/Kokoro-82M-ONNX-German-Martin |
| ONNX Runtime (onnxruntime-android) | 1.23.2 (pinned, ADR-0008) | MIT (+ its ThirdPartyNotices) | inference runtime | https://github.com/microsoft/onnxruntime |

The combined work is distributed under GPL-3.0-or-later (see LICENSE); the
Apache-2.0/MIT components remain under their own licenses.
