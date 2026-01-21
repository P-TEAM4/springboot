package com.lol.highlight.global.auth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JWT 토큰에서 인증된 사용자 정보를 주입받기 위한 어노테이션.
 * 컨트롤러 메소드 파라미터에 사용하면 User 엔티티를 직접 주입받을 수 있습니다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthUser {
}
