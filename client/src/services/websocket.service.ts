import { Client, type IMessage, type StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { WS_URL } from "@/constants";
import type { BidUpdate, LeaderboardEntry, Notification } from "@/types";

type MessageHandler<T> = (data: T) => void;
type Unsubscribe = () => void;

class WebSocketService {
  private client: Client | null = null;
  private pendingConnectCallbacks: Array<() => void> = [];

  connect(onConnect?: () => void) {
    if (this.client?.connected) {
      onConnect?.();
      return;
    }

    if (onConnect) {
      this.pendingConnectCallbacks.push(onConnect);
    }

    if (this.client) return;

    this.client = new Client({
      webSocketFactory: () => new SockJS(WS_URL) as WebSocket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        const callbacks = [...this.pendingConnectCallbacks];
        this.pendingConnectCallbacks = [];
        callbacks.forEach((cb) => cb());
      },
      onStompError: () => {
        /* connection errors surface via reconnect */
      },
    });

    this.client.activate();
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
