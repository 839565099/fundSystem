package com.fund.service;

import com.fund.vo.LoginVO;
import javax.servlet.http.HttpServletRequest;

public interface EmailLoginService {

    boolean sendLoginCode(String email);

    LoginVO loginWithCode(String email, String code, HttpServletRequest request);
}
