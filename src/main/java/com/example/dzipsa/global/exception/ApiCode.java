package com.example.dzipsa.global.exception;

public interface ApiCode {

    Integer getHttpStatus();

    Integer getCode();

    String getMessage();
}
