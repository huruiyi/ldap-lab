package com.demo.oauth.service;

// MP 3.5.17 boot4 线：ServiceImpl 从 extension 迁到了 mybatis-plus-spring 模块
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.demo.oauth.entity.AppUser;
import com.demo.oauth.mapper.AppUserMapper;
import org.springframework.stereotype.Service;

@Service
public class AppUserService extends ServiceImpl<AppUserMapper, AppUser> {

  public AppUser findByUsername(String username) {
    return lambdaQuery().eq(AppUser::getUsername, username).one();
  }
}
