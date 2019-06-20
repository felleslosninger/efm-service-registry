package no.difi.meldingsutveksling.serviceregistry.auth;

import lombok.Data;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.List;

@Data
public class OpenIdConnectAuthenticationToken extends UsernamePasswordAuthenticationToken {

    public OpenIdConnectAuthenticationToken(Object principal, Object credentials, List<String> scopes) {
        super(principal, credentials, generateAuthorities(scopes));
    }

    public OpenIdConnectAuthenticationToken(Object principal, List<String> scopes) {
        this(principal, "n/a", scopes);
    }

    private static List<GrantedAuthority> generateAuthorities(List<String> scopes) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        scopes.forEach(scope -> authorities.add(new SimpleGrantedAuthority(scope)));
        return authorities;
    }
}
