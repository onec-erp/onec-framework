package com.onec.mail;

public record MailAttachment(String filename, String contentType, byte[] content) {
}
