package com.applicationtracker.service;

import com.applicationtracker.dto.GmailMessageDto;
import com.google.api.client.http.HttpTransport;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GmailClientTest {

    @Mock
    private HttpTransport httpTransport;

    private GmailClient gmailClient;

    @BeforeEach
    void setUp() {
        gmailClient = new GmailClient(httpTransport);
    }

    @Test
    @DisplayName("Should parse headers and single-part plain text body")
    void testParseSinglePartPlainText() {
        String bodyText = "Thank you for applying to the Senior Software Engineer position at Stripe.";
        String base64Body = Base64.getUrlEncoder().encodeToString(bodyText.getBytes(StandardCharsets.UTF_8));

        MessagePart payload = new MessagePart()
                .setMimeType("text/plain")
                .setHeaders(List.of(
                        new MessagePartHeader().setName("Subject").setValue("Application Received - Stripe"),
                        new MessagePartHeader().setName("From").setValue("recruiting@stripe.com"),
                        new MessagePartHeader().setName("To").setValue("candidate@example.com")
                ))
                .setBody(new MessagePartBody().setData(base64Body));

        Message message = new Message()
                .setId("msg-stripe-1")
                .setThreadId("th-1")
                .setHistoryId(BigInteger.valueOf(12345))
                .setInternalDate(1710000000000L)
                .setSnippet("Thank you for applying...")
                .setPayload(payload);

        GmailMessageDto dto = gmailClient.parseMessage(message);

        assertThat(dto.id()).isEqualTo("msg-stripe-1");
        assertThat(dto.subject()).isEqualTo("Application Received - Stripe");
        assertThat(dto.from()).isEqualTo("recruiting@stripe.com");
        assertThat(dto.plainTextBody()).isEqualTo(bodyText);
        assertThat(dto.historyId()).isEqualTo(BigInteger.valueOf(12345));
    }

    @Test
    @DisplayName("Should extract plain text from multipart/alternative message structure")
    void testExtractPlainTextFromMultipart() {
        String plainText = "We are pleased to invite you for an interview.";
        String htmlText = "<html><body><p>We are pleased to invite you for an interview.</p></body></html>";

        MessagePart plainPart = new MessagePart()
                .setMimeType("text/plain")
                .setBody(new MessagePartBody().setData(Base64.getUrlEncoder().encodeToString(plainText.getBytes(StandardCharsets.UTF_8))));

        MessagePart htmlPart = new MessagePart()
                .setMimeType("text/html")
                .setBody(new MessagePartBody().setData(Base64.getUrlEncoder().encodeToString(htmlText.getBytes(StandardCharsets.UTF_8))));

        MessagePart multipartPayload = new MessagePart()
                .setMimeType("multipart/alternative")
                .setHeaders(List.of(
                        new MessagePartHeader().setName("Subject").setValue("Interview Invitation"),
                        new MessagePartHeader().setName("From").setValue("talent@google.com")
                ))
                .setParts(List.of(plainPart, htmlPart));

        Message message = new Message()
                .setId("msg-google-inv")
                .setPayload(multipartPayload);

        GmailMessageDto dto = gmailClient.parseMessage(message);

        assertThat(dto.subject()).isEqualTo("Interview Invitation");
        assertThat(dto.plainTextBody()).isEqualTo(plainText);
    }

    @Test
    @DisplayName("Should fallback to stripping HTML when only text/html part is available")
    void testFallbackToHtmlWhenNoPlainText() {
        String htmlText = "<p>Unfortunately, we will not be moving forward.</p>";
        MessagePart htmlPart = new MessagePart()
                .setMimeType("text/html")
                .setBody(new MessagePartBody().setData(Base64.getUrlEncoder().encodeToString(htmlText.getBytes(StandardCharsets.UTF_8))));

        MessagePart payload = new MessagePart()
                .setMimeType("multipart/mixed")
                .setParts(List.of(htmlPart));

        String extracted = gmailClient.extractPlainTextBody(payload);
        assertThat(extracted).contains("Unfortunately, we will not be moving forward.");
    }
}
