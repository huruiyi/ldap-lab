package com.demo.oauth.service;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.demo.oauth.entity.PermissionGroup;
import com.demo.oauth.mapper.PermissionGroupMapper;
import org.springframework.stereotype.Service;

@Service
public class PermissionGroupService extends ServiceImpl<PermissionGroupMapper, PermissionGroup> {
}
