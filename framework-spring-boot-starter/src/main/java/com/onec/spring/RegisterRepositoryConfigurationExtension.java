package com.onec.spring;

import com.onec.repository.RegisterRepository;

import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;

import java.util.Collection;
import java.util.Collections;

public class RegisterRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

    @Override
    public String getModuleName() {
        return "OneC Register";
    }

    @Override
    protected String getModulePrefix() {
        return "register";
    }

    @Override
    public String getRepositoryFactoryBeanClassName() {
        return RegisterRepositoryFactoryBean.class.getName();
    }

    @Override
    protected Collection<Class<?>> getIdentifyingTypes() {
        return Collections.singleton(RegisterRepository.class);
    }
}
