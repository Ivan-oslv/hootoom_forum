package com.hootoom.forum.auth.exception;

import com.hootoom.forum.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** 独立异常类型用于确保重放检测的撤销操作提交后再返回 401。 */
public class TokenReplayException extends BusinessException {
    public TokenReplayException() {
        super("TOKEN_REPLAYED", "登录状态已失效，请重新登录", HttpStatus.UNAUTHORIZED);
    }
}
