package com.applicationtracker.dto;

public record ReauthRequiredResponse(
    String error,
    String message,
    String authUrl
) {}
