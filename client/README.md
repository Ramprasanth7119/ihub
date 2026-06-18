# IHub Frontend

Production-ready Next.js frontend for the IHub Idea Auction Platform.

## Tech Stack

- Next.js 16 (App Router) + TypeScript
- Tailwind CSS v4 + ShadCN-style components
- React Query, Zustand, Axios
- React Hook Form + Zod
- Framer Motion, Recharts
- STOMP/WebSocket (`@stomp/stompjs` + SockJS) for real-time bidding

> **Note:** The Spring Boot backend uses STOMP over SockJS (not Socket.IO). The frontend `websocket.service.ts` connects to `/ws` topics matching the backend broadcast channels.

## Getting Started

```bash
# Install dependencies
npm install

# Copy environment config
cp .env.local.example .env.local

# Start dev server (requires backend on :8081)
npm run dev
```

Open [http://localhost:3000](http://localhost:3000).

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `NEXT_PUBLIC_API_URL` | `http://localhost:8081` | Spring Boot API base URL |
| `NEXT_PUBLIC_WS_URL` | `http://localhost:8081/ws` | WebSocket/STOMP endpoint |

API requests are proxied through Next.js rewrites (`/api/*` → backend) to avoid CORS issues in development.

## Project Structure

```
src/
├── app/              # Next.js App Router pages
├── components/       # UI, layout, shared components
├── features/         # Feature-specific modules (landing, dashboard, auctions)
├── services/         # API & WebSocket services
├── store/            # Zustand stores (auth, notifications)
├── hooks/            # Custom React hooks
├── providers/        # React Query & app providers
├── lib/              # Utilities, axios, JWT helpers
├── types/            # TypeScript interfaces
└── constants/        # App constants & nav config
```

## Pages

| Route | Description |
|-------|-------------|
| `/` | Landing page |
| `/login`, `/register` | Authentication |
| `/dashboard` | Role-based dashboard (Creator / Investor) |
| `/ideas` | Idea list with search & filters |
| `/ideas/new` | Create idea (Creator) |
| `/ideas/[id]` | Idea details |
| `/search` | Elasticsearch-powered discovery |
| `/auctions` | Auction list (Active / Upcoming / Closed) |
| `/auctions/[id]` | Live auction with bidding & leaderboard |
| `/notifications` | Notification center |
| `/profile` | User profile & history |

## Real-Time Topics

| Topic | Purpose |
|-------|---------|
| `/topic/auction/{id}/bids` | Live bid updates |
| `/topic/auction/{id}/leaderboard` | Leaderboard refresh |
| `/topic/user/{id}/notifications` | User notifications |
| `/topic/user/{id}/notifications/count` | Unread count |
