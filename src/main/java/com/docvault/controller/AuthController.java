package com.docvault.controller;

import com.docvault.dto.AuthDto;
import com.docvault.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new AuthDto.RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") AuthDto.RegisterRequest request,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.register(request);
            redirectAttributes.addFlashAttribute("success", "Account created! Please log in.");
            return "redirect:/auth/login";
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        }
    }

    @PostMapping("/delete-account")
    @PreAuthorize("isAuthenticated()")
    public String deleteAccount(@RequestParam String password,
                                Principal principal,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                RedirectAttributes redirectAttributes) {
        try {
            userService.deleteAccount(principal.getName(), password);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            new CookieClearingLogoutHandler("JSESSIONID", "remember-me").logout(request, response, authentication);
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            redirectAttributes.addFlashAttribute("accountDeleted", "Your account has been deleted.");
            return "redirect:/";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("deleteAccountError", ex.getMessage());
            return "redirect:/dashboard";
        }
    }
}
