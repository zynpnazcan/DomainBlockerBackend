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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.ulak.domain_blocker_backend.model.SystemLog;
import com.ulak.domain_blocker_backend.repository.SystemLogRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
@Slf4j
public class DomainBlockService {

    private static final String DOMAIN_REGEX = "^[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9](?:\\.[a-zA-Z]{2,})+$";
    private static final String SSH_SUCCESS_FLAG = "SUCCESS_SERVER_UPDATED";
    private static final String MODULE_NAME = "DOMAIN-BLOCKER-MFE";
    private static final String DEFAULT_ADMIN = "ulak_admin";

    // Aksiyon Kodları
    private static final String ACTION_BLOCKED = "DOMAIN_BLOCKED";
    private static final String ACTION_UNBLOCKED = "DOMAIN_UNBLOCKED";

    // Log Seviyeleri
    private static final String LOG_LEVEL_INFO = "INFO";
    private static final String LOG_LEVEL_WARN = "WARN";
    private static final String LOG_LEVEL_ERROR = "ERROR";

    @Autowired
    private BlockedDomainRepository repository;

    @Autowired
    private SystemLogRepository systemLogRepository;

    @Value("${ssh.host}")
    private String host;

    @Value("${ssh.user}")
    private String user;

    @Value("${ssh.password}")
    private String password;

    public boolean isValidDomain(String domain) {
        return domain != null && domain.matches(DOMAIN_REGEX);
    }

    public List<String> blockDomains(List<String> domainNames) {
        List<String> results = new ArrayList<>();

        for (String domainName : domainNames) {
            if (!isValidDomain(domainName)) {
                log.error("Invalid domain format rejected: {}", domainName);
                throw new IllegalArgumentException("INVALID_DOMAIN_FORMAT");
            }

            if (repository.existsByDomainName(domainName)) {
                throw new IllegalArgumentException("DOMAIN_ALREADY_EXISTS");
            }

            BlockedDomain blockedDomain = new BlockedDomain();
            blockedDomain.setDomainName(domainName);
            blockedDomain.setAppliedBy(DEFAULT_ADMIN);
            repository.save(blockedDomain);

            String command = "echo '" + password + "' | sudo -S sh -c \"echo '127.0.0.1 " + domainName + "' >> /etc/hosts\"";
            String sshResult = executeSshCommand(command, "Block Action: " + domainName);

            if (SSH_SUCCESS_FLAG.equals(sshResult)) {
                log.info("SSH execution successful. Domain added to /etc/hosts: {}", domainName);

                saveSystemLog(domainName, ACTION_BLOCKED, LOG_LEVEL_INFO);
            } else {
                log.error("SSH execution failed while adding {}. Details: {}", domainName, sshResult);
            }
            results.add(domainName + " execution result: " + sshResult);
        }
        return results;
    }

    @Transactional
    public String unblockDomain(String domain) {
        repository.deleteByDomainName(domain);

        String command = "echo '" + password + "' | sudo -S sed -i \"/" + domain + "/d\" /etc/hosts";
        String result = executeSshCommand(command, "Unblock Action: " + domain);

        if (SSH_SUCCESS_FLAG.equals(result)) {
            log.info("SSH execution successful. Domain removed from /etc/hosts: {}", domain);
            saveSystemLog(domain, ACTION_UNBLOCKED, LOG_LEVEL_WARN);
        } else {
            log.error("SSH execution failed on server. Details: {}", result);
        }

        return result;
    }

    public Page<BlockedDomain> getAllBlockedDomains(int page, int size, String search) {
        // Sayfa numarası ve sayfa başına kaç kayıt düşeceğini belirleyen nesne
        Pageable pageable = PageRequest.of(page, size);
        // Eğer arama kutusu boşsa eski usül hepsini getir
        if (search == null || search.trim().isEmpty()) {
            return repository.findAll(pageable);
        }
        // Eğer kelime yazıldıysa, veritabanında filtrele ve sayfala
        return repository.findByDomainNameContainingIgnoreCase(search.trim(), pageable);

    }

    private void saveSystemLog(String domainName, String actionCode, String level) {
        SystemLog systemLog = new SystemLog();
        systemLog.setTimestamp(LocalDateTime.now());
        systemLog.setLevel(level);
        systemLog.setDomainName(domainName);
        systemLog.setActionCode(actionCode);
        systemLog.setModuleName(MODULE_NAME);
        systemLogRepository.save(systemLog);
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
            log.error("SSH Connection or Command Error ({}): {}", context, e.getMessage());
            return LOG_LEVEL_ERROR + ": " + e.getMessage();
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
                return "SERVER_ERROR: " + msg;
            }
        }
        return SSH_SUCCESS_FLAG;
    }
}