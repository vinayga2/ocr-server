package com.optum.ocr.config;

import com.optum.ocr.payload.LoginRequest;
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

    List<GrantedAuthority> authorities = new ArrayList<>();
    Map<String, String> userDetails = new HashMap<>();

    private static final Logger logger = Logger.getLogger(OptumLDAP.class);

    public boolean hasAuthority(LoginRequest user, String[] groups) throws Exception {
        boolean hasAuthority = false;
        LdapContext ldap = null;
        StringBuilder ldapDetails = new StringBuilder();
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
                String temp = attrs.get("givenname").toString();
                logger.info("givenname    : " + temp + temp.substring(temp.indexOf(":") + 1));
                ldapDetails.append(temp.substring(temp.indexOf(":") + 1).trim());
                temp = attrs.get("sn").toString();
                logger.info("sn         : " + temp.substring(temp.indexOf(":") + 1));
                ldapDetails.append(" ").append(temp.substring(temp.indexOf(":") + 1).trim());
                //temp = attrs.get("mail").toString();
                // logger.info("Email ID    : " + temp.substring(temp.indexOf(":")+1));
                logger.info("Ldap Details : " + ldapDetails.toString());

                if (attrs.get("mail") != null && !(attrs.get("mail").size() == 0)) {
                    String email = attrs.get("mail").toString().substring(attrs.get("mail").toString().indexOf(":") + 1).trim();
                    logger.info("Email ID    : " + email);
                    userDetails.put("email", email);
                }

                String employeeid = attrs.get("employeeid").toString().substring(attrs.get("employeeid").toString().indexOf(":") + 1).trim();
                logger.info("employeeid : " + employeeid);

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
                    if (!hasAuthority) {
                        hasAuthority = Arrays.stream(groups).anyMatch(group -> value.equalsIgnoreCase(group));
                    }
                    GrantedAuthority grantedAuthority = new LdapAuthority(value, value);
                    authorities.add(grantedAuthority);
                }
                userDetails.put("fullname", ldapDetails.toString());
                userDetails.put("employeeid", employeeid);
                userDetails.put("memberOf", groupsValues.toString());
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
        return hasAuthority;
    }

    public List<GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public Map<String, String> getUserDetails() {
        return userDetails;
    }
}
