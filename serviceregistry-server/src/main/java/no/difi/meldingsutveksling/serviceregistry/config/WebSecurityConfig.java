package no.difi.meldingsutveksling.serviceregistry.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@SuppressWarnings("squid:S1118")
public class WebSecurityConfig {

    @Configuration
    @Order(0)
    public static class BasicAuthFilter extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/manage/**")
                    .authorizeRequests()
                    .antMatchers("/manage/health").permitAll()
                    .antMatchers("/manage/**").authenticated()
                    .and().httpBasic()
                    .and()
                    .antMatcher("/jwk")
                        .authorizeRequests()
                        .antMatchers("/jwk").permitAll();
        }
    }

    @Configuration
    @Order(1)
    public static class AdminApiSecurityConfiguration extends WebSecurityConfigurerAdapter {
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

}
