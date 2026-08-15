package com.ihub.security;

import com.ihub.dao.UserDao;
import com.ihub.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Subscription authorization for the STOMP broker.
 *
 * <p>The simple broker treats subscription destinations as Ant patterns, so an
 * allow-unless-matched design is bypassable by subscribing to a wildcard such as
 * {@code /topic/user/*}&#47;{@code notifications}. These tests pin the default-deny
 * behaviour that closes that hole.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StompAuthChannelInterceptorTest {

    private static final String SECRET = "an-interceptor-test-secret-that-is-long-enough";
    private static final Long USER_ID = 7L;

    @Mock
    private UserDao userDao;

    private StompAuthChannelInterceptor interceptor;
    private JwtUtil jwtUtil;
    private final MessageChannel channel = mock(MessageChannel.class);

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, 3_600_000L, 604_800_000L);
        interceptor = new StompAuthChannelInterceptor(jwtUtil, userDao);

        User user = new User();
        user.setId(USER_ID);
        user.setEmail("investor@ihub.test");
        user.setRole("INVESTOR");
        when(userDao.findByEmail("investor@ihub.test")).thenReturn(user);
    }

    @Test
    @DisplayName("a wildcard destination is rejected even though it targets no specific user")
    void rejectsWildcardDestination() {
        Map<String, Object> session = connectedSession();

        assertThatThrownBy(() -> subscribe("/topic/user/*/notifications", session))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("a broad ** wildcard cannot be used to capture every topic")
    void rejectsDoubleStarWildcard() {
        assertThatThrownBy(() -> subscribe("/topic/**", connectedSession()))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("an unknown destination shape is denied rather than allowed through")
    void rejectsUnknownDestination() {
        assertThatThrownBy(() -> subscribe("/topic/admin/secrets", connectedSession()))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("another user's notification topic is denied")
    void rejectsForeignUserTopic() {
        assertThatThrownBy(() -> subscribe("/topic/user/999/notifications", connectedSession()))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("an anonymous session cannot reach any user topic")
    void rejectsAnonymousUserTopic() {
        assertThatThrownBy(() -> subscribe("/topic/user/7/notifications", new HashMap<>()))
                .isInstanceOf(MessagingException.class);
    }

    @Test
    @DisplayName("a user may subscribe to their own notification topics")
    void allowsOwnUserTopic() {
        Map<String, Object> session = connectedSession();

        assertThatCode(() -> subscribe("/topic/user/7/notifications", session)).doesNotThrowAnyException();
        assertThatCode(() -> subscribe("/topic/user/7/notifications/count", session)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("auction topics stay public — the same data is served over public REST")
    void allowsPublicAuctionTopics() {
        assertThatCode(() -> subscribe("/topic/auction/12/bids", new HashMap<>()))
                .doesNotThrowAnyException();
        assertThatCode(() -> subscribe("/topic/auction/12/leaderboard", new HashMap<>()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a valid token on CONNECT establishes the session identity")
    void connectBindsIdentity() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Map<String, Object> session = new HashMap<>();
        accessor.setSessionAttributes(session);
        accessor.setNativeHeader(
                "Authorization",
                "Bearer " + jwtUtil.generateAccessToken("investor@ihub.test", "INVESTOR"));
        // Spring's StompSubProtocolHandler creates inbound frames with mutable
        // headers; without this the interceptor cannot attach the principal.
        accessor.setLeaveMutable(true);

        interceptor.preSend(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), channel);

        assertThatCode(() -> subscribe("/topic/user/7/notifications", session)).doesNotThrowAnyException();
    }

    // ------------------------------------------------------------------ helpers

    /** A session that has already completed an authenticated CONNECT. */
    private Map<String, Object> connectedSession() {
        Map<String, Object> session = new HashMap<>();
        session.put("ihubUserId", USER_ID);
        return session;
    }

    private void subscribe(String destination, Map<String, Object> sessionAttributes) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSessionAttributes(sessionAttributes);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        interceptor.preSend(message, channel);
    }
}
