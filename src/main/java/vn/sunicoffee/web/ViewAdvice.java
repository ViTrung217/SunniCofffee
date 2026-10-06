package vn.sunicoffee.web;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class ViewAdvice {
    private final Environment environment;
    public ViewAdvice(Environment e) { environment=e; }
    @ModelAttribute("demoMode") public boolean demoMode() { return environment.matchesProfiles("demo"); }
}
