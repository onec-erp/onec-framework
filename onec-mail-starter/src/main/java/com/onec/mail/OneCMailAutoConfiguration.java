package com.onec.mail;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.jdbi.v3.core.Jdbi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mail.javamail.JavaMailSender;

import javax.sql.DataSource;
import java.util.List;

@AutoConfiguration(after = MailSenderAutoConfiguration.class)
@ConditionalOnClass(JavaMailSender.class)
@ConditionalOnProperty(prefix = "onec.mail", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(MailProperties.class)
public class OneCMailAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MailTemplateRegistry mailTemplateRegistry(ApplicationContext context, MailProperties properties) {
        MailTemplateRegistry registry = new MailTemplateRegistry();
        List<String> packages = properties.getBasePackages().isEmpty()
                ? AutoConfigurationPackages.get(context)
                : properties.getBasePackages();
        new MailScanner().scan(packages).forEach(registry::register);
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public MailRenderer mailRenderer(ResourceLoader resourceLoader, MailProperties properties) {
        return new MailRenderer(resourceLoader, properties);
    }

    @Bean
    @ConditionalOnBean(JavaMailSender.class)
    @ConditionalOnMissingBean(MailDispatcher.class)
    public SmtpMailDispatcher smtpMailDispatcher(JavaMailSender javaMailSender, MailProperties properties) {
        return new SmtpMailDispatcher(javaMailSender, properties);
    }

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean
    public MailOutbox mailOutbox(DataSource dataSource) {
        MailOutbox outbox = new MailOutbox(Jdbi.create(dataSource));
        outbox.initSchema();
        return outbox;
    }

    @Bean
    @ConditionalOnBean(MailDispatcher.class)
    @ConditionalOnMissingBean
    public MailService mailService(ObjectProvider<MailDispatcher> dispatcherProvider,
                                   MailTemplateRegistry templates,
                                   MailRenderer renderer,
                                   MailProperties properties,
                                   ObjectProvider<MailOutbox> outboxProvider,
                                   ObjectMapper objectMapper) {
        MailDispatcher dispatcher = selectDispatcher(dispatcherProvider, properties.getProvider());
        return new MailService(dispatcher, templates, renderer, properties, outboxProvider.getIfAvailable(), objectMapper);
    }

    @Bean
    @ConditionalOnBean({MailOutbox.class, MailDispatcher.class})
    @ConditionalOnMissingBean
    public MailOutboxRelay mailOutboxRelay(MailOutbox outbox,
                                           ObjectProvider<MailDispatcher> dispatcherProvider,
                                           ObjectMapper objectMapper,
                                           MailProperties properties) {
        return new MailOutboxRelay(outbox, selectDispatcher(dispatcherProvider, properties.getProvider()),
                objectMapper, properties);
    }

    private MailDispatcher selectDispatcher(ObjectProvider<MailDispatcher> provider, String name) {
        List<MailDispatcher> all = provider.stream().toList();
        if (all.isEmpty()) {
            throw new IllegalStateException("No MailDispatcher beans available");
        }
        if (name == null || name.isBlank()) {
            return all.get(0);
        }
        return all.stream()
                .filter(d -> name.equalsIgnoreCase(d.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No MailDispatcher named '" + name + "' (available: "
                                + all.stream().map(MailDispatcher::name).toList() + ")"));
    }
}
