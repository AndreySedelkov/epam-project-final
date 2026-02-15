package com.epam.finaltask.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.*;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SecurityEventsListener
        implements ApplicationListener<AbstractAuthenticationEvent> {

    @Override
    public void onApplicationEvent(AbstractAuthenticationEvent event) {
        String username = event.getAuthentication().getName();

        if (event instanceof InteractiveAuthenticationSuccessEvent) {
            log.info("User '{}' logged in interactively", username);
        }
        else if (event instanceof AuthenticationSuccessEvent) {
            log.info("Authentication success for user '{}'", username);
        }
        else if (event instanceof AbstractAuthenticationFailureEvent) {
            String reason = ((AbstractAuthenticationFailureEvent) event)
                    .getException().getMessage();
            log.warn("Authentication failure for user '{}': {}", username, reason);
        }
    }
}
