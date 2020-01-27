/*
 * DBUtil.java
 *
 * Created on Sep 8, 2007, 5:30:13 PM
 *
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.optum.ocr.util;

import com.optum.ocr.bean.AbstractIBean;
import com.optum.ocr.config.DBMasterConfig;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * @author Budoy Entokwa
 */
public class DBMasterUtil {
    public static void executeStatement(String... sqls) {
        Arrays.stream(sqls).forEach(sql -> {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(DBMasterConfig.getDataSource());
            jdbcTemplate.execute(sql);
        });
    }

    public static AbstractIBean saveRecord(AbstractIBean iBean) {
        boolean isNew = iBean.getId()==null;
        EntityManagerFactory managerFactory = DBMasterConfig.getEntityManager();
        EntityManager manager = managerFactory.createEntityManager();
        try {
            manager.getTransaction().begin();
            iBean = manager.merge(iBean);
            manager.getTransaction().commit();
        }
        finally {
            manager.close();
        }
        return iBean;
    }

    public static AbstractIBean findRecordById(Class objCls, String id) {
        EntityManagerFactory managerFactory = DBMasterConfig.getEntityManager();
        EntityManager manager = managerFactory.createEntityManager();
        AbstractIBean ibean = null;
        try {
            ibean = (AbstractIBean) manager.find(objCls, Long.parseLong(id));
        }
        finally {
            manager.close();
        }
        return ibean;
    }

    public static AbstractIBean findRecordById(AbstractIBean obj) {
        AbstractIBean ibean = findRecordById(obj.getClass(), obj.getId().toString());
        return ibean;
    }

    public static AbstractIBean findFirstRecord(String sql) {
        List<AbstractIBean> lst = findAllRecord(sql, 1);
        if (lst == null || lst.isEmpty()) {
            return null;
        }
        else {
            return lst.get(0);
        }
    }

    public static List findAllRecord(String sql, int limit) {
        EntityManagerFactory managerFactory = DBMasterConfig.getEntityManager();
        EntityManager manager = managerFactory.createEntityManager();
        List lst = null;
        try {
            lst = manager.createQuery(sql).setMaxResults(limit).getResultList();
        }
        finally {
            manager.close();
        }
        return lst;
    }
}
