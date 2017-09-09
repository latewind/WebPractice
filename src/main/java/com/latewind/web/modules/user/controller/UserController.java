package com.latewind.web.modules.user.controller;
import java.util.*;

import com.google.common.collect.Lists;
import com.latewind.annotation.*;
import com.latewind.web.modules.user.service.UserService;

@Controller
public class UserController {
	@Qualifier
	private UserService userService;
	
	@RequestMapping("/list")
	public String list(){
		userService.getUser("123");
		return "/index";
	}
}
