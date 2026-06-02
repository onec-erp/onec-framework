package com.onec.ui;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves index.html at the application root so React Router (basename "/")
 * boots the SPA. Deep links are handled by {@link SpaResourceResolver}.
 */
@RestController
class SpaIndexController {

    private static final Resource INDEX = new ClassPathResource("static/ui/index.html");

    @GetMapping(value = {"/"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public Resource index() {
        return INDEX;
    }
}
