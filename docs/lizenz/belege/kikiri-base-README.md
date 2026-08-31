---
language:
- de
license: apache-2.0
tags:
- text-to-speech
- german
- kokoro
- styletts2
- multi-speaker
pipeline_tag: text-to-speech
---

# Kikiri German Base — 51 Speakers Synthetic

A German multi-speaker TTS base model trained with [StyleTTS2](https://github.com/yl4579/StyleTTS2) on a synthetic dataset of 51 German speakers.

This is a **Stage 1 base model** — it provides the acoustic foundation for speaker-adapted fine-tuning (Stage 2). It is compatible with the [Kokoro](https://github.com/hexgrad/kokoro) inference architecture.

## Model Details

| Property | Value |
|---|---|
| Architecture | StyleTTS2 Stage 1 (Kokoro-compatible) |
| Language | German (de) |
| Speakers | 51 synthetic voices |
| Training data | ~30,800 samples (synthetic, TTS-generated) |
| Training epochs | 4 |
| Validation Mel Loss | 0.286 |
| Sample rate | 24 kHz |
| G2P | misaki 0.9.4 + espeak-ng 1.50 |

## Audio Samples

Generated with the Victoria voice (Stage 2 fine-tune, `voices/victoria.pt`).

**"Schön, dass du da bist. Die Bücher liegen auf dem großen Tisch."**
<audio controls><source src="https://huggingface.co/kikiri-tts/kikiri-german-base-51speakers-synthetic/resolve/main/audio/test_01.wav" type="audio/wav"></audio>

**"Ich mache mich auf den Weg nach Aachen, um auch nachts wach zu sein."**
<audio controls><source src="https://huggingface.co/kikiri-tts/kikiri-german-base-51speakers-synthetic/resolve/main/audio/test_02.wav" type="audio/wav"></audio>

**"Er aß die Maße in der Straße, aber das Maß war voll."**
<audio controls><source src="https://huggingface.co/kikiri-tts/kikiri-german-base-51speakers-synthetic/resolve/main/audio/test_03.wav" type="audio/wav"></audio>

**"Zwei weiße Zwerge zwängen sich zwischen zwei Zweige."**
<audio controls><source src="https://huggingface.co/kikiri-tts/kikiri-german-base-51speakers-synthetic/resolve/main/audio/test_04.wav" type="audio/wav"></audio>

**"Ein Pfau pflegt seine Federn an der Pfütze."**
<audio controls><source src="https://huggingface.co/kikiri-tts/kikiri-german-base-51speakers-synthetic/resolve/main/audio/test_05.wav" type="audio/wav"></audio>

**"Warum hast du das getan? Das ist ja unglaublich!"**
<audio controls><source src="https://huggingface.co/kikiri-tts/kikiri-german-base-51speakers-synthetic/resolve/main/audio/test_06.wav" type="audio/wav"></audio>

**"Das kostet genau einhundertdreiundzwanzig Millionen Euro."**
<audio controls><source src="https://huggingface.co/kikiri-tts/kikiri-german-base-51speakers-synthetic/resolve/main/audio/test_07.wav" type="audio/wav"></audio>

## Files

| File | Description |
|---|---|
| `kikiri_german_base_51spk_ep4.pth` | Model weights (Kokoro-compatible format) |
| `voices/victoria.pt` | Victoria speaker voicepack (512-dim style embedding) |
| `audio/test_*.wav` | German phonetic test sentences |

## Usage

```python
# Uses the kokoro library as underlying framework
from kokoro import KPipeline

pipeline = KPipeline(lang_code="de", model_path="kikiri_german_base_51spk_ep4.pth")
voicepack = pipeline.load_voice("voices/victoria.pt")

text = "Guten Tag, wie geht es Ihnen?"
audio = pipeline(text, voice=voicepack)
```

## Training

- **Stage 1** trains the acoustic model on mel spectrogram reconstruction across all 51 speakers
- **Stage 2** fine-tunes a single speaker using WavLM adversarial training (bf16)
- Data pipeline: text → misaki G2P (de) → Kokoro 178-token IPA vocabulary
- All training data phoneme-validated: no `??` artifacts, no OOV symbols

## Limitations

- Trained entirely on **synthetic** (TTS-generated) audio — real human recordings may improve naturalness
- Stage 1 alone requires Stage 2 fine-tuning for production-quality single-speaker output
- German number/date normalization is handled by the caller (not built-in)

## License

Apache 2.0
