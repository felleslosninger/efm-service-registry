package no.difi.meldingsutveksling.serviceregistry.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@SuppressWarnings("squid:S1118")
public class WebSecurityConfig {

    @Configuration
    @RequiredArgsConstructor
    @Order(0)
    public static class BasicAuthFilter extends WebSecurityConfigurerAdapter {

        private final SecurityProperties props;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and().csrf().disable();
            http.antMatcher("/manage/**")
                    .authorizeRequests()
                    .antMatchers("/manage/health").permitAll()
                    .antMatchers("/manage/**").authenticated()
                    .and().httpBasic();
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.inMemoryAuthentication().withUser(props.getUser().getName())
                    .password("{noop}"+props.getUser().getPassword()).roles();
        }

    }

    @Configuration
    @RequiredArgsConstructor
    @Order(1)
    public static class AdminApiSecurityConfiguration extends WebSecurityConfigurerAdapter {

        private final SecurityProperties props;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and().csrf().disable();
            http.headers().frameOptions().sameOrigin().and()
                    .antMatcher("/api/**")
                    .authorizeRequests()
                    .antMatchers("/api/**").authenticated().and()
                    .httpBasic();
        }

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.inMemoryAuthentication().withUser(props.getUser().getName())
                    .password("{noop}"+props.getUser().getPassword()).roles();
        }
    }

    @Configuration
    @Order(2)
    public static class H2AdminFilter extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.headers().frameOptions().sameOrigin().and()
                    .antMatcher("/h2-console/**")
                    .authorizeRequests()
                    .antMatchers("/h2-console/**").permitAll();
        }
    }

    @Configuration
    @Order(3)
    public static class JwkFilter extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and().csrf().disable();
            http.antMatcher("/jwk")
                    .authorizeRequests()
                    .antMatchers("/jwk").permitAll();
        }
    }

    @Configuration
    @Order(4)
    public static class OauthFilter extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and().csrf().disable();
            http.authorizeRequests().antMatchers("/**").authenticated()
                    .and().oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);
        }
    }

}
