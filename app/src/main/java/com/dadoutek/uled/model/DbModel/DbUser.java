package com.dadoutek.uled.model.DbModel;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

/**
 * Created by hejiajun on 2018/5/16.
 */

@Entity
public class DbUser {
    @Id(autoincrement = true)
    private Long id;
    //头像
    private String avatar;
    //渠道id
    private String channel="dadou";
    //用户邮箱
    private String email;
    //用户名
    private String name;
    //用户账号
    private String account;
    //用户手机号
    private String phone;
    //认证
    private String token;
    //密码
    private String password;
    //区域id
    private String last_region_id;
    @Generated(hash = 1735856997)
    public DbUser(Long id, String avatar, String channel, String email, String name,
                  String account, String phone, String token, String password,
                  String last_region_id) {
        this.id = id;
        this.avatar = avatar;
        this.channel = channel;
        this.email = email;
        this.name = name;
        this.account = account;
        this.phone = phone;
        this.token = token;
        this.password = password;
        this.last_region_id = last_region_id;
    }
    @Generated(hash = 762027100)
    public DbUser() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getAvatar() {
        return this.avatar;
    }
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
    public String getChannel() {
        return this.channel;
    }
    public void setChannel(String channel) {
        this.channel = channel;
    }
    public String getEmail() {
        return this.email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getAccount() {
        return this.account;
    }
    public void setAccount(String account) {
        this.account = account;
    }
    public String getPhone() {
        return this.phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
    public String getToken() {
        return this.token;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public String getPassword() {
        return this.password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getLast_region_id() {
        return this.last_region_id;
    }
    public void setLast_region_id(String last_region_id) {
        this.last_region_id = last_region_id;
    }

    @Override
    public String toString() {
        return "DbUser{" +
                "id=" + id +
                ", avatar='" + avatar + '\'' +
                ", channel='" + channel + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", account='" + account + '\'' +
                ", phone='" + phone + '\'' +
                ", token='" + token + '\'' +
                ", password='" + password + '\'' +
                ", last_region_id='" + last_region_id + '\'' +
                '}';
    }
}
