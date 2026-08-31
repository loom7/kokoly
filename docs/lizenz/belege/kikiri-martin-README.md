---
language:
- de
license: apache-2.0
tags:
- text-to-speech
- german
- kokoro
- styletts2
- single-speaker
pipeline_tag: text-to-speech
base_model: kikiri-tts/kikiri-german-base-51speakers-synthetic
---

# Kikiri German — Martin Harbecke

German single-speaker TTS model fine-tuned on the **Martin Harbecke** voice using [StyleTTS2](https://github.com/yl4579/StyleTTS2) Stage 2.

Built on top of [kikiri-german-base-51speakers-synthetic](https://huggingface.co/kikiri-tts/kikiri-german-base-51speakers-synthetic).

## Demo

**StyleTTS 2 — Abstract (German)**
<audio controls><source src="https://huggingface.co/kikiri-tts/kikiri-german-martin/resolve/main/audio/styletts2_demo.wav" type="audio/wav"></audio>

## Model Details

| Property | Value |
|---|---|
| Architecture | StyleTTS2 Stage 2 (Kokoro-compatible) |
| Language | German (de) |
| Speaker | Martin Harbecke (synthetic) |
| Training samples | 627 |
| Training epochs | 10 |
| WavLM discriminator | active from epoch 4 |
| Mixed precision | bf16 |
| Sample rate | 24 kHz |

## Usage

```python
# Uses the kokoro library as underlying framework
from kokoro import KModel, KPipeline
import torch

kmodel = KModel(repo_id="hexgrad/Kokoro-82M", config="config.json", model="kikiri_german_martin_ep10.pth").to("cuda").eval()
pipeline = KPipeline(lang_code="de", model=kmodel)
voice = torch.load("voices/martin.pt", map_location="cpu")

text = "Guten Tag, wie geht es Ihnen?"
for _, _, audio in pipeline(text, voice=voice):
    # audio is a numpy array at 24kHz
    pass
```

## Files

| File | Description |
|---|---|
| `kikiri_german_martin_ep10.pth` | Kokoro-compatible model weights (Stage 2, epoch 10) |
| `voices/martin.pt` | Martin speaker voicepack (510, 1, 256) |
| `audio/styletts2_demo.wav` | Demo: StyleTTS 2 abstract read by Martin |

## License

Apache 2.0
