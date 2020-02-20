package dynamic.groovy

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
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

    public String createAndSendInactiveFile() throws IllegalAccessException, IOException, InstantiationException {
        String localFile = "${InitializerConfig.SecureFolder}/${InitializerConfig.SecureFile20}";
        String remoteFile = "sftp://${InitializerConfig.SecureEcgUsername}:${InitializerConfig.SecureEcgPassword}@${InitializerConfig.SecureEcgServer}${InitializerConfig.SecureEcgFolder}";

        byte[] bytes = createSecureFile20();

        FileWriter myWriter = new FileWriter(localFile);
        myWriter.write(new String(bytes));
        myWriter.close();

//        for jSch
//        ChannelSftp channelSftp = setupJsch();
//        channelSftp.connect();
//        String remoteDir = InitializerConfig.SecureEcgFolder;
//        channelSftp.put(localFile, remoteDir + InitializerConfig.SecureFile20);
//
//        channelSftp.exit();

//        for sshClient
        SSHClient sshClient = setupSshj();
        SFTPClient sftpClient = sshClient.newSFTPClient();

        sftpClient.put(localFile, InitializerConfig.SecureEcgFolder + InitializerConfig.SecureFile20);

        sftpClient.close();
        sshClient.disconnect();

//        for VFS - not working
//        FileSystemManager manager = VFS.getManager();
//        FileObject local = manager.resolveFile(localFile);
//        FileObject remote = manager.resolveFile(remoteFile);
//        remote.copyFrom(local, Selectors.SELECT_SELF);
//        local.close();
//        remote.close();
    }

    private SSHClient setupSshj() throws IOException {
        SSHClient client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        client.connect(InitializerConfig.SecureEcgServer);
        client.authPassword(InitializerConfig.SecureEcgUsername, InitializerConfig.SecureEcgPassword);
        return client;
    }

    private ChannelSftp setupJsch() throws JSchException {
        JSch jsch = new JSch();
        jsch.setKnownHosts("~/.ssh/known_hosts");
        Session jschSession = jsch.getSession(InitializerConfig.SecureEcgUsername, InitializerConfig.SecureEcgServer);
        jschSession.setPassword(InitializerConfig.SecureEcgPassword);
        jschSession.connect();
        return (ChannelSftp) jschSession.openChannel("sftp");
    }
}
