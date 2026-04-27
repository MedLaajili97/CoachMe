package com.coachapp.exception;

public class InvitationNotFoundException extends RuntimeException {
    public InvitationNotFoundException() {
        super("Invitation not found");
    }
}
