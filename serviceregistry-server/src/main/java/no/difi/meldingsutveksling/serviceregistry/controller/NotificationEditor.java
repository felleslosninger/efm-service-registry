package no.difi.meldingsutveksling.serviceregistry.controller;

import no.difi.meldingsutveksling.Notification;

import java.beans.PropertyEditorSupport;

/**
 * Used to make query parameter case insensitive when converting it to NotificateionObligation
 */
class NotificationEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(String text) {
        setValue(Notification.valueOf(text.toUpperCase()));
    }
}
