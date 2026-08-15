import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { WS_URL } from "@/constants";
import { useAuthStore } from "@/store/auth-store";
import type { BidUpdate, LeaderboardEntry, Notification } from "@/types";

type MessageHandler<T> = (data: T) => void;
type Unsubscribe = () => void;

/**
 * STOMP-over-SockJS client for live bidding and notifications.
 *
 * <p>SockJS cannot attach an `Authorization` header to its transport requests, so
 * the access token travels on the STOMP `CONNECT` frame instead. The backend
 * requires it before allowing a subscription to `/topic/user/{id}/**`; auction
 * topics are public and work without one.</p>
 */
class WebSocketService {
  private client: Client | null = null;
  private pendingConnectCallbacks: Array<() => void> = [];
  /** Token the live connection was opened with, so we can detect a change. */
  private connectedToken: string | null = null;

  connect(onConnect?: () => void) {
    const token = useAuthStore.getState().accessToken ?? null;

    // Signing in or out mid-session changes who the socket is allowed to hear
    // from, so the connection is rebuilt rather than reused with a stale identity.
    if (this.client && this.connectedToken !== token) {
      this.reset();
    }

    if (this.client?.connected) {
      onConnect?.();
      return;
    }

    if (onConnect) {
      this.pendingConnectCallbacks.push(onConnect);
    }

    if (this.client) return;

    this.connectedToken = token;
    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL) as WebSocket,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      // Re-read the token on every reconnect: after a refresh, the old one is
      // no longer the one the server will accept.
      beforeConnect: () => {
        const current = useAuthStore.getState().accessToken ?? null;
        this.connectedToken = current;
        if (this.client) {
          this.client.connectHeaders = current ? { Authorization: `Bearer ${current}` } : {};
        }
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        const callbacks = [...this.pendingConnectCallbacks];
        this.pendingConnectCallbacks = [];
        callbacks.forEach((cb) => cb());
      },
      onStompError: () => {
        /* the client reconnects on its own; consumers show a "connecting" state */
      },
    });

    this.client.activate();
  }

  /** Tears the connection down — called on logout and on identity change. */
  reset() {
    this.pendingConnectCallbacks = [];
    this.connectedToken = null;
    const client = this.client;
    this.client = null;
    client?.deactivate();
  }

  subscribeAuctionBids(auctionId: number, handler: MessageHandler<BidUpdate>): Unsubscribe {
    return this.subscribe(`/topic/auction/${auctionId}/bids`, handler);
  }

  subscribeAuctionLeaderboard(
    auctionId: number,
    handler: MessageHandler<LeaderboardEntry[]>
  ): Unsubscribe {
    return this.subscribe(`/topic/auction/${auctionId}/leaderboard`, handler);
  }

  subscribeUserNotifications(userId: number, handler: MessageHandler<Notification>): Unsubscribe {
    return this.subscribe(`/topic/user/${userId}/notifications`, handler);
  }

  subscribeUnreadCount(userId: number, handler: MessageHandler<number>): Unsubscribe {
    return this.subscribe(`/topic/user/${userId}/notifications/count`, handler);
  }

  isConnected(): boolean {
    return this.client?.connected ?? false;
  }

  private subscribe<T>(destination: string, handler: MessageHandler<T>): Unsubscribe {
    let subscription: StompSubscription | null = null;

    const attach = () => {
      if (!this.client?.connected || subscription) return;
      subscription = this.client.subscribe(destination, (message: IMessage) => {
        try {
          handler(JSON.parse(message.body) as T);
        } catch {
          handler(message.body as unknown as T);
        }
      });
    };

    this.connect(attach);

    return () => {
      subscription?.unsubscribe();
      subscription = null;
    };
  }
}

export const wsService = new WebSocketService();
