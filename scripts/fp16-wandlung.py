# SPDX-License-Identifier: GPL-3.0-or-later
"""
fp16-Wandlung eines Kokoro-Modells — das Upstream-Rezept.

onnxconverter-common hinterlaesst DIESER Graphfamilie gemischte Cast-Typen
(gemessen: "Type parameter (T) of Optype (Sub) bound to different types");
ORTs eigener Wandler behandelt sie und den Loop-Teilgraphen. Quelle des
Rezepts: thewh1teagle/kokoro-onnx scripts/export.py (dort woertlich begruendet).

Aufruf:  scripts/.venv/Scripts/python.exe scripts/fp16-wandlung.py EIN.onnx AUS.onnx
Werkzeuge: pip install onnx onnxruntime sympy packaging psutil
"""
import sys
import onnx
from onnxruntime.transformers.onnx_model import OnnxModel

ein, aus = sys.argv[1], sys.argv[2]
graph = OnnxModel(onnx.load(ein))
# keep_io_types: Ein-/Ausgaenge bleiben float32 — jede Variante wird exakt
# gleich aufgerufen, der Kotlin-Code aendert sich nicht.
graph.convert_float_to_float16(keep_io_types=True, use_symbolic_shape_infer=False)
graph.save_model_to_file(aus)
print("geschrieben:", aus)
