package com.coachapp.exception;

public class InvitationAlreadyUsedException extends RuntimeException {
    public InvitationAlreadyUsedException() {
        super("Invitation has already been used");
    }
}
