package com.docvault.controller;

import com.docvault.repository.DocumentRepository;
import com.docvault.repository.UserRepository;
import com.docvault.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @GetMapping("/")
    public String root(Model model) {
        long totalFiles = documentRepository.count();
        long totalStorageBytes = documentRepository.sumFileSize();

        model.addAttribute("totalFiles", totalFiles);
        model.addAttribute("storageUsed", DocumentService.formatSize(totalStorageBytes));
        model.addAttribute("totalUsers", userRepository.count());
        return "home";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}
