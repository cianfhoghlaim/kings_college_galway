# 06. Document Processing

OCR, VLM, and PDF extraction for Celtic language historical documents.

## Overview

This category covers techniques for extracting text from historical Celtic language documents, including:
- Optical Character Recognition (OCR) for scanned materials
- Vision Language Models (VLMs) for complex document understanding
- PDF extraction and processing pipelines

## Documents

| File | Description |
|------|-------------|
| `Celtic Language OCR Resource Analysis.md` | Analysis of OCR tools for Celtic scripts |
| `Open-Source VLMs For PDF Extraction.md` | VLM-based extraction strategies |

## Key Tools

### OCR
- Tesseract with Irish/Welsh language packs
- EasyOCR for multilingual support
- Google Cloud Vision for high accuracy

### VLMs
- LLaVA for document understanding
- Donut for document OCR
- LayoutLM for structured extraction

### Processing
- pdf2image for conversion
- PyMuPDF for text extraction
- pdfplumber for table extraction

## Use Cases

1. **Historical Manuscripts** - Digitizing pre-1900 Irish texts
2. **Government Documents** - Processing bilingual official publications
3. **Educational Materials** - Converting textbooks to searchable format
4. **Newspaper Archives** - Extracting Irish language journalism

## Technical Patterns

```python
# VLM-based extraction pattern
from transformers import AutoProcessor, AutoModelForVision2Seq

processor = AutoProcessor.from_pretrained("microsoft/donut-base")
model = AutoModelForVision2Seq.from_pretrained("microsoft/donut-base")

# Process document image
pixel_values = processor(image, return_tensors="pt").pixel_values
outputs = model.generate(pixel_values)
text = processor.batch_decode(outputs, skip_special_tokens=True)
```

## Related Categories

- **01-celtic-language-ai-resources** - Models for post-processing
- **02-celtic-data-acquisition** - Pipeline integration
- **03-bilingual-dataset-creation** - Corpus building from extracts
