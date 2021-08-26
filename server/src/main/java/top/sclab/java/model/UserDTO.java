package top.sclab.java.model;

import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class UserDTO implements Serializable {

    @Length(min = 6, max = 20, groups = {Validate.Login.class, Validate.Register.class})
    @NotBlank(groups = {Validate.Login.class, Validate.Register.class})
    private String username;

    @Length(min = 32, max = 32, groups = {Validate.Login.class, Validate.Register.class})
    @NotBlank(groups = {Validate.Login.class, Validate.Register.class})
    private String password;

    @Length(min = 1, max = 36, groups = Validate.Register.class)
    @NotBlank(groups = Validate.Register.class)
    private String nickname;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
