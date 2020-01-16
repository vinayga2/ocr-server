package com.optum.ocr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OptumConfig {
    @Value("${ldap.host}")
    public String host = "ldap://ad-ldap-prod.uhc.com";

    @Value("${ldap.port}")
    public int port = 389;

    @Value("${ldap.domain}")
    public String domain = "ms";

    @Value("${ldap.searchBase}")
    public String searchBase = "dc=ms,dc=ds,dc=uhc,dc=com";

    @Value("${ldap.searchFilter}")
    public String searchFilter = "(&(objectClass=person)(cn=";

    @Value("${ldap.contextFactory}")
    public String contextFactory = "com.sun.jndi.ldap.LdapCtxFactory";
}
