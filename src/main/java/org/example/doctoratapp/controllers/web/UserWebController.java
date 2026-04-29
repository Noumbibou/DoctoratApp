package org.example.doctoratapp.controllers.web;

import org.example.doctoratapp.entities.User;
import org.example.doctoratapp.services.interfaces.IUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class UserWebController {

    private final IUserService userService;

    public UserWebController(IUserService userService) {
        this.userService = userService;
    }

    @GetMapping("/index")
    public String index(Model model) {
        List<User> users = userService.findAll();
        model.addAttribute("users", users);
        return "users/index";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userService.findAll();
        model.addAttribute("users", users);
        return "users/list";
    }
}
