package com.ulak.domain_blocker_backend;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    public boolean isValidDomain(String domain) {

        String regex = "^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9](?:\\.[a-zA-Z]{2,})+$";
        return domain != null && domain.matches(regex);
    }

    public List<String> blockDomains(List<String> domainNames) {
        List<String> results = new ArrayList<>();

        for (String domainName : domainNames) {
            if (!isValidDomain(domainName)) {
                log.error("Geçersiz domain formatı reddedildi: {}", domainName);
                throw new IllegalArgumentException("Geçersiz domain formatı: " + domainName);
            }

            if (repository.existsByDomainName(domainName)) {
                throw new IllegalArgumentException("ERROR_ALREADY_EXISTS");
            }

            BlockedDomain blockedDomain = new BlockedDomain();
            blockedDomain.setDomainName(domainName);
            blockedDomain.setAppliedBy("ulak_admin");
            repository.save(blockedDomain);

            String command = "echo '" + password + "' | sudo -S sh -c \"echo '127.0.0.1 " + domainName + "' >> /etc/hosts\"";
            String sshResult = executeSshCommand(command, "Bloklama: " + domainName);
            if ("SUCCESS_SERVER_UPDATED".equals(sshResult)) {
                log.info("SSH İşlemi Başarılı: {} domaini uzak sunucudaki /etc/hosts dosyasına eklendi.", domainName);
            } else {
                log.error("SSH İşlemi Başarısız: {} eklenirken sunucuda hata oluştu. Detay: {}", domainName, sshResult);
            }
            results.add(domainName + " için işlem sonucu: " + sshResult);
        }
        return results;
    }

    @Transactional
    public String unblockDomain(String domain) {
        repository.deleteByDomainName(domain);

        String command = "echo '" + password + "' | sudo -S sed -i \"/" + domain + "/d\" /etc/hosts";
        String result = executeSshCommand(command, "Kaldırma: " + domain);

        if ("SUCCESS_SERVER_UPDATED".equals(result)) {
            log.info("SSH İşlemi Başarılı: {} domaini uzak sunucudaki /etc/hosts dosyasından temizlendi.", domain);
        } else {
            log.error("SSH İşlemi Başarısız: Sunucuda hata oluştu. Detay: {}", result);
        }

        return result;
    }

    public List<BlockedDomain> getAllBlockedDomains() {
        return repository.findAllByOrderByAppliedAtAsc();
    }

    private String executeSshCommand(String command, String context) {
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
            channel.connect();

            return checkStreamForErrors(channel, context);

        } catch (Exception e) {
            log.error("SSH Bağlantı veya Komut Hatası ({}): {}", context, e.getMessage());
            return "Hata: " + e.getMessage();
        } finally {

            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
        }
    }

    private String checkStreamForErrors(ChannelExec channel, String context) throws Exception {
        InputStream errStream = channel.getErrStream();
        while (!channel.isClosed()) {
            Thread.sleep(100);
        }

        if (errStream.available() > 0) {
            byte[] tmp = new byte[1024];
            int i = errStream.read(tmp, 0, 1024);
            String msg = new String(tmp, 0, i);
            if (!msg.contains("[sudo] password for")) {
                return "SERVER_ERROR:" + msg;
            }
        }
        return "SUCCESS_SERVER_UPDATED";
    }
}