package com.example.restaurantapi.service;

import com.example.restaurantapi.biznesobject.MenuInformation;
import com.example.restaurantapi.biznesobject.OpeningTimes;
import com.example.restaurantapi.biznesobject.RestaurantTypes;
import com.example.restaurantapi.dto.restaurant.AddRestaurantDto;
import com.example.restaurantapi.dto.restaurant.CreatedRestaurantDto;
import com.example.restaurantapi.dto.restaurant.InfoRestaurantDto;
import com.example.restaurantapi.dto.restaurant.UpdateRestaurantDto;
import com.example.restaurantapi.model.*;
import com.example.restaurantapi.repository.*;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author Szymon Królik
 */
@Service
@AllArgsConstructor
public class RestaurantService {
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final OpeningPeriodRepository openingPeriodRepository;
    private final RestaurantTypeRepository restaurantTypeRepository;
    private final ItemRepository itemRepository;
    private final VoivodeshipRepository voivodeshipRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final ValidationService validationService;
    private final EmailService emailService;
    private Map<String, String> validationResult = new HashMap<>();

    /**
     * Function for saving new restaurnt in database
     * @author Szymon Królik
     * @param addRestaurantDto
     * @return CreatedRestaurantDto
     * @throws ParseException
     */

    public ServiceReturn addRestaurantToUserAccount(AddRestaurantDto addRestaurantDto) throws ParseException {
        List<OpeningTimes> openingTimes;
        List<RestaurantType> restaurantTypes = new ArrayList<>();
        List<RestaurantTypes> createdResTypes = new ArrayList<>();
        Restaurant createdRestaurant = new Restaurant();
        ServiceReturn ret = new ServiceReturn();
        validationResult.clear();


        Optional<User> optionalUser = userRepository.findById(addRestaurantDto.getOwner());
        if (!optionalUser.isPresent()) {
            ret.setStatus(-1);
            ret.setMessage("Nie znaleziono takiego użytkownika");

            return ret;
        }

        Optional<Restaurant> optionalRestaurant = restaurantRepository.findByEmail(addRestaurantDto.getEmail());
        if (optionalRestaurant.isPresent()) {
            ret.setStatus(-1);
            validationResult.put("email", "Restauracja o podanym email już istnieje");
            ret.setErrorList(validationResult);
            ret.setValue(addRestaurantDto);
            return ret;
        }

        Optional<Restaurant> optionalRestaurantPhoneNumber = restaurantRepository.findByPhoneNumber(addRestaurantDto.getPhoneNumber());
        if (optionalRestaurantPhoneNumber.isPresent()) {
            ret.setStatus(-1);
            validationResult.put("email", "Restauracja o podanym numerze telefonu już istnieje");
            ret.setErrorList(validationResult);
            ret.setValue(addRestaurantDto);
            return ret;
        }
        validationResult = validationService.restaurantValidation(addRestaurantDto);
        if (validationResult.size() > 0) {
            ret.setStatus(-1);
            ret.setErrorList(validationResult);
            ret.setValue(addRestaurantDto);

            return ret;
        }

        //Prepare restaurant types
        for (int i = 0; i < addRestaurantDto.getRestaurantTypesList().size(); i++) {

            Optional<RestaurantType> restaurantTypeOptional = restaurantTypeRepository.findById(addRestaurantDto.getRestaurantTypesList().get(i).getId());
            if (!restaurantTypeOptional.isPresent()) {
                ret.setStatus(0);
                ret.setMessage("Nie znaleziono takiego typu restauracji");
                return  ret;
            }
            restaurantTypes.add(restaurantTypeOptional.get());
        }

        //Prepare restaurant dto for save
        Restaurant restaurant = Restaurant.of(addRestaurantDto);
        restaurant.setUser_id(optionalUser.get());
        restaurant.setRestaurantTypes(restaurantTypes);

        try {
            createdRestaurant = restaurantRepository.save(restaurant);
        } catch (Exception ex) {
            ret.setMessage("Err. Tworzenie restauracji: " + ex.getMessage());
            ret.setStatus(-1);
        }


        openingTimes = addRestaurantDto.getOpeningTimes();

        //Add opening hours to restaurant

        for (int i = 0; i < openingTimes.size(); i++) {
            OpeningPeriod openingPeriod = OpeningPeriod.of(openingTimes.get(i));
            openingPeriod.setRestaurant(createdRestaurant);

            openingPeriodRepository.save(openingPeriod);

        }


        CreatedRestaurantDto restaurantDto = CreatedRestaurantDto.of(createdRestaurant);
        restaurantDto.setOpeningTimes(openingTimes);

        //Get restaurant types to return
        List<RestaurantType> getCreatedRestaurantType = createdRestaurant.getRestaurantTypes();
        createdResTypes = getCreatedRestaurantType.stream().map(x -> RestaurantTypes.of(x)).collect(Collectors.toList());

        //Set restaurant types
        restaurantDto.setRestaurantTypes(createdResTypes);

        ret.setValue(restaurantDto);
        ret.setStatus(1);


        return ret;
    }

    /**
     * Find restaurant by specific id
     * @author Szymon Królik
     * @param id
     * @return InfoRestaurantDto
     */
    public ServiceReturn getRestaurant(int id) {
        ServiceReturn ret = new ServiceReturn();
        validationResult.clear();
        List<RestaurantTypes> restaurantTypesList = new ArrayList<>();
        List<OpeningTimes> openingTimesList = new ArrayList<>();
        List<MenuInformation> menuInformationList =  new ArrayList<>();

        Optional<Restaurant> optionalRestaurant = restaurantRepository.findById(id);
        if (!optionalRestaurant.isPresent()) {
            ret.setStatus(0);
            ret.setMessage("Nie znaleziono takiej restauracji");
            return ret;
        }

        InfoRestaurantDto infoRestaurantDto = InfoRestaurantDto.of(optionalRestaurant.get());
        //get menu list
        List<Menu> menus = optionalRestaurant.get().getMenus();
        menuInformationList = menus.stream().map(x -> MenuInformation.of(x)).collect(Collectors.toList());
        infoRestaurantDto.setMenus(menuInformationList);

        //Get opening hours
        List<OpeningPeriod> openingPeriodList = openingPeriodRepository.findByRestaurant(optionalRestaurant.get());
        if (!openingPeriodList.isEmpty() ){
            openingTimesList = openingPeriodList.stream().map(x -> OpeningTimes.of(x)).collect(Collectors.toList());
            infoRestaurantDto.setOpeningPeriod(openingTimesList);
        }

        //Get restaurant types
        List<RestaurantType> getRestaurantTypeList = optionalRestaurant.get().getRestaurantTypes();
        restaurantTypesList = getRestaurantTypeList.stream().map(x -> RestaurantTypes.of(x)).collect(Collectors.toList());



        infoRestaurantDto.setRestaurantTypes(restaurantTypesList);

        ret.setStatus(1);
        ret.setValue(infoRestaurantDto);
        return ret;

    }


    /**
     * Find restaurants by name
     * @author Szymon Królik
     * @param name
     * @return List<InfoRestaurantDto>
     */
    public ServiceReturn getRestaurantByName(String name) {
        ServiceReturn ret = new ServiceReturn();
        List<InfoRestaurantDto> infoRestaurantDtoList = new ArrayList<>();


        List<Restaurant> optionalRestaurant = restaurantRepository.findByNameContaining(name);
        if (optionalRestaurant.isEmpty()) {
            ret.setStatus(0);
            ret.setMessage("Nie znaleziono takiej restauracji");
            return ret;
        }
       for (Restaurant res : optionalRestaurant) {
           InfoRestaurantDto dto = InfoRestaurantDto.of(res);
           List<MenuInformation> menuInformationList = new ArrayList<>();
           List<OpeningTimes> openingTimesList = new ArrayList<>();
           List<RestaurantTypes> restaurantTypesList = new ArrayList<>();
           //Get menu list
           List<Menu> menus = res.getMenus();

           menuInformationList = menus.stream().map(x -> MenuInformation.of(x)).collect(Collectors.toList());
           dto.setMenus(menuInformationList);

           List<OpeningPeriod> openingPeriodList = openingPeriodRepository.findByRestaurant(res);
           if (openingPeriodList.isEmpty()) {
               openingTimesList = openingPeriodList.stream().map(x -> OpeningTimes.of(x)).collect(Collectors.toList());
               dto.setOpeningPeriod(openingTimesList);
           }

           List<RestaurantType> getRestaurantTypeList = res.getRestaurantTypes();

           restaurantTypesList = getRestaurantTypeList.stream().map(x -> RestaurantTypes.of(x)).collect(Collectors.toList());
           dto.setRestaurantTypes(restaurantTypesList);

           infoRestaurantDtoList.add(dto);
       }

        ret.setStatus(1);
        ret.setValue(infoRestaurantDtoList);
        return ret;

    }

    /**
     * Find restaurant by city
     * @author Szymon Królik
     * @param city
     * @return List<InfoRestaurantDto>
     */
    public ServiceReturn getRestaurantsByCity(String city) {
        ServiceReturn ret = new ServiceReturn();
        List<InfoRestaurantDto> infoRestaurantDtoList = new ArrayList<>();

        List<Restaurant> optionalRestaurant = restaurantRepository.findByCity(city);
        if (optionalRestaurant.isEmpty()) {
            ret.setStatus(0);
            ret.setMessage("Nie znaleziono restauracji w podanym mieście");
            return ret;
        }
        for (Restaurant res : optionalRestaurant) {
            InfoRestaurantDto dto = InfoRestaurantDto.of(res);
            List<MenuInformation> menuInformationList = new ArrayList<>();
            List<OpeningTimes> openingTimesList = new ArrayList<>();
            List<RestaurantTypes> restaurantTypesList = new ArrayList<>();
            //Get menu list
            List<Menu> menus = res.getMenus();
            menuInformationList = menus.stream().map(x -> MenuInformation.of(x)).collect(Collectors.toList());
            dto.setMenus(menuInformationList);

            List<OpeningPeriod> openingPeriodList = openingPeriodRepository.findByRestaurant(res);
            if (openingPeriodList.isEmpty()) {
                openingTimesList = openingPeriodList.stream().map(x -> OpeningTimes.of(x)).collect(Collectors.toList());
                dto.setOpeningPeriod(openingTimesList);
            }

            List<RestaurantType> getRestaurantTypeList = res.getRestaurantTypes();
            restaurantTypesList = getRestaurantTypeList.stream().map(x -> RestaurantTypes.of(x)).collect(Collectors.toList());
            dto.setRestaurantTypes(restaurantTypesList);

            infoRestaurantDtoList.add(dto);
        }

        ret.setStatus(1);
        ret.setValue(infoRestaurantDtoList);
        return ret;

    }

    /**
     * Get all restaurant types from database
     * @author Szymon Królik
     * @return List<RestaurantTypes>
     */
    public ServiceReturn getRestaurantTypes() {
        ServiceReturn ret = new ServiceReturn();
        List<RestaurantType> restaurantTypesList = restaurantTypeRepository.findAll();
        List<RestaurantTypes> restaurantTypes = new ArrayList<>();

        restaurantTypes = restaurantTypesList.stream().map(x -> RestaurantTypes.of(x)).collect(Collectors.toList());
        ret.setValue(restaurantTypes);
        return ret;
    }

    public ServiceReturn getResturantsByOwner(int ownerId) {
        ServiceReturn ret = new ServiceReturn();
        Optional<User> optionalUser = userRepository.findById(ownerId);
        List<InfoRestaurantDto> restaurantList = new ArrayList<>();
        if (!optionalUser.isPresent())  {
            ret.setMessage("Nie znaleziono takiego użytkownika");
            ret.setStatus(0);
            return ret;
        } else {
            List<Restaurant> restaurants = optionalUser.get().getRestaurants();
            restaurantList = restaurants.stream().map(x -> InfoRestaurantDto.of(x)).collect(Collectors.toList());
        }

        ret.setValue(restaurantList);
        return ret;
    }

    public ServiceReturn deleteRestaurant(int restaurantId) {
        ServiceReturn ret = new ServiceReturn();

        Optional<Restaurant> optionalRestaurant = restaurantRepository.findById(restaurantId);
        if (!optionalRestaurant.isPresent()) {
            ret.setMessage("Nie znaleziono takiej restauracji");
            ret.setStatus(0);
            return ret;
        }

        try {
            restaurantRepository.deleteById(restaurantId);
            ret.setMessage("Pomyślnie usunięto restauracje");
            ret.setStatus(1);
        } catch (Exception ex) {
            ret.setStatus(-1);
            ret.setMessage("Err. delete restaurant " + ex.getMessage());
        }

        return ret;
    }

    public ServiceReturn getVoivodeships() {
        ServiceReturn ret = new ServiceReturn();
        List<Voivodeship> voivodeships = voivodeshipRepository.findAll();
        ret.setValue(voivodeships);
        ret.setStatus(1);
        return ret;
    }

    public ServiceReturn updateRestaurant(UpdateRestaurantDto dto) throws ParseException {
        ServiceReturn ret = new ServiceReturn();
        validationResult.clear();

        Optional<User> optionalUser = userRepository.findById(dto.getOwnerId());
        if (!optionalUser.isPresent()) {
            validationResult.put("ownerId", "Nie znaleziono takiego użytkownika");
        }

        Optional<Restaurant> optionalRestaurant = restaurantRepository.findById(dto.getRestaurantId());
        if (!optionalRestaurant.isPresent())
            validationResult.put("restaurantId","Nie znaleziono takiej restauracji");

        if (!validationResult.isEmpty()) {
            ret.setErrorList(validationResult);
            ret.setStatus(0);
            ret.setValue(dto);

            return ret;
        }

        validationResult.clear();
        List<RestaurantType> restaurantTypesList = new ArrayList<>();
        for (int i = 0; i < restaurantTypesList.size(); i++) {
            if (!restaurantTypeRepository.findById(restaurantTypesList.get(i).getId()).isPresent()) {
                ret.setStatus(0);
                validationResult.put("restaurantTypes","Nie znaleziono takiego typu restauracji");
                ret.setErrorList(validationResult);
                return ret;

            }
            restaurantTypesList.add(restaurantTypeRepository.findById(restaurantTypesList.get(i).getId()).get());
        }


        Restaurant updatedRestaurant = new Restaurant();
        try {
             updatedRestaurant = restaurantRepository.save(Restaurant.updateRestaurant(optionalRestaurant.get(), dto));
        } catch (Exception ex) {
            ret.setMessage(ex.getMessage());
            return ret;
        }
        List<OpeningTimes> openingTimes = new ArrayList<>();
        openingTimes = dto.getOpeningPeriod();

        //Add opening hours to restaurant

        for (int i = 0; i < openingTimes.size(); i++) {
            OpeningPeriod openingPeriod = OpeningPeriod.of(openingTimes.get(i));
            openingPeriod.setRestaurant(updatedRestaurant);

            openingPeriodRepository.save(openingPeriod);

        }


        CreatedRestaurantDto restaurantDto = CreatedRestaurantDto.of(updatedRestaurant);
        restaurantDto.setOpeningTimes(openingTimes);

        //Get restaurant types to return
        List<RestaurantTypes> createdResTypes = new ArrayList<>();
        List<RestaurantType> getCreatedRestaurantType = updatedRestaurant.getRestaurantTypes();
        createdResTypes = getCreatedRestaurantType.stream().map(x -> RestaurantTypes.of(x)).collect(Collectors.toList());

        //Set restaurant types
        restaurantDto.setRestaurantTypes(createdResTypes);

        ret.setValue(restaurantDto);
        ret.setStatus(1);
        return ret;
    }



}
