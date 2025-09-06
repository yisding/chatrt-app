# Technology Stack

## Runtime & Platform
- **Deno** - TypeScript/JavaScript runtime
- **Val.town** - Hosting platform for serverless functions
- **TypeScript/TSX** - Primary language with JSX support

## Frameworks & Libraries
- **Hono** - Lightweight web framework for routing
- **WebRTC** - Real-time peer-to-peer communication
- **WebSocket** - Server-side connection to OpenAI Realtime API
- **npm:ws** - WebSocket client library

## APIs & Services
- **OpenAI Realtime API** - Voice AI model (gpt-realtime)
- **WebRTC API** - Browser media streaming
- **MediaDevices API** - Camera/microphone/screen access

## Configuration
- **deno.json** - Project configuration with Val.town types
- **Experimental features**: unstable-node-globals, unstable-temporal, unstable-worker-options
- **Lint rules**: no-explicit-any excluded
- **Strict mode**: disabled for flexibility

## Common Commands
```bash
# Run locally (if supported)
deno run --allow-net --allow-env main.tsx

# Deploy to Val.town
# Use Val.town web interface or CLI

# Environment setup
export OPENAI_API_KEY="your-api-key"
```

## Development Notes
- No node_modules directory (Deno native)
- Uses ESM imports with npm: prefix for Node packages
- Val.town specific utilities via esm.town imports