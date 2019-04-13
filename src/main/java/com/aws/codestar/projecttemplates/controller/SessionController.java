package com.aws.codestar.projecttemplates.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SessionController {

	@Autowired
	RedisTemplate<String, Object> redisTemplate;
	
	@Autowired
	private AuthenticationManager authManager;
	/**
	 * 用户安全认证,创建用户的安全上下文
	 * 获得的参数mail sid 参数封装到UsernamePasswordAuthenticationToken 对象
	 * 此对象传给 AuthenticationManager 进行验证,验证成功后 得到一个完整的Authentication 实例
	 * 通过调用SecurityContextHolder.getContext().setAuthentication(...)，参数传递authentication对象，来建立安全上下文（security context）
	 * @param sid 
	 * @param mail
	 * @return 
	 */
	@RequestMapping(value = "/signin/facebook", method = RequestMethod.GET)
	public String signInWithFacebook(String sid,String mail) {
		UsernamePasswordAuthenticationToken authReq = new UsernamePasswordAuthenticationToken(mail, sid);
		Authentication auth = authManager.authenticate(authReq);
		SecurityContext sc = SecurityContextHolder.getContext();
		sc.setAuthentication(auth);	    
	    return "ok";
	}
	/**
	 * 查询所有keys 通过迭代添加到ArrayList里面.
	 * @return 把这个集合返回出去
	 * 指定管理员才具有的权限
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@RequestMapping(value = "/listAllSession", method = RequestMethod.GET)
	public String listAllSession() {
		
		Set<String> redisKeys = redisTemplate.keys("*");
		// Store the keys in a List
		List<String> keysList = new ArrayList<>();
		Iterator<String> it = redisKeys.iterator();
		while (it.hasNext()) {
		       String data = it.next();
		       keysList.add(data);
		}
		
		return keysList.toString();
	}
	/**
	 * 删除
	 * @return 
	 */
	@PreAuthorize("hasRole('ADMIN')")
	@RequestMapping(value = "/deleteAllSession", method = RequestMethod.GET)
	public String deleteAllSession() {
		
		Set<String> redisKeys = redisTemplate.keys("*");
		// Store the keys in a List
		List<String> keysList = new ArrayList<>();
		Iterator<String> it = redisKeys.iterator();
		while (it.hasNext()) {
		       String data = it.next();
		       keysList.add(data);
		}
		
		redisTemplate.delete(keysList);
		
		return "done";
	}
}
