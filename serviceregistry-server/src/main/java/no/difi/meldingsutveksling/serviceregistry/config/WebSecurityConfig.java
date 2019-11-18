package no.difi.meldingsutveksling.serviceregistry.config;

import lombok.RequiredArgsConstructor;
import no.difi.meldingsutveksling.serviceregistry.auth.TokenAuthenticationFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@SuppressWarnings("squid:S1118")
public class WebSecurityConfig {

    @Configuration
    @Order(0)
    public static class ActuatorFilter extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/manage/**")
                    .authorizeRequests()
                    .antMatchers("/manage/health").permitAll()
                    .antMatchers("/manage/**").authenticated()
                    .and().httpBasic();
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

    @Configuration
    @ConditionalOnBean(TokenAuthenticationFilter.class)
    @Order(3)
    public static class OIDCTokenFilter extends WebSecurityConfigurerAdapter {

        private final TokenAuthenticationFilter tokenAuthenticationFilter;

        public OIDCTokenFilter(TokenAuthenticationFilter tokenAuthenticationFilter) {
            this.tokenAuthenticationFilter = tokenAuthenticationFilter;
        }

        @Override
        protected void configure(final HttpSecurity http) throws Exception {
            http.addFilterBefore(tokenAuthenticationFilter, BasicAuthenticationFilter.class);
            http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and().csrf().disable()
            .authorizeRequests().anyRequest().authenticated();
        }
    }


}
