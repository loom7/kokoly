# Kokoly

*(Early scaffold — the engine is under construction, milestone M0 of
[PROJEKTPLAN.md](PROJEKTPLAN.md). Nothing to install yet.)*

**Kokoly** will be an offline text-to-speech engine for Android, built on
[Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M) and
[espeak-ng](https://github.com/espeak-ng/espeak-ng), replacing the system TTS:
apps select it via the standard Android `TextToSpeech` API (`setLanguage`,
`setVoice`), languages can be enabled and disabled individually in the settings.

Planned for 1.0: German (dedicated fine-tuned voice) plus English (US/GB),
Spanish, French, Italian, Portuguese (BR) and Hindi — 42 voices, fully offline,
24 kHz. Japanese and Chinese follow in stage 2.

The pronunciation pipeline is a 1:1 port of a measured Windows reference
implementation, with a data-driven rule layer for German pronunciation fixes —
every rule is backed by a measurement and a golden test.

This is a German-first project: the in-depth documentation under
[`docs/`](docs/) is German. Issues in English are welcome.

## License

GPL-3.0-or-later (forced by bundling espeak-ng; the model weights remain
Apache-2.0). See [LICENSE](LICENSE), [NOTICE](NOTICE),
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
