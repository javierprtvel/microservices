package me.learning.microservices.PhotoAppApiAccountManagement.ui.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account-management")
public class AccountManagementController {

    @GetMapping("/status/check")
    public String status() {
        return "Working!";
    }
}
