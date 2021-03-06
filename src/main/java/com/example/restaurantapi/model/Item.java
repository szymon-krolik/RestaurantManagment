package com.example.restaurantapi.model;

import com.example.restaurantapi.dto.item.CreateItemDto;
import com.example.restaurantapi.dto.item.UpdateItemDto;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "item")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private String item_describe;

    @CreationTimestamp
    @Column(updatable = false)
    private Date createdAt;

    @UpdateTimestamp
    private Date updatedAt;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    private Double quantity;

    //Menu FK
    @ManyToMany(mappedBy = "menuItems")
    private List<Menu> menus;
    //Type FK
    @ManyToOne
    private ItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToMany(mappedBy = "itemList")
    private List<Order> orderList;

    public static Item of(CreateItemDto dto) {
        Item item = new Item();

        item.setTitle(dto.getTitle());
        item.setItem_describe(dto.getDesc());
        item.setQuantity(dto.getQuantity());
        item.setPrice(dto.getPrice());
        item.setUnit(dto.getUnit());

        return item;

    }

    public static Item updateItem(Item item, UpdateItemDto dto) {


        item.setTitle(dto.getTitle());
        item.setItem_describe(dto.getDesc());
        item.setQuantity(dto.getQuantity());
        item.setPrice(dto.getPrice());
        item.setUnit(dto.getUnit());
        item.setRestaurant(dto.getRestaurant());

        return item;
    }

}
