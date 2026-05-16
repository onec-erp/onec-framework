package com.onec.print;

public record PrintResult(byte[] content, String contentType, String filename) {
}
