package com.ecommerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderAddress {
    private String fullName;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String phone;
}
