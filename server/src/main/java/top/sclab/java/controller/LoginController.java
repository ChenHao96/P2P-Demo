package top.sclab.java.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.sclab.java.model.UserDTO;
import top.sclab.java.model.Validate;

import java.util.UUID;

@RestController
@RequestMapping("/user")
public class LoginController extends AbstractController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @RequestMapping("/login")
    public String userLogin(@Validated(Validate.Login.class) UserDTO user
            , BindingResult bindingResult) {

        synchronized (user.getUsername().intern()) {

            String key = DigestUtils.md5DigestAsHex(user.getUsername().getBytes());
            String cacheKey = String.format("user_record_%s", key);
            BoundHashOperations<String, String, String> operations = redisTemplate.boundHashOps(cacheKey);
            if (user.getPassword().equals(operations.get("password"))) {

                String userToken = operations.get("token");
                if (StringUtils.isEmpty(userToken)) {
                    userToken = UUID.randomUUID().toString().replaceAll("-", "");
                    operations.put("token", userToken);
                }

                return userToken;
            }
        }

        return null;
    }

    @RequestMapping("/register")
    public boolean userRegister(@Validated(Validate.Register.class) UserDTO user
            , BindingResult bindingResult) {

        synchronized (user.getUsername().intern()) {

            String key = DigestUtils.md5DigestAsHex(user.getUsername().getBytes());
            String cacheKey = String.format("user_record_%s", key);
            if (Boolean.FALSE.equals(redisTemplate.hasKey(cacheKey))) {

                BoundHashOperations<String, String, String> operations = redisTemplate.boundHashOps(cacheKey);
                operations.put("password", user.getPassword());
                return true;
            }
        }

        return false;
    }
}
