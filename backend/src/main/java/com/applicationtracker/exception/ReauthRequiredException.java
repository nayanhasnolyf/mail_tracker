package com.applicationtracker.exception;

import lombok.Getter;

@Getter
public class ReauthRequiredException extends RuntimeException {

    private final String authUrl;

    public ReauthRequiredException(String message) {
        this(message, "/oauth2/authorization/google");
    }

    public ReauthRequiredException(String message, String authUrl) {
        super(message);
        this.authUrl = authUrl;
    }
}
