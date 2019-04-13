package com.aws.codestar.projecttemplates.configuration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.http.client.ClientProtocolException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
@Import(ApiService.class)
public class SecurityConfig extends WebSecurityConfigurerAdapter {
	
	@Autowired
	ApiService apiService;
	
	 @Bean(name = BeanIds.AUTHENTICATION_MANAGER)
	   @Override
	   public AuthenticationManager authenticationManagerBean() throws Exception {
	       return super.authenticationManagerBean();
	   }	
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
		.cors().disable()
		.csrf().disable()		
			.authorizeRequests()
			.antMatchers("/").permitAll()		
			.antMatchers("/login").permitAll()
			//.antMatchers("/Personalpage").permitAll()
			.antMatchers("/originLogin").permitAll()
			.antMatchers("/test").hasRole("USER")
		.and()
		
			.formLogin()
			 .loginPage("/login")
			 .loginProcessingUrl("/originLogin")
			 .successHandler(new SavedRequestAwareAuthenticationSuccessHandler () {
				 /*
				  when login success 
				  	1.save user info to session 
				  	2.redirect to prior_url
				  */ 
				@Override
				public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
						Authentication authentication) throws IOException, ServletException {

					
					HttpSession session = request.getSession();
					// not good
					session.setAttribute("me", "redan");					
				
					 if (session != null) {
				            String redirectUrl = (String) session.getAttribute("url_prior_login");
				            if (redirectUrl != null) {
				                session.removeAttribute("url_prior_login");
				                getRedirectStrategy().sendRedirect(request, response, redirectUrl);
				            } else {
				                super.onAuthenticationSuccess(request, response, authentication);
				            }
				        } 
				}
				 
			 })
			 .failureForwardUrl("/registerview");
			 
//			 .defaultSuccessUrl("/", true);

	}
	/*
	 @param userId example:	 
	 	FACEBOOK --- F12345678
	    Line	 --- L12345678
	    GOOGLE	 --- G12345678
	 */
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {		
		auth.authenticationProvider(new AuthenticationProvider() {

			@Override
			public Authentication authenticate(Authentication authentication) throws AuthenticationException {				
				String email = authentication.getName();
				Object credentials = authentication.getCredentials();
				String userId = credentials == null ? "" : credentials.toString();
				try {
					if(apiService.findOneByEmail(email)==null) {						
						throw new BadCredentialsException("Authentication failed");
					}
					
					String flag = userId.substring(0, 1);
					String thirtPartyName = null;
					
					 switch(flag) {
			            case "F": 
			            	thirtPartyName = "facebook";    
			                break; 
			            case "L": 
			            	thirtPartyName = "google";    
			                break; 
			            case "G": 
			            	thirtPartyName = "line";    
			                break; 
			            default: 
			            	System.out.println("flag error");
			            	throw new BadCredentialsException("Authentication failed");
			                 
			        }										
					if(apiService.findOneByThirdPartyId(thirtPartyName, userId) != null) {
						return new UsernamePasswordAuthenticationToken(email, userId, Collections.emptyList());
					}
					
					throw new BadCredentialsException("Authentication failed");
					
				} catch (ClientProtocolException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				throw new BadCredentialsException("Authentication failed");
			}

			@Override
			public boolean supports(Class<?> clazz) {
				return clazz.equals(UsernamePasswordAuthenticationToken.class);
			}
			
		});
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
}
