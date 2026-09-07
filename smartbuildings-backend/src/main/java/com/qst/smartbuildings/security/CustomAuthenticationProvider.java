package com.qst.smartbuildings.security;

import com.qst.smartbuildings.common.AuthorizationCodeEnum;
import com.qst.smartbuildings.entity.AdminUser;
import com.qst.smartbuildings.exception.AuthException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationProvider extends AbstractUserDetailsAuthenticationProvider {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {
        AdminUser user = null;
        if (userDetails == null) {
            logger.debug("Authentication failed: 用户不存在");
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_ERR);
        }
        if (userDetails.getUsername() == null) {
            logger.debug("Authentication failed: 未提供用户名");
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_USERNAME_REQUIRED);
        }
        if (!(userDetails instanceof AdminUser)) {
            logger.debug("Authentication failed: 用户类型错误");
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_ERR);
        }
        user = (AdminUser) userDetails;
        String presentedPassword = authentication.getCredentials().toString();
        if (passwordEncoder.matches(presentedPassword, user.getPassword())) {
            authentication.setDetails(user);
        } else {
            logger.debug("Authentication failed: 密码不匹配");
            throw new BadCredentialsException("密码不正确");
        }
    }

    @Override
    protected UserDetails retrieveUser(String username,
                                       UsernamePasswordAuthenticationToken authentication)
            throws AuthenticationException {
        try {
            UserDetails loadedUser = userDetailsService.loadUserByUsername(username);
            if (loadedUser == null) {
                throw new InternalAuthenticationServiceException(
                        "UserDetailsService返回null，违反接口约定");
            }
            return loadedUser;
        } catch (UsernameNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex);
        }
    }
}