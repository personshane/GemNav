# GemNav

Multi-tier navigation app combining Gemini AI with Google Maps and HERE SDK.

## Project Structure

```
GemNav/
├── docs/              # Product documentation
├── prompts/           # AI prompt templates
├── android/           # Android-specific implementation
│   ├── intents/      # Intent handling
│   ├── ui/           # UI components
│   └── architecture/ # App architecture
├── here/              # HERE SDK integration
└── google/            # Google Maps integration
```

## Tiers

- **Free**: Gemini Nano (on-device) + Google Maps via intents
- **Plus**: Gemini Cloud + Google Maps SDK
- **Pro**: HERE SDK truck routing + optional Google Maps car routing

## Status

Initialization complete. See `docs/microproject_index.md` for progress tracking.
