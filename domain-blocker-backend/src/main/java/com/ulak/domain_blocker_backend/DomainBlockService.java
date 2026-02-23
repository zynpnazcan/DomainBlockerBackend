package com.ulak.domain_blocker_backend;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
@Service
@Slf4j
public class DomainBlockService {

    @Autowired
    private BlockedDomainRepository repository;

    @Value("${ssh.host}")
    private String host;

    @Value("${ssh.user}")
    private String user;

    @Value("${ssh.password}")
    private String password;

    public List<String> blockDomains(List<String> domainNames) {
        List<String> results = new ArrayList<>();

        for (String domainName : domainNames) {
            if (repository.existsByDomainName(domainName)) {
                throw new IllegalArgumentException(domainName + " zaten engelli listesinde bulunuyor!");
            }

            BlockedDomain blockedDomain = new BlockedDomain();
            blockedDomain.setDomainName(domainName);
            blockedDomain.setAppliedBy("ulak_admin");
            repository.save(blockedDomain);

            String command = "echo '" + password + "' | sudo -S sh -c \"echo '127.0.0.1 " + domainName + "' >> /etc/hosts\"";
            executeSshCommand(command, "Bloklama: " + domainName);

            results.add(domainName + " başarıyla engellendi.");
        }
        return results;
    }

    @Transactional
    public void unblockDomain(String domain) {
        repository.deleteByDomainName(domain);
        log.info("Veritabanından silindi: {}", domain);
        String command = "echo '" + password + "' | sudo -S sed -i \"/" + domain + "/d\" /etc/hosts";
        executeSshCommand(command, "Kaldırma: " + domain);
    }

    public List<BlockedDomain> getAllBlockedDomains() {
        return repository.findAll();
    }

    private void executeSshCommand(String command, String context) {
        Session session = null;
        ChannelExec channel = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(5000);

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            InputStream errStream = channel.getErrStream();
            channel.connect();

            while (!channel.isClosed()) {
                Thread.sleep(100);
            }

            if (errStream.available() > 0) {
                byte[] tmp = new byte[1024];
                int i = errStream.read(tmp, 0, 1024);
                String errorMsg = new String(tmp, 0, i);
                if (!errorMsg.contains("[sudo] password for")) {
                    log.error("SSH Hatası ({}): {}", context, errorMsg);
                } else {
                    log.info("SSH İşlemi Başarılı (Yetki Onaylı): {}", context);
                }
            } else {
                log.info("SSH İşlemi Başarılı: {}", context);
            }
        } catch (Exception e) {
            log.error("Sistem Hatası: {}", e.getMessage());
        }
        finally {
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
        }
    }

}