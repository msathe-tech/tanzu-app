package com.vmware.tanzu.demo.accounts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/")
public class RootController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RootController.class);

    private final AccountRepository accountRepository;

    @Value("${os.name}")
    private String osName;

    @Value("${java.runtime.name}")
    private String runtimeName;

    @Value("${java.runtime.version}")
    private String runtimeVersion;

    private PaymentService paymentService;

    private final Environment environment;

    public RootController(AccountRepository accountRepository, PaymentService paymentService, Environment environment) {
        this.accountRepository = accountRepository;
        this.paymentService = paymentService;
        this.environment = environment;
    }

    @GetMapping
    public String index(Model model) throws UnknownHostException {
        Iterable<Account> accounts = this.accountRepository.findTop50ByOrderByBalanceDesc();
        Payment[] payments = this.paymentService.getPayments();

        model.addAttribute("osName", osName);
        model.addAttribute("runtimeName", runtimeName);
        model.addAttribute("runtimeVersion", runtimeVersion);
        model.addAttribute("sslVersion", getOpenSslVersion());

        model.addAttribute("host", InetAddress.getLocalHost().getHostName());
        model.addAttribute("ip", InetAddress.getLocalHost().getHostAddress());
        model.addAttribute("port", environment.getProperty("local.server.port"));

        model.addAttribute("accounts", accounts);
        model.addAttribute("payments", payments);

        return "index";
    }

    private String getOpenSslVersion() {
        try {
            final Process proc = new ProcessBuilder("openssl", "version", "-a").start();
            if (proc.waitFor() != 0) {
                LOGGER.warn("Cannot find the OpenSSL version");
            } else {
                try (final BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                    final List<String> lines = in.lines().collect(Collectors.toUnmodifiableList());
                    if (!lines.isEmpty()) {
                        final StringBuilder buf = new StringBuilder(lines.get(0).trim());
                        if (lines.size() > 1 && !lines.get(1).contains("not available")) {
                            buf.append(" ").append(lines.get(1).trim());
                        }
                        return buf.toString();
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to get OpenSSL version", e);
            return null;
        } catch (InterruptedException ignore) {
        }
        LOGGER.warn("Failed to get OpenSSL version: no output");
        return null;
    }
}
