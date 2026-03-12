package com.example.dzipsa.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Tag(name = "Auth (Dev)", description = "개발용 로그인 페이지")
@Controller
public class DevLoginController {

    @GetMapping("/login")
    @ResponseBody
    @Operation(summary = "개발용 소셜 로그인 페이지", description = "백엔드 테스트를 위한 소셜 로그인 버튼이 있는 HTML 페이지를 반환합니다. 브라우저 주소창에 /login을 입력하여 접속하세요.")
    public String login() {
        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <title>Dzipsa 로그인 (개발용)</title>
                <style>
                    body {
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        height: 100vh;
                        flex-direction: column;
                        font-family: Arial, sans-serif;
                    }
                    h2 { margin-bottom: 20px; }
                    .btn {
                        display: block;
                        width: 200px;
                        padding: 10px;
                        margin: 10px;
                        text-align: center;
                        text-decoration: none;
                        border-radius: 5px;
                        color: white;
                        font-weight: bold;
                    }
                    .naver { background-color: #03C75A; }
                    .kakao { background-color: #FEE500; color: #000; }
                </style>
            </head>
            <body>
                <h2>Dzipsa 소셜 로그인</h2>
                <a href="/oauth2/authorization/naver" class="btn naver">네이버 로그인</a>
                <a href="/oauth2/authorization/kakao" class="btn kakao">카카오 로그인</a>
            </body>
            </html>
            """;
    }
}
