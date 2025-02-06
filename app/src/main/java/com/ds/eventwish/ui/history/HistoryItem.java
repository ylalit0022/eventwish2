package com.ds.eventwish.ui.history;

import com.ds.eventwish.data.model.SharedWish;
import com.ds.eventwish.data.model.Template;
import com.ds.eventwish.data.model.response.WishResponse;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class HistoryItem {
    private SharedWish wish;
    private Template templateId;
    private String shareTime;
    private String shortCode;
    private String recipientName;
    private String senderName;

    public HistoryItem(SharedWish wish) {
        this.wish = wish;
        this.templateId = wish.getTemplate();
        this.shortCode = wish.getShortCode();
        this.recipientName = wish.getRecipientName();
        this.senderName = wish.getSenderName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistoryItem that = (HistoryItem) o;
        return shortCode.equals(that.shortCode);
    }

    

    public Template getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Template templateId) {
        this.templateId = templateId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getShareTime() {
        return shareTime;
    }

    public void setShareTime(String shareTime) {
        this.shareTime = shareTime;
    }

}
