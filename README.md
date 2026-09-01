<img src="art/kokoly-symbol.svg" alt="Kokoly icon: a stylized hummingbird with sound arcs" width="96" align="right"/>

# Kokoly

**Kokoly** is an offline text-to-speech engine for Android, built on
[Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M) and
[espeak-ng](https://github.com/espeak-ng/espeak-ng). It replaces the system
TTS: apps select it through the standard Android `TextToSpeech` API
(`setLanguage`, `setVoice`), and languages can be enabled or disabled
individually in the settings.

- **8 languages, 42 voices, fully offline** (24 kHz): German (dedicated
  fine-tuned voice "Martin"), English (US/GB), Spanish, French, Italian,
  Portuguese (BR), Hindi. Japanese and Chinese are planned for stage 2.
- **Speech rate and pitch** follow the system TTS sliders (pitch via
  tempo-compensated resampling; a formant-preserving upgrade is planned).
- **Measured, not guessed:** the pronunciation pipeline is a 1:1 port of a
  measured Windows reference. Every German pronunciation rule is backed by a
  measurement and a character-exact golden test; energy and RAM figures are
  taken on-device, never on the charger (see
  [docs/erkenntnisse.md](docs/erkenntnisse.md)).
- **Frugal by design:** one resident ONNX session, swapped on language-group
  change; an idle timer frees it after 5 minutes; no wakelocks.

## Installation (sideload)

1. Download the APK from the latest
   [GitHub release](../../releases) and install it (Android 8.0+, arm64).
2. Open **Settings → General management → Text-to-speech output** (wording
   varies by vendor), select **Kokoly** as the preferred engine.
3. Open Kokoly's settings (gear icon next to the engine) and download the
   models (~450 MiB, Wi-Fi only by default, SHA-256 verified — see
   [models.json](models.json) for sources). The model weights are **not**
   bundled in the APK.
4. Optional: enable or disable individual languages in the same screen.

## Reporting a pronunciation error

Please open an issue with the word or sentence, what it sounds like now and
what it should sound like — there is an issue template for it. The full path
from report to shipped rule is documented in
[docs/regelwerk.md](docs/regelwerk.md) (German).

## German-first project

The in-depth documentation under [`docs/`](docs/) (architecture, measurement
journal, rule handbook, ADRs) is German; README and CONTRIBUTING are English.
Issues and PRs are welcome in either language.

## License

**GPL-3.0-or-later** (required by bundling espeak-ng). The model weights
remain Apache-2.0 and are distributed separately via
[models.json](models.json). See [LICENSE](LICENSE), [NOTICE](NOTICE),
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md); the license chain of the
German fine-tune is documented in
[docs/lizenz/martin-kette.md](docs/lizenz/martin-kette.md).
