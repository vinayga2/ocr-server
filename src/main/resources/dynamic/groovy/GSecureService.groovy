package dynamic.groovy

import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException
import com.optum.ocr.bean.LoginHistory
import com.optum.ocr.config.InitializerConfig
import com.optum.ocr.payload.SecureFileTypeEnum
import com.optum.ocr.service.SecureService
import com.optum.ocr.util.DBMasterUtil
import com.optum.ocr.util.Messages
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemManager
import org.apache.commons.vfs2.Selectors
import org.apache.commons.vfs2.VFS
import org.apache.tools.ant.filters.StringInputStream

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class GSecureService extends SecureService implements Runnable {
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
    byte[] createSecureFile(SecureFileTypeEnum fileType) throws IllegalAccessException, IOException, InstantiationException {
        byte[] retBytes = null;
        if (fileType.equals(SecureFileTypeEnum.FILE20)) {
            retBytes = createSecureFile20();
        }
        else if (fileType.equals(SecureFileTypeEnum.FILE21)) {
            retBytes = createSecureFile21();
        }
        else if (fileType.equals(SecureFileTypeEnum.FILE22)) {
            retBytes = createSecureFile22();
        }
        return retBytes;
    }

    byte[] createSecureFile22() {
        throw new RuntimeException("Secure File 22 not implemented.");
    }

    byte[] createSecureFile21() {
        throw new RuntimeException("Secure File 21 not implemented.");
    }

    byte[] createSecureFile20() {
        LocalDate last90Days = LocalDate.now().minusDays(90);
        String sql = "select a from LoginHistory a where a.lastLogin < '${last90Days.dateString}'";
        List<LoginHistory> lst = (List<LoginHistory>) DBMasterUtil.findAllRecord(sql, 2000);
        StringBuilder sb = new StringBuilder();
        sb.append("LastName,FirstName,UniqueIdentifier,RequestType,RoleName,UserId,AccountType,ResourceUserId,ResourceName\n");
        for (LoginHistory loginHistory:lst) {
            sb.append(",");                                 //LastName
            sb.append(",");                                 //FirstName
            sb.append(",");                                 //UniqueIdentifier
            sb.append("remove,");                           //RequestType
            sb.append(",");                                 //RoleName
            sb.append(loginHistory.msId).append(",");       //UserId
            sb.append(",");                                 //AccountType
            sb.append(",");                                 //ResourceUserId
            sb.append("\n");                                //ResourceName
        }
        return sb.toString().getBytes();
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

    public void createAndSendInactiveFile() throws IllegalAccessException, IOException, InstantiationException {
        String localFile = "${InitializerConfig.SecureFolder}/${InitializerConfig.SecureFile20}";
        String timeStr = LocalDateTime.now().format("YYYY-MM-DD_HH:MM");
        localFile = localFile.replaceAll("_DATETIME_", timeStr);

        byte[] bytes = createSecureFile20();

        FileWriter myWriter = new FileWriter(localFile);
        myWriter.write(new String(bytes));
        myWriter.close();

        sendThruJSch(localFile, InitializerConfig.SecureEcgFolder);
    }

    public void scheduleSendingInactiveToSecure() {
        String timeStr = LocalDateTime.now().format("YYYY-MM-DD_HH:mm");
        System.out.println("Start schedule at ${timeStr}");

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

//Change here for the hour you want ----------------------------------.at()
//        Long midnight = LocalDateTime.now().until(LocalDate.now().plusDays(1).atStartOfDay(), ChronoUnit.MINUTES);
//        scheduler.scheduleAtFixedRate(this, midnight, TimeUnit.DAYS.toMinutes(1), TimeUnit.MINUTES);

//        for testing
        scheduler.scheduleAtFixedRate(this, 1, TimeUnit.MINUTES.toMinutes(1), TimeUnit.MINUTES);
    }

    private void sendThruJSch(String src, String dest) throws JSchException {
        JSch jsch = new JSch();
        Session session = null;
        try {
            session = jsch.getSession(InitializerConfig.SecureEcgUsername, InitializerConfig.SecureEcgServer, 22);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(InitializerConfig.SecureEcgPassword);
            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            ChannelSftp sftpChannel = (ChannelSftp) channel;
            System.out.println("Sending File ${src} to ${InitializerConfig.SecureEcgServer} - ${dest}");
            sftpChannel.put(src, dest);
            sftpChannel.exit();

            session.disconnect();
            System.out.println("Disconnect");
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    void run() {
        String timeStr = LocalDateTime.now().format("YYYY-MM-DD_HH:mm");
        System.out.println("Running at ${timeStr}");
        this.createAndSendInactiveFile();
    }
}
