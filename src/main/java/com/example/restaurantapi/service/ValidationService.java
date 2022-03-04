package com.example.restaurantapi.service;


import com.example.restaurantapi.dto.menu.CreateMenuDto;
import com.example.restaurantapi.dto.restaurant.AddRestaurantDto;
import com.example.restaurantapi.dto.user.RegisterUserDto;
import com.example.restaurantapi.model.User;
import io.grpc.Server;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @Author Szymon Królik
 */
@Service
public class ValidationService {

    public List<String> errList = new ArrayList<>();

    public List<String> registerValidation(RegisterUserDto dto) {
        String password = "";
        String matchingPassword = "";
        String email = "";
        String phoneNumber = "";

        if (!ServiceFunction.isNull((Object) dto)) {
            password = dto.getPassword();
            matchingPassword = dto.getMatchingPassword();
            email = dto.getEmail();
            phoneNumber = dto.getPhone_number();

            if (ServiceFunction.isNull(password)) {
                errList.add("Proszę uzupełnić hasło");
            }
            if (ServiceFunction.isNull(matchingPassword)) {
                errList.add("Proszę uzupełnić hasło");
            }
            if (!password.equals(matchingPassword)) {
                errList.add("Hasła się różnią");
            }
            if (ServiceFunction.isNull(email)) {
                errList.add("Proszę uzupełnić email");
            }
            if (!ServiceFunction.validEmail(email)) {
                errList.add("Niepoprawny format email");
            }
            if (ServiceFunction.isNull(phoneNumber)) {
                errList.add("Proszę uzupełnić numer telefonu");
            }
            if (!ServiceFunction.validPhoneNumber(phoneNumber)) {
                errList.add("Proszę podać poprawny numer telefonu");
            }
        } else {
            errList.add("Proszę uzupełnić dane");
        }

        return errList;

    }

    public List<String> restaurantValidation(AddRestaurantDto dto) {
        String email;
        String phoneNumber;

        if (!ServiceFunction.isNull((Object) dto)) {
            email = dto.getEmail();
            phoneNumber = dto.getPhoneNumber();

            if (ServiceFunction.isNull(email)) {
                errList.add("Proszę podać email restauracji");
            }
            if (!ServiceFunction.validEmail(email)) {
                errList.add("Proszę podać prawidłowy adres email restauracji");
            }
            if (ServiceFunction.isNull(phoneNumber)) {
                errList.add("Proszę podać numer telefonu restauracji");
            }
            if (!ServiceFunction.validPhoneNumber(phoneNumber)) {
                errList.add("Proszę podać prawidłowy numer telefonu restarucaji");
            }
        } else {
            errList.add("Proszę podać dane restauracji");
        }

        return errList;
    }

    public List<String> menuValidation(CreateMenuDto dto) {
        String name;
        int creatorId;
        int menuTypeId;
        int restaurantId;

        if (!ServiceFunction.isNull(dto)) {
            if (!ServiceFunction.isNull(dto.getName())) {
                name = dto.getName();
            } else {
                errList.add("Proszę podać nazwę menu");
            }
            if (!ServiceFunction.isNull(dto.getCreatorId())) {
                creatorId = dto.getCreatorId();
            } else {
                errList.add("Proszę podać twórcę menu");
            }
            if (!ServiceFunction.isNull(dto.getMenuTypeId())) {
                menuTypeId = dto.getMenuTypeId();
            } else {
                errList.add("Proszę podać rodzaj menu");
            }
            if (!ServiceFunction.isNull(dto.getRestaurantId())) {
                restaurantId = dto.getRestaurantId();
            } else {
                errList.add("Proszę wybrać restauracje");
            }
        } else {
            errList.add("Proszę uzupełnić dane");
        }

        return errList;
    }


}
