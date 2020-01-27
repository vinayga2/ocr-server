package dynamic.groovy

import com.optum.ocr.bean.LoginHistory
import com.optum.ocr.service.SecureService
import com.optum.ocr.util.DBMasterUtil
import com.optum.ocr.util.Messages

import java.time.LocalDate

class GSecureService extends SecureService {
    @Override
    List<LoginHistory> getAllRegistered() throws IllegalAccessException, IOException, InstantiationException {
        List<LoginHistory> lst = DBMasterUtil.findAllRecord("select a from LoginHistory a", 2000);
        return lst;
    }

    @Override
    String register(String msId) throws IllegalAccessException, IOException, InstantiationException {
        DBMasterUtil.executeStatement("delete from LoginHistory where msId='$msId'");
        LoginHistory loginHistory = LoginHistory.builder().msId(msId).lastLogin(LocalDate.now()).build();
        DBMasterUtil.saveRecord(loginHistory);
        return "Ok";
    }

    @Override
    String addLoginHistory(String msId) throws IllegalAccessException, IOException, InstantiationException {
        String retVal = null;
        LoginHistory history = DBMasterUtil.findFirstRecord("select a from LoginHistory a where a.msId='$msId' order by a.lastLogin desc");
        if (history==null || history.LoginHistoryId<1) {
            retVal = Messages.getMessage("secure.notRegistered");
//            retVal = "Your account is not yet registered in the system. Please contact your administrator.";
        }
        else {
            LocalDate last90Days = LocalDate.now().minusDays(90);
            LocalDate last180Days = LocalDate.now().minusDays(180);

            if (history.lastLogin.isBefore(last180Days)) {
                retVal = "User not authorized, please contact your administrator";
            }
            else if (history.lastLogin.isBefore(last90Days)) {
                retVal = "User is not logged in for 90 days, please contact your administrator.";
            }
            else {
                DBMasterUtil.executeStatement("delete from LoginHistory where msId='$msId'");
                LoginHistory loginHistory = LoginHistory.builder().msId(msId).lastLogin(LocalDate.now()).build();
                DBMasterUtil.saveRecord(loginHistory);
                retVal = "Ok";
            }
        }

        return retVal;
    }

    @Override
    byte[] downloadInactiveAccount() throws IllegalAccessException, IOException, InstantiationException {
        LocalDate last90Days = LocalDate.now().minusDays(90);
        String sql = "select a from LoginHistory a where a.lastLogin < '${last90Days.dateString}'";
        List<LoginHistory> lst = (List<LoginHistory>) DBMasterUtil.findAllRecord(sql, 2000);
        StringBuilder sb = new StringBuilder();
        sb.append("msId,lastLogin\n");
        for (LoginHistory loginHistory:lst) {
            sb.append(loginHistory.msId).append(",").append(loginHistory.lastLogin).append("\n");
        }
        return sb.toString().getBytes();
    }
}
