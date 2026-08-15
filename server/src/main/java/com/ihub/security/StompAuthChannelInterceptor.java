package com.ihub.security;

import com.ihub.dao.UserDao;
import com.ihub.model.User;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authenticates and authorises STOMP traffic.
 *
 * <p>The {@code /ws} handshake itself stays open (SockJS cannot attach an
 * {@code Authorization} header to its transport requests), so identity is
 * established on the STOMP {@code CONNECT} frame instead, which carries native
 * headers. Without this, {@code /topic/user/{id}/notifications} was readable by
 * anyone who guessed another user's id.</p>
 *
 * <p>Auction topics stay public — bids and leaderboards are already exposed by the
 * public REST endpoints — but per-user topics require a matching identity.</p>
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);

    /** Per-user destinations: /topic/user/{userId}/... — owner only. */
    private static final Pattern USER_TOPIC = Pattern.compile("^/topic/user/(\\d+)(/[A-Za-z0-9/_-]*)?$");

    /** Public auction destinations: /topic/auction/{auctionId}/... */
    private static final Pattern AUCTION_TOPIC =
            Pattern.compile("^/topic/auction/\\d+(/[A-Za-z0-9/_-]*)?$");

    /**
     * Ant-style wildcards. Spring's simple broker stores subscription destinations as
     * Ant patterns and matches them against publish destinations, so a subscription
     * to {@code /topic/user/*&#47;notifications} would receive every user's messages.
     */
    private static final Pattern WILDCARD = Pattern.compile("[*?{}\\[\\]]");

    private static final String USER_ID_ATTRIBUTE = "ihubUserId";

    private final JwtUtil jwtUtil;
    private final UserDao userDao;

    public StompAuthChannelInterceptor(JwtUtil jwtUtil, UserDao userDao) {
        this.jwtUtil = jwtUtil;
        this.userDao = userDao;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT -> authenticate(accessor);
            case SUBSCRIBE -> authoriseSubscription(accessor);
            default -> {
                // MESSAGE / ACK / DISCONNECT need no additional checks: the broker is
                // read-only for clients and no @MessageMapping destinations exist.
            }
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String token = firstNativeHeader(accessor, "Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            // Anonymous connections are allowed; they simply cannot subscribe to
            // per-user destinations.
            return;
        }

        try {
            String raw = token.substring("Bearer ".length()).trim();
            if (!jwtUtil.isAccessToken(raw)) {
                return;
            }

            String email = jwtUtil.extractEmail(raw);
            String role = jwtUtil.extractRole(raw);
            User user = userDao.findByEmail(email);
            if (user == null) {
                return;
            }

            Principal principal = new UsernamePasswordAuthenticationToken(
                    email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
            accessor.setUser(principal);

            // Cached on the session so SUBSCRIBE frames avoid a database round trip.
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put(USER_ID_ATTRIBUTE, user.getId());
            }

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected STOMP CONNECT token: {}", e.getMessage());
        }
    }

    /**
     * Authorises a SUBSCRIBE frame.
     *
     * <p>Default-deny: only the two destination shapes the application actually
     * publishes to are permitted, and anything containing an Ant wildcard is
     * rejected outright. An allow-unless-matched design would be bypassable by
     * subscribing to a pattern such as {@code /topic/**}, which the simple broker
     * would happily match against every user's private destination.</p>
     */
    private void authoriseSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        if (destination == null || WILDCARD.matcher(destination).find()) {
            reject(destination, "wildcard or missing destination", null);
        }

        if (AUCTION_TOPIC.matcher(destination).matches()) {
            // Auction bids and leaderboards are already public over REST.
            return;
        }

        Matcher matcher = USER_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            reject(destination, "destination is not an exposed topic", null);
        }

        long requestedUserId = Long.parseLong(matcher.group(1));
        Long sessionUserId = accessor.getSessionAttributes() == null
                ? null
                : (Long) accessor.getSessionAttributes().get(USER_ID_ATTRIBUTE);

        if (sessionUserId == null || sessionUserId != requestedUserId) {
            reject(destination, "not the owner of this destination", sessionUserId);
        }
    }

    private void reject(String destination, String reason, Long sessionUserId) {
        log.warn("Blocked STOMP subscription to {} — {} (session user: {})",
                destination, reason, sessionUserId);
        throw new MessagingException("Not authorised to subscribe to this destination");
    }

    private String firstNativeHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}
