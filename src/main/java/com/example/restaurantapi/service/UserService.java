package com.example.restaurantapi.service;

import com.example.restaurantapi.dto.user.LoggeduserDto;
import com.example.restaurantapi.dto.user.LoginUserDto;
import com.example.restaurantapi.dto.user.RegisterUserDto;
import com.example.restaurantapi.dto.user.ResendMailDto;
import com.example.restaurantapi.model.ConfirmationToken;
import com.example.restaurantapi.model.User;
import com.example.restaurantapi.repository.ConfirmationTokenRepository;
import com.example.restaurantapi.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.rowset.serial.SerialClob;
import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @Author Szymon Królik
 */
@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {



    private final UserRepository userRepository;
    private final ValidationService validationService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final ConfirmationTokenService confirmationTokenService;
    private final ConfirmationTokenRepository confirmationTokenRepository;
    private final EmailService emailService;
    private List<String> validationResult = new ArrayList<>();


    /**
     * Save user in database
     * @author Szymon Królik
     * @param registerUserDto
     * @return createdUser
     */
    public ServiceReturn createUser(RegisterUserDto registerUserDto) {
        ServiceReturn ret = new ServiceReturn();
        validationResult.clear();
        Optional<User> optionalUser = userRepository.findByEmail(registerUserDto.getEmail());
        if (optionalUser.isEmpty()) {
            validationResult = validationService.registerValidation(registerUserDto);
            if (validationResult.size() > 0) {
                ret.setStatus(-1);
                ret.setErrorList(validationResult);
                ret.setValue(registerUserDto);

                return ret;
            } else {
                User user = User.of(registerUserDto);
                final String encryptedPassword = bCryptPasswordEncoder.encode(user.getPassword());
                user.setPassword(encryptedPassword);
                final User createdUser = userRepository.save(user);
                final ConfirmationToken confirmationToken = new ConfirmationToken(createdUser);
                confirmationTokenService.saveConfirmationToken(confirmationToken);

//                sendConfirmationToken(user.getEmail(), confirmationToken.getConfirmationToken());

                ret.setStatus(1);
                ret.setErrorList(null);
                ret.setValue((Object) createdUser);
            }
        } else {

            ret.setStatus(-1);
            validationResult.add("Email został już wykorzystany");
            ret.setErrorList(validationResult);
            return ret;
        }

        return ret;

    }

    /**
     * Login user
     * @author Szymon Królik
     * @param loginUserDto
     * @return user
     */
    public ServiceReturn loginUser(LoginUserDto loginUserDto) {
        ServiceReturn ret = new ServiceReturn();
        String password = "";
        String login = "";
        Optional<User> userOptional = Optional.empty();;
        validationResult.clear();

        if (!ServiceFunction.isNull(loginUserDto.getPassword())) {
            password = loginUserDto.getPassword();
        } else {
            ret.setStatus(0);
            validationResult.add("Proszę uzupełnić hasło");
        }

        if (!ServiceFunction.isNull(loginUserDto.getLogin())) {
            login = loginUserDto.getLogin();
        } else {
            ret.setStatus(0);
            validationResult.add("Proszę uzupełnić login");
        }

        if (validationResult.size() > 0) {
            ret.setStatus(0);
            ret.setErrorList(validationResult);
            ret.setValue((Object) loginUserDto);
            return ret;
        }


        Object[] userExist = userExist(loginUserDto);
        if (!(Boolean)userExist[0]) {
            ret.setStatus(0);
            ret.setMessage("Nie znaleziono takiego użytkownika");
            return ret;
        }

        userOptional =(Optional<User>) userExist[1];
        User user = userOptional.get();
//        if (!ServiceFunction.enableUser(user)) {
//            ret.setStatus(0);
//            ret.setMessage("Proszę najpierw aktywować swoje konto");
//            return ret;
//        }

        if (bCryptPasswordEncoder.matches(password,user.getPassword())) {
            LoggeduserDto dto = LoggeduserDto.of(user);
            ret.setStatus(1);
            ret.setValue(dto);
            return ret;
        } else {
            ret.setStatus(0);
            loginUserDto.setPassword(null);
            ret.setValue(loginUserDto);
            ret.setMessage("Proszę wprowadzić poprawne hasło");
        }



        return ret;


    }

    /**
     * Resend register mail confirmation
     * @author Szymon Królik
     * @param dto
     * @return only status
     */
    public ServiceReturn resendMail(ResendMailDto dto) {
        ServiceReturn ret = new ServiceReturn();
        ret = loginUser(LoginUserDto.of(dto));

        if (ret.getStatus() == 1) {
            User user = new User();
            user = (User) ret.getValue();
            final ConfirmationToken confirmationToken = new ConfirmationToken(user);
            confirmationTokenService.saveConfirmationToken(confirmationToken);

            sendConfirmationToken(user.getEmail(), confirmationToken.getConfirmationToken());
            ret.setStatus(1);
            return ret;
        }

        return ret;

    }

    /**
     * Send email with link to reset user password
     * @author Szymon Królik
     * @param email
     * @return
     */
    public ServiceReturn forgotPassword(String email) {
        ServiceReturn ret = new ServiceReturn();
        String userEmail = "";
        if (!ServiceFunction.isNull(email)) {
            userEmail = email;
        } else {
            ret.setStatus(-1);
            ret.setMessage("Proszę wprowadzić email");
            return ret;
        }

        Optional<User> optionalUser = userRepository.findByEmail(userEmail);
        if (optionalUser.isEmpty()) {
            ret.setStatus(-1);
            ret.setMessage("Nie znaleziono użytkownika o podanym email");
            return ret;
        }

        User user = optionalUser.get();
        final ConfirmationToken forgotPasswordToken = new ConfirmationToken(user);
        confirmationTokenService.saveConfirmationToken(forgotPasswordToken);
        sendForgotPasswordToken(user.getEmail(), forgotPasswordToken.getConfirmationToken());

        ret.setStatus(1);
        ret.setMessage("Email do zresetowania hasła został wysłany");
        return ret;


    }


    /*
    ret[0]: True = exist, False = not exist
    ret[1]: Optional User object
     */
    public Object[] userExist(LoginUserDto dto) {
        Object[] ret = new Object[2];
        User user = new User();
        Optional<User> userOptional = Optional.empty();
        userOptional = userRepository.findByEmail(dto.getLogin());
        if (userOptional.isEmpty()) {
            userOptional = userRepository.findByPhoneNumber(dto.getLogin());
            if (userOptional.isPresent()) {
                ret[0] = (Object)true;
                ret[1] = (Object)userOptional;
            } else {
                ret[0] = (Object)false;
                ret[1] = (Object)null;
            }
        } else {
            ret[0] = (Object)true;
            ret[1] = (Object)userOptional;
        }
        return ret;
    }


    /**
     * Send register confirmation token
     * @author Szymon Królik
     * @param userMail
     * @param token
     */
    public void sendConfirmationToken(String userMail, String token) {
        final SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        String activationLink = "http://127.0.0.1:8080/user/confirm?token=" + token;
        String mailMessage = "Dziękujemy za rejestracje, proszę kliknąć w link aby aktywować konto \n";
        simpleMailMessage.setTo(userMail);
        simpleMailMessage.setSubject("Mail confirmation");
        simpleMailMessage.setFrom("<MAIL>");
        simpleMailMessage.setText(mailMessage + activationLink);

        emailService.sendEmail(simpleMailMessage);

    }

    /**
     * Send token to user for reset password
     * @author Szymon Królik
     * @param userMail
     * @param token
     */
    public void sendForgotPasswordToken(String userMail, String token) {
        final SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        String forgotPasswordLink = "http://127.0.0.1:8080/user/forgot-password?token=" + token;
        String mailMessage = "Kliknij w link aby zmienić hasło \n";
        simpleMailMessage.setTo(userMail);
        simpleMailMessage.setSubject("Forgot password");
        simpleMailMessage.setFrom("<MAIL>");
        simpleMailMessage.setText(mailMessage + forgotPasswordLink);

        emailService.sendEmail(simpleMailMessage);
    }

    /**
     * Allows to change user password with correct token
     * @author Szymon Królik
     * @param token
     * @param password
     * @return
     */
    public ServiceReturn changePassword(String token, String password) {
        ServiceReturn ret = new ServiceReturn();
        String newPassword = "";
        if (ServiceFunction.isNull(password)) {
            ret.setStatus(-1);
            ret.setMessage("Proszę podać hasło");
        }

        Optional<ConfirmationToken> confirmationTokenOptional = confirmationTokenRepository.findConfirmationTokenByConfirmationToken(token);
        if (confirmationTokenOptional.isEmpty()) {
            ret.setStatus(-1);
            ret.setMessage("nie znaleziono takiego tokenu");
            return ret;
        }

        User user = confirmationTokenOptional.get().getUser();
        final String encryptedPassword = bCryptPasswordEncoder.encode(password);
        user.setPassword(encryptedPassword);
        userRepository.save(user);
        confirmationTokenService.deleteForgotPasswordToken(confirmationTokenOptional.get().getId());
        ret.setStatus(1);
        ret.setMessage("Hasło pomyślnie zmienione");
        return ret;

    }

    /**
     * Set enabled on true for user after confirm activation link
     * @author Szymon Królik
     * @author Szymon Królik
     * @param confirmationToken
     */
    public void confirmUser(ConfirmationToken confirmationToken) {
        final User user = confirmationToken.getUser();

        user.setEnabled(true);
        userRepository.save(user);

        confirmationTokenService.deleteConfirmationToken(confirmationToken.getId());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return null;
    }


}
