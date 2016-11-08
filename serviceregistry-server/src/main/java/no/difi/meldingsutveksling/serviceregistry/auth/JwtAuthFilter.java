package no.difi.meldingsutveksling.serviceregistry.auth;

import no.difi.meldingsutveksling.serviceregistry.config.ServiceregistryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Component
public class JwtAuthFilter extends GenericFilterBean {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Autowired
    private ServiceregistryProperties props;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        if (!props.getAuth().isEnable()) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String token = request.getHeader("X-Access-Token");
        if (token != null && validateToken(token)) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            sendUnauthorizedResponse(response);
        }
    }

    private boolean validateToken(String token) {

        LinkedMultiValueMap<String, String> attrMap = new LinkedMultiValueMap<>();
        attrMap.add("token", token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(attrMap, headers);

        FormHttpMessageConverter formHttpMessageConverter = new FormHttpMessageConverter();
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(formHttpMessageConverter);

        URI oidcTokenUri;
        try {
            oidcTokenUri = props.getAuth().getOidcUrl().toURI();
        } catch (URISyntaxException e) {
            log.error("Error converting property to URI", e);
            throw new RuntimeException(e);
        }
        URI fullUri = UriComponentsBuilder.fromUri(oidcTokenUri)
                .pathSegment("idporten-oidc-provider/tokeninfo")
                .build().toUri();

        log.info("Fetching tokeninfo for token: {}", token);
        ResponseEntity<IdportenOidcTokenInfoResponse> responseEntity = restTemplate.exchange(fullUri, HttpMethod.POST,
                httpEntity, IdportenOidcTokenInfoResponse.class);
        log.info("Response: {}", responseEntity.toString());

        IdportenOidcTokenInfoResponse response = responseEntity.getBody();
        if (response.isActive()) {
            log.info("Token active.");
            return true;
        }

        log.info("Token inactive.");
        return false;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Access denied.");
    }

}
