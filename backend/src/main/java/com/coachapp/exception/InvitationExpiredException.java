package com.coachapp.exception;

public class InvitationExpiredException extends RuntimeException {
    public InvitationExpiredException() {
        super("Invitation has expired");
    }
}
