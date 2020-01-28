package com.optum.ocr.config;

import com.optum.ocr.payload.LoginRequest;
import com.optum.ocr.payload.Profile;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.ldap.userdetails.LdapAuthority;
import org.springframework.stereotype.Component;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.*;

@Component
public class OptumLDAP {
    @Autowired
    OptumConfig config;

    private static final Logger logger = Logger.getLogger(OptumLDAP.class);

    public Profile userAuthority(LoginRequest user, String[] groups) throws Exception {
        Profile profile = new Profile();

        LdapContext ldap = null;
        StringBuilder groupsValues = new StringBuilder();
        try {
            String filter = config.searchFilter + user.getUsername() + "))";
            final SearchControls ctrls = new SearchControls();
            ctrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            String[] attrIDs = {"distinguishedName",
                    "sn",
                    "givenname",
                    "mail",
                    "telephonenumber",
                    "memberOf",
                    "employeeid"};
            ctrls.setReturningAttributes(attrIDs);

            final Hashtable env = new Hashtable();
            env.put(Context.INITIAL_CONTEXT_FACTORY, config.contextFactory);
            env.put(Context.PROVIDER_URL, config.host);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, user.getUsername() + "@" + config.domain);
            env.put(Context.SECURITY_CREDENTIALS, user.getPassword());

            ldap = new InitialLdapContext(env, null);
            NamingEnumeration<SearchResult> result = ldap.search(config.searchBase, filter, ctrls);
            if (result.hasMore()) {
                SearchResult rs = result.next();
                Attributes attrs = rs.getAttributes();
                String givenName = attrs.get("givenname").toString();
                profile.firstName = givenName.substring(givenName.indexOf(":") + 1).trim();
                logger.info("profile.firstName    : " + profile.firstName);

                String surName = attrs.get("sn").toString();
                profile.lastName = surName.substring(surName.indexOf(":") + 1).trim();
                logger.info("profile.lastName         : " + profile.lastName);

                if (attrs.get("mail") != null && !(attrs.get("mail").size() == 0)) {
                    profile.email = attrs.get("mail").toString().substring(attrs.get("mail").toString().indexOf(":") + 1).trim();
                }

                profile.employeeId = attrs.get("employeeid").toString().substring(attrs.get("employeeid").toString().indexOf(":") + 1).trim();
                logger.info("profile.employeeId : " + profile.employeeId);

                String memberValue = attrs.get("memberOf").toString();
                String mvalues = memberValue.substring(memberValue.indexOf(":") + 1);
                System.out.println("memberValue" + mvalues);
                String[] values = mvalues.split(",");

                for (int i = 0; i < values.length; i++) {
                    String value = values[i].substring(values[i].indexOf("=") + 1);
                    if (i == values.length - 1 && value.startsWith("pdut_ui")) {
                        groupsValues.append(value);
                    } else if (value.startsWith("pdut_ui")) {
                        groupsValues.append(value).append(",");
                    }
                    if (!profile.hasAuthority) {
                        profile.hasAuthority = Arrays.stream(groups).anyMatch(group -> value.equalsIgnoreCase(group));
                    }
                    GrantedAuthority grantedAuthority = new LdapAuthority(value, value);
                    profile.authorities.add(grantedAuthority);
                }
                profile.fullName = profile.firstName+" "+profile.lastName;
                profile.memberOf = groupsValues.toString();
            }
        } catch (final AuthenticationException ex) {
            ex.printStackTrace();
            throw new AuthenticationException("Error in validating ldap: " + ex.getMessage());
        } catch (final NamingException ex) {
            ex.printStackTrace();
            throw new AuthenticationException("Error in validating ldap: " + ex.getMessage());
        } catch (final Exception e) {
            e.printStackTrace();
            throw new Exception("Error in ActiveDirectory.authenticate(): " + e.getMessage());
        } finally {
            try {
                ldap.close();
            } catch (final Exception e) {
                e.printStackTrace();
                logger.error("Exception occurred:" + e.getMessage());
            }
        }
        return profile;
    }
}
