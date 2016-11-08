package no.difi.meldingsutveksling.serviceregistry.auth;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.common.base.MoreObjects;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class IdportenOidcTokenInfoResponse {

    private boolean active;
    private String tokenType;
    private int expiresIn;
    private int exp;
    private int iat;
    private String scope;
    private String clientId;
    private String clientOrgno;

    public IdportenOidcTokenInfoResponse() {
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public int getIat() {
        return iat;
    }

    public void setIat(int iat) {
        this.iat = iat;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientOrgno() {
        return clientOrgno;
    }

    public void setClientOrgno(String clientOrgno) {
        this.clientOrgno = clientOrgno;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("active", active)
                .add("tokenType", tokenType)
                .add("expiresIn", expiresIn)
                .add("exp", exp)
                .add("iat", iat)
                .add("scope", scope)
                .add("clientId", clientId)
                .add("clientOrgno", clientOrgno)
                .toString();
    }
}
