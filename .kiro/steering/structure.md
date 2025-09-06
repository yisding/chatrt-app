# Project Structure

## Root Files
- **main.tsx** - Main application entry point with Hono routing
- **deno.json** - Deno configuration with Val.town types and experimental features
- **README.md** - Project documentation and setup instructions

## Directory Structure

### `/frontend/`
- **index.html** - Single-page application with embedded CSS/JavaScript
- Complete WebRTC client implementation
- Responsive UI with video mode selection (audio-only, webcam, screen)
- Real-time logging and connection status

### `/routes/`
- **rtc.ts** - WebRTC endpoint handling (`POST /rtc`)
- **observer.ts** - WebSocket observer for OpenAI Realtime API (`POST /observer/:callId`)
- **utils.ts** - Shared utilities (headers, session configuration)

### `/.kiro/`
- **steering/** - AI assistant guidance documents
- **settings/** - Kiro configuration files

### `/.vt/`
- **state.json** - Val.town state management

## Architecture Patterns

### API Routes
- RESTful endpoints using Hono framework
- Modular route organization in `/routes/` directory
- Shared utilities for common functionality

### WebRTC Flow
1. Client creates offer via `POST /rtc`
2. Server forwards to OpenAI Realtime API
3. Background observer WebSocket established
4. Real-time audio/video streaming

### File Organization
- Single HTML file with embedded styles/scripts
- TypeScript routes with clear separation of concerns
- Utility functions extracted to shared modules
- Configuration centralized in deno.json

## Naming Conventions
- **Routes**: lowercase with descriptive names (rtc.ts, observer.ts)
- **Utilities**: shared functionality in utils.ts
- **Frontend**: single index.html with embedded assets
- **Config**: standard Deno/Val.town conventions