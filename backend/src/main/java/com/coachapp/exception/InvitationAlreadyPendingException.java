package com.coachapp.exception;

public class InvitationAlreadyPendingException extends RuntimeException {
    public InvitationAlreadyPendingException(String email) {
        super("A pending invitation already exists for: " + email);
    }
}
