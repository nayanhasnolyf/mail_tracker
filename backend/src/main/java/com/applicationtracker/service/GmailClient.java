package com.applicationtracker.service;

import com.applicationtracker.dto.GmailMessageDto;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.History;
import com.google.api.services.gmail.model.ListHistoryResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.Profile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * Service encapsulating Google API client interactions for reading Gmail messages and history.
 */
@Slf4j
@Service
public class GmailClient {

    private static final String APPLICATION_NAME = "Application Tracker";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final HttpTransport httpTransport;

    public GmailClient() {
        try {
            this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Failed to initialize Google NetHttpTransport", e);
        }
    }

    // Constructor for testing with mock transport
    public GmailClient(HttpTransport httpTransport) {
        this.httpTransport = httpTransport;
    }

    public Gmail createGmailService(String accessToken) {
        HttpRequestInitializer requestInitializer = request -> {
            request.getHeaders().setAuthorization("Bearer " + accessToken);
            request.setConnectTimeout(15000);
            request.setReadTimeout(15000);
        };

        return new Gmail.Builder(httpTransport, JSON_FACTORY, requestInitializer)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Lists messages matching the given query.
     */
    public List<Message> listMessages(String accessToken, String query, Long maxResults) throws IOException {
        Gmail gmail = createGmailService(accessToken);
        ListMessagesResponse response = gmail.users().messages().list("me")
                .setQ(query)
                .setMaxResults(maxResults != null ? maxResults : 100L)
                .execute();

        return response.getMessages() != null ? response.getMessages() : Collections.emptyList();
    }

    /**
     * Lists mailbox history starting from startHistoryId.
     */
    public List<History> listHistory(String accessToken, BigInteger startHistoryId, Long maxResults) throws IOException {
        Gmail gmail = createGmailService(accessToken);
        ListHistoryResponse response = gmail.users().history().list("me")
                .setStartHistoryId(startHistoryId)
                .setHistoryTypes(List.of("messageAdded"))
                .setMaxResults(maxResults != null ? maxResults : 100L)
                .execute();

        return response.getHistory() != null ? response.getHistory() : Collections.emptyList();
    }

    /**
     * Retrieves the user's Gmail profile (e.g. to get current mailbox historyId).
     */
    public Profile getUserProfile(String accessToken) throws IOException {
        Gmail gmail = createGmailService(accessToken);
        return gmail.users().getProfile("me").execute();
    }

    /**
     * Fetches a single message with full metadata and extracts plain-text body content.
     */
    public GmailMessageDto fetchMessage(String accessToken, String messageId) throws IOException {
        Gmail gmail = createGmailService(accessToken);
        Message message = gmail.users().messages().get("me", messageId)
                .setFormat("full")
                .execute();

        return parseMessage(message);
    }

    /**
     * Parses a raw Gmail Message into GmailMessageDto extracting headers and plain-text body.
     */
    public GmailMessageDto parseMessage(Message message) {
        String subject = null;
        String from = null;
        String to = null;

        if (message.getPayload() != null && message.getPayload().getHeaders() != null) {
            for (MessagePartHeader header : message.getPayload().getHeaders()) {
                if ("Subject".equalsIgnoreCase(header.getName())) {
                    subject = header.getValue();
                } else if ("From".equalsIgnoreCase(header.getName())) {
                    from = header.getValue();
                } else if ("To".equalsIgnoreCase(header.getName())) {
                    to = header.getValue();
                }
            }
        }

        String plainTextBody = extractPlainTextBody(message.getPayload());

        Instant internalDate = message.getInternalDate() != null
                ? Instant.ofEpochMilli(message.getInternalDate())
                : Instant.now();

        return new GmailMessageDto(
                message.getId(),
                message.getThreadId(),
                message.getHistoryId(),
                subject,
                from,
                to,
                message.getSnippet(),
                plainTextBody,
                internalDate
        );
    }

    /**
     * Recursively extracts plain text from a MessagePart.
     */
    public String extractPlainTextBody(MessagePart part) {
        if (part == null) {
            return "";
        }

        StringBuilder plainTextBuilder = new StringBuilder();
        StringBuilder htmlFallbackBuilder = new StringBuilder();

        extractTextRecursive(part, plainTextBuilder, htmlFallbackBuilder);

        if (!plainTextBuilder.isEmpty()) {
            return plainTextBuilder.toString().trim();
        }

        if (!htmlFallbackBuilder.isEmpty()) {
            // Convert simple HTML tags to plain text
            return stripHtmlTags(htmlFallbackBuilder.toString()).trim();
        }

        return "";
    }

    private void extractTextRecursive(MessagePart part, StringBuilder plainText, StringBuilder htmlFallback) {
        String mimeType = part.getMimeType() != null ? part.getMimeType().toLowerCase() : "";

        if (part.getBody() != null && part.getBody().getData() != null) {
            String decoded = decodeBase64Url(part.getBody().getData());
            if ("text/plain".equalsIgnoreCase(mimeType)) {
                plainText.append(decoded).append("\n");
            } else if ("text/html".equalsIgnoreCase(mimeType)) {
                htmlFallback.append(decoded).append("\n");
            }
        }

        if (part.getParts() != null) {
            for (MessagePart childPart : part.getParts()) {
                extractTextRecursive(childPart, plainText, htmlFallback);
            }
        }
    }

    private String decodeBase64Url(String base64UrlData) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(base64UrlData);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to Base64-URL decode message body part", e);
            return "";
        }
    }

    private String stripHtmlTags(String html) {
        if (html == null) return "";
        return html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("<[^>]*>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("\\s+", " ");
    }
}
