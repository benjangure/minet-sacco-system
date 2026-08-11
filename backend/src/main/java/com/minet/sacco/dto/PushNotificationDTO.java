package com.minet.sacco.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for Push Notification Payload
 * Represents the notification data to be sent to users
 */
public class PushNotificationDTO {

    private String title;
    private String body;
    private String icon;
    private String badge;
    private String tag;
    
    @JsonProperty("requireInteraction")
    private Boolean requireInteraction;
    
    private Boolean silent;
    private String url;
    
    @JsonProperty("clickAction")
    private String clickAction;
    
    private String type;
    private Map<String, Object> data;
    private List<NotificationAction> actions;

    public PushNotificationDTO() {
    }

    public PushNotificationDTO(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public PushNotificationDTO(String title, String body, String type, String url) {
        this.title = title;
        this.body = body;
        this.type = type;
        this.url = url;
        this.clickAction = url;
    }

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getBadge() {
        return badge;
    }

    public void setBadge(String badge) {
        this.badge = badge;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Boolean getRequireInteraction() {
        return requireInteraction;
    }

    public void setRequireInteraction(Boolean requireInteraction) {
        this.requireInteraction = requireInteraction;
    }

    public Boolean getSilent() {
        return silent;
    }

    public void setSilent(Boolean silent) {
        this.silent = silent;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        this.clickAction = url;
    }

    public String getClickAction() {
        return clickAction;
    }

    public void setClickAction(String clickAction) {
        this.clickAction = clickAction;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public List<NotificationAction> getActions() {
        return actions;
    }

    public void setActions(List<NotificationAction> actions) {
        this.actions = actions;
    }

    /**
     * Inner class representing notification action buttons
     */
    public static class NotificationAction {
        private String action;
        private String title;
        private String icon;

        public NotificationAction() {
        }

        public NotificationAction(String action, String title) {
            this.action = action;
            this.title = title;
        }

        public NotificationAction(String action, String title, String icon) {
            this.action = action;
            this.title = title;
            this.icon = icon;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon;
        }

        @Override
        public String toString() {
            return "NotificationAction{" +
                    "action='" + action + '\'' +
                    ", title='" + title + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "PushNotificationDTO{" +
                "title='" + title + '\'' +
                ", body='" + body + '\'' +
                ", type='" + type + '\'' +
                ", url='" + url + '\'' +
                '}';
    }

    /**
     * Builder class for convenient notification construction
     */
    public static class Builder {
        private final PushNotificationDTO notification;

        public Builder(String title, String body) {
            notification = new PushNotificationDTO(title, body);
        }

        public Builder icon(String icon) {
            notification.setIcon(icon);
            return this;
        }

        public Builder badge(String badge) {
            notification.setBadge(badge);
            return this;
        }

        public Builder tag(String tag) {
            notification.setTag(tag);
            return this;
        }

        public Builder requireInteraction(boolean requireInteraction) {
            notification.setRequireInteraction(requireInteraction);
            return this;
        }

        public Builder silent(boolean silent) {
            notification.setSilent(silent);
            return this;
        }

        public Builder url(String url) {
            notification.setUrl(url);
            return this;
        }

        public Builder type(String type) {
            notification.setType(type);
            return this;
        }

        public Builder data(Map<String, Object> data) {
            notification.setData(data);
            return this;
        }

        public Builder actions(List<NotificationAction> actions) {
            notification.setActions(actions);
            return this;
        }

        public PushNotificationDTO build() {
            return notification;
        }
    }
}
